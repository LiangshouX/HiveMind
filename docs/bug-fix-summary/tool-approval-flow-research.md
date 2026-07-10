# 工具审批流程研究报告

> 日期: 2026-07-08
> 范围: HiveMind Agent Engine 工具审批（Tool Guard/Approval）完整流程

## 1. 概述

HiveMind 的工具审批系统采用 **Hook + Delegate** 双层防护架构，在 Agent 推理后、工具执行前插入安全检查。当工具被判定为高风险时，暂停 Agent 执行，等待用户在前端审批后恢复。

## 2. 核心组件

| 组件 | 文件 | 职责 |
|------|------|------|
| `ToolGuardEngine` | `agents/guard/ToolGuardEngine.java` | 风险评估引擎，根据配置规则判断工具调用是否允许/需要审批 |
| `TdAgentToolGuardHook` | `agents/hooks/TdAgentToolGuardHook.java` | AgentScope Hook，监听 `PostReasoningEvent`，拦截工具调用 |
| `GuardedAgentTool` | `agents/guard/GuardedAgentTool.java` | AgentTool 装饰器，执行前二次检查审批状态 |
| `ToolApprovalService` | `agents/guard/approval/ToolApprovalService.java` | 审批记录 CRUD，管理 PENDING→APPROVED/REJECTED→EXECUTED 生命周期 |
| `ToolApprovalDocument` | `domain/tool/model/ToolApprovalDocument.java` | MongoDB 审批记录文档 |
| `TdAgentToolkitFactory` | `agents/TdAgentToolkitFactory.java` | 创建 Toolkit，注册工具并应用 GuardedAgentTool 包装 |
| `TdAgentStreamingServiceImpl` | `application/impl/TdAgentStreamingServiceImpl.java` | 流式服务，实现 `approveAndResume`/`rejectAndResume` |

## 3. 完整流程追踪

### 3.1 正常流程（无需审批）

```
用户消息 → TdAgentStreamingServiceImpl.stream()
  → chatService.buildContext(request)
  → agentFactory.createAgent(context)
    → 创建 Toolkit（工具已注册）
    → 注册 TdAgentToolGuardHook
  → agent.stream(userMessage)
    → LLM 推理 → 生成 ToolUseBlock
    → TdAgentToolGuardHook.onEvent(PostReasoningEvent)
      → ToolGuardEngine.evaluate() → 允许
    → GuardedAgentTool.callAsync(param)
      → ToolGuardEngine.evaluate() → 允许
      → delegate.callAsync(param) → 真正执行工具
      → markExecuted() → 标记审批记录为 EXECUTED
    → 工具结果返回 Agent → 继续推理 → 最终结果
```

### 3.2 审批触发流程

```
用户消息 → agent.stream(userMessage)
  → LLM 推理 → 生成 ToolUseBlock（如 run_shell_command）
  → TdAgentToolGuardHook.onEvent(PostReasoningEvent)
    → ToolGuardEngine.evaluate("run_shell_command", input)
      → 配置 approvalRequired=true → 返回 requiresApproval=true
    → toolApprovalService.createPendingApprovals()
      → 保存 ToolApprovalDocument(status=PENDING) 到 MongoDB
    → postReasoningEvent.stopAgent()  ← 暂停 Agent
  → cleanup()
    → agentSessionStateService.save(context, agent, paused=true)
    → activeSessionRegistry.unregister(sessionKey)
    → SSE 发送 APPROVAL_REQUIRED 事件
```

### 3.3 审批恢复流程（当前实现 — 有 BUG）

```
用户点击批准 → TdAgentChatController.approve()
  → streamingService.approveAndResume(request)
    → chatService.buildContext(userId, sessionId, title)
    → toolApprovalService.approve(approvalIds)  ← 标记为 APPROVED
    → agentFactory.createAgent(context)  ← 创建新 Agent 实例
    → agentSessionStateService.restore(context, agent)  ← 从 MongoDB 恢复状态
    → 构建 Msg(TOOL, "工具调用已获批准，请继续执行")
    → agent.stream(toolResponse)  ← 发送给 Agent
```

## 4. BUG 根因分析

### 4.1 问题现象

审批后 Agent 陷入死循环：每次审批通过后，Agent 收到"工具调用已获批准，请继续执行"作为工具"结果"，但没有获得真正的工具输出。Agent 尝试重新调用工具 → 再次触发审批 → 再次批准 → 再次收到"已批准" → 无限循环。

### 4.2 根因

`approveAndResume` 方法在审批通过后，**没有真正执行被批准的工具**，而是发送了一条文本消息 `Msg(TOOL, "工具调用已获批准，请继续执行")` 作为工具结果。

**关键问题**：Agent 的 TOOL 消息应该包含工具的**实际执行输出**（如 Shell 命令的 stdout），而不是审批确认文本。Agent 收到"已批准"后：

1. 将其视为工具输出 → 发现无有用信息
2. 再次推理 → 决定重新调用工具
3. `GuardedAgentTool.callAsync()` → `ToolGuardEngine.evaluate()` → `requiresApproval=true`
4. 新的 toolCallId → `isApproved(newToolCallId)` = false → 创建新的 PENDING 审批
5. `ToolSuspendException` → Agent 暂停 → 回到步骤 1

**补充：为什么每次重试都是新的 toolCallId？**

AgentScope 的 ReActAgent 在每次生成工具调用时，会为 `ToolUseBlock` 分配新的唯一 ID（如 `call_00_xxx`）。审批记录是按 `toolCallId` 粒度存储的，第一次审批绑定的是原始的 `call_00_Efu...`，但 Agent 重试时生成了新的 `call_00_GJB...`，这个新 ID 没有审批记录 → 再次触发审批 → 死循环。

### 4.3 消息序列佐证

从 MongoDB `conversation_memory` 可以看到循环模式：

```
ASSISTANT: (tool_use: run_ipython_cell, toolCallId=call_00_Efu...)
TOOL:     "工具调用已获批准，请继续执行"              ← 没有实际输出！
ASSISTANT: (tool_use: run_ipython_cell, toolCallId=call_00_GJB...)  ← 重试，新 ID
TOOL:     "工具调用已获批准，请继续执行"              ← 同样
ASSISTANT: (tool_use: run_shell_command, toolCallId=call_00_7kv...) ← 换工具
TOOL:     "工具调用已获批准，请继续执行"              ← 同样
...（循环 8 次后 Agent 放弃）
ASSISTANT: "看起来工具一直卡在审批流程"
```

### 4.4 两层防护的交互问题

系统有两层防护：

| 层 | 组件 | 时机 | 作用 |
|----|------|------|------|
| 第一层 | `TdAgentToolGuardHook` | 推理后、工具执行前 | 拦截并暂停 Agent，创建审批记录 |
| 第二层 | `GuardedAgentTool` | 工具实际执行时 | 二次检查 `isApproved()`，未批准则抛 `ToolSuspendException` |

**正常执行时**：两层都通过 → 工具执行 → `markExecuted()`。

**审批恢复时**：第一层已经通过（Hook 不再拦截，因为审批已 APPROVED），但 `approveAndResume` 绕过了第二层（`GuardedAgentTool`），直接构造了假的 TOOL 消息。

## 5. 修复方案

### 5.1 核心思路

审批通过后，**真正执行被批准的工具**，将实际输出作为 TOOL 消息返回给 Agent。

### 5.2 实现方案

修改 `TdAgentStreamingServiceImpl.approveAndResume()`：

**当前（有 BUG）**：
```java
Msg toolResponse = Msg.builder()
    .role(MsgRole.TOOL)
    .content(approvals.stream()
        .map(approval -> new ToolResultBlock(
            approval.getToolCallId(),
            approval.getToolName(),
            TextBlock.builder().text("工具调用已获批准，请继续执行").build()))
        .toList())
    .build();
return execute(context, agent, agent.stream(toolResponse, ...), false, null, null);
```

**修复后**：
1. 从 `ToolApprovalDocument` 获取 `toolName` 和 `toolInputJson`（工具名 + 原始输入参数）
2. 通过 `TdAgentToolkitFactory.createToolkit(context)` 创建 toolkit
3. 在 toolkit 中按名称查找对应的 `AgentTool`（会是 `GuardedAgentTool` 包装）
4. 为每个审批通过的工具构建 `ToolCallParam`（包含 toolCallId 和 input）
5. 调用 `GuardedAgentTool.callAsync(param)` — 因为 `isApproved()` 返回 true，会真正执行工具
6. 收集实际的 `ToolResultBlock` 结果
7. 构建 `Msg(TOOL)` 消息，包含真实的工具输出
8. 发送给 Agent 继续推理

### 5.3 关键设计决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 工具执行入口 | 通过 `TdAgentToolkitFactory` 创建新 toolkit | 不依赖 agent 内部状态，解耦清晰 |
| 审批粒度 | 逐个工具执行，收集结果 | 支持多工具同时审批的场景 |
| 拒绝流程 | 保持现有逻辑不变 | 拒绝时不需要执行工具，"未通过审批"消息是正确的 |
| 错误处理 | 工具执行失败时返回错误信息作为工具结果 | Agent 可以据此调整策略 |

### 5.4 影响范围

| 文件 | 改动类型 |
|------|---------|
| `TdAgentStreamingServiceImpl.approveAndResume()` | 重写：执行工具后返回真实结果 |
| `TdAgentToolkitFactory` | 新增 `createToolkitForApproval()` 方法，创建用于审批执行的 toolkit |

### 5.5 AgentScope SDK 关键约束

- `ToolCallParam` 需要 `ToolUseBlock`（包含 id、name、input）
- `AgentTool.callAsync(ToolCallParam)` 返回 `Mono<ToolResultBlock>`
- `GuardedAgentTool` 包装了原始工具，审批通过后会委托给原始工具执行
- `Toolkit` 没有按名称查找工具的 API，需要遍历或新增查找方法

## 6. 相关配置

```yaml
# application-agentic.yaml
tdagent:
  tool-guard:
    enabled: true              # 是否启用工具防护
    strict-mode: false         # 严格模式（检查敏感路径）
    pending-expire-minutes: 30 # 审批记录有效期
```

## 7. AgentScope SDK 关键 API

| API | 用途 |
|-----|------|
| `PostReasoningEvent.stopAgent()` | 暂停 Agent 执行（Hook 中使用） |
| `PostReasoningEvent.setReasoningMessage()` | 替换推理结果（拒绝时使用） |
| `ToolSuspendException` | 工具级别暂停（GuardedAgentTool 中使用） |
| `ToolResultBlock` / `ToolResultBlock.error()` | 构造工具结果消息 |
| `AgentTool.callAsync(ToolCallParam)` | 异步执行工具 |

## 8. 附录：完整文件清单

| 文件 | 行数 | 角色 |
|------|------|------|
| `agents/guard/ToolGuardEngine.java` | 175 | 风险评估引擎 |
| `agents/guard/ToolGuardDecision.java` | - | 评估结果 DTO |
| `agents/guard/GuardedAgentTool.java` | 91 | AgentTool 装饰器 |
| `agents/hooks/TdAgentToolGuardHook.java` | 230 | PostReasoningEvent Hook |
| `agents/guard/approval/ToolApprovalService.java` | 209 | 审批记录 CRUD |
| `domain/tool/model/ToolApprovalDocument.java` | 85 | MongoDB 文档 |
| `domain/shared/enums/ToolApprovalStatus.java` | 50 | 状态枚举 |
| `infrastructure/mongo/repository/ToolApprovalRepository.java` | - | Spring Data Repository |
| `agents/TdAgentToolkitFactory.java` | 190 | Toolkit 工厂 |
| `agents/TdAgentFactory.java` | 220 | Agent 工厂 |
| `application/impl/TdAgentStreamingServiceImpl.java` | 539 | 流式服务（含 approveAndResume） |
| `adapter/controller/TdAgentChatController.java` | 199 | REST 控制器 |
