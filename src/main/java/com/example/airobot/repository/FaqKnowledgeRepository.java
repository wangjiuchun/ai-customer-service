package com.example.airobot.repository;

import com.example.airobot.entity.FaqKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * FAQ 知识库 Repository
 */
@Repository
public interface FaqKnowledgeRepository extends JpaRepository<FaqKnowledge, Long> {

    /**
     * 根据分类查询
     */
    List<FaqKnowledge> findByCategoryAndStatus(String category, Integer status);

    /**
     * 根据关键词搜索
     */
    List<FaqKnowledge> findByStatus(Integer status);

    /**
     * 增加命中次数
     */
    @Modifying
    @Query("UPDATE FaqKnowledge f SET f.hitCount = f.hitCount + 1 WHERE f.id = :id")
    void incrementHitCount(Long id);
}
