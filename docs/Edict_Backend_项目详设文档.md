# Edict Backend（edict-backend）AI Agent 项目详设文档

面向目录：[edict-backend](file:///d:/Code/Java/TangDynasty/example/edict-backend/)

> 定位：这是一个“事件驱动的多 Agent 协作后端”，以“三省六部”做领域隐喻，通过 Redis Streams 事件总线实现可靠编排与派发，并用 WebSocket 将事件实时推送给前端。

***

## 1. 项目目标与整体形态

Edict Backend 的核心目标是把“任务”变为可编排的事件流：

- **任务状态机**：任务沿“三省六部”流程流转（起草/审核/派发/执行/审查/完成），每次流转都以事件形式记录与广播。
- **事件驱动编排**：由编排器消费事件，自动决定下一步派发给哪个 Agent。
- **可靠派发执行**：派发器消费派发事件，调用外部 Agent 执行器（OpenClaw CLI），把输出与心跳写回事件总线。
- **实时观测**：WebSocket 订阅 Redis Pub/Sub，把所有 topic 的事件实时推送给 UI，取代轮询。

项目形态：

- Web 服务：FastAPI 应用入口在 [main.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/main.py)
- Worker 进程：编排器与派发器在 [workers](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/) 下以独立入口函数运行（`run_orchestrator/run_dispatcher`）
- 持久化：SQLAlchemy Async + Postgres；事件/思考/todo 等均有 ORM 模型（见 [models](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/)）

***

## 2. 目录结构与分层边界

以 `app/` 为根包（见 [app](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/)），建议按以下分层理解：

- **入口与装配**
  - FastAPI 入口： [main.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/main.py)
  - 配置加载： [config.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/config.py)
  - DB 引擎/Session： [db.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/db.py)

- **接口层（API）**
  - 任务 API： [api/tasks.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py)
  - 事件 API： [api/events.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/events.py)
  - Agent 信息： [api/agents.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/agents.py)
  - 管理诊断： [api/admin.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/admin.py)
  - WebSocket： [api/websocket.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/websocket.py)
  - 兼容旧版 ID： [api/legacy.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/legacy.py)

- **领域与持久化（Models）**
  - Task： [models/task.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py)
  - Event： [models/event.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/event.py)
  - Todo： [models/todo.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/todo.py)
  - Thought： [models/thought.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/thought.py)

- **服务层（Services）**
  - 事件总线： [services/event_bus.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py)
  - 任务服务： [services/task_service.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/task_service.py)

- **后台执行（Workers）**
  - OrchestratorWorker： [workers/orchestrator_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py)
  - DispatchWorker： [workers/dispatch_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py)

***

## 3. 配置与依赖

### 3.1 配置来源与策略

配置通过 Pydantic Settings 从环境变量加载（支持 `.env`）：

- 配置模型： [Settings](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/config.py#L7-L75)
- 单例缓存：`get_settings()`（LRU Cache）[config.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/config.py#L73-L75)

关键配置项：

- Postgres：`postgres_*` + `database_url_override`（[config.py:L8-L15](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/config.py#L8-L15)）
- Redis：`redis_url`（[config.py:L16-L17](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/config.py#L16-L17)）
- OpenClaw：`openclaw_bin/openclaw_project_dir`（[config.py:L26-L30](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/config.py#L26-L30)）
- 调度参数：stall/timeout/scan interval 等（[config.py:L35-L41](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/config.py#L35-L41)）

### 3.2 依赖栈

依赖清单在 [requirements.txt](file:///d:/Code/Java/TangDynasty/example/edict-backend/requirements.txt)：

- Web：FastAPI + Uvicorn
- DB：SQLAlchemy asyncio + asyncpg + Alembic
- Redis：redis asyncio（hiredis）
- 配置：pydantic + pydantic-settings + dotenv
- 其他：httpx

***

## 4. 启动与装配（FastAPI 应用）

入口： [main.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/main.py)

关键装配点：

- Lifespan（启动/关闭）负责确保 EventBus 建连并在关机时释放连接：
  - 建连：[`bus = await get_event_bus()`](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/main.py#L33-L47)
  - 关闭：`await bus.close()`
- 路由挂载：
  - Tasks：`/api/tasks`（[main.py:L66-L73](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/main.py#L66-L73)）
  - Agents：`/api/agents`
  - Events：`/api/events`
  - Admin：`/api/admin`
  - WebSocket：`/ws`
  - Legacy：`/api/tasks` 下的 by-legacy 子路由

***

## 5. 数据层（DB 与模型）

### 5.1 DB 引擎与会话管理

实现： [db.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/db.py)

- `create_async_engine(settings.database_url)` + 连接池参数（[db.py:L14-L20](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/db.py#L14-L20)）
- `async_sessionmaker(..., expire_on_commit=False)`（[db.py:L22](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/db.py#L22)）
- FastAPI 依赖 `get_db()`：自动 commit/rollback（[db.py:L30-L39](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/db.py#L30-L39)）

### 5.2 Task 状态机（领域模型）

实现： [models/task.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py)

- 状态枚举 `TaskState`（[task.py:L28-L41](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py#L28-L41)）
- 合法流转 `STATE_TRANSITIONS`（[task.py:L47-L56](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py#L47-L56)）
- 状态→Agent 映射 `STATE_AGENT_MAP`（[task.py:L58-L65](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py#L58-L65)）
- 六部组织→Agent 映射 `ORG_AGENT_MAP`（[task.py:L67-L75](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py#L67-L75)）
- Task 表字段：以 `id/title/state/org/now/...` 为主，并通过 JSONB 承载 `flow_log/progress_log/todos/scheduler`（[task.py:L78-L112](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py#L78-L112)）
- `to_dict()` 输出兼容旧 live_status 格式（[task.py:L118-L142](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py#L118-L142)）

### 5.3 事件/思考/Todo 的持久化模型

- 事件审计表： [Event](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/event.py#L16-L48)
- 结构化 Todo： [Todo](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/todo.py#L16-L67)
- 思考流： [Thought](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/thought.py#L16-L55)

***

## 6. 事件总线设计（Redis Streams + Pub/Sub）

实现： [services/event_bus.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py)

### 6.1 Topic 与 Stream 命名

- Topic 常量集中定义（task/agent 两类）：[event_bus.py:L22-L39](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py#L22-L39)
- Stream Key：`edict:stream:{topic}`（[event_bus.py:L40-L73](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py#L40-L73)）
- Pub/Sub：`edict:pubsub:{topic}`（用于 WebSocket 实时推送）[event_bus.py:L102-L104](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py#L102-L104)

### 6.2 事件结构（publish）

`publish()` 会构造统一事件 envelope（event_id/trace_id/timestamp/topic/event_type/producer/payload/meta），并将 `payload/meta` JSON 序列化存入 Redis Stream（[event_bus.py:L74-L105](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py#L74-L105)）。

### 6.3 可靠消费（消费者组 + ACK + 认领）

该项目用 Redis Streams 的消费者组机制保障“至少一次”投递：

- 创建消费者组（幂等）：`xgroup_create`（[event_bus.py:L107-L116](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py#L107-L116)）
- 消费：`xreadgroup`（[event_bus.py:L117-L148](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py#L117-L148)）
- 确认：`xack`（[event_bus.py:L150-L155](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py#L150-L155)）
- 崩溃恢复：`xautoclaim` 认领 stale pending（[event_bus.py:L161-L184](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py#L161-L184)）

这也是 “取代旧架构 daemon 线程 + kill -9 丢失派发” 的核心工程动机（见 worker 文件头注释： [orchestrator_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py)、[dispatch_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py)）。

***

## 7. 编排与派发（Workers）

### 7.1 OrchestratorWorker：状态机驱动与自动派发

实现： [workers/orchestrator_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py)

职责：

- 监听多个 task topic（created/status/completed/stalled），消费事件并决定后续动作（[orchestrator_worker.py:L38-L44](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py#L38-L44)）
- 启动时先恢复 stale pending（[orchestrator_worker.py:L80-L90](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py#L80-L90)）
- `task.created` → 选择状态对应 agent（默认太子）→ 发布 `task.dispatch.request`（[orchestrator_worker.py:L125-L143](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py#L125-L143)）
- `task.status` → 按 `STATE_AGENT_MAP` 自动派发下一棒；若进入 Assigned，则按六部 org 映射（[orchestrator_worker.py:L144-L177](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py#L144-L177)）

### 7.2 DispatchWorker：调用 OpenClaw 执行 agent

实现： [workers/dispatch_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py)

职责：

- 消费 `task.dispatch` topic（消费者组 `dispatcher`），并用 Semaphore 限制并发（[dispatch_worker.py:L37-L45](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py#L37-L45)）
- 运行 OpenClaw CLI：`openclaw agent --agent {agent} -m {message}`（[dispatch_worker.py:L153-L157](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py#L153-L157)）
- 通过环境变量把运行上下文注入到 agent（task_id/trace_id/api_url）（[dispatch_worker.py:L159-L163](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py#L159-L163)）
- 发布 `agent.heartbeat`（开始）与 `agent.thoughts`（输出），成功后 ACK（[dispatch_worker.py:L103-L139](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py#L103-L139)）
- 失败不 ACK，等待 Redis 重新投递给本 consumer 或其他 consumer（[dispatch_worker.py:L140-L143](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py#L140-L143)）

***

## 8. API 设计要点

### 8.1 Tasks API（CRUD + 状态流转）

实现： [api/tasks.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py)

核心端点：

- `GET /api/tasks` 列表与过滤（[tasks.py:L83-L102](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L83-L102)）
- `POST /api/tasks` 创建（[tasks.py:L120-L136](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L120-L136)）
- `GET /api/tasks/{task_id}` 查询（[tasks.py:L138-L149](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L138-L149)）
- `POST /api/tasks/{task_id}/transition` 状态流转（[tasks.py:L151-L173](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L151-L173)）
- `POST /api/tasks/{task_id}/dispatch` 手动派发（[tasks.py:L175-L188](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L175-L188)）
- `POST /api/tasks/{task_id}/progress` 进度写入（[tasks.py:L190-L202](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L190-L202)）
- `PUT /api/tasks/{task_id}/todos` todo 更新（[tasks.py:L204-L216](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L204-L216)）
- `GET /api/tasks/live-status` 旧格式兼容（[tasks.py:L104-L108](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L104-L108)）

### 8.2 Events API（审计 + Redis Stream 诊断）

实现： [api/events.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/events.py)

- `GET /api/events` 查 Postgres `events` 表（[events.py:L19-L54](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/events.py#L19-L54)）
- `GET /api/events/stream-info?topic=...` 查 Redis Stream 信息（[events.py:L56-L62](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/events.py#L56-L62)）
- `GET /api/events/topics` 列 topic 常量（[events.py:L64-L86](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/events.py#L64-L86)）

### 8.3 Admin API（探活 + pending 事件查看 + 配置脱敏）

实现： [api/admin.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/admin.py)

- `GET /api/admin/health/deep`：DB + Redis 探活（[admin.py:L18-L39](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/admin.py#L18-L39)）
- `GET /api/admin/pending-events`：查看某 topic/group pending（[admin.py:L42-L64](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/admin.py#L42-L64)）
- `GET /api/admin/config`：返回脱敏配置（[admin.py:L79-L90](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/admin.py#L79-L90)）

### 8.4 WebSocket：实时推送

实现： [api/websocket.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/websocket.py)

- `/ws`：订阅 `edict:pubsub:*`，把所有 topic 的事件推给前端（[websocket.py:L26-L56](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/websocket.py#L26-L56)）
- `/ws/task/{task_id}`：只推送 payload.task_id 匹配的事件（[websocket.py:L103-L140](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/websocket.py#L103-L140)）

### 8.5 Legacy：旧版 ID 兼容

实现： [api/legacy.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/legacy.py)

- 通过 `tags contains [legacy_id]` 或 `meta.legacy_id` 找到任务（[legacy.py:L22-L35](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/legacy.py#L22-L35)）
- 提供旧 ID 的 transition/progress/todos/get（[legacy.py:L52-L116](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/legacy.py#L52-L116)）

***

## 9. 一次任务的端到端链路（从创建到执行）

典型链路（意图层面）：

1) 创建任务（API）→ 写库 → 发布 `task.created`
   - API 调用服务层：[`get_task_service`](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L74-L79)
   - 服务层发布事件：[`TaskService.create_task`](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/task_service.py#L36-L95)

2) 编排器消费 `task.created` → 发布 `task.dispatch.request`
   - [`OrchestratorWorker._on_task_created`](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py#L125-L143)

3) 派发器消费 `task.dispatch` → 调用 OpenClaw → 发布 `agent.heartbeat/agent.thoughts` → ACK
   - [`DispatchWorker._dispatch`](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py#L91-L143)

4) 前端通过 WebSocket 实时接收 event（状态变更、输出、心跳等）
   - [`websocket_endpoint`](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/websocket.py#L26-L56)

5) 状态流转（人工或 agent 回写）→ `task.status` → 编排器再次派发下一棒
   - [`TaskService.transition_state`](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/task_service.py#L98-L149)
   - [`OrchestratorWorker._on_task_status`](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py#L144-L177)

***

## 10. 当前实现中的一致性风险（必须关注）

从源码现状看，存在“领域模型/服务层/接口层”明显不一致，可能代表两套 schema/实现正在混用：

- `TaskService` 与 `Tasks API` 把主键当作 `uuid.UUID`，并依赖 `task.task_id / task.trace_id / description / assignee_org / tags / meta` 等字段：
  - [TaskService.create_task](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/task_service.py#L36-L95)
  - [Tasks API create_task](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py#L120-L136)
- 但当前 `models/task.py` 的 `Task` ORM 主键是 `id: String(32)` 且字段集合不同（`org/official/now/eta/...`），也不存在 `task_id/trace_id/meta/tags/description`：
  - [Task ORM](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/models/task.py#L78-L112)
- 同样，`TaskState` 枚举命名也存在差异（API/Service 使用的值与 ORM 枚举值不一致），会导致运行期 `ValueError/AttributeError` 或 `db.get(Task, uuid)` 查不到记录。

建议在进一步工程化前，先统一以下三处的“事实来源”：

1) Task 表主键类型与字段集合（UUID vs 自定义 JJC-xxxx 字符串）
2) TaskState 的枚举值与流转路径（前后端/UI/Worker/Service 全一致）
3) EventBus 的 trace_id 语义（是“任务追踪 ID”还是“任务主键”）

***

## 11. 二次开发与扩展点

- 增加新的 topic（例如更细粒度的 agent 输出、工具调用、质量审查结果）
  - 在 [event_bus.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py) 统一新增常量与下游消费逻辑
- 增加新的编排策略（例如 stalled 自动升级/重试/换 agent）
  - 在 [orchestrator_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py) 扩展 `_on_task_stalled` 或在 `_on_task_status` 中引入策略表
- 增加新的执行器（除了 OpenClaw CLI）
  - 在 [dispatch_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py) 抽象 `_call_openclaw`，增加 HTTP Gateway/SDK 等实现
- 增加更强的审计与回放
  - 目前 `Event` ORM 存在但 Redis 事件未落库，若要可回放，建议在消费端或 publish 端补一层“写 events 表”的落库逻辑（注意幂等）

***

## 12. 推荐阅读顺序（从哪看起）

- 入口与路由装配： [main.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/main.py)
- 事件总线实现： [event_bus.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/event_bus.py)
- 编排器与派发器： [orchestrator_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/orchestrator_worker.py)、[dispatch_worker.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/workers/dispatch_worker.py)
- 任务 API 与服务层： [tasks.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/tasks.py)、[task_service.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/services/task_service.py)
- WebSocket 实时推送： [websocket.py](file:///d:/Code/Java/TangDynasty/example/edict-backend/app/api/websocket.py)

