package com.example.airobot.repository;

import com.example.airobot.entity.Intention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 意图分类 Repository
 */
@Repository
public interface IntentionRepository extends JpaRepository<Intention, Long> {

    /**
     * 根据名称查询
     */
    Optional<Intention> findByName(String name);

    /**
     * 查询启用的意图
     */
    List<Intention> findByStatusOrderByPriorityDesc(Integer status);
}
