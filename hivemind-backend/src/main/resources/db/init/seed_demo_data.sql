-- ============================================================
-- HiveMind 演示种子数据 (Demo Seed Data)
-- ------------------------------------------------------------
-- 目的：让部署后的控制台「一打开就有东西看」。
-- 特性：
--   1. 全部幂等（先按业务主键 DELETE 再 INSERT），可重复执行；
--   2. 时间全部相对 NOW() 生成，永远落在「最近 30 天」窗口内，
--      保证 Token 用量看板默认区间有数据；
--   3. 仅写入演示账号 user-001 的数据。
-- 执行：mysql -u root -p hivemind < seed_demo_data.sql
-- ============================================================

USE hivemind;

-- ---------- 1. Token 用量记录（最近 30 天，覆盖 3 个模型） ----------
DELETE FROM sys_token_usage
WHERE user_id = 'user-001'
  AND session_id LIKE 'seed-session-%';

INSERT INTO sys_token_usage
  (user_id, session_id, message_id, model_provider, model_name,
   input_tokens, output_tokens, total_tokens, cached_tokens, usage_time, created_at)
SELECT 'user-001',
       CONCAT('seed-session-', LPAD(1 + (n % 6), 2, '0')),
       CONCAT('seed-msg-', LPAD(n, 3, '0')),
       CASE n % 3 WHEN 0 THEN 'deepseek' WHEN 1 THEN 'MiMo' ELSE 'dashscope' END,
       CASE n % 3 WHEN 0 THEN 'deepseek-chat' WHEN 1 THEN 'mimo-v2.5-pro' ELSE 'qwen-max' END,
       1200 + (n * 137) % 8200,
       90 + (n * 31) % 1500,
       1200 + (n * 137) % 8200 + 90 + (n * 31) % 1500,
       CASE WHEN n % 4 = 0 THEN 320 + (n * 17) % 900 ELSE 0 END,
       NOW() - INTERVAL (n % 30) DAY - INTERVAL (n * 37 % 1440) MINUTE,
       NOW() - INTERVAL (n % 30) DAY - INTERVAL (n * 37 % 1440) MINUTE
FROM (
  SELECT (a.n + b.n * 10 + c.n * 100 + 1) AS n
  FROM
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
) seed_numbers;

-- ---------- 2. 模型供应商：补充一个「无需 API Key」的 Ollama 本地模型 ----------
DELETE FROM sys_models
WHERE user_id = 'user-001' AND model_provider_id = 'ollama';

INSERT INTO sys_models
  (user_id, model_provider_id, provider_name, model_provider_type, is_provider_activated,
   is_system_built_in, base_url, api_key, models_json, model_id, model_name)
VALUES
  ('user-001', 'ollama', 'Ollama', 'SYSTEM', 0, 1,
   'http://localhost:11434/v1', NULL,
   '[{"id":"qwen2.5:7b","name":"Qwen2.5 7B (本地)","supportsMultimodal":false,"supportsVideo":false}]',
   'qwen2.5:7b', 'Qwen2.5 7B (本地)');

-- ---------- 3. MCP 服务配置 ----------
DELETE FROM sys_mcp WHERE user_id = 'user-001';

INSERT INTO sys_mcp
  (user_id, mcp_server, mcp_server_type, mcp_tool, is_server_activated, is_tool_activated, tool_param_config)
VALUES
  ('user-001', 'reme-memory', 'SYSTEM', 'search/read/write/auto_memory', 1, 1,
   '{"timeoutSeconds":60,"topK":5}'),
  ('user-001', 'playwright-mcp', 'CUSTOM', 'browser_navigate/browser_click/browser_snapshot', 1, 1,
   '{"headless":true,"viewport":"1280x720"}'),
  ('user-001', 'github-mcp', 'CUSTOM', 'search_repositories/get_file_contents/create_issue', 0, 0,
   '{"repo":"liangshoux/hivemind"}');

-- ---------- 4. 智能体任务（多角色协作状态机） ----------
DELETE FROM agent_tasks WHERE user_id = 'user-001' AND task_id LIKE 'demo-task-%';

INSERT INTO agent_tasks
  (task_id, session_id, user_id, title, official_id, state, priority, block_reason,
   review_round, prev_state, output_result, ac_criteria, archived, deleted, create_time, update_time)
VALUES
  ('demo-task-001', 'seed-session-01', 'user-001', '分析 Q3 销售数据并生成经营简报',
   1, 'DONE', 'high', NULL, 1, 'AGENT_REVIEWER',
   '已完成 3 个区域的数据清洗与归因分析，输出 8 条可执行结论，其中 3 条与库存周转直接相关。',
   '包含同比/环比对比、区域排名、异常点说明；结论需附带数据来源。', 0, 0,
   NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),

  ('demo-task-002', 'seed-session-02', 'user-001', '排查网关 5xx 突增并输出根因报告',
   1, 'AGENT_REVIEWER', 'urgent', NULL, 1, 'AGENT_PLANNER',
   NULL,
   '需定位到具体服务与代码路径，给出复现步骤与修复建议。', 0, 0,
   NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 1 HOUR),

  ('demo-task-003', 'seed-session-03', 'user-001', '为新同事编写 Agent 平台上手指南',
   1, 'AGENT_PLANNER', 'normal', NULL, 0, 'AGENT_TRIAGE',
   NULL,
   '覆盖登录、模型配置、技能库、工具库、用量统计五个模块，附可复制命令。', 0, 0,
   NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),

  ('demo-task-004', 'seed-session-04', 'user-001', '重构 Token 用量统计接口的聚合逻辑',
   1, 'DOING', 'normal', NULL, 0, 'ASSIGNED',
   NULL,
   '聚合 SQL 与 DTO 映射一致；单元测试覆盖 summary/by-model/by-provider/by-date 四条路径。', 0, 0,
   NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 HOUR),

  ('demo-task-005', 'seed-session-05', 'user-001', '评估 MCP 服务器接入方案的兼容性风险',
   1, 'PENDING', 'normal', '等待安全团队评审沙箱权限策略', 0, NULL,
   NULL,
   '覆盖 SSE 与 streamable-http 两种传输的兼容性矩阵。', 0, 0,
   NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR),

  ('demo-task-006', 'seed-session-06', 'user-001', '整理竞品多 Agent 协作方案对比',
   1, 'FINISH', 'low', NULL, 2, 'DONE',
   '已完成 4 家方案的对比矩阵，结论：差异化应放在「角色编排 + 工具治理」而非模型能力。',
   '对比维度含编排模型、记忆机制、工具安全、成本控制。', 0, 0,
   NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 10 DAY);

-- ---------- 5. 任务报告（评审产出物） ----------
DELETE FROM task_reports WHERE user_id = 'user-001' AND task_id LIKE 'demo-task-%';

INSERT INTO task_reports
  (user_id, task_id, task_title, task_content, task_result, approval_state, delivery_time)
VALUES
  ('user-001', 'demo-task-001', '分析 Q3 销售数据并生成经营简报',
   '按区域拆解销售数据，定位增速下滑的 3 个主要品类。',
   '结论：华东区毛利率环比下降 3.2pct，主因是折扣策略；建议在 Q4 恢复阶梯定价。',
   'APPROVED', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR),

  ('user-001', 'demo-task-002', '排查网关 5xx 突增并输出根因报告',
   '结合网关访问日志与下游链路追踪，定位异常时间窗。',
   '根因初步定位在订单服务的连接池配置，修复方案已提交待评审。',
   'WAITING', NOW() - INTERVAL 30 MINUTE),

  ('user-001', 'demo-task-006', '整理竞品多 Agent 协作方案对比',
   '收集公开资料并整理成统一维度的对比矩阵。',
   '输出 1 份对比矩阵文档，含 4 家方案、12 个评估维度。',
   'APPROVED', NOW() - INTERVAL 10 DAY);

-- ---------- 6. 定时任务 ----------
DELETE FROM scheduled_job WHERE user_id = 'user-001' AND job_id LIKE 'demo-job-%';

INSERT INTO scheduled_job
  (job_id, job_name, user_id, is_activated, cron_config, time_zone, job_description,
   job_require_content, job_drived_task_id, create_time, update_time)
VALUES
  ('demo-job-daily-standup', '每日站会总结', 'user-001', 1, '0 30 9 * * *', 'Asia/Shanghai',
   '每天 09:30 自动汇总团队站会记录并生成摘要',
   '{"prompt":"汇总今日站会结论、阻塞项与责任人，输出不超过 5 条要点"}',
   NULL, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 1 DAY),

  ('demo-job-token-audit', 'Token 用量周报', 'user-001', 1, '0 0 18 * * 1', 'Asia/Shanghai',
   '每周一 18:00 统计上周各模型用量与费用，异常时告警',
   '{"prompt":"统计上周 Token 用量 Top 模型，费用环比增幅超过 30% 时给出说明"}',
   NULL, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 2 DAY),

  ('demo-job-sandbox-check', '沙箱健康巡检', 'user-001', 0, '0 */15 * * * *', 'Asia/Shanghai',
   '每 15 分钟检查沙箱容器存活状态，异常时触发清理',
   '{"prompt":"检查所有沙箱容器状态，回收已停止容器"}',
   NULL, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 3 DAY);

-- ---------- 7. 定时任务执行记录 ----------
DELETE FROM scheduled_job_run_record WHERE user_id = 'user-001' AND job_id LIKE 'demo-job-%';

INSERT INTO scheduled_job_run_record
  (user_id, job_id, job_start_time, job_finish_time, duration_ms)
SELECT 'user-001', j.job_id,
       NOW() - INTERVAL (n * 7 + 2) HOUR,
       NOW() - INTERVAL (n * 7 + 2) HOUR + INTERVAL (35 + n * 11) SECOND,
       (35 + n * 11) * 1000
FROM (
  SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) seed_run
CROSS JOIN (SELECT 'demo-job-daily-standup' job_id UNION ALL SELECT 'demo-job-token-audit') j;

-- ---------- 8. 云端技能（OSS 元数据） ----------
DELETE FROM skills_meta_manage WHERE user_id = 'user-001' AND skill_id LIKE 'demo-skill-%';

INSERT INTO skills_meta_manage
  (skill_id, user_id, name, description, current_version, status, tags, dependencies,
   execution_env, file_manifest, created_at, updated_at)
VALUES
  ('demo-skill-report', 'user-001', 'report-writer',
   '把原始数据与结论整理成结构化经营报告，输出摘要、指标表与行动建议。',
   '2.1.0', 'published', '["报告","分析"]', '[]',
   '{"runtime":"python3.11","timeoutSeconds":120}',
   '[{"path":"SKILL.md","size":2048},{"path":"templates/report.md","size":1024}]',
   NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 2 DAY),

  ('demo-skill-incident', 'user-001', 'incident-reviewer',
   '对线上故障线索做分级归因，输出复现步骤、影响面与修复优先级。',
   '1.3.0', 'published', '["运维","排障"]', '[]',
   '{"runtime":"python3.11","timeoutSeconds":180}',
   '[{"path":"SKILL.md","size":3072}]',
   NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 6 DAY),

  ('demo-skill-onboarding', 'user-001', 'platform-onboarding',
   '为新成员生成 Agent 平台上手路径，按角色裁剪内容并附带可复制命令。',
   '0.9.0', 'draft', '["文档","入职"]', '[]',
   '{"runtime":"python3.11","timeoutSeconds":90}',
   '[{"path":"SKILL.md","size":1536}]',
   NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY),

  ('demo-skill-legacy', 'user-001', 'legacy-etl',
   '历史遗留的批量 ETL 技能，已被新的数据管道方案取代。',
   '1.0.0', 'archived', '["ETL","历史"]', '[]',
   '{"runtime":"python3.11","timeoutSeconds":60}',
   '[{"path":"SKILL.md","size":768}]',
   NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 15 DAY);
