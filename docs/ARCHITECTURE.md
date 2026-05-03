# TDAgent Agent Engine 架构文档

> 基于 AgentScope-Java 框架的 AI Agent 后端引擎

## 📐 整体架构

项目采用 **DDD（领域驱动设计）分层架构**，各层职责清晰：

```
┌─────────────────────────────────────────────────────────┐
│                   客户端 (Web/Mobile)                    │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP/SSE
┌────────────────────────▼────────────────────────────────┐
│              adapter/ (适配器层)                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Controller: TdAgentChatController,               │  │
│  │             TdAgentSkillController               │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              application/ (应用层)                       │
│  ┌──────────────────┐  ┌────────────────────────────┐  │
│  │ Service 接口     │  │ DTO (ChatRequest/Response) │  │
│  │ - ITdAgentChat   │  │ - SessionHistoryResponse   │  │
│  │ - IStreaming     │  │ - ToolApprovalAction       │  │
│  │ - IChatCommand   │  │ - SkillUpsertRequest       │  │
│  └────────┬─────────┘  └────────────────────────────┘  │
│           │ 实现                                        │
│  ┌────────▼─────────┐                                  │
│  │ Service 实现     │                                  │
│  │ - ChatServiceImpl│                                  │
│  │ - StreamingImpl  │                                  │
│  │ - CommandImpl    │                                  │
│  └────────┬─────────┘                                  │
└────────────────────────┼────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                domain/ (领域层) ⭐ 核心                  │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ agent/       - ReActAgent 工厂、Hook、Prompt    │   │
│  │ memory/      - 对话记忆模型、ReMe 服务          │   │
│  │ session/     - 会话状态模型、会话管理           │   │
│  │ tool/        - Tool Guard、工具审批、内置工具   │   │
│  │ skill/       - Skill 模型、SkillBox 管理        │   │
│  │ provider/    - 模型供应商注册表、模型配置       │   │
│  │ streaming/   - 流式事件、会话注册               │   │
│  │ shared/      - 领域枚举、领域常量               │   │
│  └─────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│           infrastructure/ (基础设施层)                   │
│  ┌──────────────────────────────────────────────────┐  │
│  │ mongo/    - Spring Data MongoDB Repository       │  │
│  │ mysql/    - MyBatis Plus Mapper + PO             │  │
│  │ sandbox/  - 沙箱环境管理                         │  │
│  │ config/   - TdAgentProperties (外部化配置)       │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              common/ (通用层)                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │ exception/  - BizException, ErrorCodeEnum        │  │
│  │ util/       - MessageMapper, SoulPromptLoader    │  │
│  │ config/     - JacksonConfig                      │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 📦 包结构详细说明

### 1. domain/ - 领域层

领域层是系统的**核心**，包含所有业务模型和规则。

#### domain/agent/ - Agent 聚合根

```
agent/
├── factory/
│   ├── TdAgentFactory           # ReActAgent 创建工厂
│   ├── TdAgentModelFactory      # 模型实例工厂（DashScope/OpenAI）
│   └── TdAgentToolkitFactory    # Toolkit 工厂
├── hook/
│   ├── TdAgentMemoryCompactionHook  # 记忆压缩 Hook
│   └── TdAgentToolGuardHook         # Tool Guard Hook
├── core/
│   ├── ConversationSessionContext   # 会话上下文
│   └── TdAgentPromptService         # System Prompt 构建
└── MongoConversationMemory      # MongoDB 对话记忆实现
```

#### domain/memory/ - 记忆聚合

```
memory/
├── model/
│   ├── ConversationMemoryDocument   # 完整对话历史 Document
│   ├── ConversationViewDocument     # 会话视图 Document（摘要）
│   ├── StoredMessage                # 存储的消息
│   └── StoredMessageContent         # 消息内容
├── service/ (从 agents/memory/ 迁移)
│   ├── TdAgentMemoryManager         # 记忆管理器
│   └── reme/TdAgentReMeService      # ReMe 长期记忆集成
└── repository/ (接口定义在领域层)
```

#### domain/tool/ - 工具聚合

```
tool/
├── model/
│   └── ToolApprovalDocument         # 工具审批 Document
├── guard/
│   ├── ToolGuardEngine              # Tool Guard 决策引擎
│   ├── ToolGuardDecision            # 决策结果
│   ├── GuardedAgentTool             # 被守卫包装的工具
│   └── approval/
│       └── ToolApprovalService      # 工具审批服务
└── builtin/
    ├── TdAgentBuiltinTools          # 内置工具注册
    └── SystemTimeTool               # 系统时间工具
```

#### domain/shared/ - 共享领域对象

```
shared/
├── enums/
│   ├── TdAgentProviderType          # 供应商类型 (DASHSCOPE/OPENAI)
│   ├── ToolRiskLevel                # 工具风险等级
│   └── ToolApprovalStatus           # 审批状态
└── constants/
    └── SoulPromptConstants          # Soul Prompt 常量
```

### 2. application/ - 应用层

应用层负责**用例编排**，协调领域对象完成具体业务场景。

```
application/
├── ITdAgentChatService              # 同步聊天服务
├── ITdAgentStreamingService         # 流式聊天服务 (SSE)
├── IChatCommandService              # 命令处理服务 (/clear, /history)
├── ConversationPersistenceService   # 对话持久化服务
├── impl/
│   ├── TdAgentChatServiceImpl       # 同步聊天实现
│   ├── TdAgentStreamingServiceImpl  # 流式聊天实现
│   └── ChatCommandServiceImpl       # 命令处理实现
└── dto/
    ├── ChatRequest                  # 聊天请求 DTO
    ├── ChatResponse                 # 聊天响应 DTO
    ├── SessionHistoryResponse       # 会话历史响应
    ├── InterruptRequest             # 中断请求 DTO
    ├── SkillUpsertRequest           # Skill 更新请求 DTO
    └── ToolApprovalActionRequest    # 工具审批请求 DTO
```

### 3. infrastructure/ - 基础设施层

基础设施层提供**技术实现**，包括数据库访问、外部服务集成等。

```
infrastructure/
├── mongo/
│   ├── repository/                  # Spring Data MongoDB Repository
│   │   ├── ConversationMemoryRepository
│   │   ├── ConversationViewRepository
│   │   ├── AgentSessionStateRepository
│   │   ├── AgentSkillRepository
│   │   ├── AgentSkillStateRepository
│   │   └── ToolApprovalRepository
│   └── config/
│       └── MongoDbDiagnosticConfig  # MongoDB 诊断配置
├── mysql/
│   ├── mapper/                      # MyBatis Plus Mapper
│   │   └── SkillMetaManageMapper
│   ├── po/                          # 持久化对象
│   │   └── SkillMetaManagePO
│   └── support/                     # 仓储实现
│       ├── dto/
│       │   └── SkillPageQuery
│       ├── SkillMetaManageSupport
│       └── impl/
│           └── SkillMetaManageSupportImpl
├── sandbox/
│   └── TdAgentSandboxManager        # 沙箱环境管理
└── config/
    └── TdAgentProperties            # 外部化配置属性
```

## 🔄 核心流程

### 1. 同步聊天流程

```
Client → TdAgentChatController
            ↓
      TdAgentChatServiceImpl.chat()
            ↓
      ├─ 检查命令 (IChatCommandService)
      ├─ 创建会话上下文 (ConversationSessionContext)
      ├─ 创建 Agent (TdAgentFactory.createAgent())
      │     ├─ 构建 System Prompt
      │     ├─ 初始化 MongoDB Memory
      │     ├─ 创建 Toolkit + SkillBox
      │     ├─ 注册 Hooks (MemoryCompaction, ToolGuard)
      │     └─ 配置 ReMe 长期记忆（可选）
      ├─ 恢复会话状态 (AgentSessionStateService)
      ├─ Agent.call(userMessage)  ← AgentScope 执行
      ├─ 保存会话状态
      └─ 返回 ChatResponse
```

### 2. 流式聊天流程

```
Client → TdAgentChatController.stream()
            ↓
      TdAgentStreamingServiceImpl.stream()
            ↓
      ├─ 创建 SSE Emitter
      ├─ 创建 Agent (同同步流程)
      ├─ 订阅 Agent 流式事件
      ├─ 转发事件到 SSE Emitter
      │     ├─ TEXT_CHUNK - 文本片段
      │     ├─ TOOL_CALL - 工具调用
      │     ├─ TOOL_RESULT - 工具结果
      │     └─ COMPLETE - 完成
      └─ 返回 SseEmitter
```

### 3. Tool Guard 审批流程

```
Agent 调用工具
      ↓
TdAgentToolGuardHook.intercept()
      ↓
      ├─ 工具风险评估 (ToolGuardEngine)
      ├─ 检查是否需要审批
      │     ├─ 否 → 直接执行
      │     └─ 是 → 创建 ToolApprovalDocument
      │           ↓
      │     返回 PAUSE 状态，等待用户审批
      │           ↓
      ├─ 用户批准 → approveAndResume()
      ├─ 用户拒绝 → rejectAndResume()
      └─ 继续执行或中断
```

## 🛠️ 技术栈

| 类别              | 技术                   | 版本              |
|-----------------|----------------------|-----------------|
| **AI Agent 框架** | AgentScope-Java      | 1.0.12          |
| **Web 框架**      | Spring Boot          | 4.0.x           |
| **文档数据库**       | MongoDB              | via Spring Data |
| **关系数据库**       | MySQL + MyBatis Plus | 3.5.15          |
| **长期记忆**        | ReMe Service         | AgentScope 扩展   |
| **可观测性**        | AgentScope Studio    | localhost:5174  |
| **JSON 处理**     | FastJSON2            | 2.0.43          |
| **本地缓存**        | Caffeine             | 3.1.8           |

## 📝 开发规范

### 依赖方向

```
adapter → application → domain ← infrastructure
                      ↑
                   common
```

**严格禁止**：

- ❌ 领域层依赖基础设施层
- ❌ 应用层直接依赖基础设施层
- ❌ 跨层调用（如 adapter 直接调用 infrastructure）

### 命名约定

- 领域模型：`*Document`, `*Model`
- 仓储接口：`*Repository` (领域层定义)
- 仓储实现：`*RepositoryImpl` 或 Spring Data Repository
- 应用服务：`I*Service` (接口), `*ServiceImpl` (实现)
- DTO：`*Request`, `*Response`
- 工厂：`*Factory`
- 异常：`*Exception`

### 何时放在哪个包

| 组件类型   | 应该放在                           | 示例                           |
|--------|--------------------------------|------------------------------|
| 业务模型   | `domain/*/model/`              | ConversationMemoryDocument   |
| 业务规则   | `domain/*/service/`            | TdAgentMemoryManager         |
| 用例编排   | `application/`                 | TdAgentChatServiceImpl       |
| 数据传输   | `application/dto/`             | ChatRequest                  |
| 数据库访问  | `infrastructure/*/repository/` | ConversationMemoryRepository |
| 外部 API | `adapter/controller/`          | TdAgentChatController        |
| 通用异常   | `common/exception/`            | BizException                 |
| 领域枚举   | `domain/shared/enums/`         | ToolRiskLevel                |

## 📚 相关文档

- [AgentScope-Java 官方文档](https://agentscope.io/)

---

*最后更新：2026-05-03 | 作者：LiangshouX*
