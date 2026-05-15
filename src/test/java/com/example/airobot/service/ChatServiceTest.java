package com.example.airobot.service;

import com.example.airobot.model.ChatRequest;
import com.example.airobot.model.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Test
    void chat_withValidRequest_shouldReturnResponse() {
        ChatRequest request = ChatRequest.builder()
                .message("你好")
                .userId("test-user")
                .build();

        ChatResponse response = chatService.chat(request);

        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertNotNull(response.getReply());
        assertFalse(response.getReply().isEmpty());
    }
}