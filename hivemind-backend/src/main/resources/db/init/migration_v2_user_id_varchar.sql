-- ============================================================
-- HiveMind - Migration v2 (2026-11-07)
--
-- 变更：user_id 由 BIGINT UNSIGNED 统一为 VARCHAR(64)
--
-- 背景：sys_user.user_id 是 VARCHAR(50) 的业务用户标识（如 'user-001'），
-- 而任务/定时任务/MCP/报告/渠道等表却把 user_id 定义为 BIGINT，
-- 与用户体系不匹配。Service 层对非数字 userId 执行 Long.valueOf(userId)
-- 会抛出 NumberFormatException，导致这些接口全部 500。
--
-- 修复：将相关表的 user_id 改为 VARCHAR(64)，与 sys_user / sys_models /
-- sys_token_usage 保持一致。
--
-- 本脚本可重复执行（幂等）。
-- ============================================================

USE `hivemind`;

-- ---------- 1. agent_tasks ----------
SET @ddl = IF(
  (SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='hivemind' AND TABLE_NAME='agent_tasks' AND COLUMN_NAME='user_id') = 'bigint',
  'ALTER TABLE `agent_tasks`
     MODIFY COLUMN `user_id` VARCHAR(64) NOT NULL COMMENT ''登录的用户ID（业务用户标识，与 sys_user.user_id 对齐）''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. task_reports ----------
SET @ddl = IF(
  (SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='hivemind' AND TABLE_NAME='task_reports' AND COLUMN_NAME='user_id') = 'bigint',
  'ALTER TABLE `task_reports`
     MODIFY COLUMN `user_id` VARCHAR(64) NOT NULL COMMENT ''任务所属的用户ID（业务用户标识）''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. scheduled_job ----------
SET @ddl = IF(
  (SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='hivemind' AND TABLE_NAME='scheduled_job' AND COLUMN_NAME='user_id') = 'bigint',
  'ALTER TABLE `scheduled_job`
     MODIFY COLUMN `user_id` VARCHAR(64) NOT NULL COMMENT ''任务所属的用户ID（业务用户标识）''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 4. scheduled_job_run_record ----------
SET @ddl = IF(
  (SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='hivemind' AND TABLE_NAME='scheduled_job_run_record' AND COLUMN_NAME='user_id') = 'bigint',
  'ALTER TABLE `scheduled_job_run_record`
     MODIFY COLUMN `user_id` VARCHAR(64) NOT NULL COMMENT ''任务所属的用户ID（业务用户标识）''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 5. sys_mcp ----------
SET @ddl = IF(
  (SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='hivemind' AND TABLE_NAME='sys_mcp' AND COLUMN_NAME='user_id') = 'bigint',
  'ALTER TABLE `sys_mcp`
     MODIFY COLUMN `user_id` VARCHAR(64) NOT NULL COMMENT ''所属的用户ID（业务用户标识）''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 6. sys_channels ----------
SET @ddl = IF(
  (SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='hivemind' AND TABLE_NAME='sys_channels' AND COLUMN_NAME='user_id') = 'bigint',
  'ALTER TABLE `sys_channels`
     MODIFY COLUMN `user_id` VARCHAR(64) NOT NULL COMMENT ''登录的用户ID（业务用户标识）''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 7. 存量数据回填 ----------
-- 早期若存在以数字 user_id 落库的历史数据（如 1、2），转换为 sys_user 中
-- 对应的业务 userId，避免与 VARCHAR 语义冲突。
UPDATE `agent_tasks` t
  JOIN `sys_user` u ON u.`id` = t.`user_id`
  SET t.`user_id` = u.`user_id`
WHERE t.`user_id` REGEXP '^[0-9]+$';

UPDATE `task_reports` t
  JOIN `sys_user` u ON u.`id` = t.`user_id`
  SET t.`user_id` = u.`user_id`
WHERE t.`user_id` REGEXP '^[0-9]+$';

UPDATE `scheduled_job` t
  JOIN `sys_user` u ON u.`id` = t.`user_id`
  SET t.`user_id` = u.`user_id`
WHERE t.`user_id` REGEXP '^[0-9]+$';

UPDATE `scheduled_job_run_record` t
  JOIN `sys_user` u ON u.`id` = t.`user_id`
  SET t.`user_id` = u.`user_id`
WHERE t.`user_id` REGEXP '^[0-9]+$';

UPDATE `sys_mcp` t
  JOIN `sys_user` u ON u.`id` = t.`user_id`
  SET t.`user_id` = u.`user_id`
WHERE t.`user_id` REGEXP '^[0-9]+$';

UPDATE `sys_channels` t
  JOIN `sys_user` u ON u.`id` = t.`user_id`
  SET t.`user_id` = u.`user_id`
WHERE t.`user_id` REGEXP '^[0-9]+$';
