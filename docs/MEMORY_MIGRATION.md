# Memory 系统改造说明

## 改造概述

本次改造将项目的 Memory 系统从自定义的 `MongoConversationMemory`（继承 `InMemoryMemory`）迁移到 AgentScope 框架原生的 `AutoContextMemory`，实现以下目标：

1. **使用框架原生的智能上下文管理** - AutoContextMemory 提供 6 种渐进式压缩策略
2. **MongoDB 仅作为 Session 存储后端** - 不参与记忆压缩逻辑
3. **解决系统消息显示混乱问题** - 过滤框架注入的系统消息
4. **支持双存储机制** - workingMessages（压缩后）+ originalMessages（完整历史）

## 架构变更

### 1. 新增组件

#### AutoContextMemoryService
- **位置**: `com.liangshou.tangdynasty.agentic.agents.memory.AutoContextMemoryService`
- **职责**:
  - 创建和配置 AutoContextMemory 实例（通过 `TdAgentModelFactory` 创建 Model）
  - 创建 AutoContextHook（自动触发压缩）
  - 创建 ContextOffloadTool（支持内容卸载）
  - 提供消息过滤功能（过滤系统消息）
  - 保存 AutoContextMemory 状态到 MongoDB
- **依赖注入**:
  - `ConversationMemoryRepository` - MongoDB 数据访问
  - `ObjectMapper` - JSON 序列化工具
  - `TdAgentModelFactory` - 模型工厂（用于创建压缩所需的 LLM 实例）

#### ConversationMemoryDocument 新字段
- **workingMessages**: 工作存储消息（压缩后的上下文）
- **originalMessages**: 原始存储消息（完整的、未压缩的历史）
- **offloadContext**: 卸载上下文映射（Key: UUID, Value: 消息列表的 JSON）
- **compressionEvents**: 压缩事件记录列表

### 2. 修改组件

#### TdAgentFactory
- **替换**: `MongoConversationMemory` → `AutoContextMemory`
- **替换**: `TdAgentMemoryCompactionHook` → `AutoContextHook`
- **新增**: 注册 `ContextOffloadTool` 到 Toolkit
- **删除**: 不再依赖 `TdAgentMemoryManager`

#### TdAgentChatServiceImpl
- **修改**: `getSessionHistory()` 方法使用 `AutoContextMemoryService.getSessionHistoryForDisplay()` 过滤系统消息
- **新增**: 依赖 `AutoContextMemoryService`

### 3. 保留组件（向后兼容）

#### MongoConversationMemory
- **状态**: 保留但不再使用
- **原因**: 可能需要回滚或迁移旧数据

#### TdAgentMemoryManager & TdAgentMemoryCompactionHook
- **状态**: 保留但不再使用
- **原因**: 压缩逻辑已由 AutoContextMemory 框架处理

## MongoDB 数据结构

### ConversationMemoryDocument 字段说明

```java
{
  "id": "userId:sessionId",
  "userId": "user123",
  "sessionId": "session456",
  
  // 传统模式（向后兼容）
  "messages": [...],
  
  // AutoContextMemory 模式（新）
  "working_messages": [...],      // 压缩后的工作消息
  "original_messages": [...],     // 完整原始消息（仅追加）
  "offload_context": {            // 卸载上下文
    "uuid1": "[...]",
    "uuid2": "[...]"
  },
  "compression_events": [...],    // 压缩事件记录
  
  "roundCount": 10,
  "compactionCount": 2,
  "title": "会话标题",
  "createdAt": "2026-05-04T10:00:00Z",
  "updatedAt": "2026-05-04T11:00:00Z"
}
```

### 数据迁移策略

1. **新会话**: 直接使用 AutoContextMemory 模式
2. **旧会话**: 保留 `messages` 字段，新增 `workingMessages` 和 `originalMessages`
3. **前端显示**: 优先使用 `workingMessages`，如果为空则回退到 `messages`

## 系统消息过滤规则

### 过滤逻辑

在以下位置添加过滤：

1. **AutoContextMemoryService.getMessagesForDisplay()**
   - 从 AutoContextMemory 获取消息时过滤

2. **AutoContextMemoryService.getSessionHistoryForDisplay()**
   - 从 MongoDB 加载历史时过滤

3. **TdAgentChatServiceImpl.getSessionHistory()**
   - API 返回前端时调用过滤方法

### 过滤规则

```java
// 过滤掉 name 以 _memory 结尾的消息（如 long_term_memory）
if (message.getName() != null && message.getName().endsWith("_memory")) {
    return false;
}

// 过滤掉 name="system" 的消息
if ("system".equalsIgnoreCase(message.getName())) {
    return false;
}

// 保留其他所有消息
return true;
```

## AutoContextMemory 配置

### 默认配置

```java
AutoContextConfig config = AutoContextConfig.builder()
    .msgThreshold(30)                    // 触发压缩的消息数量阈值
    .lastKeep(10)                        // 保留最近 10 条消息不压缩
    .tokenRatio(0.75)                    // Token 使用率达到 75% 时触发压缩
    .maxToken(128 * 1024)                // 最大 Token 限制（128K）
    .largePayloadThreshold(5 * 1024)     // 大型消息阈值（5K 字符）
    .minConsecutiveToolMessages(6)       // 压缩所需最小连续工具消息数
    .currentRoundCompressionRatio(0.3)   // 当前轮次压缩到 30%
    .build();
```

### 压缩策略（6 种渐进式）

1. **压缩历史工具调用** - 查找连续工具调用消息并智能压缩
2. **卸载大型消息（带保护）** - 保护最新助手响应和 lastKeep 消息
3. **卸载大型消息（无保护）** - 仅保护最新助手响应
4. **摘要历史对话轮次** - 对用户-助手对话对进行智能摘要
5. **摘要当前轮次大型消息** - 压缩当前轮次超过阈值的消息
6. **压缩当前轮次消息** - 当历史已压缩但上下文仍超限时触发

## 使用示例

### 创建 Agent

```java
// TdAgentFactory.createAgent() 中自动处理
AutoContextMemory memory = autoContextMemoryService.createMemory(systemPrompt);
toolkit.registerTool(autoContextMemoryService.createContextOffloadTool(memory));

ReActAgent agent = ReActAgent.builder()
    .name("TDAgent")
    .sysPrompt(systemPrompt)
    .model(modelFactory.create())
    .toolkit(toolkit)
    .memory(memory)
    .hook(autoContextMemoryService.createHook())  // AutoContextHook 自动触发压缩
    .maxIters(properties.getModel().getMaxIters())
    .build();
```

### 获取会话历史（前端 API）

```java
// TdAgentChatServiceImpl.getSessionHistory()
List<StoredMessage> filteredMessages = 
    autoContextMemoryService.getSessionHistoryForDisplay(userId, sessionId);

return SessionHistoryResponse.builder()
    .session(view)
    .messages(filteredMessages)  // 已过滤系统消息
    .build();
```

## 注意事项

### 1. 状态持久化

- **AutoContextMemory 状态** 通过 `AgentSessionStateService` 使用 AgentScope 的 Session 接口保存
- **MongoDB 文档** 作为辅助存储，保存 workingMessages 和 originalMessages
- **压缩事件** 记录在 `compressionEvents` 字段中，可用于分析压缩效果

### 2. ThinkingBlock 保存

- AutoContextMemory 原生支持 ThinkingBlock 的保存
- 工作消息中的 ThinkingBlock 会被完整保存到 MongoDB
- 前端刷新后可以正确显示推理内容

### 3. 不完整工具调用清理

**问题现象**：恢复会话状态后，Agent 抛出异常：
```
IllegalStateException: Pending tool calls exist without results.
Enable PendingToolRecoveryHook or provide tool results.
Pending IDs: [call_959d372238f249068f209fbf]
```

**根本原因**：
- AutoContextMemory 在序列化和反序列化消息时，会保存所有消息（包括未完成的 ToolUseBlock）
- 当恢复状态时，这些 pending tool calls 被加载到 Agent 的 memory 中
- ReActAgent 检测到有 ToolUseBlock 但没有对应的 ToolResultBlock，抛出异常

**解决方案**：
- `AutoContextMemoryService.cleanMemoryToolCalls(ReActAgent agent)` 方法在状态恢复后调用
- 收集所有已完成的工具调用 ID（从 ToolResultBlock 消息中）
- 遍历工作消息，移除只有 ToolUseBlock 且 ID 不在已完成列表中的消息
- 在 `TdAgentStreamingServiceImpl` 的 `stream()`, `approveAndResume()`, `rejectAndResume()` 方法中调用

**调用时机**：
```java
agentSessionStateService.restore(context, agent);
autoContextMemoryService.cleanMemoryToolCalls(agent); // 清理不完整工具调用
```

### 4. 性能优化

- **自动压缩**: 在 LLM 推理前自动触发（通过 AutoContextHook）
- **内容卸载**: 大型消息自动卸载到 offloadContext，通过 UUID 按需重载
- **双存储**: workingMessages 用于快速访问，originalMessages 用于完整追溯

### 5. 向后兼容

- 保留 `MongoConversationMemory` 和相关类
- `ConversationMemoryDocument.messages` 字段保留用于旧数据
- 前端 API 优先使用 `workingMessages`，回退到 `messages`

## 测试建议

### 1. 基础功能测试

- [ ] 创建新会话，验证 AutoContextMemory 初始化
- [ ] 发送多条消息，验证自动压缩触发
- [ ] 刷新页面，验证 workingMessages 正确加载
- [ ] 检查系统消息是否被过滤

### 2. 压缩策略测试

- [ ] 触发策略 1：连续 6+ 条工具调用消息
- [ ] 触发策略 2/3：发送超过 5K 字符的大型消息
- [ ] 触发策略 4：对话超过 30 条消息
- [ ] 触发策略 5/6：Token 使用率达到 75%

### 3. 数据完整性测试

- [ ] 验证 originalMessages 仅追加模式
- [ ] 验证 offloadContext 正确保存和加载
- [ ] 验证 compressionEvents 记录完整
- [ ] 验证 ThinkingBlock 内容不丢失

### 4. 边界情况测试

- [ ] 新会话（无历史数据）
- [ ] 旧会话（有 messages 但无 workingMessages）
- [ ] 清空会话
- [ ] 删除会话

## 回滚方案

如果遇到问题需要回滚：

1. **恢复 TdAgentFactory**: 将 `AutoContextMemory` 改回 `MongoConversationMemory`
2. **恢复 Hook**: 将 `AutoContextHook` 改回 `TdAgentMemoryCompactionHook`
3. **数据兼容**: `ConversationMemoryDocument.messages` 字段保留，旧代码可继续使用

## 后续优化建议

1. **配置外部化**: 将 AutoContextConfig 参数移到 `application.yml`
2. **监控增强**: 添加压缩事件的监控和告警
3. **性能调优**: 根据实际使用情况调整阈值配置
4. **数据迁移工具**: 开发工具将旧 `messages` 迁移到 `workingMessages` + `originalMessages`

## 参考资料

- [AgentScope AutoContextMemory 文档](https://java.agentscope.io/zh/task/memory.html#autocontextmemory)
- [AutoContextMemory 详细文档](example/agentscope-java/agentscope-extensions/agentscope-extensions-autocontext-memory/README_zh.md)
- [Session 管理文档](example/agentscope-java/docs/zh/task/session.md)
