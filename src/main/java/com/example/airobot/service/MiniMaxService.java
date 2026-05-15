package com.example.airobot.service;

import com.example.airobot.config.MiniMaxConfig;
import com.example.airobot.model.MiniMaxRequest;
import com.example.airobot.model.MiniMaxResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * MiniMax API 服务
 */
@Slf4j
@Service
public class MiniMaxService {
    
    private final MiniMaxConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public MiniMaxService(MiniMaxConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .build();
    }
    
    /**
     * 发送对话请求
     *
     * @param messages 消息列表
     * @return AI 回复内容
     */
    public String chat(List<MiniMaxRequest.Message> messages) {
        // 构建请求
        MiniMaxRequest request = MiniMaxRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .temperature(config.getTemperature())
                .max_tokens(config.getMaxTokens())
                .build();
        
        log.debug("发送请求到 MiniMax: {}", request);
        
        try {
            // 发送请求 - 使用 v2 接口
            String response = webClient.post()
                    .uri("/v1/text/chatcompletion_v2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            
            log.debug("收到 MiniMax 响应: {}", response);
            
            // 解析响应
            MiniMaxResponse chatResponse = objectMapper.readValue(response, MiniMaxResponse.class);
            
            // 检查响应状态
            if (chatResponse.getBase_resp() != null && chatResponse.getBase_resp().getStatus_code() != 0) {
                log.error("MiniMax API 返回错误: {}", chatResponse.getBase_resp().getStatus_msg());
                return "抱歉，服务暂时不可用：" + chatResponse.getBase_resp().getStatus_msg();
            }
            
            if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                // 解析 choices[0].messages[0].content
                return chatResponse.getChoices().get(0).extractContent();
            }
            
            return "抱歉，我现在无法回答您的问题，请稍后再试。";
            
        } catch (Exception e) {
            log.error("调用 MiniMax API 失败", e);
            return "抱歉，服务暂时不可用，请稍后再试。错误信息: " + e.getMessage();
        }
    }
    
    /**
     * 异步发送对话请求
     */
    public Mono<String> chatAsync(List<MiniMaxRequest.Message> messages) {
        MiniMaxRequest request = MiniMaxRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .temperature(config.getTemperature())
                .max_tokens(config.getMaxTokens())
                .build();
        
        return webClient.post()
                .uri("/v1/text/chatcompletion_v2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .map(response -> {
                    try {
                        MiniMaxResponse chatResponse = objectMapper.readValue(response, MiniMaxResponse.class);
                        if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                            return chatResponse.getChoices().get(0).extractContent();
                        }
                        return "抱歉，我现在无法回答您的问题。";
                    } catch (Exception e) {
                        log.error("解析响应失败", e);
                        return "抱歉，响应解析失败。";
                    }
                })
                .onErrorReturn("抱歉，服务暂时不可用，请稍后再试。");
    }
}

