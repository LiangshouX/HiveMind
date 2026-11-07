# HiveMind

> **Enterprise-Grade AI Agent Platform with Multi-User Isolation**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AgentScope](https://img.shields.io/badge/AgentScope-2.0.0-blue.svg)](https://github.com/agentscope-ai/agentscope-java)
[![React](https://img.shields.io/badge/React-19-61DAFB.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6.svg)](https://www.typescriptlang.org/)

HiveMind 是一个面向云端部署的**多用户隔离 AI 助理平台**。基于 [AgentScope-Java](https://github.com/agentscope-ai/agentscope-java) 构建的 ReAct Agent 引擎，集成工具调用安全防护（Tool Guard）、分层记忆系统、沙箱执行环境、流式推理等企业级能力，为每个用户提供独立、安全、可扩展的 AI 助理服务。

---

## 🚀 快速开始

**环境要求：** JDK 17+ · Node.js 18+ · MySQL 8.0+ · MongoDB 6.0+ · Python 3.10+（ReMe）· Docker（可选）

### 1. 一键启动（推荐，Docker Compose）

```bash
git clone <repo> && cd HiveMind
docker compose up -d --build
# 前端 http://localhost  ·  后端 http://localhost:8080  ·  Swagger http://localhost:8080/swagger-ui.html
```

首次启动时 MySQL 会自动执行 `db/init/` 下的建表、迁移与**演示种子数据**脚本，
因此控制台一打开就有技能、任务、定时任务和 30 天用量曲线可看。

### 2. 本地开发

```bash
# 基础设施
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=hivemind -p 3306:3306 mysql:8.0
docker run -d --name mongodb -p 27017:27017 mongo:7.0

# ReMe 长期记忆服务（0.4.2.1，streamable HTTP MCP，:2333）
cd reme-server && cp .env.example .env && pip install -r requirements.txt
./scripts/start_mcp.sh          # 等价于 reme start service.backend=mcp mcp.transport=streamable-http

# 构建并启动
mvn clean install
mvn spring-boot:run -pl hivemind-launcher    # 后端 :8080
cd website && npm install && npm run dev      # 前端 :4173
```

### 3. 演示账号

| 用户 ID | 密码 | 说明 |
|---------|------|------|
| `user-001` | `hivemind-user-001` | 预置演示账号，已包含技能 / 任务 / 定时任务 / 30 天 Token 用量 |

### 4. 配置自己的模型（平台不托管密钥）

平台**不提供也不托管任何模型 API Key**。登录后在「管理 → 模型配置」中，
从**内置供应商清单**里选择一家（DashScope / DeepSeek / OpenAI / Moonshot / 智谱 /
硅基流动 / Ollama 本地），页面会自动填好接口地址与常用模型，**你只需填入自己的 Key**。

供应商目录由 `GET /api/v1/providers/catalog` 提供，只包含 `baseUrl` / 模型清单 /
控制台链接，**不包含任何密钥**。

---

## 📐 系统架构

<p align="center">
  <img src=".\docs\0.imgs\STRUCTURE.png" width="100%" alt="HiveMind Architecture" />
</p>

---

## ✨ 核心能力

### 🤖 ReAct Agent 引擎

基于 [AgentScope-Java](https://github.com/agentscope-ai/agentscope-java) 构建的 ReAct（Reasoning + Acting）Agent，支持最多 **200 轮**推理-行动迭代。Agent 能够自主拆解复杂任务、选择工具、执行操作并验证结果。通过 `TdAgentFactory` 统一创建实例，集成模型、工具、记忆、Skill、Hook 等全部组件。

### 🛡️ Tool Guard 三层安全引擎

自研的工具调用防护机制，确保 Agent 的每一次外部操作都在安全边界内：

<p align="center">
  <img src=".\docs\0.imgs\ToolGuard.png" width="55%" alt="Tool Guard Flow" />
</p>

- **敏感路径自动拦截**：`.env`、`.git`、`id_rsa`、`pom.xml` 等关键文件受严格模式保护
- **用户级工具配置**：每个用户可独立设置工具启用/禁用、拒绝模式、风险等级
- **审批工作流**：Agent 暂停 → 前端展示审批卡片 → 用户批准/拒绝 → 恢复执行

### 💾 分层记忆系统

| 层次 | 存储 | 职责 |
|------|------|------|
| **短期记忆** | MongoDB | 会话级对话历史、Agent 运行状态、工具调用上下文、审批记录 |
| **长期记忆** | ReMe Server (Python) | 跨会话语义化记忆检索、知识存储、经验积累、Per-User Workspace || **记忆压缩** | 自动触发 | Token/消息数阈值触发 → 保留最近 6 条 + LLM 生成摘要（最大 2400 字符） |

`MongoConversationMemory` 持久化完整对话历史，支持跨页面刷新恢复。每个用户拥有独立的 Memory Workspace，数据完全隔离。

### 🏖️ 沙箱执行环境

| 工具域 | 能力 | 风险等级 |
|--------|------|---------|
| **SANDBOX** | Shell 命令、Python 代码（IPython）、文件读写、目录操作、代码编辑 | HIGH |
| **BROWSER** | 网页导航、元素交互、截图、文本快照、等待加载 | HIGH |
| **BUILTIN** | 会话管理、历史预览、记忆搜索、系统时间 | LOW |

### 🔌 MCP · Skill · SOUL

- **MCP 工具协议**：支持 [Model Context Protocol](https://modelcontextprotocol.io/)，可动态接入外部工具服务（SYSTEM / CUSTOM 两级），Server 级 / Tool 级独立启用/禁用；传输方式支持 **streamable HTTP**（ReMe 0.4.2.x 默认）与 SSE（旧版兼容），MCP 初始化失败时自动降级到 HTTP 直连
- **Skill 技能系统**：内置 + 用户自定义技能，支持 OSS 云存储，classpath 扫描自动注册
- **SOUL 人格系统**：通过 Markdown 定义 Agent 人格和行为准则，用户可通过 Profile 系统（`SOUL.md` / `AGENTS.md` / `PROFILE.md`）自定义 Agent 行为模式

### 📡 流式推理 & Token 追踪

基于 SSE（Server-Sent Events）的实时流式对话，事件类型：`REASONING`（思考链）· `TOOL_RESULT`（工具结果）· `RESULT`（最终回答）· `APPROVAL_REQUIRED`（待审批）· `ERROR` · `DONE`

自动记录每次 LLM 调用的 Token 消耗，用量看板支持**按模型 / 按供应商 / 按日期**三个维度，
估算费用由外部可配置的单价表（`tdagent.token-pricing`，单位：元 / 百万 Token）计算，
匹配顺序为 `provider+model` → `model` → `provider` 兜底价。

---

## 🛠️ 技术栈

| 层 | 技术 |
|----|------|
| **前端** | React 19.2 · TypeScript 5.9 · Ant Design 6.3 · Ant Design X 2.4 · Vite 5 · Zustand 5 · React Router 7 |
| **后端** | Java 17 · Spring Boot 4.0.3 · Spring Security · JWT 4.4 · MyBatis-Plus 3.5.15 · Caffeine 3.1.8 |
| **Agent 引擎** | AgentScope-Java 2.0.0 · ReAct Agent · Tool Guard · ReMe 0.4.2.1 (MCP streamable HTTP) · MCP · Skill |
| **数据库** | MySQL 8.0（业务数据） · MongoDB 7.0（Agent 运行时） |
| **外部服务** | DashScope / DeepSeek / OpenAI Compatible · ReMe Server (Python/FastAPI) · Aliyun OSS · AgentScope Studio |
| **部署** | Docker · Testcontainers（集成测试） · SpringDoc OpenAPI（API 文档） |

---

## 📁 项目结构

```
HiveMind/
├── hivemind-launcher/              # Spring Boot 启动入口，聚合所有子模块
├── hivemind-agent-engine/          # AI Agent 引擎核心
│   ├── agents/                     #   Agent 工厂、Tool Guard、记忆、沙箱、Skill、流式
│   ├── application/                #   服务层（Chat、Streaming、Persistence）
│   ├── domain/                     #   领域模型（Memory、Profile、Session、Skill、Tool）
│   ├── infrastructure/             #   MongoDB Repository、MySQL Mapper、OSS 存储
│   ├── common/                     #   配置、枚举、异常、工具类
│   └── src/main/resources/
│       ├── application-agentic.yaml    # Agent 引擎配置
│       ├── provider/builtin_provider.json  # 内置 LLM 供应商（7 家，含控制台链接）
│       ├── profiles/                   # SOUL 人格模板
│       └── skills/                     # 内置 Skill 定义
├── hivemind-backend/               # 业务后端服务
│   ├── adapter/controller/         #   REST API（Auth、Task、Model、MCP、Cron）
│   ├── service/                    #   业务服务实现
│   ├── infrastructure/             #   MySQL PO/Mapper、MongoDB Repository
│   └── src/main/resources/db/init/ #   建表 + 迁移 + 演示种子数据（幂等 SQL）
├── website/                        # 前端控制台（React 19 + TypeScript）
│   └── src/
│       ├── pages/                  #   页面（Login、Workspace、TaskCenter、Admin）
│       ├── components/             #   组件（Auth、Console、Model）
│       ├── hooks/                  #   useAgentConsole（流式对话核心 Hook）
│       └── services/               #   API 客户端（fetch-based）
├── reme-server/                    # ReMe 长期记忆服务（Python FastAPI）
└── pom.xml                         # Maven 父工程
```

**模块依赖：** `hivemind-launcher` → `hivemind-backend` → `hivemind-agent-engine`

---

## 🏛️ 架构设计

### DDD 分层

HiveMind 遵循领域驱动设计分层架构，严格控制依赖方向：

```
Adapter (REST Controllers)
  │
  ▼
Application (Service · DTO · 业务编排)
  │
  ▼
Domain (领域模型 · 领域服务 · 零外部依赖)
  ▲
  │ 实现
Infrastructure (MongoDB · MySQL · OSS · 外部集成)
  ▲
Common (异常 · 工具类 · 配置)
```

- Domain 层**不依赖** Infrastructure 层（依赖倒置）
- Application 层**不直接调用** Infrastructure 层
- 无跨层调用

### 核心设计模式

| 模式 | 应用 |
|------|------|
| **工厂** | `TdAgentFactory` 统一创建 ReActAgent 实例 |
| **装饰器** | `GuardedAgentTool` 包装原始工具，注入安全检查 |
| **策略** | `ToolGuardEngine` 根据配置执行不同安全评估策略 |
| **观察者** | Hook 机制（`MemoryCompactionHook`、`ToolGuardHook`） |
| **责任链** | Tool Guard 三层防护：Deny → Approve → Allow |
| **模板方法** | Profile 系统：SOUL.md + AGENTS.md + PROFILE.md 组合注入 |

---

## 📖 API 概览

> 所有 REST 端点统一返回 `Result<T>` 包装：`{ code: "SUCCESS", message, data }`；
> 业务错误码形如 `HME_{域}_{编号}`（如 `HME_SKILL_001`），由全局异常处理器统一转换。

### Agent 引擎（`/api/v1/tdagent/`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/chat` | POST | 同步聊天 |
| `/chat/stream` | POST | 流式聊天（SSE：REASONING / TOOL_RESULT / RESULT / APPROVAL_REQUIRED / ERROR / DONE） |
| `/approvals/approve` | POST | 批准工具调用并恢复执行（SSE） |
| `/approvals/reject` | POST | 拒绝工具调用并恢复执行（SSE） |
| `/approvals/me/{sessionId}` | GET | 待审批工具列表 |
| `/chat/interrupt` | POST | 中断正在执行的 Agent |
| `/sessions/me` | GET | 当前用户会话列表 |
| `/sessions/me/{sessionId}` | GET / DELETE | 会话历史 / 删除会话 |
| `/sessions/{sessionId}/generate-title` | POST | LLM 生成会话标题 |
| `/active-models` | GET | 已激活的模型清单 |
| `/profiles` | GET/PUT | 用户 Profile（SOUL / AGENTS / PROFILE）管理 |
| `/skills/users/{userId}` | GET | Skill 列表 |
| `/skills/cloud/all?userId=` | GET | 内置 + 云端技能合并列表 |

### 其他

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/login` · `/api/v1/auth/me` | POST / GET | 登录 / 当前用户（JWT） |
| `/api/v1/providers/catalog` | GET | **内置模型供应商清单**（不含任何 Key） |
| `/api/v1/providers/test-connection` | POST | 供应商连通性测试 |
| `/api/v1/providers/{id}/activation?active=true` | PATCH | 启用 / 停用供应商 |
| `/api/agent/token-usage/{summary\|by-model\|by-provider\|by-date}` | GET | Token 用量与估算费用 |
| `/api/agent/tool-config` | GET / PUT | 工具启用状态、风险等级、拒绝模式 |
| `/api/agent/sandboxes/health` | GET | 沙箱运行时健康状态 |
| `/api/agent/task-template` | GET / POST / PUT / DELETE | 任务模板 |
| `/api/scheduled-jobs` | GET / POST / PUT / DELETE | 定时任务调度 |
| `/api/agent-tasks` | GET / POST / PUT / DELETE | 智能体任务 |
| `/api/task-reports` · `/api/task-flow-logs` | CRUD | 任务报告 / 流转日志 |
| `/api/sys-models` · `/api/sys-mcps` · `/api/sys-token-usages` | CRUD | 模型 / MCP / 用量配置 |
| `/api/v1/memory/*` | GET / PUT | 长期记忆文件读写与检索 |

---

## ⚙️ 配置

Agent 引擎核心配置（`application-agentic.yaml`）：

```yaml
tdagent:
  model:
    provider-id: dashscope              # LLM 供应商（dashscope / deepseek / openai）
    model-name: qwen-max                # 模型名称
    max-iters: 200                      # 最大推理轮次
    stream: true                        # 流式输出
    enable-thinking: true               # 思考模式
  sandbox:
    enabled: true                       # 沙箱总开关
    browser-enabled: true               # 浏览器工具
    filesystem-enabled: true            # 文件系统工具
  tool-guard:
    enabled: true                       # Tool Guard 开关
    strict-mode: true                   # 严格模式（敏感路径拦截）
    pending-expire-minutes: 60          # 审批请求过期时间（分钟）
  reme:
    enabled: true                       # ReMe 长期记忆
    base-url: http://localhost:2333     # ReMe Server 地址
    top-k: 5                            # 检索 Top-K
    mcp:
      enabled: true                      # 优先走 MCP，失败自动降级 HTTP
      transport: streamable-http         # ReMe 0.4.2.x 默认在 /mcp 挂载 streamable HTTP
      streamable-http:
        url: http://localhost:2333/mcp
      sse:                               # 旧版 ReMe 兼容
        url: http://localhost:2333/sse
  token-pricing:                         # Token 单价（单位：元 / 百万 Token）
    enabled: true
    currency: CNY
    models:                              # 匹配顺序：provider+model → model → provider
      - provider: deepseek
        model: deepseek-chat
        input: 2.0
        output: 8.0
  compaction:
    trigger-message-count: 20           # 消息数触发阈值
    trigger-character-count: 24000      # 字符数触发阈值
    keep-recent-messages: 6             # 压缩时保留最近消息数
    max-summary-characters: 2400        # 摘要最大字符数
  streaming:
    enabled: true
    incremental: true                   # 增量流式
  skill:
    enabled: true
    builtin-location: classpath:skills  # 内置 Skill 路径
```

供应商定义：`hivemind-agent-engine/src/main/resources/provider/builtin_provider.json`

| 供应商 | Provider ID | 模型示例 | 需要 API Key |
|--------|------------|---------|--------------|
| 阿里云百炼 | `dashscope` | qwen-max, qwen3-max, qwen-plus | 是 |
| DeepSeek | `deepseek` | deepseek-chat, deepseek-reasoner | 是 |
| OpenAI | `openai` | gpt-4o, gpt-4o-mini | 是 |
| Moonshot 月之暗面 | `moonshot` | moonshot-v1-8k / 32k / 128k | 是 |
| 智谱 GLM | `zhipu` | glm-4-plus, glm-4-air, glm-4-flash | 是 |
| 硅基流动 SiliconFlow | `siliconflow` | Qwen2.5-7B-Instruct, DeepSeek-V3 | 是 |
| Ollama 本地部署 | `ollama` | qwen2.5:7b, llama3.2:3b | **否** |

> 目录本身只提供 `baseUrl` / 模型清单 / 控制台链接与申请 Key 的入口，
> **不保存也不下发任何密钥**；每个用户在自己的 Provider 记录中填入自己的 Key。

### 跨域部署

前端默认走同源相对路径（由 Nginx `/api/` 反代到后端）。若前后端分离部署，
构建时传入 `VITE_API_URL=https://your-host/api`，并在后端设置
`CORS_ALLOWED_ORIGINS=https://your-host`（逗号分隔多个）。

---

## 📄 许可证

[AGPL-3.0](LICENSE)
