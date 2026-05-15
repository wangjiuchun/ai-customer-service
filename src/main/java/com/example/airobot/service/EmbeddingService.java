package com.example.airobot.service;

import com.example.airobot.config.MiniMaxConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * MiniMax Embedding 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final MiniMaxConfig miniMaxConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * 将文本转为向量
     * @param text 输入文本
     * @return 浮点数向量
     */
    public List<Double> embed(String text) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(miniMaxConfig.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + miniMaxConfig.getApiKey())
                    .build();

            Map<String, Object> requestBody = Map.of(
                    "model", miniMaxConfig.getEmbeddingModel(),
                    "texts", List.of(text)
            );

            String response = webClient.post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Embedding 响应: {}", response);

            // 解析响应中的向量
            return parseEmbeddingResponse(response);

        } catch (Exception e) {
            log.error("Embedding 调用失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析 MiniMax embedding 响应
     * MiniMax API 返回格式: {"data":[{"embedding":[0.1,0.2,...]}]}
     */
    private List<Double> parseEmbeddingResponse(String response) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
            com.fasterxml.jackson.databind.JsonNode data = root.path("data");

            if (data.isArray() && !data.isEmpty()) {
                com.fasterxml.jackson.databind.JsonNode embedding = data.get(0).path("embedding");
                if (embedding.isArray()) {
                    List<Double> result = new java.util.ArrayList<>();
                    for (com.fasterxml.jackson.databind.JsonNode node : embedding) {
                        result.add(node.asDouble());
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("解析 embedding 响应失败: {}", e.getMessage());
        }
        return List.of();
    }
}
