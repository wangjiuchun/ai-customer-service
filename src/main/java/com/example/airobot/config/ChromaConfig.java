package com.example.airobot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Chroma 向量数据库配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chroma")
public class ChromaConfig {

    /**
     * Chroma 服务器地址
     */
    private String host = "localhost";

    /**
     * Chroma 服务器端口
     */
    private int port = 8000;

    /**
     * Collection 名称
     */
    private String collectionName = "faq-knowledge";

    /**
     * 获取完整 URL
     */
    public String getUrl() {
        return "http://" + host + ":" + port;
    }
}