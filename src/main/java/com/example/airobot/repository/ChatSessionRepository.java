package com.example.airobot.repository;

import com.example.airobot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会话 Repository
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /**
     * 根据会话ID查询
     */
    Optional<ChatSession> findBySessionId(String sessionId);

    /**
     * 根据用户ID查询最近会话
     */
    Optional<ChatSession> findTopByUserIdOrderByCreatedAtDesc(String userId);
}
