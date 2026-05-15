package com.example.airobot.service;

import com.example.airobot.entity.ChatSession;
import com.example.airobot.model.ConversationContext;
import com.example.airobot.repository.ChatSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 会话管理服务
 */
@Slf4j
@Service
public class SessionManagementService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatSessionRepository sessionRepository;

    private static final String SESSION_KEY_PREFIX = "ai-chat:session:";

    @Value("${chat.session-ttl:30}")
    private int sessionTtl;

    public SessionManagementService(StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper,
                                    ChatSessionRepository sessionRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sessionRepository = sessionRepository;
    }

    /**
     * 获取或创建会话
     */
    public ConversationContext getOrCreateSession(String sessionId, String userId) {
        // 如果提供了 sessionId，尝试获取现有会话
        if (sessionId != null && !sessionId.isEmpty()) {
            ConversationContext existing = getSession(sessionId);
            if (existing != null) {
                return existing;
            }
        }

        // 创建新会话
        String newSessionId = UUID.randomUUID().toString();
        ConversationContext newContext = ConversationContext.builder()
                .sessionId(newSessionId)
                .userId(userId)
                .messages(new ArrayList<>())
                .turn(0)
                .createdAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        // 初始化系统提示
        newContext.addUserMessage(ConversationContext.SYSTEM_PROMPT);

        // 保存会话到数据库
        saveSessionToDb(newContext);

        // 保存会话到 Redis
        saveSession(newContext);

        return newContext;
    }

    /**
     * 获取会话（从 Redis）
     */
    public ConversationContext getSession(String sessionId) {
        try {
            String key = SESSION_KEY_PREFIX + sessionId;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, ConversationContext.class);
            }
        } catch (JsonProcessingException e) {
            log.error("解析会话数据失败: {}", sessionId, e);
        }
        return null;
    }

    /**
     * 保存会话（到 Redis）
     */
    public void saveSession(ConversationContext context) {
        try {
            String key = SESSION_KEY_PREFIX + context.getSessionId();
            String json = objectMapper.writeValueAsString(context);
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(sessionTtl));
        } catch (JsonProcessingException e) {
            log.error("保存会话数据失败: {}", context.getSessionId(), e);
        }
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        if (sessionId != null) {
            // 删除 Redis 中的会话
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
            // 更新数据库中的会话状态
            sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                session.setStatus(0);
                session.setEndedAt(LocalDateTime.now());
                sessionRepository.save(session);
            });
        }
    }

    /**
     * 更新会话轮次
     */
    public void updateSessionTurn(String sessionId, int turn) {
        try {
            sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                session.setTurnCount(turn);
                sessionRepository.save(session);
            });
        } catch (Exception e) {
            log.error("更新会话轮次失败: {}", sessionId, e);
        }
    }

    /**
     * 保存会话到数据库
     */
    private void saveSessionToDb(ConversationContext context) {
        try {
            ChatSession session = ChatSession.builder()
                    .sessionId(context.getSessionId())
                    .userId(context.getUserId())
                    .status(1)
                    .turnCount(context.getTurn())
                    .build();
            sessionRepository.save(session);
        } catch (Exception e) {
            log.error("保存会话到数据库失败: {}", context.getSessionId(), e);
        }
    }
}
