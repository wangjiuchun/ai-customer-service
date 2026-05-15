package com.example.airobot.controller;

import com.example.airobot.entity.FaqKnowledge;
import com.example.airobot.repository.FaqKnowledgeRepository;
import com.example.airobot.service.RAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FAQ 知识库管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
public class FaqController {

    private final FaqKnowledgeRepository faqRepository;
    private final RAGService ragService;

    /**
     * 获取所有 FAQ
     */
    @GetMapping
    public ApiResponse<List<FaqKnowledge>> getAll() {
        return ApiResponse.success(faqRepository.findAll());
    }

    /**
     * 新增 FAQ
     */
    @PostMapping
    public ApiResponse<FaqKnowledge> create(@RequestBody FaqKnowledge faq) {
        FaqKnowledge saved = faqRepository.save(faq);
        // 添加到向量索引
        ragService.addFaqToIndex(saved);
        return ApiResponse.success(saved);
    }

    /**
     * 更新 FAQ
     */
    @PutMapping("/{id}")
    public ApiResponse<FaqKnowledge> update(@PathVariable Long id, @RequestBody FaqKnowledge faq) {
        faq.setId(id);
        FaqKnowledge saved = faqRepository.save(faq);
        // 增量更新：先删除旧索引，再添加新索引
        ragService.removeFaqFromIndex(id);
        ragService.addFaqToIndex(saved);
        return ApiResponse.success(saved);
    }

    /**
     * 删除 FAQ
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        faqRepository.deleteById(id);
        ragService.removeFaqFromIndex(id);
        return ApiResponse.success("删除成功");
    }
}
