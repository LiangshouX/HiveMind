-- Flyway Migration: V1.0.0__init_schema.sql
-- Create core tables for TangDynasty with primary keys, optimistic locks, logical deletion, and indexes.

-- 1. 部门与官员表 (Agents/Officials)
CREATE TABLE `sys_department` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '部门编码 (如 Zhongshu, Menxia)',
  `name` VARCHAR(100) NOT NULL COMMENT '部门名称 (如 中书省)',
  `description` VARCHAR(255) COMMENT '部门职责描述',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE `sys_official` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键',
  `agent_id` VARCHAR(50) NOT NULL COMMENT 'Agent标识',
  `name` VARCHAR(100) NOT NULL COMMENT '官员名称 (如 中书令)',
  `dept_id` BIGINT NOT NULL COMMENT '所属部门ID',
  `model` VARCHAR(100) NOT NULL COMMENT '当前使用的模型',
  `system_prompt` TEXT COMMENT '系统提示词(Soul)',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_id` (`agent_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='官员(Agent)表';

-- 2. 任务/旨意表 (Tasks)
CREATE TABLE `td_task` (
  `id` VARCHAR(50) NOT NULL COMMENT '任务ID (如 JJC-20260228-E2E)',
  `title` VARCHAR(255) NOT NULL COMMENT '任务标题',
  `official_id` BIGINT COMMENT '当前负责官员ID',
  `dept_id` BIGINT COMMENT '当前负责部门ID',
  `state` VARCHAR(50) NOT NULL COMMENT '当前状态',
  `priority` VARCHAR(20) DEFAULT 'normal' COMMENT '优先级: critical/high/normal/low',
  `block_reason` VARCHAR(255) COMMENT '阻滞原因',
  `review_round` INT DEFAULT 0 COMMENT '审议轮数',
  `prev_state` VARCHAR(50) COMMENT '被中断前的状态',
  `output_result` TEXT COMMENT '最终产出结果',
  `ac_criteria` TEXT COMMENT '验收标准',
  `archived` TINYINT(1) DEFAULT 0 COMMENT '是否归档',
  `archived_at` DATETIME COMMENT '归档时间',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_state` (`state`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务(旨意)表';

-- 3. 任务流转日志表 (Task Flow Log)
CREATE TABLE `td_task_flow_log` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键',
  `task_id` VARCHAR(50) NOT NULL COMMENT '任务ID',
  `from_node` VARCHAR(100) COMMENT '来源节点',
  `to_node` VARCHAR(100) COMMENT '目标节点',
  `remark` TEXT COMMENT '流转备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务流转记录表';

-- 4. 任务活动日志表 (Task Activity Log - Progress/Todos)
CREATE TABLE `td_task_activity_log` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键',
  `task_id` VARCHAR(50) NOT NULL COMMENT '任务ID',
  `agent_id` VARCHAR(50) COMMENT '汇报Agent',
  `content` TEXT COMMENT '汇报内容',
  `state_snapshot` VARCHAR(50) COMMENT '状态快照',
  `tokens_used` INT DEFAULT 0 COMMENT '消耗Token',
  `cost` DECIMAL(10,4) DEFAULT 0.0000 COMMENT '消耗成本',
  `elapsed_ms` BIGINT DEFAULT 0 COMMENT '耗时(毫秒)',
  `todos_snapshot` JSON COMMENT 'Todos快照',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务活动/汇报记录表';

-- 5. 技能与工具表 (Skills/Tools)
CREATE TABLE `sys_skill` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键',
  `agent_id` VARCHAR(50) NOT NULL COMMENT '所属Agent',
  `skill_name` VARCHAR(100) NOT NULL COMMENT '技能名称',
  `description` VARCHAR(255) COMMENT '技能描述',
  `trigger_condition` VARCHAR(255) COMMENT '触发条件',
  `source_url` VARCHAR(255) COMMENT '远程技能URL',
  `is_remote` TINYINT(1) DEFAULT 0 COMMENT '是否远程技能',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能表';

-- 6. 用户与权限表 (Users/Roles for RBAC)
CREATE TABLE `sys_user` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `nickname` VARCHAR(100) COMMENT '昵称(如 皇上)',
  `role` VARCHAR(50) DEFAULT 'USER' COMMENT '角色',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- Initialize basic data
INSERT INTO `sys_department` (`code`, `name`, `description`) VALUES 
('Zhongshu', '中书省', '规划官、方案起草总负责'),
('Menxia', '门下省', '审议官、质量把握人'),
('Shangshu', '尚书省', '派发官、执行总指挥'),
('Libu', '礼部', '文档编制官'),
('Hubu', '户部', '数据分析官'),
('Bingbu', '兵部', '代码实现官'),
('Xingbu', '刑部', '测试审查官'),
('Gongbu', '工部', '基础设施官'),
('Libu_hr', '吏部', '人力资源官');

-- Insert default admin user (Password: admin123 -> BCrypt hash)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '皇上', 'ADMIN');
