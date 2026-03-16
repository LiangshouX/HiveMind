# 后端详细设计（可落地版）——三省六部多 Agent 协作个人助理

面向文档与实现约束：

- 需求文档：[require.md](file:///d:/Code/Java/TangDynasty/require.md)
- CoPaw Agent 详设：[CoPaw_Agent_项目详设文档.md](file:///d:/Code/Java/TangDynasty/CoPaw_Agent_项目详设文档.md)
- Edict Backend 详设：[Edict_Backend_项目详设文档.md](file:///d:/Code/Java/TangDynasty/Edict_Backend_项目详设文档.md)
- 现有后端工程（Spring Boot）：[tang-dynasty-backend](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/)
- 现有前端工程（React）：[website](file:///d:/Code/Java/TangDynasty/website/)

约束与目标（关键）：

1. “Trae Agent / CoPaw / Edict”均 **零改动集成**：后端通过配置与适配层对接，不能要求改动 CoPaw/Edict 源码。  
2. 对前端 `website/src/api.ts` 已存在的接口形态 **保持兼容**，新增接口按版本/能力扩展，不破坏现有。  
3. 文档覆盖 **15 个菜单模块 + 1 个公共模块**（合计 16 个“核心模块”）。其中菜单模块来自需求清单（上朝、旨意库、频道、旨意看板、奏折、定时任务、朝纲、技能库、工具库、MCP、官员管理、模型、环境变量、御林军、大司农）；公共模块为“鉴权与审计（用户/租户/权限/审计）”，用于承载 OAuth2.1/JWT/RBAC/审计日志等横切需求。  
4. 输出物：  
   - `docs/backend_detail_design.md`（本文）  
   - `docs/openapi.yaml`（OpenAPI 3.1，可供前端导入生成 Mock）  
   - `docs/diagrams/*.drawio` 与 `docs/diagrams/*.png`（ER/架构/时序）  

***

## 0. 术语与角色映射

业务隐喻与系统角色对应（来自 [require.md](file:///d:/Code/Java/TangDynasty/require.md)）：

- 皇上：用户（User）
- 丞相（ChengXiang）：消息接入与分拣/路由（闲聊 vs 旨意）
- 中书省（Zhongshu）：方案规划与拆解（Todos）
- 门下省（Menxia）：审议与封驳（最多 3 轮）
- 尚书省（Shangshu）：派发执行、汇总复奏
- 六部（礼/户/兵/刑/工/吏）：并行执行子任务

系统中的“官员/官职”即 Agent 的配置实例，包含：提示词（SOUL/PROFILE/AGENTS）、模型配置、技能/工具权限、输出限制等。

***

## 1. 总体架构（确保零改动集成）

### 1.1 组件拓扑

建议采用“模块化单体 + 外部能力适配”的落地形态：

- **Spring Boot 主后端（tang-dynasty-backend）**：  
  - 对前端暴露统一 API（含 WebSocket）  
  - 管理 DB（MySQL）与缓存（Redis）  
  - 负责鉴权、RBAC、审计、限流、幂等等横切能力  
  - 作为 **CoPaw/Edict 的集成网关**（HTTP/WS/事件桥接）

- **CoPaw（Python Agent Runtime）**：不修改源码，保持其运行与 API 形态。Spring Boot 通过“CoPaw Adapter”访问其：技能/工具/MCP/模型/环境变量/安全/TokenUsage 等能力（详见 CoPaw 详设）。

- **Edict Engine（事件驱动编排与派发）**：不修改源码，保持 `edict-backend` 的事件总线/worker。Spring Boot 通过“Edict Adapter”订阅/查询任务事件，并将其投射为前端所需的看板数据与实时推送（详见 Edict 详设）。

### 1.2 系统架构图（文件）

- 系统架构图（Drawio/PNG）：  
  - `docs/diagrams/system_architecture.drawio`  
  - `docs/diagrams/system_architecture.png`

### 1.3 集成边界与兼容策略

为确保“零改动集成”，所有外部系统交互必须通过后端可配置的适配器完成：

- CoPaw Adapter（HTTP）：  
  - 基地址：`COPAW_BASE_URL`（例如 `http://127.0.0.1:8088`）  
  - 主要调用：`/api/tools`、`/api/skills/*`、`/api/mcp/*`、`/api/models/*`、`/api/envs/*`、`/api/token_usage/*`（以 CoPaw 实际路由为准）
  - 失败降级：读写本地 DB（sys_tool/sys_skill/sys_mcp/sys_model/sys_config/sys_env_var），并标记 `syncStatus.ok=false`

- Edict Adapter（事件桥接）：  
  - Redis Streams/HTTP 任选其一：  
    - 若 Edict 以 Redis Streams 为主：Spring Boot 订阅 `edict:pubsub:*` 实时转发至前端 WS  
    - 若 Edict 提供 HTTP 查询（如 `GET /api/tasks`）：Spring Boot 定时/按需拉取并写入本地任务表作为缓存
  - 关键目标：将 Edict 事件模型投射为前端既有的 `live-status/task-activity/scheduler-state` 等接口结构

***

## 2. 领域模型（ER 图、字段、索引、约束）

### 2.1 存储选型与版本锁定

- 关系型数据库：MySQL 8.0.36（InnoDB，utf8mb4）  
- 缓存/分布式锁：Redis 7.2.x  
- 搜索（可选）：OpenSearch 2.x 或 Elasticsearch 8.x（用于审计日志/事件检索）  

现有初始化脚本参考：[init.sql](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/init.sql)

### 2.2 ER 图（文件）

- ER 图（Drawio/PNG）：  
  - `docs/diagrams/er.drawio`  
  - `docs/diagrams/er.png`

### 2.3 核心表清单（覆盖 16 模块）

说明：以下是“可直接落地”的建议表结构；其中 `sys_*` 命名与现有脚本保持一致，并在不足处补充扩展表。字段类型均为 MySQL 可执行单位。

#### 2.3.1 用户/鉴权/审计（公共模块，模块 16）

- `sys_user`：用户（皇上/管理员/普通用户）
- `sys_role`：角色（RBAC）
- `sys_permission`：权限点（细粒度到接口级）
- `sys_user_role`、`sys_role_permission`：关联表
- `sys_audit_log`：审计日志（安全、配置、审批、导出等）
- `sys_idempotency_key`：幂等键存储（防重入/重试）

#### 2.3.2 上朝（聊天/会话）

沿用/扩展现有：

- `sys_conversation`：会话
- `sys_message`：消息（支持 role + meta）

#### 2.3.3 旨意库（模板）

新增：

- `sys_edict_template`：旨意模板（分类/参数 schema/预估 token/费用）
- `sys_edict_template_version`：模板版本（灰度与回滚）

#### 2.3.4 旨意看板（任务）

沿用/扩展现有：

- `sys_task`：任务/旨意实例（主表）
- `sys_task_log`：流转与进度日志（flow/progress/review）
- `sys_task_scheduler`：调度元数据（分布式调度/重试/升级）
- `sys_memorial`：奏折（任务产出归档，见 2.3.5）

#### 2.3.5 奏折（产出物）

新增：

- `sys_memorial`：奏折正文/引用附件/状态（draft/submitted/approved）
- `sys_attachment`：附件（文件元信息，存对象存储 URL）

#### 2.3.6 频道（消息通道）

沿用/扩展现有：

- `sys_channel`：渠道配置（密钥必须加密存储，见安全方案）
- `sys_channel_delivery_log`：投递日志（用于排障与审计）

#### 2.3.7 定时任务

新增：

- `sys_job`：定时任务定义（Cron/一次性/事件触发）
- `sys_job_run`：运行记录（开始/结束/错误/耗时/重试）

#### 2.3.8 朝纲（工作区/规范）

新增：

- `sys_workspace_file`：工作区文件（SOUL.md/AGENTS.md/PROFILE.md/HEARTBEAT.md 等）
- `sys_court_rule`：朝纲规则（如工具审批策略、访问控制）

#### 2.3.9 技能库 / 工具库 / MCP / 模型 / 环境变量

沿用/扩展现有：

- `sys_skill`、`sys_tool`、`sys_mcp`、`sys_model`、`sys_config`
- 新增 `sys_env_var`：环境变量（Secret 必须加密/脱敏）

#### 2.3.10 官员管理

沿用/扩展现有：

- `sys_official`：官员（Agent 实例配置）
- 新增 `sys_official_policy`：权限/限额/安全策略（工具白名单、审批阈值等）
- 新增 `sys_official_prompt`：提示词文件引用/版本（可与 workspace_file 关联）

#### 2.3.11 御林军（安全）

新增：

- `sys_security_policy`：全局安全策略（RBAC、工具审批、黑白名单）
- `sys_access_token`：可选（如需要黑名单/撤销）

#### 2.3.12 大司农（成本与 token）

沿用/扩展现有：

- `sys_token_usage`：按天/模型聚合
- 新增 `sys_token_usage_raw`：按请求粒度明细（用于审计/核算/对账）

### 2.4 MySQL DDL（可直接执行）

以下脚本为“可运行单元”，建议作为 `V1__init.sql`（Flyway）或 `001_init.sql`（Liquibase）执行。版本锁定：Flyway 10.18.2 / Liquibase 4.29.2（二选一）。

```sql
-- MySQL 8.0.36 / InnoDB / utf8mb4
CREATE DATABASE IF NOT EXISTS `tang_dynasty` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `tang_dynasty`;

-- ========== 用户与权限（模块16） ==========
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `display_name` VARCHAR(128) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `resource` VARCHAR(256) NOT NULL,
  `action` VARCHAR(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`code`),
  KEY `idx_resource_action` (`resource`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(64) NOT NULL,
  `actor_user_id` BIGINT DEFAULT NULL,
  `actor_ip` VARCHAR(64) DEFAULT NULL,
  `action` VARCHAR(64) NOT NULL,
  `resource` VARCHAR(256) NOT NULL,
  `payload` JSON DEFAULT NULL,
  `result` VARCHAR(16) NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_actor_time` (`actor_user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_idempotency_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `key_hash` CHAR(64) NOT NULL,
  `request_fingerprint` CHAR(64) NOT NULL,
  `response_body` JSON DEFAULT NULL,
  `http_status` INT NOT NULL DEFAULT 200,
  `expire_time` DATETIME NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_hash` (`key_hash`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 上朝（会话/消息） ==========
-- 复用现有 sys_conversation/sys_message（见 init.sql），此处略。

-- ========== 旨意库（模板） ==========
CREATE TABLE IF NOT EXISTS `sys_edict_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_key` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `category` VARCHAR(64) NOT NULL,
  `param_schema` JSON NOT NULL,
  `prompt_template` LONGTEXT NOT NULL,
  `estimate_tokens` INT NOT NULL DEFAULT 0,
  `estimate_cost_cny` DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_key` (`template_key`),
  KEY `idx_category_enabled` (`category`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_edict_template_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL,
  `version` INT NOT NULL,
  `param_schema` JSON NOT NULL,
  `prompt_template` LONGTEXT NOT NULL,
  `changelog` TEXT DEFAULT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_version` (`template_id`,`version`),
  KEY `idx_template_active` (`template_id`,`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 奏折（产出） ==========
CREATE TABLE IF NOT EXISTS `sys_memorial` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(64) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `content_md` LONGTEXT NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_status_time` (`status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_type` VARCHAR(32) NOT NULL,
  `owner_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `mime` VARCHAR(128) NOT NULL,
  `size_bytes` BIGINT NOT NULL,
  `url` TEXT NOT NULL,
  `sha256` CHAR(64) NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_type`,`owner_id`),
  UNIQUE KEY `uk_sha256` (`sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 定时任务 ==========
CREATE TABLE IF NOT EXISTS `sys_job` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `job_key` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(16) NOT NULL,
  `cron` VARCHAR(64) DEFAULT NULL,
  `payload` JSON DEFAULT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_key` (`job_key`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_job_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `job_id` BIGINT NOT NULL,
  `run_id` VARCHAR(64) NOT NULL,
  `status` VARCHAR(16) NOT NULL,
  `started_at` DATETIME DEFAULT NULL,
  `ended_at` DATETIME DEFAULT NULL,
  `error_message` TEXT DEFAULT NULL,
  `metrics` JSON DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_run` (`job_id`,`run_id`),
  KEY `idx_job_status_time` (`job_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 朝纲（工作区/规则） ==========
CREATE TABLE IF NOT EXISTS `sys_workspace_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `path` VARCHAR(255) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `etag` CHAR(64) NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_path` (`path`),
  KEY `idx_etag` (`etag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_court_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_key` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_key` (`rule_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 环境变量（Secret） ==========
CREATE TABLE IF NOT EXISTS `sys_env_var` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `key_name` VARCHAR(128) NOT NULL,
  `value_ciphertext` LONGTEXT NOT NULL,
  `value_masked` VARCHAR(256) NOT NULL,
  `scope` VARCHAR(16) NOT NULL DEFAULT 'GLOBAL',
  `owner_id` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_scope_owner` (`key_name`,`scope`,`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

***

## 3. API 设计（REST + WebSocket，鉴权/限流/幂等/版本）

### 3.1 基础约定（与现有前端兼容）

前端当前约定后端响应形态为 `{ code, data, message }`（见 [api.ts](file:///d:/Code/Java/TangDynasty/website/src/api.ts#L27-L33)），现有后端统一包装类为 [Result.java](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/common/Result.java)。

- **成功**：HTTP 200，`code=200`，`data` 为业务负载
- **业务失败**：HTTP 200 或 4xx，`code!=200`，`message` 为原因（建议统一为 4xx 并保留 code）
- **错误码规范**：见 8.4

### 3.2 鉴权（OAuth2.1 + JWT）

要求：OAuth2.1 + JWT，并能兼容现有前端 `Authorization: Bearer <token>` 注入（[api.ts:L12-L20](file:///d:/Code/Java/TangDynasty/website/src/api.ts#L12-L20)）。

落地建议（版本锁定）：

- Spring Authorization Server 1.4.2（OAuth2.1）
- JWT：RS256，Key Rotation（KMS 或 Secret 管理）
- Token TTL：Access 15min，Refresh 7d（可配置）
- 接口鉴权：除 `/health`、`/api`、`/api/chat/completions`（可选匿名）外均需鉴权

OpenAPI 中的 `securitySchemes` 必须包含 `bearerAuth` 与 `oauth2`（见 `docs/openapi.yaml`）。

### 3.3 限流（接口级 + 用户级）

版本锁定（建议）：

- Bucket4j 8.10.1（本地/Redis 令牌桶）

策略：

- 默认：用户级 60 req/min，IP 级 120 req/min
- 高成本接口（如 chat/completions、create-task、scheduler-scan）：单用户 10 req/min
- 管理接口（官员管理/模型/环境变量/安全）：需要管理员角色且更低频

返回头：

- `X-RateLimit-Limit` / `X-RateLimit-Remaining` / `X-RateLimit-Reset`

### 3.4 幂等（Idempotency-Key）

对以下接口强制幂等：

- 创建类：`POST /api/create-task`、`POST /api/chat/completions`（可选）、`POST /api/mcp`、`POST /api/models`、`POST /api/tools` 等

约定：

- 请求头：`Idempotency-Key: <uuid>`
- 服务端记录：`sys_idempotency_key`（hash key + request fingerprint）
- 语义：同 key + 同 fingerprint 返回首次响应；同 key + fingerprint 不同返回 409

### 3.5 版本号策略（多版本共存）

为零改动兼容，现有路径保留 `/api/*`；新增能力采用以下策略之一：

- Path 版本：`/api/v1/*`（新增） + `/api/*`（兼容保留）
- 或 Header 版本：`X-Api-Version: 1`（推荐用于网关内部）

本次 OpenAPI 规范以 `/api/*` 为主，另提供 `/api/v1/*` 的别名（`x-alias` 标注）。

### 3.6 WebSocket 设计

前端需要实时能力的模块：旨意看板、奏折状态、定时任务运行、Token 消耗更新、审批事件等。

统一 WS：

- `GET /ws`：订阅所有事件（topic 维度）
- `GET /ws/task/{taskId}`：订阅单任务事件

消息 envelope（JSON）：

```json
{
  "type": "event",
  "topic": "task.status",
  "traceId": "uuid-or-taskId",
  "ts": "2026-03-17T12:00:00Z",
  "data": { "taskId": "JJC-...", "from": "...", "to": "...", "payload": {} }
}
```

***

## 4. 服务层拆分（模块边界、依赖、事件流）

### 4.1 形态选择：模块化单体（推荐落地）

在 Spring Boot 单体内做清晰模块边界（符合 require.md 的分层要求），并保留未来拆微服务的演进点。

模块划分（与 16 模块一一对应）：

1) morning-court（上朝）  
2) edict-library（旨意库）  
3) channels（频道）  
4) edict-board（旨意看板）  
5) memorials（奏折）  
6) scheduler（定时任务）  
7) censorate（朝纲）  
8) skills（技能库）  
9) tools（工具库）  
10) mcp（MCP）  
11) officials（官员管理）  
12) models（模型）  
13) envs（环境变量）  
14) guards（御林军/安全）  
15) finance（大司农/成本）  
16) iam-audit（鉴权/审计）

依赖方向（必须单向）：

- adapter/controller → service → infrastructure.datasource.support → infrastructure.datasource.mapper/po
- service 之间通过接口协作，跨模块事件通过 EventBus（见第 5 节）

### 4.2 数据访问隔离（硬性要求）

根据 require.md“重要要求”：

- 任何 CRUD 均下沉到 `infrastructure.datasource.support`（不可在 controller/service 直接写 SQL/mapper）。  
- service 只编排业务规则与事务边界。  

现有工程已存在 mapper/po 与 service/impl；后续落地时在 `support` 中引入 Repository 风格封装以满足隔离约束。

***

## 5. 事件总线与消息格式（Kafka/RabbitMQ Topic、Schema、死信）

### 5.1 选型与双通道策略

为满足“可靠编排 + 实时推送”，建议双通道：

1) **Kafka（主总线，可靠）**：用于领域事件、跨模块解耦、回放与审计  
2) **Redis Pub/Sub（实时通道）**：用于 WebSocket 推送（低延迟、无需回放）

版本锁定：

- Kafka 3.8.x（KRaft 模式）
- Schema Registry：Confluent 7.7.x 或 Apicurio 3.x（任选）

若当前阶段不引入 Kafka，可用 Redis Streams（参考 Edict）作为过渡；但需保留 DLQ 与可观测性。

### 5.2 Topic 规划（可直接落地）

命名：`td.<module>.<event>`，并按环境/租户加前缀（可选）。

- `td.task.created` / `td.task.state_changed` / `td.task.archived`
- `td.dispatch.requested` / `td.dispatch.started` / `td.dispatch.completed` / `td.dispatch.failed`
- `td.memorial.generated` / `td.memorial.submitted`
- `td.tool.policy_changed` / `td.security.policy_changed`
- `td.token.usage_recorded`

死信：

- `td.dlq.<originTopic>`

### 5.3 消息 Schema（JSON Schema，OpenAPI 对齐）

统一 envelope：

```json
{
  "eventId": "uuid",
  "traceId": "uuid-or-taskId",
  "occurredAt": "2026-03-17T12:00:00Z",
  "topic": "td.task.state_changed",
  "producer": "tang-dynasty-backend",
  "version": 1,
  "payload": {}
}
```

payload 示例（task.state_changed）：

```json
{
  "taskId": "JJC-20260317-abcd",
  "from": "Menxia",
  "to": "Assigned",
  "reason": "准奏",
  "reviewRound": 2
}
```

### 5.4 死信策略（DLQ）

触发条件：

- 消费失败且超过重试阈值（默认 5 次）
- payload 校验失败（schema 不匹配）
- 幂等冲突不可恢复（如重复事件且业务不允许）

处理：

- 投递到 `td.dlq.*` 并写入 `sys_audit_log`（action=DLQ）
- 管理端提供“重放/忽略/修复后重放”能力（御史台/御林军模块）

***

## 6. 定时任务调度（分布式锁、分片、失败重试、告警）

### 6.1 选型

推荐两级调度：

- 轻量周期任务：Spring Scheduler + ShedLock（Redis）  
- 复杂任务编排：Quartz Cluster（DB 持久化）  

版本锁定：

- Quartz 2.3.2
- ShedLock 5.16.0

### 6.2 分布式锁与分片

- 分布式锁：Redis `SET key value NX PX ttl`（ShedLock 已封装）
- 分片：对 `taskId` 做一致性哈希映射到 `shardId`，每个实例负责一部分任务扫描/重试，避免全量扫描

### 6.3 失败重试与监控

- 重试：指数退避（1m/2m/4m/8m… capped 30m），最大重试次数 `maxRetry`（默认 3）
- 监控：  
  - job_run 记录入库（sys_job_run）  
  - Prometheus 指标（见第 8 节）  
  - 告警：连续失败、长时间无心跳、DLQ 堆积

***

## 7. 缓存与一致性（Redis 结构、淘汰、穿透防护）

### 7.1 Redis Key 规划

- 会话/消息：
  - `td:conv:{sessionId}`（hash）
  - `td:msg:{sessionId}`（list，最近 N 条）
- 看板：
  - `td:task:live_status`（string/json，TTL 2s~5s）
  - `td:task:{taskId}`（hash，TTL 60s）
- 配置：
  - `td:cfg:{key}`（string/json，TTL 300s）
- 限流：
  - `td:rl:{userId}:{route}`（bucket4j）
- 幂等：
  - `td:idem:{keyHash}`（TTL 与 db 一致）

### 7.2 一致性策略

- 写路径：DB 为准，写后发事件 → 消费者更新缓存（Cache-Aside + Event Invalidate）
- 读路径：优先缓存，miss 回源 DB 并回填
- 防穿透：  
  - 布隆过滤器（RedisBloom 或本地 Guava Bloom）用于 taskId/edictTemplateKey  
  - 空值缓存（TTL 30s）用于不存在记录

### 7.3 多级缓存

- L1：应用内 Caffeine（TTL 1s~5s）用于极热接口（live-status、agent-config）
- L2：Redis（TTL 60s~300s）

***

## 8. 安全方案（OAuth2.1 + JWT、RBAC、审计、脱敏）

### 8.1 RBAC 粒度到接口级

权限点示例：

- `TASK_READ`：GET `/api/live-status`、GET `/api/task-activity/*`
- `TASK_WRITE`：POST `/api/create-task`、POST `/api/task-action`
- `MODEL_ADMIN`：POST `/api/models`
- `ENV_ADMIN`：POST `/api/envs/*`
- `SECURITY_ADMIN`：POST `/api/security/policies/*`

实现：

- Spring Security Method Security：`@PreAuthorize("hasAuthority('TASK_WRITE')")`

### 8.2 审计日志（必做）

记录范围：

- 任何配置变更（渠道/技能/工具/MCP/模型/官员/安全/环境变量）
- 任何“执行/派发/重试/升级/回滚”
- 任何导出（奏折导出、配置导出）

写入：

- DB：`sys_audit_log`
- 结构化日志：JSON（见 8.3）

### 8.3 脱敏规则

- env var：只返回 `value_masked`（如 `sk-****abcd`），原值仅在服务端解密使用
- 渠道密钥、OAuth client_secret、webhook：同上
- 审计日志：对 payload 中敏感字段递归脱敏（key 命中 `password|token|secret|apiKey`）

### 8.4 错误码规范（与 Result 兼容）

HTTP + 业务码双层：

- HTTP：语义化（401/403/404/409/429/500）
- 业务码（示例，放入 `ErrorCode` 枚举，见 [ErrorCode.java](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/common/ErrorCode.java)）：
  - `AUTH_INVALID_TOKEN`
  - `RBAC_FORBIDDEN`
  - `IDEMPOTENCY_CONFLICT`
  - `RATE_LIMITED`
  - `DOWNSTREAM_COPAW_UNAVAILABLE`
  - `DOWNSTREAM_EDICT_UNAVAILABLE`

***

## 9. 可观测性（Prometheus、OTel、结构化日志、告警）

版本锁定：

- Spring Boot 3.3.x + Micrometer 1.13.x
- OpenTelemetry Java Agent 2.x（或 SDK 集成）
- Prometheus 2.54.x + Grafana 11.x

指标（示例）：

- `http_server_requests_seconds_count/sum`（按 route/status）
- `td_task_dispatch_total{result=success|failed|timeout}`
- `td_eventbus_consumer_lag{topic=...}`
- `td_dlq_total{topic=...}`
- `td_token_usage_tokens_total{model=...}`

链路追踪：

- `traceId` 贯穿：HTTP → service → event publish/consume → 下游（CoPaw/Edict）
- 对外请求统一注入 `traceparent` 与 `X-Trace-Id`

结构化日志：

- JSON 输出字段：`ts, level, traceId, spanId, userId, route, latencyMs, errorCode`

告警（示例）：

- 5min 内 5xx 比例 > 2%
- DLQ 堆积 > 100
- scheduler 任务连续失败 > 3 次
- 下游 CoPaw 探活失败持续 1min

***

## 10. 容量评估与性能基线（QPS、RT99、扩容阈值、压测脚本）

初始基线（可落地，可按实际调整）：

- `GET /api/live-status`：RT99 < 200ms，QPS 200，缓存命中率 > 95%
- `POST /api/chat/completions`：RT99 < 8s（取决于模型），并发 20
- `POST /api/create-task`：RT99 < 300ms，QPS 50，幂等覆盖 100%

扩容阈值（HPA）：

- CPU > 70% 持续 5min 或 p95 > 300ms（非 LLM 接口）

压测脚本（k6，版本 0.52.0）示例：

```js
import http from "k6/http";
import { sleep } from "k6";

export const options = {
  vus: 50,
  duration: "2m",
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(99)<200"],
  },
};

export default function () {
  http.get("http://localhost:8080/api/live-status");
  sleep(0.2);
}
```

***

## 11. 灰度与回滚（FeatureFlag、DB 零停机迁移、多版本共存）

Feature Flag：

- 表：`sys_config` 中以 `ff.<feature>` 存储开关
- 发布策略：按用户/角色/租户灰度（可扩展到 `sys_feature_flag`）

DB 零停机迁移：

- 约束：先加字段/表（兼容旧代码）→ 灰度启用新代码写双写 → 回填 → 切读 → 清理旧字段

接口多版本：

- 新能力走 `/api/v1/*` 或 `X-Api-Version`，旧接口保留

***

## 12. 测试（单元/集成/契约）与 CI 门禁（覆盖率≥90%）

目标：单元+集成+契约综合覆盖率 ≥ 90%，作为 MR 门禁。

版本锁定：

- JUnit 5.10.x
- Mockito 5.x
- Testcontainers 1.20.x（MySQL/Redis/Kafka）
- Pact 4.x（契约测试）
- JaCoCo 0.8.12

测试分层：

- 单元测试：service/support 层（无 DB）
- 集成测试：controller + DB（Testcontainers）
- 契约测试：OpenAPI 作为 source of truth，前端 mock 与后端实现对齐

GitLab CI（示例）：

```yaml
stages: [lint, test, it, contract, build]

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

lint:
  stage: lint
  image: maven:3.9.9-eclipse-temurin-21
  script:
    - mvn -q -pl tang-dynasty-backend -DskipTests=true spotless:check

unit_test:
  stage: test
  image: maven:3.9.9-eclipse-temurin-21
  script:
    - mvn -pl tang-dynasty-backend test jacoco:report
  artifacts:
    when: always
    reports:
      junit: tang-dynasty-backend/target/surefire-reports/TEST-*.xml
  coverage: '/TOTAL.*?(\d+\%)$/'

integration_test:
  stage: it
  image: maven:3.9.9-eclipse-temurin-21
  services:
    - name: docker:dind
  script:
    - mvn -pl tang-dynasty-backend -DskipUTs=true verify

contract_test:
  stage: contract
  image: maven:3.9.9-eclipse-temurin-21
  script:
    - mvn -pl tang-dynasty-backend -DskipTests=false -Pcontract-test test

build:
  stage: build
  image: maven:3.9.9-eclipse-temurin-21
  script:
    - mvn -pl tang-dynasty-launcher -DskipTests package
```

覆盖率门禁：

- JaCoCo rule：instruction coverage >= 0.90；低于阈值直接 fail pipeline

***

## 13. 部署拓扑（K8s Helm、ConfigMap/Secret、HPA、PDB）

版本锁定：

- Kubernetes 1.30.x
- Helm 3.16.x

拓扑：

- tang-dynasty-backend Deployment（2~10 副本，HPA）
- tang-dynasty-launcher（如为聚合启动器，可做 1 副本或合并到 backend）
- MySQL（建议托管 RDS 或 Operator）
- Redis（哨兵或集群）
- Kafka（可选，生产建议独立集群）

关键资源：

- ConfigMap：非敏感配置（端口、feature flags 默认值）
- Secret：DB 密码、JWT 私钥、CoPaw/Edict 访问凭证
- PDB：`minAvailable: 1`（避免滚动时全挂）

Helm values 样例（片段）：

```yaml
image:
  repository: registry.example.com/tang-dynasty/backend
  tag: "1.0.0"
env:
  SPRING_PROFILES_ACTIVE: "prod"
  COPAW_BASE_URL: "http://copaw:8088"
resources:
  requests: { cpu: "250m", memory: "512Mi" }
  limits: { cpu: "2", memory: "2Gi" }
hpa:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

***

## 14. 灾备方案（双活/主备，RPO≤5min，RTO≤30min）

建议：

- MySQL：跨 AZ 主从或双活（MGR/云厂商方案），binlog 备份 + PITR
- Redis：主从 + 哨兵，AOF 每秒
- 对象存储：跨区域复制（奏折附件）
- Kafka：跨 AZ 副本因子 >= 3

指标：

- RPO ≤ 5min：binlog/AOF/对象存储复制延迟监控
- RTO ≤ 30min：自动故障切换 + 演练脚本

演练记录模板：每月一次，记录在 `sys_audit_log`（action=DR_DRILL）。

***

## 15. 上线 Checklist（顺序、配置、预热、监控）

1) DB migration（只增不改）→ 验证  
2) 部署新版本（灰度 10%）→ 验证关键接口  
3) 配置中心同步（渠道/CoPaw 地址/鉴权参数）  
4) 缓存预热（live-status、agent-config、模型列表）  
5) 监控大盘验证（5xx、延迟、DLQ、下游探活）  
6) 全量切流 → 观察 30min → 完成发布  
7) 回滚预案：镜像 tag 回退 + feature flag 关闭 + DB 回滚（如有）

***

## 16. 16 个核心模块逐条详设（领域模型 + API + 服务边界 + 事件流）

> 本节按菜单逐条覆盖。每个模块均包含：领域对象/表、核心 API（REST/WS）、事件、缓存、安全控制点。

### 模块 1：上朝（聊天）

领域对象：

- Conversation（`sys_conversation`）
- Message（`sys_message`）

现有接口（已实现，需保持兼容）：

- `POST /api/chat/completions`（见 [ChatController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/ChatController.java#L35-L87)）
- `GET /api/chat/conversations`
- `GET /api/chat/conversations/{sessionId}/messages`

安全：

- 可允许匿名（可配置），但写入 DB 需绑定匿名 userId（如 `ANON`）

事件：

- `td.chat.message_created`（用于审计与 token 统计）

### 模块 2：旨意库（模板）

领域对象：

- EdictTemplate（`sys_edict_template`）
- EdictTemplateVersion（`sys_edict_template_version`）

核心 API（新增，v1 推荐）：

- `GET /api/edict-templates?category=...`
- `POST /api/edict-templates`（幂等）
- `POST /api/edict-templates/{templateKey}/versions`
- `POST /api/edict-templates/{templateKey}/activate?version=...`

事件：

- `td.edict_template.changed`

缓存：

- `td:edict:tpl:{templateKey}`（TTL 300s）

### 模块 3：频道

领域对象：

- Channel（`sys_channel`）
- ChannelDeliveryLog（`sys_channel_delivery_log`）

API（新增，需与 CoPaw channel 概念兼容）：

- `GET /api/channels`
- `POST /api/channels`（加密存储）
- `POST /api/channels/{name}/toggle`
- `POST /api/channels/{name}/test`

事件：

- `td.channel.changed`

### 模块 4：旨意看板（任务）

领域对象：

- Task（`sys_task`）
- TaskLog（`sys_task_log`）
- SchedulerMeta（`sys_task_scheduler`）

现有接口（已实现/部分 stub）：

- `GET /api/live-status`（见 [DashboardController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/DashboardController.java#L26-L32)）
- `POST /api/create-task`（见 [TaskController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/TaskController.java#L27-L47)）
- `POST /api/task-action`（cancel/retry/complete）
- `GET /api/task-activity/{taskId}`（见 TaskController/DashboardController）
- `GET /api/scheduler-state/{taskId}`（stub）
- `POST /api/scheduler-scan`（stub）
- `POST /api/scheduler-retry`（复用 task-action retry）

新增接口（满足需求“叫停/取消/恢复/审查”等）：

- `POST /api/review-action`（review approve/reject）
- `POST /api/advance-state`
- `POST /api/scheduler-escalate`
- `POST /api/scheduler-rollback`

WebSocket：

- `GET /ws/task/{taskId}` 推送状态、进度、心跳

事件（Kafka/Redis）：

- `td.task.created/state_changed/progressed/stalled/archived`
- DLQ：`td.dlq.td.task.*`

### 模块 5：奏折

领域对象：

- Memorial（`sys_memorial`）
- Attachment（`sys_attachment`）

API（新增）：

- `GET /api/memorials?status=...`
- `GET /api/memorials/{taskId}`
- `POST /api/memorials/{taskId}/generate`（从 task.result 生成 markdown）
- `POST /api/memorials/{taskId}/submit`
- `POST /api/memorials/{taskId}/copy-link`（生成一次性分享链接，可审计）

事件：

- `td.memorial.generated/submitted`

### 模块 6：定时任务

领域对象：

- Job（`sys_job`）
- JobRun（`sys_job_run`）

API（新增）：

- `GET /api/jobs`
- `POST /api/jobs`
- `POST /api/jobs/{jobKey}/toggle`
- `GET /api/jobs/{jobKey}/runs`

调度实现：

- Quartz（集群）+ Redis 锁（防并发）

事件：

- `td.job.started/completed/failed`

### 模块 7：朝纲

领域对象：

- WorkspaceFile（`sys_workspace_file`）
- CourtRule（`sys_court_rule`）

API（新增）：

- `GET /api/workspace/files`
- `GET /api/workspace/files/{path}`
- `PUT /api/workspace/files/{path}`（ETag 并发控制）
- `GET /api/court-rules`
- `POST /api/court-rules`
- `POST /api/court-rules/{ruleKey}/toggle`

与 CoPaw 对齐：

- CoPaw 使用 WORKING_DIR 下的 `SOUL.md/AGENTS.md/PROFILE.md/HEARTBEAT.md` 组装 sys prompt；本模块提供“浏览器内编辑”能力，并通过 Adapter 写回 CoPaw 工作目录（零改动）

### 模块 8：技能库

领域对象：

- Skill（`sys_skill`）
- SkillRemote（可用 sys_skill 增加 sourceUrl 字段，或独立表）

现有接口（已实现）：

- `GET /api/skills`（[SkillController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/SkillController.java#L23-L27)）
- `GET /api/remote-skills`（目前返回空，需实现为 DB/远端仓库）
- `POST /api/add-remote-skill`、`/update-remote-skill`、`/remove-remote-skill`
- `GET /api/skill-content/{agentId}/{skillName}`

与 CoPaw 对齐：

- CoPaw 技能目录：builtin/customized/active；本模块以 DB 作为“控制面”，通过 Adapter 同步到 CoPaw `active_skills/`（零改动）

### 模块 9：工具库

领域对象：

- Tool（`sys_tool`）

现有接口（已实现）：

- `GET /api/tools`、`POST /api/tools`、`DELETE /api/tools/{id}`（[ToolController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/ToolController.java#L14-L37)）

与 CoPaw 对齐：

- CoPaw 内置工具开关在 `config.json`；本模块提供 UI 控制 → Adapter 写回 CoPaw 配置（零改动）

### 模块 10：MCP

领域对象：

- McpClient（`sys_mcp`）

现有接口（已实现）：

- `GET /api/mcp`、`POST /api/mcp`、`DELETE /api/mcp/{id}`（[McpController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/McpController.java#L11-L33)）

与 CoPaw 对齐：

- CoPaw MCP 支持热更新；本模块做控制面，最终同步到 CoPaw MCP clients 配置（零改动）

### 模块 11：官员管理

领域对象：

- Official（`sys_official`）
- OfficialPolicy/Prompt（扩展表）

现有接口（已实现基础）：

- `GET /api/officials`、`POST /api/officials`（[OfficialController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/OfficialController.java#L10-L28)）
- `GET /api/agent-config`（由 officials + models 组合，见 [AgentController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/AgentController.java#L27-L57)）

新增接口（满足需求“录用新官员/配置权限/输出限制”）：

- `POST /api/officials/{name}/policy`
- `POST /api/officials/{name}/prompts`（引用 workspace files/版本）
- `POST /api/officials/{name}/toggle`

### 模块 12：模型

领域对象：

- Model（`sys_model`）
- Provider（可由 sys_model.provider 承载）

现有接口：

- `GET /api/models`、`POST /api/models`、`DELETE /api/models/{id}`（[ModelController](file:///d:/Code/Java/TangDynasty/tang-dynasty-backend/src/main/java/com/liangshou/adapter/controller/ModelController.java#L11-L33)）
- `POST /api/set-model`（为官员/agent 设置模型）

与 CoPaw 对齐：

- CoPaw ProviderManager 支持多 provider/active model；本模块做 UI 控制 → Adapter 调用 CoPaw providers API（零改动）

### 模块 13：环境变量

领域对象：

- EnvVar（`sys_env_var`）

API（新增）：

- `GET /api/envs`
- `POST /api/envs`（加密存储，返回 masked）
- `DELETE /api/envs/{key}`

与 CoPaw 对齐：

- CoPaw 有 `envs.json` 持久化并在启动时注入 `os.environ`；本模块通过 Adapter 写入（零改动）

### 模块 14：御林军（安全）

领域对象：

- SecurityPolicy（`sys_security_policy`）
- AuditLog（`sys_audit_log`）

API（新增）：

- `GET /api/security/policies`
- `POST /api/security/policies`
- `GET /api/audit-logs?traceId=...`

与 CoPaw 对齐：

- CoPaw Tool-Guard 审批闭环（deny/guard/approve）；本模块负责策略配置与审计聚合（零改动）

### 模块 15：大司农（Token 消耗）

领域对象：

- TokenUsage（日聚合 `sys_token_usage` + 明细 `sys_token_usage_raw`）

API（新增/对齐）：

- `GET /api/token-usage?from=...&to=...`
- `GET /api/token-usage/by-model`

与 CoPaw 对齐：

- CoPaw 有 token usage 记录工具/路由；本模块聚合并提供前端报表。

### 模块 16：公共模块（鉴权与审计）

API（新增）：

- `POST /api/auth/token`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/me`

审计：

- 所有写接口统一 AOP 记录到 `sys_audit_log`

***

## 17. 需求追踪矩阵（require.md → 章节 → 接口）

| require.md 需求点 | 本文章节 | 关键接口/数据 |
|---|---|---|
| 三省六部流转模型（丞相→中书→门下→尚书→六部→回奏） | 0、1、16（模块4/11） | `/api/create-task`、`/api/task-action`、WS `/ws/task/{taskId}`、事件 `td.task.*` |
| 任务规格书字段（id/title/org/state/flow_log/progress_log/todos/_scheduler 等） | 2、16（模块4） | `sys_task/sys_task_log/sys_task_scheduler`、OpenAPI Task schema |
| 前端菜单：上朝/旨意库/频道/旨意看板/奏折/定时任务/朝纲/技能库/工具库/MCP/官员管理/模型/环境变量/御林军/大司农 | 16 | 对应模块 1-15 的 API 与表 |
| 前端组件要求：Ant Design/Ant Design X（前端） | 非后端范围 | OpenAPI 提供 mock 供前端生成 |
| 后端分层：adapter/controller、service、infrastructure.datasource.support（禁止 controller/service 直接 CRUD） | 4.2 | support 层规范与依赖方向 |

***

## 18. 评审通过准则（四方签字）

评审清单与签字页（模板）：

- 架构评审：组件拓扑、模块边界、事件总线、部署拓扑
- 安全评审：OAuth2.1/JWT、RBAC、审计、脱敏、密钥管理
- 性能评审：容量基线、缓存策略、压测脚本与阈值
- 测试评审：覆盖率≥90%、契约测试、CI 门禁

签字（姓名/日期）：

- 架构：____________  日期：______
- 安全：____________  日期：______
- 性能：____________  日期：______
- 测试：____________  日期：______

***

## 19. 附录：交付物清单

- 详细设计：`docs/backend_detail_design.md`（本文）
- OpenAPI 3.1：`docs/openapi.yaml`
- 图：
  - `docs/diagrams/er.drawio`、`docs/diagrams/er.png`
  - `docs/diagrams/system_architecture.drawio`、`docs/diagrams/system_architecture.png`
  - `docs/diagrams/sequence_mainflow.drawio`、`docs/diagrams/sequence_mainflow.png`

