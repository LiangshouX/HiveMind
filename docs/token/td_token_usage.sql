-- Token Usage 统计表 - Agent 引擎专用
-- 用于记录 Agent 对话中的 LLM Token 使用量

CREATE TABLE IF NOT EXISTS `td_token_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(128) NOT NULL COMMENT '会话ID',
  `message_id` VARCHAR(128) NOT NULL COMMENT '消息ID（AgentScope Msg ID）',
  
  -- 模型信息
  `model_provider` VARCHAR(64) NOT NULL COMMENT '模型供应商（dashscope/openai）',
  `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称（如 qwen-max、gpt-4o）',
  
  -- Token 用量
  `input_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入Token数',
  `output_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出Token数',
  `total_tokens` INT NOT NULL DEFAULT 0 COMMENT '总Token数',
  `cached_tokens` INT DEFAULT 0 COMMENT '缓存命中Token数（可选）',
  
  -- 时间信息
  `usage_time` DATETIME NOT NULL COMMENT '使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  INDEX `idx_user_time` (`user_id`, `usage_time`),
  INDEX `idx_user_model` (`user_id`, `model_name`),
  INDEX `idx_user_provider` (`user_id`, `model_provider`),
  INDEX `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent Token使用统计表';
