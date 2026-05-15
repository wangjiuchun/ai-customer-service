package com.example.airobot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * AI 回复
     */
    private String reply;

    /**
     * 回复时间
     */
    private LocalDateTime timestamp;

    /**
     * 当前对话轮次
     */
    private int turn;

    /**
     * 快捷问题推荐（可选）
     */
    private List<String> suggestions;

    /**
     * 是否为转人工建议
     */
    private boolean transferToHuman;
}
