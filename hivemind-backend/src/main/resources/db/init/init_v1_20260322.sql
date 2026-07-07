-- MySQL 8.0.36 / InnoDB / utf8mb4
CREATE DATABASE IF NOT EXISTS `hivemind` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `hivemind`;

-- ========== 用户与权限 ==========
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键',
  `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID',
  `phone_number` VARCHAR(20) DEFAULT NULL COMMENT '用户电话号码，包含国际区号',
  `password` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `nickname` VARCHAR(100) COMMENT '昵称',
  `role` VARCHAR(50) DEFAULT 'USER' COMMENT '角色',
   -- 微信登录核心字段
  `wechat_openid` VARCHAR(64) DEFAULT NULL COMMENT '微信应用唯一标识',
  `wechat_unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信开放平台统一标识',
  -- 微信用户资料
  `wechat_nickname` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信昵称',
  `wechat_avatar` VARCHAR(255) DEFAULT NULL COMMENT '微信头像URL',
  `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',

  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userid` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ================================ 任务中心场景 ================================
-- 1. AI Agent通讯渠
CREATE TABLE IF NOT EXISTS `sys_channels` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '登录的用户ID',
  `channel_name` VARCHAR(64) NOT NULL COMMENT '渠道名',
  `is_active` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用: 0-否, 1-是',
  `bot_prefix` VARCHAR(32) DEFAULT '@bot' COMMENT '机器人前缀',
  `show_tool_message` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否显示工具信息: 0-否, 1-是',
  `show_thinking` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否显示思考过程: 0-否, 1-是',
  `documentation_address` VARCHAR(255) DEFAULT NULL COMMENT '说明文档地址',
  `specific_config` JSON DEFAULT NULL COMMENT '渠道对应的针对性配置',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_channel_name` (`channel_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Agent通讯渠道配置表';

-- 2. 任务表 (Tasks)
CREATE TABLE IF NOT EXISTS `agent_tasks` (
  `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID, 如 JJC-20260228-E2E',
  `session_id` VARCHAR(64) DEFAULT NULL COMMENT '任务对应的session_id',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '登录的用户ID',
  `title` VARCHAR(255) DEFAULT NULL COMMENT '任务标题',
  `official_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '当前负责Agent ID',
  `state` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '当前状态: PENDING/AGENT_TRIAGE/AGENT_PLANNER/AGENT_REVIEWER/ASSIGNED/DOING/PREVIEW/DONE/FINISH',
  `priority` VARCHAR(16) DEFAULT 'normal' COMMENT '优先级: critical/high/normal/low',
  `block_reason` VARCHAR(512) DEFAULT NULL COMMENT '阻滞原因',
  `review_round` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '要求修改的轮数',
  `prev_state` VARCHAR(32) DEFAULT NULL COMMENT '被中断前的状态',
  `output_result` TEXT COMMENT '最终产出结果',
  `ac_criteria` TEXT COMMENT '验收标准',
  `archived` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否归档: 0-否, 1-是',
  `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_official_id` (`official_id`),
  KEY `idx_state` (`state`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务实例主表';

-- 3. 任务报告/审批表
CREATE TABLE IF NOT EXISTS `task_reports` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '任务所属的用户ID',
  `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID, 对应 agent_tasks 的任务id',
  `task_title` VARCHAR(255) DEFAULT NULL COMMENT '任务标题',
  `task_content` TEXT NOT NULL COMMENT '任务指令内容',
  `task_result` TEXT COMMENT '任务输出结果',
  `approval_state` VARCHAR(32) NOT NULL DEFAULT 'WAITING' COMMENT '批阅状态: WAITING/APPROVAL/REFUSAL/REDO',
  `delivery_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报告递交时间/创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_approval_state` (`approval_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务报告/审批表';

-- 4. 定时任务管理表
CREATE TABLE IF NOT EXISTS `scheduled_job` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
  `job_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '任务所属的用户ID',
  `is_activated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启: 0-否, 1-是',
  `cron_config` VARCHAR(64) DEFAULT '0 8 * * *' COMMENT '定时任务cron表达式',
  `time_zone` VARCHAR(32) DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `job_description` VARCHAR(512) DEFAULT NULL COMMENT '任务描述',
  `job_require_content` JSON DEFAULT NULL COMMENT '请求内容, Json串描述的message',
  `job_drived_task_id` VARCHAR(64) DEFAULT NULL COMMENT '定时任务驱动的任务ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_id` (`job_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_activated` (`is_activated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务管理表';

-- 5. 定时任务执行记录
CREATE TABLE IF NOT EXISTS `scheduled_job_run_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '任务所属的用户ID',
  `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
  `job_start_time` DATETIME NOT NULL COMMENT '任务开始执行时间',
  `job_finish_time` DATETIME DEFAULT NULL COMMENT '任务执行结束时间',
  `duration_ms` BIGINT UNSIGNED DEFAULT 0 COMMENT '执行耗时(毫秒)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_job_finish_time` (`job_finish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行记录表';


-- ================================ 管理场景 ================================
-- 1. 模型配置表
CREATE TABLE IF NOT EXISTS `sys_models` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '任务所属的用户ID',
  `model_provider_id` VARCHAR(64) NOT NULL COMMENT '模型供应商',
  `model_provider_type` VARCHAR(32) NOT NULL DEFAULT 'SYSTEM' COMMENT '供应商类型: SYSTEM/CUSTOM/LOCAL',
  `is_provider_activated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '模型供应商是否启用: 0-否, 1-是',
  `base_url` VARCHAR(255) DEFAULT NULL COMMENT '访问地址',
  `api_key` VARCHAR(512) DEFAULT NULL COMMENT '秘钥, 加密存储',
  `model_id` VARCHAR(128) NOT NULL COMMENT '请求的模型ID',
  `model_name` VARCHAR(128) DEFAULT NULL COMMENT '模型名称',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_provider_id` (`model_provider_id`),
  KEY `idx_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型配置表';

-- 2. MCP配置表
CREATE TABLE IF NOT EXISTS `sys_mcp` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '任务所属的用户ID',
  `mcp_server` VARCHAR(128) NOT NULL COMMENT 'MCP Server',
  `mcp_server_type` VARCHAR(32) NOT NULL DEFAULT 'SYSTEM' COMMENT 'MCP类型: SYSTEM/CUSTOM',
  `mcp_tool` VARCHAR(128) DEFAULT NULL COMMENT 'MCP Server下对应的具体工具',
  `is_server_activated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用Server: 0-否, 1-是',
  `is_tool_activated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用Tool: 0-否, 1-是',
  `tool_param_config` JSON DEFAULT NULL COMMENT '工具参数配置',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_mcp_server` (`mcp_server`),
  KEY `idx_server_type` (`mcp_server_type`),
  KEY `idx_mcp_tool` (`mcp_tool`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP配置表';

-- ================================ 服务场景 ================================
-- 1. Token消耗统计表
-- ----------------------------
DROP TABLE IF EXISTS `sys_token_usage`;
CREATE TABLE IF NOT EXISTS `sys_token_usage` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '任务所属的用户ID',
  `model_provider_id` VARCHAR(64) NOT NULL COMMENT '模型供应商',
  `model_id` VARCHAR(128) NOT NULL COMMENT '请求的模型ID',
  `official_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '调用的Agent',
  `prompt_tokens` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输入Token数',
  `completion_tokens` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输出Token数',
  `total_tokens` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总Token数',
  `cached_prompt_tokens` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '命中缓存的Token数量',
  `record_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_provider_id` (`model_provider_id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token消耗统计表';
