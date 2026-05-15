package com.example.airobot.service;

import com.example.airobot.entity.Intention;
import com.example.airobot.model.MiniMaxRequest;
import com.example.airobot.repository.IntentionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRecognitionService {

    private final MiniMaxService miniMaxService;
    private final IntentionRepository intentionRepository;
    private final ObjectMapper objectMapper;

    private static final double CONFIDENCE_THRESHOLD = 0.7;

    public enum IntentResult {
        RECOGNIZED,    // 意图明确识别
        LOW_CONFIDENCE, // 置信度低于阈值，需要降级到 AI 意图识别
        UNRECOGNIZED   // 无法识别，回退到通用处理
    }

    public static class Recognition {
        public final IntentResult result;
        public final Intention intention;
        public final double confidence;

        public Recognition(IntentResult result, Intention intention, double confidence) {
            this.result = result;
            this.intention = intention;
            this.confidence = confidence;
        }
    }

    public Recognition recognize(String userMessage) {
        List<Intention> activeIntentions = intentionRepository.findByStatusOrderByPriorityDesc(1);

        if (activeIntentions.isEmpty()) {
            return new Recognition(IntentResult.UNRECOGNIZED, null, 0.0);
        }

        // 精确匹配关键词
        for (Intention intention : activeIntentions) {
            if (matchKeywords(userMessage, intention.getKeywords())) {
                return new Recognition(IntentResult.RECOGNIZED, intention, 1.0);
            }
        }

        // AI 意图识别
        return recognizeWithAI(userMessage, activeIntentions);
    }

    private boolean matchKeywords(String message, String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return false;
        }
        String[] keywordList = keywords.split("[,，]");
        for (String keyword : keywordList) {
            if (message.contains(keyword.trim())) {
                return true;
            }
        }
        return false;
    }

    private Recognition recognizeWithAI(String userMessage, List<Intention> intentions) {
        String prompt = buildIntentPrompt(userMessage, intentions);

        List<MiniMaxRequest.Message> messages = List.of(
                MiniMaxRequest.Message.builder()
                        .role("user")
                        .content(prompt)
                        .build()
        );

        try {
            String reply = miniMaxService.chat(messages);
            return parseAIResponse(reply, intentions);
        } catch (Exception e) {
            log.error("AI 意图识别失败: {}", e.getMessage());
            return new Recognition(IntentResult.UNRECOGNIZED, null, 0.0);
        }
    }

    private String buildIntentPrompt(String userMessage, List<Intention> intentions) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户消息: ").append(userMessage).append("\n\n");
        sb.append("请从以下意图类别中选择最匹配的一个，返回JSON格式：\n");
        sb.append("{\"intent\": \"意图名称\", \"confidence\": 0.0-1.0}\n\n");
        sb.append("可用意图：\n");

        for (Intention intention : intentions) {
            sb.append("- ").append(intention.getName())
                    .append(": ").append(intention.getDescription()).append("\n");
        }

        sb.append("\n只返回JSON，不要其他内容。");
        return sb.toString();
    }

    private Recognition parseAIResponse(String reply, List<Intention> intentions) {
        try {
            String jsonContent = reply;
            if (reply.contains("```")) {
                int start = reply.indexOf("```");
                int end = reply.lastIndexOf("```");
                if (start != end) {
                    jsonContent = reply.substring(start + 3, end).trim();
                }
            }

            var node = objectMapper.readTree(jsonContent);
            String intentName = node.path("intent").asText();
            double confidence = node.path("confidence").asDouble(0.0);

            Intention matched = intentions.stream()
                    .filter(i -> i.getName().equals(intentName))
                    .findFirst()
                    .orElse(null);

            if (matched == null) {
                return new Recognition(IntentResult.UNRECOGNIZED, null, confidence);
            }

            if (confidence >= CONFIDENCE_THRESHOLD) {
                return new Recognition(IntentResult.RECOGNIZED, matched, confidence);
            } else {
                return new Recognition(IntentResult.LOW_CONFIDENCE, matched, confidence);
            }

        } catch (Exception e) {
            log.error("解析AI意图识别响应失败: {}", e.getMessage());
            return new Recognition(IntentResult.UNRECOGNIZED, null, 0.0);
        }
    }
}
