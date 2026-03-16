CREATE DATABASE IF NOT EXISTS `tang_dynasty` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `tang_dynasty`;

-- 1. Conversations (Sessions)
CREATE TABLE IF NOT EXISTS `sys_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `session_id` VARCHAR(64) NOT NULL COMMENT 'Session UUID',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'Session Title',
    `type` VARCHAR(32) DEFAULT 'chat' COMMENT 'Type: chat, task',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Conversation Sessions';

-- 2. Messages
CREATE TABLE IF NOT EXISTS `sys_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `session_id` VARCHAR(64) NOT NULL COMMENT 'Session UUID',
    `role` VARCHAR(32) NOT NULL COMMENT 'Role: user, assistant, system',
    `content` LONGTEXT COMMENT 'Message Content',
    `meta` JSON DEFAULT NULL COMMENT 'Metadata (tokens, model, etc.)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Chat Messages';

-- 3. Tasks (Edicts)
CREATE TABLE IF NOT EXISTS `sys_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `task_id` VARCHAR(64) NOT NULL COMMENT 'Task ID (JJC-Date-Seq)',
    `title` VARCHAR(255) NOT NULL COMMENT 'Task Title',
    `description` TEXT COMMENT 'Task Description',
    `status` VARCHAR(32) NOT NULL DEFAULT 'Pending' COMMENT 'Status: Pending, Assigned, Doing, Review, Done',
    `priority` VARCHAR(32) DEFAULT 'normal' COMMENT 'Priority: critical, high, normal, low',
    `official` VARCHAR(64) DEFAULT NULL COMMENT 'Responsible Official',
    `department` VARCHAR(64) DEFAULT NULL COMMENT 'Responsible Department',
    `payload` JSON DEFAULT NULL COMMENT 'Task Payload (JSON)',
    `result` JSON DEFAULT NULL COMMENT 'Task Result (JSON)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tasks / Edicts';

-- 4. Task Logs (Flow Log & Progress Log)
CREATE TABLE IF NOT EXISTS `sys_task_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `task_id` VARCHAR(64) NOT NULL COMMENT 'Task ID',
    `type` VARCHAR(32) NOT NULL COMMENT 'Type: flow, progress',
    `content` JSON NOT NULL COMMENT 'Log Content',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task Logs';

-- 5. Channels
CREATE TABLE IF NOT EXISTS `sys_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `name` VARCHAR(64) NOT NULL COMMENT 'Channel Name (feishu, dingtalk)',
    `config` JSON NOT NULL COMMENT 'Configuration (token, secret)',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT 'Enabled',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Communication Channels';

-- 6. Officials (San Sheng Liu Bu)
CREATE TABLE IF NOT EXISTS `sys_official` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `name` VARCHAR(64) NOT NULL COMMENT 'Official Name',
    `title` VARCHAR(64) NOT NULL COMMENT 'Official Title',
    `department` VARCHAR(64) NOT NULL COMMENT 'Department',
    `bio` TEXT COMMENT 'Biography / Persona',
    `prompt` TEXT COMMENT 'System Prompt',
    `model_config` JSON DEFAULT NULL COMMENT 'Model Configuration',
    `skills` JSON DEFAULT NULL COMMENT 'Skills List',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Officials';

-- 7. Skills
CREATE TABLE IF NOT EXISTS `sys_skill` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `name` VARCHAR(64) NOT NULL COMMENT 'Skill Name',
    `description` TEXT COMMENT 'Description',
    `script` LONGTEXT COMMENT 'Skill Script (Python/JS)',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT 'Enabled',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skills Library';

-- 8. Tools
CREATE TABLE IF NOT EXISTS `sys_tool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `name` VARCHAR(64) NOT NULL COMMENT 'Tool Name',
    `description` TEXT COMMENT 'Description',
    `config` JSON DEFAULT NULL COMMENT 'Configuration',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT 'Enabled',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tools Library';

-- 9. MCP Clients
CREATE TABLE IF NOT EXISTS `sys_mcp` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `name` VARCHAR(64) NOT NULL COMMENT 'Client Name',
    `config` JSON NOT NULL COMMENT 'Configuration',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT 'Enabled',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP Clients';

-- 10. Models
CREATE TABLE IF NOT EXISTS `sys_model` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `provider` VARCHAR(64) NOT NULL COMMENT 'Provider (OpenAI, Anthropic)',
    `name` VARCHAR(64) NOT NULL COMMENT 'Model Name',
    `config` JSON DEFAULT NULL COMMENT 'Configuration',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT 'Enabled',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_name` (`provider`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM Models';

-- 11. Token Usage
CREATE TABLE IF NOT EXISTS `sys_token_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `date` DATE NOT NULL COMMENT 'Date',
    `model` VARCHAR(64) NOT NULL COMMENT 'Model Name',
    `tokens` BIGINT DEFAULT 0 COMMENT 'Token Count',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_model` (`date`, `model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token Usage Stats';

-- 12. Configs
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `key_name` VARCHAR(64) NOT NULL COMMENT 'Config Key',
    `value` TEXT COMMENT 'Config Value',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System Configurations';
