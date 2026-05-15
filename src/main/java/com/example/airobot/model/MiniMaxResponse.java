package com.example.airobot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MiniMax API 响应模型 (v2 版本)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniMaxResponse {
    
    /**
     * 响应 ID
     */
    private String id;
    
    /**
     * 对话 ID
     */
    private String conversation_id;
    
    /**
     * 创建时间戳
     */
    private long created;
    
    /**
     * 模型
     */
    private String model;
    
    /**
     * 对象类型
     */
    private String object;
    
    /**
     * 使用量统计
     */
    private Usage usage;
    
    /**
     * 选择列表
     */
    private List<Choice> choices;
    
    /**
     * 基础响应状态
     */
    private BaseResp base_resp;
    
    /**
     * 输入是否敏感
     */
    private Boolean input_sensitive;
    
    /**
     * 输出是否敏感
     */
    private Boolean output_sensitive;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /**
         * 总 token 数
         */
        private int total_tokens;
        
        /**
         * 提示词 token 数
         */
        private int prompt_tokens;
        
        /**
         * 生成 token 数
         */
        private int completion_tokens;
        
        /**
         * 总字符数
         */
        private int total_characters;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        /**
         * 索引
         */
        private int index;

        /**
         * 消息 (v2 版本是单个对象，不是数组)
         */
        private Message message;

        /**
         * 完成原因
         */
        private String finish_reason;

        /**
         * 提取回复内容
         * 实际格式: choices[0].message.content
         */
        public String extractContent() {
            if (message != null && message.getContent() != null && !message.getContent().isEmpty()) {
                return message.getContent();
            }
            return null;
        }
    }
    
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
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseResp {
        /**
         * 状态码，0 表示成功
         */
        private int status_code;
        
        /**
         * 状态消息
         */
        private String status_msg;
    }
}

