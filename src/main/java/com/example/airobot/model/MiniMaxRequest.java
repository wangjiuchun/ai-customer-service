package com.example.airobot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MiniMax API 请求模型 (v2 版本)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniMaxRequest {
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 对话消息列表
     */
    private List<Message> messages;
    
    /**
     * 最大 token 数
     */
    private int max_tokens;
    
    /**
     * 温度参数（0-1，越高越有创造性）
     */
    private double temperature;
    
    /**
     * 核采样参数
     */
    private Double top_p;
    
    /**
     * 是否流式输出
     */
    private Boolean stream;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色：user/assistant/system
         */
        private String role;
        
        /**
         * 消息内容
         */
        private String content;
    }
}
