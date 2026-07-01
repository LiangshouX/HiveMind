CREATE TABLE IF NOT EXISTS `skills_meta_manage` (
  `skill_id` VARCHAR(36) NOT NULL COMMENT '技能唯一标识(UUID)',
  `user_id` VARCHAR(36) NOT NULL COMMENT '归属用户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '技能名称',
  `description` TEXT COMMENT '技能描述/摘要(用于列表展示与语义检索)',
  `current_version` VARCHAR(20) NOT NULL DEFAULT '1.0.0' COMMENT '当前生效版本',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published/deprecated/archived',
  `tags` JSON COMMENT '标签数组, 例: ["agent", "finance", "parser"]',
  `dependencies` JSON COMMENT '外部依赖配置, 例: {"tools": ["web_search"], "models": ["gpt-4o"]}',
  `execution_env` JSON COMMENT '运行环境声明, 例: {"runtime": "python", "version": "3.11", "memory_limit": "512m"}',
  `file_manifest` JSON DEFAULT NULL COMMENT '文件清单: 逻辑路径 -> S3 Key/ETag/Size 映射',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`skill_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`(50)),
  KEY `idx_updated_at` (`updated_at`),
  -- MySQL 8.0.17+ JSON 多值索引 (加速 tag 过滤查询)
  KEY `idx_tags` ((CAST(`tags` AS CHAR(255) ARRAY))),
  -- 软删除查询优化索引
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent技能元数据表';