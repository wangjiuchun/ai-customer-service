-- AI 客服系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_customer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_customer;

-- 1. 会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话唯一标识',
    user_id VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-活跃, 0-已结束',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    turn_count INT DEFAULT 0 COMMENT '对话轮次',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 2. 消息记录表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    turn INT DEFAULT 1 COMMENT '当前轮次',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (session_id) REFERENCES chat_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息记录表';

-- 3. 意图分类表
CREATE TABLE IF NOT EXISTS intention (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '意图名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '意图描述',
    keywords VARCHAR(500) DEFAULT NULL COMMENT '关键词(逗号分隔)',
    response_template TEXT DEFAULT NULL COMMENT '响应模板',
    priority INT DEFAULT 0 COMMENT '优先级',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意图分类表';

-- 4. 快捷问题表
CREATE TABLE IF NOT EXISTS quick_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    content VARCHAR(255) NOT NULL COMMENT '问题内容',
    description VARCHAR(255) DEFAULT NULL COMMENT '问题描述',
    category VARCHAR(50) DEFAULT NULL COMMENT '分类',
    click_count INT DEFAULT 0 COMMENT '点击次数',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快捷问题表';

-- 5. FAQ 知识库表
CREATE TABLE IF NOT EXISTS faq_knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    question VARCHAR(500) NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '答案',
    category VARCHAR(50) DEFAULT NULL COMMENT '分类',
    tags VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)',
    similar_questions VARCHAR(1000) DEFAULT NULL COMMENT '相似问题(逗号分隔)',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    hit_count INT DEFAULT 0 COMMENT '命中次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_question (question(255)),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='FAQ知识库表';

-- 6. 转人工记录表
CREATE TABLE IF NOT EXISTS transfer_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    transfer_reason VARCHAR(500) DEFAULT NULL COMMENT '转人工原因',
    transfer_status TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-已接入, 2-已结束',
    operator_id VARCHAR(64) DEFAULT NULL COMMENT '接入客服ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    connected_at DATETIME DEFAULT NULL COMMENT '接入时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    INDEX idx_session_id (session_id),
    INDEX idx_transfer_status (transfer_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转人工记录表';

-- 7. 对话统计表
CREATE TABLE IF NOT EXISTS chat_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_sessions INT DEFAULT 0 COMMENT '总会话数',
    total_messages INT DEFAULT 0 COMMENT '总消息数',
    avg_turns DECIMAL(5,2) DEFAULT 0 COMMENT '平均对话轮次',
    transfer_count INT DEFAULT 0 COMMENT '转人工次数',
    active_sessions INT DEFAULT 0 COMMENT '活跃会话数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话统计表';

-- 初始化快捷问题数据
INSERT INTO quick_question (content, description, category, sort_order) VALUES
('有什么产品推荐？', '了解产品推荐', 'product', 1),
('如何联系人工客服？', '转人工客服', 'service', 2),
('你们的工作时间是？', '工作时间咨询', 'service', 3),
('如何修改密码？', '账户设置', 'account', 4),
('有优惠活动吗？', '促销活动咨询', 'promotion', 5)
ON DUPLICATE KEY UPDATE content=VALUES(content);

-- 初始化FAQ知识库数据
INSERT INTO faq_knowledge (question, answer, category, tags) VALUES
('你们的产品有什么优势？', '我们的产品具有以下优势：\n1. 高性价比\n2. 完善的售后服务\n3. 快速响应\n4. 持续更新迭代', 'product', '优势,特点,好处')
ON DUPLICATE KEY UPDATE answer=VALUES(answer);
