# TangDynasty 项目架构设计方案

> 设计日期：2026-05-08  
> 状态：待审核

---

## 一、当前架构分析

### 1.1 现状概览

```
tang-dynasty-launcher/          ← 启动模块 (Spring Boot 入口)
    ├── 依赖 tang-dynasty-backend
    └── 依赖 tang-dynasty-agent-engine

tang-dynasty-backend/           ← 业务后端
    ├── 用户认证 (Auth)
    ├── 奏折/任务管理 (Edict)
    ├── 系统配置 (Sys*)
    ├── 定时任务 (ScheduledJob)
    └── 依赖 tang-dynasty-agent-engine (⚠️ 反向依赖)

tang-dynasty-agent-engine/      ← AI Agent 引擎
    ├── Agent 核心 (Factory, Model, Tool, Hook, Guard)
    ├── 记忆管理 (ReMe, MongoDB 对话存储)
    ├── 会话管理 (Session, State)
    ├── Skill 系统
    ├── Profile 配置 CRUD      ← ⚠️ 混入 CRUD
    ├── Tool 配置 CRUD          ← ⚠️ 混入 CRUD
    ├── Token 用量 CRUD         ← ⚠️ 混入 CRUD
    ├── Skill 云端 CRUD + OSS   ← ⚠️ 混入 CRUD
    └── 8 个 MongoDB Repository ← ⚠️ 混入数据访问

reme-server/                    ← ReMe 长期记忆服务 (Python)
    └── 独立部署，通过 HTTP API 通信
```

### 1.2 核心问题

| 问题 | 描述 | 严重程度 |
|------|------|----------|
| **P1: 职责混杂** | agent-engine 中混入大量 CRUD 操作（Profile、Tool Config、Token Usage、Skill Meta），违背了"纯引擎"定位 | 🔴 高 |
| **P2: 数据访问泄漏** | agent-engine 直接持有 8 个 MongoDB Repository + 2 个 MySQL Mapper，基础设施侵入核心层 | 🔴 高 |
| **P3: 反向依赖** | backend 依赖 agent-engine，但 agent-engine 本应是独立的、可复用的引擎库 | 🟡 中 |
| **P4: Controller 膨胀** | agent-engine 有 5 个 Controller，承担了部分业务 API 职责 | 🟡 中 |
| **P5: 配置分散** | 数据库配置分散在 backend 和 agent-engine 中，难以统一管理 | 🟡 中 |
| **P6: 边界模糊** | Agent 需要读取业务数据（任务、用户），但没有清晰的数据访问契约 | 🟡 中 |

### 1.3 问题根源

**核心矛盾**：agent-engine 的定位是"AI Agent 核心引擎"，但为了实现完整的 Agent 对话功能，它不得不处理：
- 对话历史的持久化（MongoDB）
- 用户偏好配置（Profile CRUD）
- 工具权限管理（Tool Config CRUD）
- 资源用量统计（Token Usage CRUD）
- 技能包管理（Skill CRUD + OSS）

这些操作本身不是 Agent 引擎的核心逻辑，而是**支撑 Agent 运行的基础设施和业务服务**。

---

## 二、目标架构设计原则

### 2.1 核心原则

1. **单一职责**：每个模块只关注自己核心的业务领域
2. **依赖倒置**：高层模块不依赖低层模块，都依赖抽象接口
3. **单向依赖**：依赖方向必须清晰、单向
4. **基础设施后置**：数据访问、存储等基础设施由业务层拥有，引擎通过接口使用
5. **可测试性**：核心引擎可以脱离数据库独立测试

### 2.2 模块定位

| 模块 | 定位 | 类比 |
|------|------|------|
| **tang-dynasty-agent-engine** | AI Agent 核心引擎库，纯 Java 库，**不含 Web 层** | 类似 JPA、MyBatis 这样的框架库 |
| **tang-dynasty-backend** | 业务后端，拥有所有数据访问和业务逻辑 | 类似 Spring Data 这样的数据服务层 |
| **tang-dynasty-launcher** | 启动入口，组合所有模块 | 类似 Application.java 启动器 |
| **reme-server** | 独立的长期记忆微服务 | 外部依赖服务 |

---

## 三、目标架构设计

### 3.1 整体架构图

```
┌──────────────────────────────────────────────────────────────────┐
│                     tang-dynasty-launcher                        │
│                   (Spring Boot 启动入口)                           │
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐   │
│ │                  Spring ApplicationContext                  │   │
│ │                                                            │   │
│ │   ┌────────────────────────────────────────────────────┐   │   │
│   │ │          tang-dynasty-backend (业务层)             │   │   │
│   │ │                                                     │   │   │
│   │ │  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │   │   │
│   │ │  │  Controller  │  │   Service   │  │ Repository │ │   │   │
│   │ │  │   (API层)    │→ │  (业务层)   │→ │  (数据层)   │ │   │   │
│   │ │  └─────────────┘  └──────┬──────┘  └────────────┘ │   │   │
│   │ │                          │                         │   │   │
│   │ │  ┌───────────────────────┼──────────────────────┐  │   │   │
│   │ │  │   所有 MySQL Mapper   │   所有 MongoDB Repo   │  │   │   │
│   │ │  │   所有业务 CRUD       │   所有文档存储        │  │   │   │
│   │ │  └───────────────────────┼──────────────────────┘  │   │   │
│   │ └─────────────────────────┼─────────────────────────┘   │   │
│   │                           │ 实现接口                     │   │
│   │ ┌─────────────────────────┼─────────────────────────┐   │   │
│   │ │   AgentServiceAdapter   │ (Agent 业务适配层)       │   │   │
│   │ │   - 提供用户数据         ↓                         │   │   │
│   │ │   - 提供任务数据    ┌──────────────────────┐       │   │   │
│   │ │   - 提供配置数据    │   接口契约层          │       │   │   │
│   │ │   - 持久化对话       │  AgentStoragePort    │       │   │   │
│   │ │   - 记录Token用量    │  AgentConfigPort     │       │   │   │
│   │ │   - 管理Skill       │  AgentSkillPort      │       │   │   │
│   │ │   - 管理Profile     │  AgentProfilePort    │       │   │   │
│   │ │                     └──────────────────────┘       │   │   │
│   └─────────────────────────────┬───────────────────────┘   │   │
│                                 │ 依赖                       │   │
│   ┌─────────────────────────────┼───────────────────────┐   │   │
│   │  tang-dynasty-agent-engine  │ (纯引擎库)             │   │   │
│   │                             ↓                        │   │   │
│   │  ┌──────────────────────────────────────────────┐   │   │   │
│   │  │          Agent 核心引擎                        │   │   │   │
│   │  │                                               │   │   │   │
│   │  │  TdAgentFactory ← 创建 Agent                   │   │   │   │
│   │  │  TdAgentModelFactory ← 模型                    │   │   │   │
│   │  │  TdAgentToolkitFactory ← 工具集                │   │   │   │
│   │  │  ToolGuardEngine ← 工具防护                    │   │   │   │
│   │  │  TdAgentSkillService ← Skill 管理              │   │   │   │
│   │  │  MemoryManager ← 记忆管理                      │   │   │   │
│   │  │  SessionManager ← 会话管理                     │   │   │   │
│   │  │  ProviderRegistry ← 模型供应商                 │   │   │   │
│   │  │                                               │   │   │   │
│   │  │  ┌────────────────────────────────────────┐   │   │   │   │
│   │  │  │   端口接口 (Ports) - 引擎需要的能力抽象   │   │   │   │   │
│   │  │  │   - ConversationStoragePort            │   │   │   │   │
│   │  │  │   - SessionStateStoragePort            │   │   │   │   │
│   │  │  │   - ProfileStoragePort                 │   │   │   │   │
│   │  │  │   - SkillStoragePort                   │   │   │   │   │
│   │  │  │   - ToolConfigStoragePort              │   │   │   │   │
│   │  │  │   - TokenUsageRecorderPort             │   │   │   │   │
│   │  │  └────────────────────────────────────────┘   │   │   │   │
│   │  └──────────────────────────────────────────────┘   │   │   │
│   └─────────────────────────────────────────────────────┘   │   │
│                                                             │   │
│   路由配置:                                                  │   │
│   /api/v1/auth/*          → backend AuthController          │   │
│   /api/v1/edict/*         → backend EdictController         │   │
│   /api/v1/sys-*/*         → backend SysController           │   │
│   /api/v1/tdagent/*       → backend AgentApiController      │   │
│                                 ↓ 委托                       │   │
│                           agent-engine 核心引擎              │   │
│                                                             │   │
│   └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 详细模块设计

#### 3.2.1 tang-dynasty-agent-engine (纯引擎库)

**定位**：AI Agent 核心引擎，提供 Agent 创建、对话执行、工具调用、记忆管理等能力

**关键变化**：
- ❌ 移除所有 Controller
- ❌ 移除所有 MongoDB Repository
- ❌ 移除所有 MySQL Mapper/PO/Support
- ❌ 移除所有 CRUD 服务实现
- ❌ 移除 OSS 存储相关代码
- ✅ 保留 Agent 核心组件（Factory、Model、Tool、Hook、Guard、Skill）
- ✅ 保留领域模型（Document 改为纯 Java Bean）
- ✅ 新增**端口接口**（Ports），定义引擎需要的存储能力抽象

**包结构**：

```
com.liangshou.tangdynasty.agentic/
├── core/                           # 核心引擎（原 agents/ 目录重命名）
│   ├── agent/                      # Agent 创建
│   │   ├── TdAgentFactory.java
│   │   ├── TdAgentModelFactory.java
│   │   └── TdAgentToolkitFactory.java
│   ├── prompt/                     # Prompt 构建
│   │   └── TdAgentPromptService.java
│   ├── streaming/                  # 流式事件
│   │   ├── TdAgentStreamEvent.java
│   │   └── TdAgentActiveSessionRegistry.java
│   ├── guard/                      # 工具防护
│   │   ├── ToolGuardEngine.java
│   │   ├── ToolGuardDecision.java
│   │   └── GuardedAgentTool.java
│   ├── hook/                       # Agent Hooks
│   │   ├── TdAgentMemoryCompactionHook.java
│   │   └── TdAgentToolGuardHook.java
│   └── tools/                      # 系统工具
│       ├── SystemToolRegistry.java
│       ├── TdAgentBuiltinTools.java
│       └── SystemTimeTool.java
│
├── memory/                         # 记忆管理
│   ├── TdAgentMemoryManager.java
│   └── reme/
│       └── TdAgentReMeService.java
│
├── session/                        # 会话管理
│   └── AgentSessionStateService.java
│
├── skill/                          # Skill 管理
│   └── TdAgentSkillService.java
│
├── provider/                       # 模型供应商
│   ├── TdAgentProviderRegistry.java
│   ├── TdAgentProviderDescriptor.java
│   └── TdAgentModelDescriptor.java
│
├── port/                           # ★ 新增：端口接口层（Ports）
│   ├── ConversationStoragePort.java        # 对话存储接口
│   ├── SessionStateStoragePort.java        # 会话状态存储接口
│   ├── ProfileStoragePort.java             # Profile 存储接口
│   ├── SkillStoragePort.java               # Skill 存储接口
│   ├── ToolConfigStoragePort.java          # 工具配置存储接口
│   ├── TokenUsageRecorderPort.java         # Token 用量记录接口
│   └── SkillArtifactStoragePort.java       # Skill 文件存储接口（OSS）
│
├── model/                          # 领域模型（原 domain/ 目录，纯 Java Bean）
│   ├── conversation/
│   │   ├── ConversationMemory.java
│   │   └── ConversationView.java
│   ├── session/
│   │   └── AgentSessionState.java
│   ├── profile/
│   │   └── AgentProfile.java
│   ├── skill/
│   │   ├── AgentSkill.java
│   │   └── AgentSkillState.java
│   ├── tool/
│   │   ├── ToolConfig.java
│   │   └── ToolApproval.java
│   └── shared/                     # 共享枚举/常量
│
├── config/                         # 配置
│   └── TdAgentProperties.java
│
└── common/                         # 通用组件
    ├── exception/
    └── util/
```

**关键设计说明 - 端口接口（Ports）**：

这是架构的核心变化，采用**端口适配器模式**（Port-Adapter Pattern），引擎通过接口定义它需要的能力，而不关心具体实现。

```java
// 示例：对话存储端口接口
public interface ConversationStoragePort {
    void save(String userId, String sessionId, List<Message> messages);
    List<Message> load(String userId, String sessionId);
    ConversationView findByUserId(String userId);
    void deleteByUserIdAndSessionId(String userId, String sessionId);
}

// 示例：Profile 存储端口接口
public interface ProfileStoragePort {
    Optional<AgentProfile> findByUserId(String userId);
    void save(String userId, AgentProfile profile);
    void deleteByUserId(String userId);
}
```

#### 3.2.2 tang-dynasty-backend (业务后端)

**定位**：完整的业务后端，拥有所有数据访问、业务逻辑和 API

**关键变化**：
- ✅ 保留所有现有 Controller、Service、Repository、Mapper
- ✅ 新增 agent-engine 中移除的 CRUD 功能
- ✅ 实现 agent-engine 定义的所有端口接口（Ports）
- ✅ 新增 Agent API Controller，作为 agent-engine 的 HTTP 入口
- ⚠️ 移除对 agent-engine 的依赖（改为 launcher 依赖它）

**新增包结构**：

```
com.liangshou/
├── ... (现有结构保持不变)
│
├── agent/                          # ★ 新增：Agent 业务适配层
│   ├── controller/
│   │   └── AgentApiController.java         # 统一 Agent API 入口
│   │       ├── POST /api/v1/tdagent/chat   # 对话
│   │       ├── GET  /api/v1/tdagent/chat/stream  # 流式对话
│   │       ├── GET  /api/v1/tdagent/sessions   # 会话列表
│   │       └── ...
│   │
│   ├── service/
│   │   ├── AgentChatService.java             # Agent 对话业务服务
│   │   ├── AgentStreamingService.java        # Agent 流式业务服务
│   │   └── AgentConversationService.java     # 会话管理服务
│   │
│   └── adapter/                      # ★ 实现 agent-engine 的端口接口
│       ├── MongoConversationStorageAdapter.java    # 实现 ConversationStoragePort
│       ├── MongoSessionStateStorageAdapter.java    # 实现 SessionStateStoragePort
│       ├── MongoProfileStorageAdapter.java         # 实现 ProfileStoragePort
│       ├── MongoSkillStorageAdapter.java           # 实现 SkillStoragePort
│       ├── MongoToolConfigStorageAdapter.java      # 实现 ToolConfigStoragePort
│       ├── MySqlTokenUsageRecorderAdapter.java     # 实现 TokenUsageRecorderPort
│       └── OssSkillArtifactStorageAdapter.java     # 实现 SkillArtifactStoragePort
│
└── infrastructure/
    ├── mongo/                        # 扩展：新增 Agent 相关 Repository
    │   ├── repository/
    │   │   ├── (现有...)
│   │   ├── ConversationMemoryRepository.java   # 从 engine 移入
│   │   ├── ConversationViewRepository.java     # 从 engine 移入
│   │   ├── AgentProfileRepository.java         # 从 engine 移入
│   │   ├── AgentSkillRepository.java           # 从 engine 移入
│   │   ├── AgentSkillStateRepository.java      # 从 engine 移入
│   │   ├── AgentSessionStateRepository.java    # 从 engine 移入
│   │   ├── ToolConfigRepository.java           # 从 engine 移入
│   │   └── ToolApprovalRepository.java         # 从 engine 移入
│   └── domain/                   # MongoDB 文档实体（从 engine domain 移入）
│       ├── ConversationMemoryDocument.java
│       ├── ConversationViewDocument.java
│       ├── AgentProfileDocument.java
│       ├── AgentSkillDocument.java
│       ├── AgentSkillStateDocument.java
│       ├── AgentSessionStateDocument.java
│       ├── ToolConfigDocument.java
│       └── ToolApprovalDocument.java
│
    └── storage/                      # 从 engine 移入
        ├── OssProperties.java
        ├── ObjectStorageService.java
        ├── AliyunOssStorageService.java
        └── SkillFileStorageService.java
```

#### 3.2.3 tang-dynasty-launcher (启动模块)

**定位**：纯粹的启动入口，组合所有模块

**关键变化**：
- ✅ 保持现有结构不变
- ✅ 修改依赖方向：只依赖 backend，不再直接依赖 agent-engine
- ✅ 在 Spring 配置中注册端口接口的 Adapter 绑定

```java
@SpringBootApplication
@ComponentScan("com.liangshou")
@MapperScan({
    "com.liangshou.infrastructure.datasource.mapper",
    // Agent 相关的 MySQL Mapper（如果需要）
})
public class TangApplication {
    public static void main(String[] args) {
        SpringApplication.run(TangApplication.class, args);
    }
}
```

**配置合并**：

```yaml
# application.yaml
spring:
  profiles:
    include:
      - backend              # backend 配置
    # agentic 配置合并到 backend 中，不再单独 include
```

---

## 四、依赖关系设计

### 4.1 模块依赖图

```
tang-dynasty-launcher
    └── tang-dynasty-backend
            └── tang-dynasty-agent-engine  ← 单向依赖，backend 依赖 engine
```

**说明**：
- launcher 只依赖 backend
- backend 依赖 agent-engine（作为库使用）
- agent-engine 不依赖任何其他项目模块，是纯库

### 4.2 agent-engine pom.xml 变化

```xml
<!-- 移除的依赖 -->
- spring-boot-starter-web              ← 不再有 Web 层
- spring-boot-starter-data-mongodb     ← 不再有数据访问
- mybatis-plus-spring-boot4-starter    ← 不再有数据访问
- aliyun-sdk-oss                       ← 不再有 OSS 存储
- caffeine                             ← 不再有本地缓存服务

<!-- 保留的依赖 -->
+ agentscope-core                      ← AgentScope 核心 SDK
+ agentscope-runtime-agentscope        ← AgentScope 运行时
+ agentscope-extensions-reme           ← ReMe 客户端
+ agentscope-extensions-studio         ← Studio 可观测性
+ agentscope-extensions-autocontext-memory  ← 自动上下文记忆
+ spring-boot-starter (仅基础)          ← Spring 基础
+ lombok                               ← 代码简化
+ jackson                              ← JSON 序列化
```

### 4.3 backend pom.xml 变化

```xml
<!-- 新增的依赖 -->
+ tang-dynasty-agent-engine            ← 新增：依赖 agent-engine 库
+ spring-boot-starter-data-mongodb     ← 从 engine 移入
+ aliyun-sdk-oss                       ← 从 engine 移入
+ caffeine                             ← 从 engine 移入

<!-- 保留的依赖 -->
+ mybatis-plus-spring-boot4-starter
+ mysql-connector-j
+ spring-boot-starter-security
+ jjwt
+ ... (其他现有依赖)
```

---

## 五、数据流设计

### 5.1 Agent 对话流程

```
用户请求
   ↓
[backend] AgentApiController.chat()
   ↓
[backend] AgentChatService.execute()
   ├── 1. 从 MySQL 加载用户配置 → ProfileStorageAdapter
   ├── 2. 从 MongoDB 加载对话历史 → ConversationStorageAdapter
   ├── 3. 从 MySQL 加载工具配置 → ToolConfigStorageAdapter
   ├── 4. 从 MongoDB 加载 Skill → SkillStorageAdapter
   ↓
[engine] TdAgentFactory.create()
   ├── 使用上述数据配置 Agent
   ├── 注入 Storage Ports 实现
   ↓
[engine] Agent.call(userMessage)
   ├── 执行 ReAct 循环
   ├── 调用 Tool Guard 检查
   ├── 调用 ReMe 服务检索长期记忆
   ├── 调用 Hook 进行记忆压缩
   ↓
[engine] 通过 Port 回调保存数据
   ├── ConversationStoragePort.save()  → [backend] 保存对话到 MongoDB
   ├── TokenUsageRecorderPort.record() → [backend] 记录 Token 用量到 MySQL
   ↓
[backend] AgentChatService 组装响应
   ↓
返回给用户
```

### 5.2 端口接口调用时序

```
┌──────────┐          ┌──────────┐          ┌──────────┐
│ backend  │          │ engine   │          │ 外部服务  │
└────┬─────┘          └────┬─────┘          └────┬─────┘
     │                     │                     │
     │  1. 提供数据         │                     │
     │────────────────────>│                     │
     │  (通过 Port 接口)    │                     │
     │                     │                     │
     │                     │  2. 创建 Agent       │
     │                     │                     │
     │                     │  3. 执行对话         │
     │                     │                     │
     │                     │  4. 检索长期记忆 ──────────> ReMe Server
     │                     │                     │
     │                     │  5. 回调保存数据      │
     │<────────────────────│                     │
     │  (通过 Port 接口)    │                     │
     │                     │                     │
     │  6. 持久化到 DB ───────────────────────────> MongoDB / MySQL
     │                     │                     │
```

---

## 六、配置管理设计

### 6.1 配置合并

所有配置统一到 backend 模块中：

```
tang-dynasty-backend/src/main/resources/
├── application-backend.yaml           # 主配置（包含 Agent 配置）
├── application-backend-dev.yaml       # 开发环境
├── application-backend-prod.yaml      # 生产环境
└── db/
    └── migration/                     # Flyway 迁移脚本
```

**application-backend.yaml 新增 Agent 配置段**：

```yaml
# ========== Agent Engine 配置 ==========
tdagent:
  agent:
    max-iterations: 50
    tool-call-timeout: 30s
  reme:
    enabled: true
    base-url: http://localhost:8002
    api-key: ${REME_API_KEY:}
    personal-memory-enabled: true
    task-memory-enabled: true
    tool-memory-enabled: true
  skill:
    oss-enabled: true
    max-size-mb: 50
  studio:
    enabled: false
    base-url: http://localhost:5174
```

### 6.2 TdAgentProperties 位置

`TdAgentProperties` 保留在 agent-engine 中，但改为纯配置类，不依赖 Spring Boot 自动配置。由 backend 模块通过 `@ConfigurationProperties` 绑定。

---

## 七、API 路由设计

### 7.1 路由统一由 backend 管理

| 路由前缀 | Controller (backend) | 委托给 engine |
|----------|---------------------|---------------|
| `/api/v1/auth/*` | AuthController | - |
| `/api/edict-memorials/*` | EdictMemorialController | - |
| `/api/edict-tasks/*` | EdictTasksController | - |
| `/api/scheduled-jobs/*` | ScheduledJobController | - |
| `/api/sys-*/*` | 对应 SysController | - |
| `/api/v1/tdagent/chat` | AgentApiController | TdAgentFactory + Agent |
| `/api/v1/tdagent/sessions` | AgentApiController | SessionManager |
| `/api/v1/tdagent/profiles` | AgentProfileController | ProfileStoragePort |
| `/api/v1/tdagent/skills` | AgentSkillController | SkillStoragePort |
| `/api/agent/tool-config/*` | ToolConfigController | ToolConfigStoragePort |
| `/api/agent/token-usage/*` | TokenUsageController | TokenUsageRecorderPort |

### 7.2 Controller 归属

| Controller | 归属模块 | 说明 |
|------------|----------|------|
| AuthController | backend | 用户认证 |
| EdictMemorialController | backend | 奏折管理 |
| EdictTasksController | backend | 任务管理 |
| ScheduledJobController | backend | 定时任务 |
| SysChannelsController | backend | 渠道配置 |
| SysMcpController | backend | MCP 配置 |
| SysModelsController | backend | 模型配置 |
| SysTokenUsageController | backend | Token 统计 |
| SysUserController | backend | 用户管理 |
| **AgentApiController** | **backend** | **Agent 对话 API（新增）** |
| **AgentProfileController** | **backend** | **Profile CRUD（从 engine 移入）** |
| **AgentSkillController** | **backend** | **Skill CRUD（从 engine 移入）** |
| **ToolConfigController** | **backend** | **工具配置 CRUD（从 engine 移入）** |
| **TokenUsageController** | **backend** | **Token 用量 CRUD（从 engine 移入）** |

---

## 八、迁移方案

### 8.1 迁移步骤

| 阶段 | 内容 | 预计影响 |
|------|------|----------|
| **Phase 1** | 在 engine 中定义端口接口（Ports） | 无破坏性变更 |
| **Phase 2** | 将 engine 中的 MongoDB Repository 和 MySQL Mapper 移至 backend | 需调整包名 |
| **Phase 3** | 将 engine 中的 CRUD 服务移至 backend | 需调整依赖 |
| **Phase 4** | 将 engine 中的 Controller 移至 backend | API 路径保持不变 |
| **Phase 5** | 将 engine 中的 Document 模型移至 backend | 改为 backend domain |
| **Phase 6** | 将 engine 中的 OSS 存储移至 backend | 配置调整 |
| **Phase 7** | 清理 engine 中的冗余代码和依赖 | pom.xml 清理 |
| **Phase 8** | 在 backend 中实现所有端口接口（Adapters） | 绑定到 Spring 容器 |
| **Phase 9** | 修改依赖方向和启动配置 | 依赖调整 |
| **Phase 10** | 测试验证 | 全量测试 |

### 8.2 迁移风险

| 风险 | 缓解措施 |
|------|----------|
| API 路径变化导致前端不兼容 | 保持所有 API 路径不变 |
| 数据库配置丢失 | 合并配置时仔细核对 |
| Agent 功能异常 | 分阶段迁移，每阶段后测试 |
| 依赖冲突 | 统一管理依赖版本 |

---

## 九、目标架构优势

### 9.1 对比分析

| 维度 | 当前架构 | 目标架构 |
|------|----------|----------|
| **职责清晰度** | engine 混杂 CRUD | engine 纯引擎，backend 全业务 |
| **依赖方向** | 双向依赖 | 严格单向：launcher → backend → engine |
| **可测试性** | 需要数据库才能测试 engine | engine 可 Mock Ports 独立测试 |
| **可复用性** | engine 难以被其他项目复用 | engine 可作为独立库引用 |
| **扩展性** | 新增存储类型需改 engine | 实现 Port 接口即可扩展 |
| **部署灵活性** | 引擎和业务耦合 | 引擎可独立升级 |
| **代码组织** | 基础设施侵入核心 | 核心干净，基础设施后置 |

### 9.2 设计模式应用

| 模式 | 应用场景 |
|------|----------|
| **端口适配器模式** | engine 通过 Ports 定义需求，backend 通过 Adapters 实现 |
| **依赖倒置原则** | 高层（engine）定义接口，低层（backend）实现 |
| **单一职责原则** | engine 专注 Agent 逻辑，backend 专注业务和数据 |
| **开闭原则** | 新增存储类型只需新增 Adapter，不修改 engine |

---

## 十、注意事项

### 10.1 engine 模块的 Spring Boot 定位

迁移后，agent-engine 不再是 Spring Boot 应用，而是**纯 Java 库**。它：
- 不再有 `@SpringBootApplication`
- 不再有 Controller
- 不再有 Repository/Mapper
- 只提供核心类和接口，由使用者（backend）通过 Spring 配置组装

### 10.2 配置管理

- `TdAgentProperties` 保留在 engine 中，作为纯 POJO
- backend 通过 `@ConfigurationProperties(prefix = "tdagent")` 绑定
- 所有 YAML 配置集中在 backend 模块

### 10.3 MongoDB 和 MySQL 归属

- 所有数据访问（MongoDB + MySQL）归属 backend 模块
- engine 不直接访问数据库，只通过 Port 接口请求数据操作

### 10.4 ReMe 服务

- ReMe 服务保持独立部署不变
- engine 中的 `TdAgentReMeService` 保留，作为 HTTP 客户端调用 ReMe
- 配置由 backend 提供

---

## 十一、总结

### 11.1 核心变化

```
变化前: engine 是"大杂烩"，包含引擎 + CRUD + 数据访问 + Web API
变化后: engine 是"纯引擎库"，只包含 Agent 核心逻辑，通过 Port 接口定义需求
       backend 是"业务层"，拥有所有数据访问、CRUD、Web API，并实现 engine 的 Port 接口
```

### 11.2 一句话概括

**agent-engine 退居为纯引擎库，backend 前进为业务适配层，通过端口适配器模式解耦引擎与基础设施。**

### 11.3 架构成熟度对标

目标架构与成熟的企业级架构模式一致：
- 类似 **Spring Data** 模式：框架定义 Repository 接口，应用层实现
- 类似 **JPA/Hibernate** 模式：ORM 框架不直接访问数据库，由应用层配置数据源
- 类似 **六边形架构**（Hexagonal Architecture）：核心业务在内，基础设施在外
