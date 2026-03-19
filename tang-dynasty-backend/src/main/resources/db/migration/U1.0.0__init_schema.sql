-- Flyway Undo Migration: U1.0.0__init_schema.sql
-- Drop all created tables

DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_skill`;
DROP TABLE IF EXISTS `td_task_activity_log`;
DROP TABLE IF EXISTS `td_task_flow_log`;
DROP TABLE IF EXISTS `td_task`;
DROP TABLE IF EXISTS `sys_official`;
DROP TABLE IF EXISTS `sys_department`;
