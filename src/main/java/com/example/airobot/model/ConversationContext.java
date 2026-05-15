package com.example.airobot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话上下文（存储在 Redis 中）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SYSTEM_PROMPT = """
            你是一个专业的AI智能客服助手。请遵循以下规则：
            1. 用友好、专业的态度回复客户
            2. 回答简洁明了，不超过200字
            3. 如果无法回答客户问题，建议转人工客服
            4. 不要编造虚假信息
            5. 适当使用emoji让对话更生动（但不要过度）
            """;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 消息历史
     */
    @Builder.Default
    private List<MiniMaxRequest.Message> messages = new ArrayList<>();

    /**
     * 当前对话轮次
     */
    @Builder.Default
    private int turn = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        this.messages.add(MiniMaxRequest.Message.builder()
                .role("user")
                .content(content)
                .build());
    }

    /**
     * 添加 AI 消息
     */
    public void addAssistantMessage(String content) {
        this.messages.add(MiniMaxRequest.Message.builder()
                .role("assistant")
                .content(content)
                .build());
    }

    /**
     * 增加轮次
     */
    public void incrementTurn() {
        this.turn++;
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveAt = LocalDateTime.now();
    }
}
