package com.example.airobot.controller;

import com.example.airobot.model.ChatRequest;
import com.example.airobot.model.ChatResponse;
import com.example.airobot.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送消息
     *
     * POST /api/chat/send
     * {
     *   "sessionId": "可选，会话ID",
     *   "message": "用户消息",
     *   "userId": "可选，用户ID"
     * }
     */
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到对话请求: sessionId={}, userId={}, message={}",
                request.getSessionId(), request.getUserId(), request.getMessage());

        try {
            ChatResponse response = chatService.chat(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("处理对话请求失败", e);
            return ApiResponse.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取欢迎消息
     *
     * GET /api/chat/welcome
     * GET /api/chat/welcome?sessionId=xxx&userId=xxx
     */
    @GetMapping("/welcome")
    public ApiResponse<ChatResponse> getWelcome(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId) {
        log.info("获取欢迎消息: sessionId={}, userId={}", sessionId, userId);

        try {
            ChatResponse response = chatService.getWelcomeMessage(sessionId, userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取欢迎消息失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 清除会话
     *
     * DELETE /api/chat/session/{sessionId}
     */
    @DeleteMapping("/session/{sessionId}")
    public ApiResponse<String> clearSession(@PathVariable String sessionId) {
        log.info("清除会话: sessionId={}", sessionId);

        try {
            chatService.clearSession(sessionId);
            return ApiResponse.success("会话已清除");
        } catch (Exception e) {
            log.error("清除会话失败", e);
            return ApiResponse.error("清除失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     *
     * GET /api/chat/health
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "ok",
                "service", "AI Customer Service",
                "version", "1.0.0"
        ));
    }
}
