package com.example.airobot.repository;

import com.example.airobot.entity.QuickQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 快捷问题 Repository
 */
@Repository
public interface QuickQuestionRepository extends JpaRepository<QuickQuestion, Long> {

    /**
     * 查询启用的快捷问题
     */
    List<QuickQuestion> findByStatusOrderBySortOrderAsc(Integer status);

    /**
     * 根据分类查询
     */
    List<QuickQuestion> findByCategoryAndStatusOrderBySortOrderAsc(String category, Integer status);

    /**
     * 增加点击次数
     */
    @Modifying
    @Query("UPDATE QuickQuestion q SET q.clickCount = q.clickCount + 1 WHERE q.id = :id")
    void incrementClickCount(Long id);
}
