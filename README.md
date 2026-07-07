# HiveMind

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AgentScope](https://img.shields.io/badge/AgentScope-1.0.12-blue.svg)](https://agentscope.io/)

**HiveMind** 是一个基于多 Agent 协作架构的云端 AI 助理平台，通过制度化、流程化的任务流转机制，实现复杂的多 Agent 协作工作流。

---

## 📖 简介

HiveMind 构建了一个层次分明、职责清晰的多 Agent 协作系统。系统能够自动将复杂任务拆解为子任务，分配给不同的 Agent 执行，并通过审查机制确保执行质量。

### 核心理念

- **制度化协作**：建立规范的任务流转机制，通过 Triage → Planner → Reviewer → Executor 四阶段流转
- **职责分离**：不同 Agent 承担不同角色，各司其职，通过 SOUL 提示词系统定义角色人格
- **安全可控**：Tool Guard 三层安全机制（deny → guard/approve → allow）确保工具调用的安全性
- **长期记忆**：ReMe 支持跨会话的记忆检索与复用，通过记忆压缩机制管理上下文

---

## ✨ 核心特性

| 特性 | 描述 |
|------|------|
| 🤖 **多Agent协作** | 基于 Triage/Planner/Reviewer/Executor 四角色分工的任务编排系统 |
| 🧠 **ReAct Agent** | 推理 + 行动的 Agent 执行模式，支持最大 200 轮迭代 |
| 🛡️ **Tool Guard** | 三层安全防护：deny（无条件禁止）→ guard（需要审批）→ allow（允许执行） |
| 💾 **长期记忆** | ReMe 跨会话记忆检索，支持记忆压缩和上下文管理 |
| 🔌 **Skill 系统** | 内置 + 用户自定义技能扩展，支持 OSS 存储 |
| 🔗 **MCP 集成** | Model Context Protocol 支持，可扩展外部工具 |
| 📱 **多渠道接入** | 钉钉、飞书、Discord 等多平台接入 |
| ⏰ **定时任务** | Cron 计划任务调度 |
| 📊 **Token 统计** | 模型调用费用统计与监控 |
| 🔒 **沙箱环境** | 代码执行、文件操作、浏览器自动化沙箱 |
| 📝 **SOUL 系统** | 基于 Markdown 的 Agent 人格定义系统 |
| 🎯 **流式响应** | 支持增量流式输出，实时展示 Agent 思考过程 |

---

## 🏗️ 多Agent协作流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户                                      │
│                    发送任务 / 闲聊消息                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Triage Agent (AGENT_TRIAGE)                    │
│              消息分拣：识别任务 or 闲聊                           │
│              SOUL: profiles/AGENT_TRIAGE/SOUL.md                 │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │ 任务                          │ 闲聊
              ▼                               ▼
┌─────────────────────────┐         ┌─────────────────┐
│  Planner Agent           │         │   直接回复       │
│  (AGENT_PLANNER)         │         │                 │
│  任务规划：拆解子任务     │         │                 │
│  SOUL: profiles/         │         │                 │
│  AGENT_PLANNER/SOUL.md   │         │                 │
└─────────────────────────┘         └─────────────────┘
              │
              ▼
┌─────────────────────────┐
│  Reviewer Agent          │
│  (AGENT_REVIEWER)        │
│  审查：审查方案           │
│  通过→执行层             │
│  驳回→规划层 (最多 3 轮)  │
│  SOUL: profiles/         │
│  AGENT_REVIEWER/SOUL.md  │
└─────────────────────────┘
              │
              ▼
┌─────────────────────────┐
│  Executor Agent          │
│  (AGENT_EXECUTOR)        │
│  执行：派发执行           │
│  汇总结果→规划层         │
│  SOUL: profiles/         │
│  AGENT_EXECUTOR/SOUL.md  │
└─────────────────────────┘
              │
              ▼
┌─────────────────────────┐
│   报告呈报              │
│   用户审阅              │
└─────────────────────────┘
```

### Agent 角色说明

| 角色 | 职责 | SOUL 文件 |
|------|------|-----------|
| **AGENT_TRIAGE** | 消息分拣，识别任务类型，路由到相应处理器 | `profiles/AGENT_TRIAGE/SOUL.md` |
| **AGENT_PLANNER** | 任务规划，将复杂任务拆解为可执行的子任务 | `profiles/AGENT_PLANNER/SOUL.md` |
| **AGENT_REVIEWER** | 方案审查，验证任务规划的合理性和可行性 | `profiles/AGENT_REVIEWER/SOUL.md` |
| **AGENT_EXECUTOR** | 任务执行，派发执行子任务并汇总结果 | `profiles/AGENT_EXECUTOR/SOUL.md` |

---

## 🚀 快速开始

### 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| **JDK** | 17 | 17+ |
| **Python** | 3.10 | 3.10-3.13 |
| **Node.js** | 18 | 20+ |
| **MySQL** | 8.0 | 8.0.36+ |
| **MongoDB** | 6.0 | 7.0+ |
| **Docker** | 20.10 | 24.0+ |

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/your-org/hivemind.git
cd hivemind
```

#### 2. 启动基础设施

```bash
# 启动 MySQL
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=hivemind \
  -p 3306:3306 \
  mysql:8.0

# 启动 MongoDB
docker run -d --name mongodb \
  -p 27017:27017 \
  mongo:7.0
```

#### 3. 配置后端

```bash
# 复制配置文件
cp hivemind-backend/src/main/resources/application-backend.yaml.example \
   hivemind-backend/src/main/resources/application-backend.yaml

# 编辑配置文件，设置数据库连接和 API Key
```

#### 4. 启动 ReMe Server（可选，使用长期记忆时需要）

```bash
cd reme-server

# 配置环境变量
cp .env.example .env
# 编辑 .env 设置 DASHSCOPE_API_KEY 等

# 启动服务
python start.py
# 或使用 Docker
docker-compose up -d
```

#### 5. 构建并启动后端

```bash
# 根目录执行
mvn clean install

# 启动应用
mvn spring-boot:run -pl hivemind-launcher
```

#### 6. 启动前端控制台

```bash
cd website
npm install
npm run dev
```

访问 `http://localhost:5173` 即可使用控制台。

---

## 📁 项目结构

```
hivemind/
├── hivemind-launcher/        # 🚀 启动模块（主入口）
├── hivemind-agent-engine/    # 🤖 Agent 引擎核心
│   ├── agents/                   # Agent 核心组件
│   │   ├── guard/                # Tool Guard 安全防护
│   │   ├── memory/               # 记忆管理（MongoDB + ReMe）
│   │   ├── skill/                # Skill 技能系统
│   │   └── tools/                # 工具注册与管理
│   ├── profiles/                 # SOUL 人格定义文件
│   │   ├── AGENT_TRIAGE/SOUL.md  # 分拣 Agent 人格
│   │   ├── AGENT_PLANNER/SOUL.md # 规划 Agent 人格
│   │   ├── AGENT_REVIEWER/SOUL.md# 审查 Agent 人格
│   │   └── AGENT_EXECUTOR/SOUL.md# 执行 Agent 人格
│   └── skills/                   # 内置技能定义
├── hivemind-backend/         # 🏢 业务后端服务
├── website/                      # 💻 前端控制台
├── reme-server/                  # 🧠 Python 长期记忆服务
├── example/                      # 📝 示例代码
├── docs/                         # 📚 项目文档
└── pom.xml                       # Maven 父工程
```

### 模块说明

| 模块 | 职责 | 技术栈 |
|------|------|--------|
| **hivemind-launcher** | 应用启动入口，聚合所有子模块 | Spring Boot 4.0.3 |
| **hivemind-agent-engine** | AI Agent 核心运行时，提供对话、工具调用、记忆管理等能力 | AgentScope 1.0.12 + MongoDB |
| **hivemind-backend** | 用户管理、任务管理、配置管理等业务服务 | Spring Boot + MyBatis Plus 3.5.15 |
| **website** | Web 用户界面，提供与 AI 助理交互的所有功能 | React + TypeScript + Ant Design |
| **reme-server** | 提供与 AgentScope-ReMe 兼容的长期记忆检索服务 | FastAPI + ReMe |

---

## 📖 使用文档

### 基本用法

#### 1. 登录系统

访问 `http://localhost:5173`，使用默认账号登录（首次启动需注册）。

#### 2. 创建任务

在控制台输入你的需求，系统会自动识别任务类型并分配给相应的 Agent：

```
请帮我分析最近的项目代码，找出可以优化的地方
```

#### 3. 查看任务进度

在任务列表中可以查看任务的实时状态和流转记录。

#### 4. 配置模型

在设置页面配置 LLM 供应商和 API Key：

- 阿里云百炼（DashScope）
- OpenAI
- 其他兼容 OpenAI API 的供应商

### 高级用法

#### Tool Guard 配置

在 `application-agentic.yaml` 中配置工具防护规则：

```yaml
tdagent:
  tool-guard:
    enabled: true
    strict-mode: true
    pending-expire-minutes: 60  # 审批请求过期时间（分钟）
```

**工具风险等级**：
- **LOW**：低风险，可直接执行（如 `get_session_id`、`get_history_preview`）
- **MEDIUM**：中风险，需要审批（如 `move_file`）
- **HIGH**：高风险，需要严格审批（如 `run_shell_command`、`fs_write_file`）

**工具分类**：
- **BUILTIN**：内置工具（会话管理、历史记录、记忆搜索）
- **SANDBOX**：沙盒工具（Shell 命令、Python 代码、文件操作）
- **BROWSER**：浏览器工具（网页导航、截图、元素交互）

#### 自定义 Skill

在 `hivemind-agent-engine/src/main/resources/skills/` 目录下创建自定义技能：

```json
{
  "name": "code_review",
  "description": "代码审查技能",
  "prompt": "你是一名资深代码审查专家...",
  "tools": ["read_file", "search_code"]
}
```

**Skill 配置选项**：
```yaml
tdagent:
  skill:
    enabled: true
    builtin-location: classpath:skills
    builtin-enabled-by-default: true
    custom-enabled-by-default: true
    storage:
      oss:
        enabled: false
        accessKeyId: ""
        accessKeySecret: ""
```

#### MCP 集成

配置 Model Context Protocol 以扩展 Agent 能力。MCP 配置存储在 MySQL 数据库中，支持动态管理：

```yaml
# 在数据库 sys_mcp 表中配置
# 支持 SYSTEM 和 CUSTOM 两种类型
# 可以启用/禁用单个 Server 或 Tool
```

**MCP 表结构**：
- `mcp_server`：MCP 服务器配置
- `mcp_server_type`：类型（SYSTEM/CUSTOM）
- `mcp_tool`：具体工具配置
- `is_server_activated`：是否启用服务器
- `is_tool_activated`：是否启用工具

---

## 🛡️ Tool Guard 安全体系

HiveMind 提供三层工具调用防护机制，通过 `ToolGuardEngine` 和 `GuardedAgentTool` 实现：

```
┌─────────────────────────────────────────────────────────────┐
│                      Tool Guard 决策流程                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户请求 → Agent 推理 → 工具调用                            │
│                          │                                  │
│                          ▼                                  │
│              ┌───────────────────────┐                      │
│              │  检查 denyPatterns    │                      │
│              │  (无条件禁止)          │                      │
│              └───────────┬───────────┘                      │
│                          │ 命中                             │
│                          ▼                                  │
│                    直接阻断 ❌                               │
│                                                             │
│              ┌───────────────────────┐                      │
│              │  检查 approvalRequired │                      │
│              │  (需要审批)            │                      │
│              └───────────┬───────────┘                      │
│                          │ 命中规则                         │
│                          ▼                                  │
│              写入 pending_approval → 返回 ToolSuspendException│
│                          │                                  │
│                          ▼                                  │
│              用户确认审批                                    │
│                          │                                  │
│                          ▼                                  │
│              验证参数一致性 → 消费审批 → 允许执行 ✅          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 内置工具清单

| 工具名称 | 分类 | 风险等级 | 描述 |
|---------|------|---------|------|
| `get_session_id` | BUILTIN | LOW | 获取当前会话 ID |
| `get_history_preview` | BUILTIN | LOW | 获取历史对话预览 |
| `search_memory` | BUILTIN | LOW | 搜索长期记忆（ReMe） |
| `run_shell_command` | SANDBOX | HIGH | 执行 Shell 命令 |
| `run_ipython_cell` | SANDBOX | HIGH | 执行 Python 代码 |
| `fs_read_file` | SANDBOX | LOW | 读取文件内容 |
| `fs_write_file` | SANDBOX | HIGH | 写入文件内容 |
| `edit_file` | SANDBOX | HIGH | 编辑文件（查找替换） |
| `move_file` | SANDBOX | MEDIUM | 移动或重命名文件 |
| `list_directory` | SANDBOX | LOW | 列出目录内容 |
| `search_files` | SANDBOX | LOW | 搜索文件 |
| `browser_navigate` | BROWSER | HIGH | 导航到指定网页 |
| `browser_snapshot` | BROWSER | LOW | 获取网页文本快照 |
| `browser_click` | BROWSER | HIGH | 点击网页元素 |
| `browser_type` | BROWSER | HIGH | 在网页输入框中输入文本 |
| `browser_wait_for` | BROWSER | LOW | 等待页面元素加载 |
| `browser_take_screenshot` | BROWSER | LOW | 截取网页截图 |

---

## 🧪 测试

```bash
# 运行单元测试
mvn test

# 运行集成测试（需要 Docker）
mvn verify

# 前端测试
cd website
npm test
```

---

## 📄 配置说明

### 核心配置文件

| 文件 | 路径 | 描述 |
|------|------|------|
| `application-agentic.yaml` | `hivemind-agent-engine/src/main/resources/` | Agent 引擎配置 |
| `application-backend.yaml` | `hivemind-backend/src/main/resources/` | 后端业务配置 |
| `builtin_provider.json` | `hivemind-agent-engine/src/main/resources/provider/` | LLM 供应商配置 |
| `profiles/` | `hivemind-agent-engine/src/main/resources/profiles/` | SOUL 人格定义 |
| `skills/` | `hivemind-agent-engine/src/main/resources/skills/` | 内置技能定义 |
| `.env` | `reme-server/` | ReMe Server 环境变量 |

### Agent 引擎配置示例

```yaml
tdagent:
  observability:
    enabled: false
    url: http://localhost:5174  # AgentScope Studio 地址
  system-prompt:
    product-name: HiveMindAgent
    owner-name: HiveMind
    max-history-preview: 12
  model:
    provider-id: dashscope
    model-name: qwen-max
    max-iters: 200
    stream: true
    enable-thinking: true
  sandbox:
    enabled: true
    browser-enabled: true
    filesystem-enabled: true
    strict-startup: false
  tool-guard:
    enabled: true
    strict-mode: true
    pending-expire-minutes: 60
  reme:
    enabled: true
    base-url: http://localhost:8002
    timeout-seconds: 60
    top-k: 5
  compaction:
    enabled: true
    trigger-mode: TOKEN
    context-window-size: 0
    threshold-ratio: 0.85
    output-reserve-tokens: 0
    head-ratio: 0.10
    min-messages-since-compaction: 3
    trigger-message-count: 20
    trigger-character-count: 24000
    keep-recent-messages: 6
    max-summary-characters: 2400
  streaming:
    enabled: true
    incremental: true
  skill:
    enabled: true
    builtin-location: classpath:skills
    builtin-enabled-by-default: true
    custom-enabled-by-default: true
```

### 配置项说明

#### 模型配置 (model)
- `provider-id`：LLM 供应商 ID（dashscope/openai/deepseek 等）
- `model-name`：模型名称
- `max-iters`：最大迭代次数（默认 200）
- `stream`：是否启用流式输出
- `enable-thinking`：是否启用思考模式

#### 沙箱配置 (sandbox)
- `enabled`：是否启用沙箱
- `browser-enabled`：是否启用浏览器工具
- `filesystem-enabled`：是否启用文件系统工具
- `strict-startup`：是否严格启动模式

#### 记忆压缩配置 (compaction)
- `trigger-mode`：触发模式（TOKEN/MESSAGE_COUNT）
- `threshold-ratio`：触发压缩的阈值比例（0.85 = 85%）
- `keep-recent-messages`：保留最近消息数量
- `max-summary-characters`：摘要最大字符数

#### 流式输出配置 (streaming)
- `enabled`：是否启用流式输出
- `incremental`：是否增量输出

---

## 🧠 记忆系统架构

HiveMind 采用分层记忆架构，支持短期和长期记忆：

### 记忆层次

```
┌─────────────────────────────────────────────────────────────┐
│                     记忆系统架构                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │ 短期记忆         │    │ 长期记忆         │                │
│  │ (MongoDB)        │    │ (ReMe)          │                │
│  │                  │    │                  │                │
│  │ • 会话状态       │    │ • 跨会话记忆     │                │
│  │ • 对话历史       │    │ • 知识检索       │                │
│  │ • 工具上下文     │    │ • 经验积累       │                │
│  └─────────────────┘    └─────────────────┘                │
│                                                             │
│              ┌───────────────────────┐                      │
│              │ 记忆压缩机制           │                      │
│              │ (Context Compressor)  │                      │
│              │                       │                      │
│              │ • 自动压缩长对话       │                      │
│              │ • 保留关键信息         │                      │
│              │ • 生成对话摘要         │                      │
│              └───────────────────────┘                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 记忆组件

| 组件 | 职责 | 存储位置 |
|------|------|----------|
| **MongoConversationMemory** | 管理会话级对话历史 | MongoDB |
| **MongoAgentSession** | 持久化 Agent 内部状态 | MongoDB |
| **TdAgentMemoryManager** | 管理记忆压缩和清理 | MongoDB |
| **TdAgentReMeService** | 集成 ReMe 长期记忆 | ReMe Server |
| **ContextCompressor** | 执行对话压缩和摘要生成 | MongoDB |

### 记忆压缩机制

当对话消息数量超过阈值时，自动触发记忆压缩：

1. **触发条件**：消息数量 > 20 条 或 字符数 > 24000
2. **压缩策略**：保留最近 6 条消息，生成摘要
3. **摘要限制**：最大 2400 字符
4. **压缩比例**：阈值 85%，头部保留 10%

---

## 📚 文档资源

| 文档 | 描述 |
|------|------|
| [架构设计](docs/ARCHITECTURE.md) | Agent 引擎架构文档 |
| [请求链路](docs/copaw/请求链路.md) | 请求处理流程 |
| [Sandbox 指南](docs/agentscope/SandBox.md) | 沙箱镜像配置 |
| [ReMe 认证](reme-server/AUTH.md) | API Key 认证配置 |
| [ReMe 说明](reme-server/README.md) | ReMe Server 使用文档 |

---

## 📝 SOUL 人格系统

SOUL 系统是 HiveMind 的核心特性之一，通过 Markdown 文件定义 Agent 的人格和行为：

### SOUL 文件结构

```
profiles/
├── AGENT_TRIAGE/
│   └── SOUL.md          # 分拣 Agent 人格定义
├── AGENT_PLANNER/
│   └── SOUL.md          # 规划 Agent 人格定义
├── AGENT_REVIEWER/
│   └── SOUL.md          # 审查 Agent 人格定义
└── AGENT_EXECUTOR/
    └── SOUL.md          # 执行 Agent 人格定义
```

### SOUL 加载机制

通过 `SoulPromptLoader` 工具类加载 SOUL 文件：
- 支持缓存机制，避免重复读取
- 支持批量扫描所有 SOUL 配置
- 支持动态更新（清除缓存后重新加载）

### SOUL 文件示例

```markdown
# AGENT_TRIAGE - 消息分拣专家

## 角色定义
你是一名专业的消息分拣专家，负责识别用户消息的类型并路由到相应的处理器。

## 核心职责
1. 分析用户消息的意图
2. 判断是任务请求还是闲聊对话
3. 将任务路由到 Planner Agent
4. 直接回复闲聊消息

## 行为准则
- 准确识别任务关键词
- 保持回复简洁明了
- 记录分拣决策原因
```

---

## 💻 前端控制台

前端控制台基于 React + TypeScript + Ant Design 构建，提供以下功能：

### 核心功能

| 功能 | 描述 |
|------|------|
| **实时对话** | 与 AI 助理进行实时对话，支持流式输出 |
| **任务管理** | 查看和管理任务状态，支持状态流转 |
| **Agent 控制** | 启动/停止 Agent，查看 Agent 状态 |
| **配置管理** | 配置 LLM 供应商、MCP 服务器等 |
| **监控面板** | 查看 Token 使用情况、任务统计等 |

### 状态流转

```
Inbox → Pending → Triage → Planner → Reviewer → Assigned → Doing → Review → Done
                                                              ↓
                                                           Blocked
```

### 技术栈

- **前端框架**：React 18 + TypeScript
- **UI 组件**：Ant Design 5
- **状态管理**：Zustand
- **构建工具**：Vite
- **包管理**：npm/yarn

---

## 🔧 常见问题

### Q: 启动时提示 MongoDB 连接失败

确保 MongoDB 已启动且连接字符串正确：
```bash
docker ps | grep mongodb
# 检查 application-backend.yaml 中的 mongodb.uri 配置
```

### Q: Agent 无法调用工具

检查以下配置：
1. Tool Guard 是否禁用了该工具（检查 `denyPatterns`）
2. MCP 客户端是否正确配置
3. 模型是否支持 function calling
4. 工具风险等级是否需要审批（检查 `approvalRequired`）

### Q: ReMe Server 无法启动

1. 检查 Python 版本（3.10-3.13）
2. 确保 `.env` 文件中配置了必要的 API Key
3. 检查端口 8002 是否被占用
4. 检查 MongoDB 连接是否正常

### Q: 记忆压缩不生效

1. 检查 `compaction.enabled` 是否为 true
2. 确认消息数量超过 `trigger-message-count`（默认 20）
3. 检查字符数是否超过 `trigger-character-count`（默认 24000）
4. 查看日志中的压缩触发信息

---

## 🏛️ 架构设计原则

### DDD 分层架构

HiveMind 遵循领域驱动设计（DDD）的分层架构：

```
adapter → application → domain ← infrastructure
                      ↑
                   common
```

**依赖规则**：
- Domain 层不依赖 Infrastructure 层
- Application 层不直接依赖 Infrastructure 层
- 无跨层调用（如 Adapter 直接调用 Infrastructure）

### 模块职责

| 层 | 职责 | 包路径 |
|----|------|--------|
| **Adapter** | REST 控制器、外部接口适配 | `adapter/controller/` |
| **Application** | 业务服务实现、DTO 定义 | `application/` |
| **Domain** | 领域模型、领域服务、枚举常量 | `domain/` |
| **Infrastructure** | 数据访问、外部集成 | `infrastructure/` |
| **Common** | 共享异常、工具类、配置 | `common/` |

### 核心设计模式

1. **工厂模式**：`TdAgentFactory` 统一创建 Agent 实例
2. **装饰器模式**：`GuardedAgentTool` 包装原始工具，添加安全防护
3. **策略模式**：`ToolGuardEngine` 根据配置执行不同的安全策略
4. **观察者模式**：Hook 机制（MemoryCompactionHook、ToolGuardHook）
5. **责任链模式**：Tool Guard 三层防护机制

---

## 🚀 性能优化

### Agent 执行优化

- **最大迭代次数**：默认 200 轮，防止无限循环
- **流式输出**：支持增量流式输出，实时展示思考过程
- **记忆压缩**：自动压缩长对话，减少 Token 消耗
- **工具缓存**：工具定义缓存，避免重复创建

### 数据库优化

- **MongoDB**：会话状态、对话历史、工具配置
- **MySQL**：用户管理、任务管理、MCP 配置
- **Caffeine**：本地缓存，减少数据库查询

### 记忆优化

- **分层记忆**：短期记忆（MongoDB）+ 长期记忆（ReMe）
- **智能压缩**：基于 Token/消息数的自动压缩
- **摘要生成**：保留关键信息，生成对话摘要

---

## 🤝 贡献指南

欢迎贡献代码、文档或建议！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 开发规范

- **代码风格**：遵循 Java 编码规范，使用 Lombok 简化代码
- **测试覆盖**：新功能需添加单元测试，关键路径需集成测试
- **文档更新**：修改功能时同步更新 README 和相关文档
- **提交信息**：使用语义化提交信息（如 `feat: 添加新功能`）

---

## 📄 许可证

本项目采用 Apache 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

<div align="center">

**HiveMind** - 让 AI 协作高效有序

[⬆ 返回顶部](#hivemind)

</div>
