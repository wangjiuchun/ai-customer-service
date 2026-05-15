package com.example.airobot.service;

import com.example.airobot.model.ChatRequest;
import com.example.airobot.model.ChatResponse;
import com.example.airobot.model.ConversationContext;
import com.example.airobot.repository.QuickQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @MockBean
    private MiniMaxService miniMaxService;

    @MockBean
    private SessionManagementService sessionManagementService;

    @MockBean
    private MessagePersistenceService messagePersistenceService;

    @MockBean
    private RAGService ragService;

    @MockBean
    private QuickQuestionRepository quickQuestionRepository;

    @MockBean
    private IntentRecognitionService intentRecognitionService;

    @BeforeEach
    void setUp() {
        // Mock RAG service to return empty list
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of());

        // Mock quick questions
        when(quickQuestionRepository.findByStatusOrderBySortOrderAsc(1))
            .thenReturn(List.of());
    }

    @Test
    void chat_withValidRequest_shouldReturnResponse() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("你好")
                .userId("test-user")
                .build();

        ConversationContext mockContext = ConversationContext.builder()
                .sessionId("test-session")
                .userId("test-user")
                .messages(new ArrayList<>())
                .turn(0)
                .build();

        when(sessionManagementService.getOrCreateSession(any(), any()))
            .thenReturn(mockContext);
        when(miniMaxService.chat(any())).thenReturn("你好，有什么可以帮你的？");

        // When
        ChatResponse response = chatService.chat(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertEquals("你好，有什么可以帮你的？", response.getReply());
    }
}
