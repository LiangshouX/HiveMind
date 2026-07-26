# AgentScope-Java 1.0.12 → 2.0.0 升级报告

> 升级日期：2026-07-26
> 框架源码：`.lib-repo/agentscope-java-2.0.0`

---

## 1. 升级概述

将 HiveMind 项目依赖的 AgentScope-Java SDK 从 **1.0.12** 升级到 **2.0.0**。2.0.0 是一次重大重构，涉及模块拆分、API 废弃与替换、架构模式变更。本次升级确保所有业务功能使用 2.0.0 的新 API，消除主代码中对已废弃接口的依赖。

### 升级范围

- `hivemind-agent-engine` 模块的 POM 依赖
- 所有 `io.agentscope.*` 包的 import 路径
- 模型、Hook、记忆、Session、沙箱、流式 API 全面迁移
- 编译通过，102 个测试全部通过

---

## 2. Maven 依赖变更

### 2.1 版本升级

```xml
<!-- 旧版本 -->
<agentscope.version>1.0.12</agentscope.version>

<!-- 新版本 -->
<agentscope.version>2.0.0</agentscope.version>
```

### 2.2 依赖增删

| 变更 | Artifact | 说明 |
|------|----------|------|
| **删除** | `agentscope-runtime-agentscope` | 沙箱运行时模块已移除，功能迁移到 `agentscope-harness` |
| **删除** | `agentscope-extensions-autocontext-memory` | 自动上下文记忆模块已移除 |
| **新增** | `agentscope-extensions-model-dashscope` | DashScope 模型从 core 拆分到独立扩展 |
| **新增** | `agentscope-extensions-model-openai` | OpenAI 模型从 core 拆分到独立扩展 |
| **新增** | `agentscope-harness` | 新沙箱模块（Docker Sandbox + Shell + Filesystem） |
| **保留** | `agentscope-core` | 核心模块，版本升级到 2.0.0 |
| **保留** | `agentscope-extensions-reme` | ReMe 长期记忆扩展 |
| **保留** | `agentscope-extensions-studio` | Studio 可观测性扩展 |

---

## 3. API 迁移详情

### 3.1 模型类迁移（包路径变更）

模型类从 `io.agentscope.core.model` 迁移到 `io.agentscope.extensions.model`：

| 旧路径 | 新路径 |
|--------|--------|
| `io.agentscope.core.model.DashScopeChatModel` | `io.agentscope.extensions.model.dashscope.DashScopeChatModel` |
| `io.agentscope.core.model.OpenAIChatModel` | `io.agentscope.extensions.model.openai.OpenAIChatModel` |
| `io.agentscope.core.model.DashScopeChatFormatter` | `io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter` |
| `io.agentscope.core.model.OpenAIChatFormatter` | `io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter` |
| `io.agentscope.core.model.DeepSeekFormatter` | `io.agentscope.extensions.model.openai.formatter.DeepSeekFormatter` |

**影响文件**：`TdAgentModelFactory.java`、`TdAgentModelFactoryTest.java`

### 3.2 Session/State 迁移

2.0.0 移除了 `Session`、`SessionKey`、`SimpleSessionKey` 接口，替换为 `AgentStateStore`：

| 旧 API | 新 API | 说明 |
|--------|--------|------|
| `Session` + `SessionKey` | `AgentStateStore` | 状态存储接口 |
| `MongoAgentSession`（已删除） | `MongoAgentStateStore`（新建） | MongoDB 状态存储实现 |
| `agent.saveTo(session, key)` | `builder.stateStore(store)` | 状态由框架自动管理 |
| `agent.loadFrom(session, key)` | `builder.defaultSessionId(id)` | 框架自动加载 |

**新建文件**：`MongoAgentStateStore.java` — 实现 `AgentStateStore` 接口，使用 `(userId, sessionId, key)` 三元组寻址。

**删除文件**：`MongoAgentSession.java` — 旧版 `Session` 实现，不再需要。

### 3.3 Hook → Middleware 迁移

2.0.0 废弃了 `Hook` 接口（`@Deprecated(forRemoval=true)`），替换为 `MiddlewareBase`：

| 旧 API | 新 API | 说明 |
|--------|--------|------|
| `Hook` 接口 | `MiddlewareBase` 接口 | 中间件提供 5 个拦截点 |
| `builder.hook(new XxxHook())` | `builder.middleware(new XxxMiddleware())` | 注册方式变更 |
| `onEvent(HookEvent)` | `onAgent/onReasoning/onActing/onModelCall/onSystemPrompt` | 细粒度拦截 |

**新建中间件**：

| 文件 | 替代 | 拦截点 |
|------|------|--------|
| `TdAgentMemoryCompactionMiddleware.java` | `TdAgentMemoryCompactionHook` | `onReasoning()` — 推理阶段拦截，注入压缩摘要 |
| `TdAgentToolGuardMiddleware.java` | `TdAgentToolGuardHook` | `onReasoning()` — 拦截工具调用事件流，评估风险 |
| `StudioForwardMiddleware.java` | `StudioMessageHook` | `onAgent()` — 拦截 `AgentResultEvent`，转发到 Studio |

**删除文件**：`TdAgentMemoryCompactionHook.java`、`TdAgentToolGuardHook.java`

### 3.4 长期记忆迁移

2.0.0 废弃了整个 `LongTermMemory` 体系：

| 废弃 API | 替代方案 |
|----------|----------|
| `LongTermMemory` 接口 | 应用层自行集成 |
| `LongTermMemoryTools` | 自定义 `@Tool` 注解工具 |
| `builder.longTermMemory()` | 在 Toolkit 中注册 `@Tool` 工具 |
| `builder.longTermMemoryMode()` | 移除，Agent 通过工具调用主动管理记忆 |

**新建文件**：`ReMeMemoryTools.java` — 使用 `@Tool` 注解定义 `record_to_memory` 和 `retrieve_from_memory` 工具，直接调用 `TdAgentReMeService`。

### 3.5 Memory 接口迁移

2.0.0 废弃了 `Memory`、`InMemoryMemory` 等接口：

| 废弃 API | 替代方案 |
|----------|----------|
| `Memory` 接口 | `AgentState.getContext()` |
| `InMemoryMemory` | `AgentState.getContext()` |
| `builder.memory(memory)` | `builder.stateStore(store)` |

**重构文件**：`MongoConversationMemory.java` — 不再继承 `InMemoryMemory`，改为独立的辅助类，仅被压缩中间件使用。

### 3.6 流式 API 迁移

2.0.0 废弃了 `stream()` 方法，替换为 `streamEvents()`：

| 旧 API | 新 API |
|--------|--------|
| `agent.stream(msg)` | `agent.streamEvents(msg)` |
| `Event` + `EventType` | `AgentEvent` 层次结构 |
| `StreamOptions` | 移除 |

**新事件类型**：

| 事件 | 说明 |
|------|------|
| `AgentResultEvent` | 最终结果消息 |
| `AgentEndEvent` | Agent 调用结束 |
| `ThinkingBlockDeltaEvent` | 推理过程增量 |
| `ThinkingBlockEndEvent` | 推理过程结束 |
| `TextBlockDeltaEvent` | 文本回复增量 |
| `TextBlockEndEvent` | 文本回复结束 |
| `ToolCallStartEvent` | 工具调用开始 |
| `ToolCallEndEvent` | 工具调用结束 |
| `ToolResultEndEvent` | 工具执行结果 |
| `RequireUserConfirmEvent` | 需要用户确认（HITL） |

**影响文件**：`TdAgentStreamingServiceImpl.java`

### 3.7 沙箱迁移

2.0.0 移除了 `agentscope-runtime-agentscope` 模块，替换为 `agentscope-harness`：

| 旧 API | 新 API |
|--------|--------|
| `SandboxService` | `DockerSandboxClient` |
| `BaseSandbox` / `BrowserSandbox` | `DockerSandbox` |
| `ToolkitInit` | `SandboxBackedFilesystem` + `ShellExecuteTool` + `FilesystemTool` |

**影响文件**：`TdAgentToolkitFactory.java`、`TdAgentSandboxManager.java`

### 3.8 工具注册变更

2.0.0 中 `ShellExecuteTool` 和 `FilesystemTool` 使用 `@Tool` 注解而非实现 `AgentTool` 接口：

```java
// 旧：registerTool(AgentTool)
// 新：registerTool(Object) — 同时支持 AgentTool 和 @Tool 注解对象
toolkit.registerTool(new ShellExecuteTool(...));
toolkit.registerTool(new FilesystemTool(...));
```

**影响文件**：`TdAgentToolkitFactory.java`

---

## 4. 数据兼容性处理

### 4.1 ObjectMapper 配置

MongoDB 中存储的旧格式数据（1.0.12 序列化）包含已移除字段（如 `ChatUsage.totalTokens`）。为确保反序列化兼容：

```java
// MongoAgentStateStore 构造函数
this.objectMapper = objectMapper.copy()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
```

### 4.2 对话消息持久化

2.0.0 中 `AgentStateStore` 自动管理 `AgentSessionStateDocument`，但前端会话列表和标题生成读取的是 `ConversationMemoryDocument`。需要在流式/同步调用完成后手动持久化：

```java
AgentState agentState = agent.getAgentState();
if (agentState != null) {
    List<Msg> messages = agentState.getContext();
    persistenceService.replaceMessages(context, messages, "", null, false);
}
```

**影响文件**：`TdAgentStreamingServiceImpl.java`、`TdAgentChatServiceImpl.java`

---

## 5. 变更文件清单

### 5.1 新建文件

| 文件 | 说明 |
|------|------|
| `agents/session/MongoAgentStateStore.java` | AgentStateStore MongoDB 实现 |
| `agents/hooks/TdAgentMemoryCompactionMiddleware.java` | 记忆压缩中间件 |
| `agents/hooks/TdAgentToolGuardMiddleware.java` | 工具防护中间件 |
| `agents/hooks/StudioForwardMiddleware.java` | Studio 消息转发中间件 |
| `agents/memory/reme/ReMeMemoryTools.java` | ReMe @Tool 记忆工具 |

### 5.2 删除文件

| 文件 | 说明 |
|------|------|
| `agents/session/MongoAgentSession.java` | 旧 Session 实现 |
| `agents/hooks/TdAgentMemoryCompactionHook.java` | 旧记忆压缩 Hook |
| `agents/hooks/TdAgentToolGuardHook.java` | 旧工具防护 Hook |

### 5.3 重大修改文件

| 文件 | 变更内容 |
|------|----------|
| `hivemind-agent-engine/pom.xml` | 依赖版本和 artifact 变更 |
| `agents/TdAgentFactory.java` | 全面重写：middleware 替代 hook，stateStore 替代 memory |
| `agents/TdAgentModelFactory.java` | 模型类 import 路径迁移 |
| `agents/TdAgentToolkitFactory.java` | 沙箱工具注册方式变更 |
| `agents/sandbox/TdAgentSandboxManager.java` | DockerSandboxClient 替代旧沙箱 |
| `agents/memory/MongoConversationMemory.java` | 移除 InMemoryMemory 继承 |
| `agents/memory/reme/ReMeMemoryTools.java` | 移除 LongTermMemory 依赖 |
| `application/impl/TdAgentStreamingServiceImpl.java` | streamEvents + AgentEvent 迁移 + 消息持久化 |
| `application/impl/TdAgentChatServiceImpl.java` | AgentState + 消息持久化 |
| `agents/session/AgentSessionStateService.java` | 移除 saveTo/loadFrom，新增 createStateStore |

---

## 6. 已知遗留

| 项目 | 状态 | 说明 |
|------|------|------|
| `builder.skillBox()` | deprecated | 2.0.0 推荐 `skillRepository()`，但 `TdAgentSkillService` 当前返回 `SkillBox`，需后续适配 |
| `StudioMessageHook` | deprecated | 框架提供的类仍实现 `Hook`，我们已用自定义 `StudioForwardMiddleware` 替代 |
| `Hook` 接口本身 | deprecated | 框架仍保留，我们的代码已不直接使用 |

---

## 7. 验证结果

```
mvn clean compile — BUILD SUCCESS
mvn test — 102 tests, 0 failures
```

所有模块编译通过，全部测试通过。
