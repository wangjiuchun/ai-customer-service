package com.example.airobot.service;

import com.example.airobot.entity.ChatMessage;
import com.example.airobot.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息持久化服务
 */
@Slf4j
@Service
public class MessagePersistenceService {

    private final ChatMessageRepository messageRepository;

    public MessagePersistenceService(ChatMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 保存消息到数据库
     */
    public void saveMessage(String sessionId, String role, String content, int turn) {
        try {
            ChatMessage message = ChatMessage.builder()
                    .sessionId(sessionId)
                    .role(role)
                    .content(content)
                    .turn(turn)
                    .build();
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("保存消息到数据库失败: sessionId={}, role={}", sessionId, role, e);
            // Make failure non-fatal to avoid transaction rollback
        }
    }
}
