package com.example.airobot.repository;

import com.example.airobot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息记录 Repository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 根据会话ID查询所有消息
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 根据会话ID统计消息数
     */
    long countBySessionId(String sessionId);
}
