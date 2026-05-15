package com.example.airobot.service;

import com.example.airobot.config.ChromaConfig;
import com.example.airobot.entity.FaqKnowledge;
import com.example.airobot.repository.FaqKnowledgeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索增强生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final ChromaConfig chromaConfig;
    private final EmbeddingService embeddingService;
    private final FaqKnowledgeRepository faqRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private WebClient chromaClient;

    @PostConstruct
    public void init() {
        chromaClient = webClientBuilder
                .baseUrl(chromaConfig.getUrl())
                .build();

        // 初始化时创建 collection (如果不存在)
        createCollectionIfNotExists();
    }

    /**
     * 创建 Collection (如果不存在)
     */
    private void createCollectionIfNotExists() {
        try {
            chromaClient.post()
                    .uri("/api/v1/collections")
                    .bodyValue(Map.of("name", chromaConfig.getCollectionName()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Chroma collection '{}' 创建成功", chromaConfig.getCollectionName());
        } catch (Exception e) {
            log.info("Chroma collection 已存在或创建失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化知识库 (从数据库加载FAQ并写入Chroma)
     */
    public void initializeKnowledgeBase() {
        List<FaqKnowledge> faqs = faqRepository.findByStatus(1);
        if (faqs.isEmpty()) {
            log.info("没有需要初始化的 FAQ");
            return;
        }

        for (FaqKnowledge faq : faqs) {
            addFaqToIndex(faq);
        }
        log.info("知识库初始化完成，共导入 {} 条 FAQ", faqs.size());
    }

    /**
     * 添加单条 FAQ 到向量索引
     */
    public void addFaqToIndex(FaqKnowledge faq) {
        try {
            // 构建要向量化的文本
            String text = buildTextForEmbedding(faq);

            // 获取向量
            List<Double> embedding = embeddingService.embed(text);
            if (embedding.isEmpty()) {
                log.error("FAQ {} embedding 失败", faq.getId());
                return;
            }

            // 构建 Chroma 文档
            Map<String, Object> document = Map.of(
                    "id", "faq_" + faq.getId(),
                    "embedding", embedding,
                    "document", text,
                    "metadata", Map.of(
                            "question", faq.getQuestion(),
                            "answer", faq.getAnswer(),
                            "category", faq.getCategory() != null ? faq.getCategory() : "",
                            "id", faq.getId()
                    )
            );

            // 存入 Chroma
            chromaClient.post()
                    .uri("/api/v1/collections/" + chromaConfig.getCollectionName() + "/add")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(document)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("FAQ {} 已添加到向量索引", faq.getId());

        } catch (Exception e) {
            log.error("添加 FAQ {} 到向量索引失败: {}", faq.getId(), e.getMessage());
        }
    }

    /**
     * 从向量索引删除单条 FAQ
     */
    public void removeFaqFromIndex(Long faqId) {
        try {
            chromaClient.delete()
                    .uri("/api/v1/collections/" + chromaConfig.getCollectionName() + "/delete")
                    .bodyValue(Map.of("ids", List.of("faq_" + faqId)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.debug("FAQ {} 已从向量索引删除", faqId);
        } catch (Exception e) {
            log.error("从向量索引删除 FAQ {} 失败: {}", faqId, e.getMessage());
        }
    }

    /**
     * 检索相关 FAQ
     * @param query 用户查询
     * @param topK 返回数量
     * @return 相关 FAQ 列表
     */
    public List<FaqKnowledge> retrieve(String query, int topK) {
        try {
            // 获取查询向量
            List<Double> queryEmbedding = embeddingService.embed(query);
            if (queryEmbedding.isEmpty()) {
                log.warn("查询 embedding 失败，返回空结果");
                return List.of();
            }

            // Chroma 查询
            Map<String, Object> requestBody = Map.of(
                    "query_embeddings", List.of(queryEmbedding),
                    "n_results", topK,
                    "include", List.of("documents", "metadatas")
            );

            String response = chromaClient.post()
                    .uri("/api/v1/collections/" + chromaConfig.getCollectionName() + "/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseQueryResponse(response);

        } catch (Exception e) {
            log.error("RAG 检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析 Chroma 查询响应
     */
    private List<FaqKnowledge> parseQueryResponse(String response) {
        List<FaqKnowledge> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode metadatas = root.path("metadatas");

            if (metadatas.isArray() && !metadatas.isEmpty()) {
                JsonNode firstResult = metadatas.get(0);
                if (firstResult.isArray()) {
                    for (JsonNode metadata : firstResult) {
                        FaqKnowledge faq = FaqKnowledge.builder()
                                .id(metadata.path("id").asLong())
                                .question(metadata.path("question").asText())
                                .answer(metadata.path("answer").asText())
                                .category(metadata.path("category").asText())
                                .build();
                        results.add(faq);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析 Chroma 响应失败: {}", e.getMessage());
        }
        return results;
    }

    /**
     * 构建增强 Prompt
     */
    public String buildEnhancedPrompt(String query, List<FaqKnowledge> retrievedFaqs) {
        if (retrievedFaqs.isEmpty()) {
            return query;
        }

        StringBuilder knowledge = new StringBuilder();
        for (int i = 0; i < retrievedFaqs.size(); i++) {
            FaqKnowledge faq = retrievedFaqs.get(i);
            knowledge.append(String.format("【知识 %d】\n问题: %s\n答案: %s\n\n",
                    i + 1, faq.getQuestion(), faq.getAnswer()));
        }

        return String.format("""
                你是一个专业的AI智能客服助手。请根据以下知识库内容回答用户问题。

                【相关知识】
                %s
                【用户问题】
                %s

                请结合知识库内容给出准确回答，如果知识库没有相关信息，请如实告知。
                """, knowledge.toString(), query);
    }

    /**
     * 构建向量化的文本
     */
    private String buildTextForEmbedding(FaqKnowledge faq) {
        StringBuilder sb = new StringBuilder();
        sb.append("问题: ").append(faq.getQuestion()).append("\n");
        sb.append("答案: ").append(faq.getAnswer()).append("\n");
        if (faq.getSimilarQuestions() != null && !faq.getSimilarQuestions().isEmpty()) {
            sb.append("相似问题: ").append(faq.getSimilarQuestions());
        }
        return sb.toString();
    }
}