package com.example.airobot.service;

import com.example.airobot.config.MiniMaxConfig;
import com.example.airobot.entity.FaqKnowledge;
import com.example.airobot.entity.QuickQuestion;
import com.example.airobot.model.ChatRequest;
import com.example.airobot.model.ChatResponse;
import com.example.airobot.model.ConversationContext;
import com.example.airobot.model.MiniMaxRequest;
import com.example.airobot.repository.QuickQuestionRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话服务
 */
@Slf4j
@Service
public class ChatService {

    private final MiniMaxService miniMaxService;
    private final MiniMaxConfig miniMaxConfig;
    private final SessionManagementService sessionManagementService;
    private final MessagePersistenceService messagePersistenceService;
    private final QuickQuestionRepository quickQuestionRepository;
    private final RAGService ragService;
    private final IntentRecognitionService intentRecognitionService;

    @Value("${chat.max-turns:20}")
    private int maxTurns;

    @Value("${chat.welcome-message:您好！我是AI智能客服，很高兴为您服务。请问有什么可以帮您的？}")
    private String welcomeMessage;

    public ChatService(MiniMaxService miniMaxService,
                       MiniMaxConfig miniMaxConfig,
                       SessionManagementService sessionManagementService,
                       MessagePersistenceService messagePersistenceService,
                       QuickQuestionRepository quickQuestionRepository,
                       RAGService ragService,
                       IntentRecognitionService intentRecognitionService) {
        this.miniMaxService = miniMaxService;
        this.miniMaxConfig = miniMaxConfig;
        this.sessionManagementService = sessionManagementService;
        this.messagePersistenceService = messagePersistenceService;
        this.quickQuestionRepository = quickQuestionRepository;
        this.ragService = ragService;
        this.intentRecognitionService = intentRecognitionService;
    }

    /**
     * 处理对话请求
     */
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        // 获取或创建会话
        ConversationContext context = sessionManagementService.getOrCreateSession(request.getSessionId(), request.getUserId());

        // 检查是否超过最大轮次
        if (context.getTurn() >= maxTurns) {
            return buildTransferResponse(context, "对话轮次已达上限，建议转人工服务。");
        }

        // 添加用户消息到上下文
        context.addUserMessage(request.getMessage());
        context.incrementTurn();
        context.updateLastActiveTime();

        // 保存用户消息到数据库
        messagePersistenceService.saveMessage(context.getSessionId(), "user", request.getMessage(), context.getTurn());
        sessionManagementService.updateSessionTurn(context.getSessionId(), context.getTurn());

        // Step 0: 意图识别
        IntentRecognitionService.Recognition intentRecognition = intentRecognitionService.recognize(request.getMessage());
        if (intentRecognition.result == IntentRecognitionService.IntentResult.RECOGNIZED
                && intentRecognition.intention != null
                && intentRecognition.intention.getResponseTemplate() != null
                && !intentRecognition.intention.getResponseTemplate().isBlank()) {
            String reply = intentRecognition.intention.getResponseTemplate();
            context.addAssistantMessage(reply);
            messagePersistenceService.saveMessage(context.getSessionId(), "assistant", reply, context.getTurn());
            sessionManagementService.saveSession(context);
            return ChatResponse.builder()
                    .sessionId(context.getSessionId())
                    .reply(reply)
                    .timestamp(LocalDateTime.now())
                    .turn(context.getTurn())
                    .suggestions(generateSuggestions(context))
                    .transferToHuman(false)
                    .build();
        }

        // Step 1: 检索相关 FAQ
        List<FaqKnowledge> relevantFaqs = ragService.retrieve(request.getMessage(), 3);

        // Step 2: 构建消息列表 (添加增强 Prompt)
        String systemPrompt = relevantFaqs.isEmpty()
                ? ConversationContext.SYSTEM_PROMPT
                : ragService.buildEnhancedPrompt(request.getMessage(), relevantFaqs);
        List<MiniMaxRequest.Message> messages = buildMessages(context, systemPrompt);

        // Step 3: 调用 AI
        String reply = miniMaxService.chat(messages);

        // 添加 AI 回复到上下文
        context.addAssistantMessage(reply);

        // 保存 AI 回复到数据库
        messagePersistenceService.saveMessage(context.getSessionId(), "assistant", reply, context.getTurn());

        // 保存上下文到 Redis
        sessionManagementService.saveSession(context);

        // 构建响应
        return ChatResponse.builder()
                .sessionId(context.getSessionId())
                .reply(reply)
                .timestamp(LocalDateTime.now())
                .turn(context.getTurn())
                .suggestions(generateSuggestions(context))
                .transferToHuman(reply.contains("转人工") || reply.contains("人工客服"))
                .build();
    }

    /**
     * 获取欢迎消息
     */
    public ChatResponse getWelcomeMessage(String sessionId, String userId) {
        ConversationContext context = sessionManagementService.getOrCreateSession(sessionId, userId);

        return ChatResponse.builder()
                .sessionId(context.getSessionId())
                .reply(welcomeMessage)
                .timestamp(LocalDateTime.now())
                .turn(context.getTurn())
                .suggestions(Arrays.asList("有什么产品推荐？", "如何联系人工客服？", "你们的工作时间是？"))
                .transferToHuman(false)
                .build();
    }

    /**
     * 清除会话
     */
    @Transactional
    public void clearSession(String sessionId) {
        sessionManagementService.clearSession(sessionId);
    }

    /**
     * 构建发送给 AI 的消息列表
     */
    private List<MiniMaxRequest.Message> buildMessages(ConversationContext context, String systemPrompt) {
        List<MiniMaxRequest.Message> messages = new ArrayList<>();

        // 添加系统提示
        messages.add(MiniMaxRequest.Message.builder()
                .role("system")
                .content(systemPrompt)
                .build());

        // 限制上下文长度
        List<MiniMaxRequest.Message> history = context.getMessages();
        int startIndex = Math.max(1, history.size() - miniMaxConfig.getContextSize());
        for (int i = startIndex; i < history.size(); i++) {
            messages.add(history.get(i));
        }

        return messages;
    }

    @PostConstruct
    public void init() {
        try {
            ragService.initializeKnowledgeBase();
            log.info("RAG 知识库初始化完成");
        } catch (Exception e) {
            log.warn("RAG 知识库初始化失败，将使用无 RAG 模式: {}", e.getMessage());
        }
    }

    /**
     * 生成建议问题
     */
    private List<String> generateSuggestions(ConversationContext context) {
        try {
            // 从数据库获取快捷问题
            List<QuickQuestion> questions = quickQuestionRepository.findByStatusOrderBySortOrderAsc(1);
            return questions.stream()
                    .limit(5)
                    .map(QuickQuestion::getContent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取快捷问题失败", e);
            return Arrays.asList(
                    "还有其他问题吗？",
                    "我想联系人工客服",
                    "换个话题聊聊"
            );
        }
    }

    /**
     * 构建转人工响应
     */
    private ChatResponse buildTransferResponse(ConversationContext context, String message) {
        return ChatResponse.builder()
                .sessionId(context.getSessionId())
                .reply(message + "\n\n您可以输入\"转人工\"联系人工客服，或开启新会话。")
                .timestamp(LocalDateTime.now())
                .turn(context.getTurn())
                .transferToHuman(true)
                .build();
    }

}
