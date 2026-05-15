package com.example.airobot.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Value;

/**
 * MiniMax API 配置
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "minimax")
public class MiniMaxConfig {

    /**
     * API Key
     */
    @NotBlank
    @Value("${minimax.api-key}")
    private String apiKey;

    /**
     * API 地址
     */
    private String baseUrl;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 上下文保留消息数
     */
    private int contextSize;

    /**
     * 温度参数（创造性）
     */
    private double temperature;

    /**
     * 最大 token 数
     */
    private int maxTokens;

    /**
     * Embedding 模型名称
     */
    private String embeddingModel = "embo-01";
}