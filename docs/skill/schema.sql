-- Skill 元数据表（MySQL 8.0+）
-- 用于存储云端 Skill 的元信息和版本管理

CREATE TABLE IF NOT EXISTS `skills_meta_manage` (
  `skill_id` VARCHAR(36) NOT NULL COMMENT 'Skill 唯一标识（UUID）',
  `user_id` VARCHAR(36) NOT NULL COMMENT '用户 ID',
  `name` VARCHAR(100) NOT NULL COMMENT 'Skill 名称',
  `description` TEXT COMMENT 'Skill 描述',
  `current_version` VARCHAR(20) NOT NULL DEFAULT '1.0.0' COMMENT '当前版本号',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published/deprecated/archived',
  `tags` JSON COMMENT '标签列表',
  `dependencies` JSON COMMENT '外部依赖配置',
  `execution_env` JSON COMMENT '运行环境配置',
  `file_manifest` JSON NOT NULL COMMENT '文件清单快照（版本路径映射）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`skill_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_user_name` (`user_id`, `name`),
  KEY `idx_updated_at` (`updated_at`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill 元数据管理表';

-- 索引说明：
-- idx_user_status: 用于用户技能列表查询和状态过滤
-- idx_user_name: 用于检查同名技能是否存在
-- idx_updated_at: 用于按时间排序
-- idx_deleted_at: 用于软删除过滤

-- 初始化示例数据（可选）
-- INSERT INTO `skills_meta_manage` (
--   `skill_id`, `user_id`, `name`, `description`, `current_version`, `status`, 
--   `tags`, `file_manifest`, `created_at`, `updated_at`
-- ) VALUES (
--   UUID(), 'user-001', 'example-skill', '示例技能', '1.0.0', 'draft',
--   '["example", "demo"]',
--   '{"version": "1.0.0", "objectKey": "user-001/skill-id/v1.0.0/skill.tar.gz"}',
--   NOW(), NOW()
-- );
