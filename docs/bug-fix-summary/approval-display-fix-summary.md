# 审批消息与工具调用展示一致性修复报告
> 修复日期：2026-07-10

## 问题描述

推理过程中和刷新后，前端对话展示存在不一致问题：

1. **流式阶段**：只显示审批消息，没有工具调用和工具结果消息
2. **刷新后**：显示工具调用和工具结果消息，但没有审批消息

此外，推理过程中所有消息类型（推理、工具调用、工具结果）的内容都无法正确显示。

---

## 问题根因分析

### 1. 审批消息存储不一致

审批信息存储在独立的 `tool_approvals` 集合中，与 `conversations_memory` 集合不互通：

| 集合 | 内容 | 问题 |
|------|------|------|
| `tool_approvals` | 审批记录（PENDING→APPROVED） | 审批完成后状态变化，`listPending` 获取不到 |
| `conversations_memory` | 对话消息（只有 text/thinking/tool_use/tool_result） | **没有 approval 类型** |

### 2. AgentScope 事件内容提取问题

AgentScope 的流式事件 Msg 对象内容类型不一致：

| 事件类型 | Msg 包含的内容块 | `getTextContent()` 结果 |
|---------|-----------------|------------------------|
| REASONING | ThinkingBlock | ❌ 空（不包含 TextBlock） |
| TOOL_RESULT | ToolResultBlock | ❌ 空（不包含 TextBlock） |
| AGENT_RESULT | TextBlock | ✅ 正常 |

---

## 修复方案

### 1. 统一审批消息存储（方案A：嵌入对话消息）

将审批信息作为 `approval` 类型嵌入 `conversations_memory`，实现单一数据源。

#### 后端改动

**文件：`StoredMessageContent.java`**
```java
// 新增 approval 相关字段
private String approvalId;      // 审批记录 ID
private String riskLevel;       // 风险等级
private String reason;          // 审批原因
private String status;          // 审批状态（APPROVED/REJECTED）
private String reviewComment;   // 审批备注
```

**文件：`MessageMapper.java`**
```java
// 新增 approval 类型转换
public StoredMessageContent toStoredContent(ToolApprovalDocument approval) {
    return StoredMessageContent.builder()
            .type("approval")
            .id(approval.getToolCallId())
            .name(approval.getToolName())
            .input(approval.getToolInputJson())
            .approvalId(approval.getId())
            .riskLevel(approval.getRiskLevel())
            .reason(approval.getReason())
            .status(approval.getStatus().name())
            .reviewComment(approval.getReviewComment())
            .build();
}
```

**文件：`IConversationPersistenceService.java`**
```java
// 新增接口方法
void appendApprovalMessages(ConversationSessionContext context, List<ToolApprovalDocument> approvals);
```

**文件：`ConversationPersistenceServiceImpl.java`**
```java
// 实现追加审批消息逻辑
@Override
public void appendApprovalMessages(ConversationSessionContext context, List<ToolApprovalDocument> approvals) {
    // 1. 加载当前消息列表
    // 2. 找到最后一个 TOOL_RESULT 或 REASONING 的位置
    // 3. 在该位置之后插入 approval 消息
    // 4. 保存到 MongoDB
}
```

**文件：`TdAgentStreamingServiceImpl.java`**
```java
// 审批完成后写入消息
@Override
public SseEmitter approveAndResume(ToolApprovalActionRequest request) {
    // ... 审批逻辑 ...
    // 新增：将审批记录写入对话消息历史
    conversationPersistenceService.appendApprovalMessages(context, approvals);
    // ... 继续执行 ...
}
```

### 2. 修复 REASONING 事件内容提取

**文件：`TdAgentStreamingServiceImpl.java`**
```java
private TdAgentStreamEvent toStreamEvent(...) {
    String content = "";
    if (msg != null) {
        if (type == TdAgentStreamEventType.REASONING) {
            // REASONING 事件：从 ThinkingBlock 提取内容
            content = msg.getContent().stream()
                    .filter(ThinkingBlock.class::isInstance)
                    .map(ThinkingBlock.class::cast)
                    .map(ThinkingBlock::getThinking)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        } else if (type == TdAgentStreamEventType.TOOL_RESULT) {
            // TOOL_RESULT 事件：从 ToolResultBlock 提取内容
            content = msg.getContent().stream()
                    .filter(ToolResultBlock.class::isInstance)
                    .map(ToolResultBlock.class::cast)
                    .map(this::extractToolResultText)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        } else {
            content = msg.getTextContent();
        }
    }
    // ...
}
```

### 3. 修复 TOOL_RESULT 事件工具调用信息提取

AgentScope 的 TOOL_RESULT 事件 Msg 不包含 ToolUseBlock，需要从 REASONING 事件缓存。

**文件：`TdAgentStreamingServiceImpl.java`**
```java
// 缓存最近的 ToolUseBlock 信息
AtomicReference<ToolUseBlock> lastToolUseBlock = new AtomicReference<>();

// 在 REASONING 事件时缓存
if (streamEventType == TdAgentStreamEventType.REASONING && event.getMessage() != null) {
    extractAndCacheToolUseBlock(event.getMessage(), lastToolUseBlock);
}

// 在 TOOL_RESULT 事件时使用缓存
if (streamEventType == TdAgentStreamEventType.TOOL_RESULT) {
    ToolUseBlock cachedToolUse = lastToolUseBlock.getAndSet(null);
    if (cachedToolUse != null) {
        populateToolUseMetadata(cachedToolUse, eventMetadata);
    }
}
```

### 4. 前端渲染支持

**文件：`types.ts`**
```typescript
export interface StoredMessageContent {
  // ... 现有字段 ...
  // approval 类型字段
  approvalId?: string;
  riskLevel?: string;
  reason?: string;
  status?: string;      // APPROVED / REJECTED
  reviewComment?: string;
}
```

**文件：`useAgentConsole.ts`**
```typescript
// mapStoredMessage 中处理 approval 类型
if (item.type === "approval") {
  const toolInput = item.input || "";
  const formattedContent = toolInput ? `\`\`\`json\n${toolInput}\n\`\`\`` : "";
  const title = item.status === "APPROVED" ? "审批通过" : "审批拒绝";
  return createBlock("approval", title, formattedContent, {
    toolName: item.name,
    approvals: [{ /* 审批信息 */ }],
  });
}
```

---

## 修改文件清单

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `StoredMessageContent.java` | 字段新增 | 新增 5 个 approval 相关字段 |
| `MessageMapper.java` | 方法新增 | 新增 `toStoredContent(ToolApprovalDocument)` |
| `IConversationPersistenceService.java` | 接口新增 | 新增 `appendApprovalMessages` 方法 |
| `ConversationPersistenceServiceImpl.java` | 实现新增 | 实现追加审批消息逻辑 |
| `TdAgentStreamingServiceImpl.java` | 核心修复 | 修复内容提取、缓存 ToolUseBlock、审批后写入消息 |
| `website/src/types.ts` | 类型扩展 | StoredMessageContent 新增 approval 字段 |
| `website/src/hooks/useAgentConsole.ts` | 渲染逻辑 | mapStoredMessage 处理 approval 类型 |

---

## 验证结果

1. **流式阶段** ✅
   - 推理内容正确显示
   - 工具调用正确显示（包含工具名和输入参数）
   - 工具结果正确显示（包含执行输出）
   - 审批消息正确显示（等待审批状态）

2. **刷新后** ✅
   - 所有消息类型正确加载
   - 审批消息正确显示（审批通过/拒绝状态）
   - 与流式阶段展示一致

3. **回归测试** ✅
   - 普通对话功能正常
   - 不涉及审批的工具调用正常
   - 会话历史加载正常

---

## 总结

本次修复解决了以下核心问题：

1. **数据一致性**：将审批信息嵌入对话消息，实现单一数据源
2. **内容提取**：正确提取 ThinkingBlock 和 ToolResultBlock 的内容
3. **元数据传递**：通过缓存机制在 REASONING 和 TOOL_RESULT 事件间传递工具调用信息

修复后，流式阶段和刷新后的对话展示完全一致，用户体验得到显著提升。
