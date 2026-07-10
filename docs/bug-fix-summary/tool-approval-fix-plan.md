# 工具审批修复计划

> 日期: 2026-07-08
> 
> 基于: [tool-approval-flow-research](tool-approval-flow-research.md)

## 问题

`approveAndResume` 审批通过后不执行工具，只发送 `"工具调用已获批准，请继续执行"` 作为工具结果，导致 Agent 陷入审批死循环。

## 修复方案

### 核心改动：`TdAgentStreamingServiceImpl.approveAndResume()`

审批通过后，**真正执行被批准的工具**，将实际输出作为 TOOL 消息返回给 Agent。

**流程：**
1. 审批通过（标记 APPROVED）
2. 创建新 Agent + 恢复状态
3. 创建 Toolkit（与 Agent 使用相同的工具集）
4. 对每个已批准的工具调用：
   - 从 `ToolApprovalDocument` 获取 `toolName` + `toolInputJson`
   - 在 Toolkit 中查找对应工具（`Toolkit.getTool(name)`）
   - 构建 `ToolCallParam`（包含原始 toolCallId）
   - 调用 `tool.callAsync(param)` — `GuardedAgentTool` 检查 `isApproved()`=true → 真正执行
   - 收集 `ToolResultBlock`
5. 用真实结果构建 `Msg(TOOL)` 消息
6. 发送给 Agent 继续推理

### 关键 API

- `Toolkit.getTool(String name)` → `AgentTool`
- `ToolCallParam.builder().toolUseBlock(toolUseBlock).build()`
- `ToolUseBlock.builder().id(toolCallId).name(toolName).input(inputMap).build()`
- `AgentTool.callAsync(ToolCallParam)` → `Mono<ToolResultBlock>`

### 改动文件

| 文件 | 改动 |
|------|------|
| `TdAgentStreamingServiceImpl` | 重写 `approveAndResume`：执行工具后返回真实结果 |
| `TdAgentToolkitFactory` | 新增公开方法 `createPublicToolkit(context)` 供审批流程使用 |

### 拒绝流程

`rejectAndResume` 保持不变 — 拒绝时不需要执行工具，发送"未通过审批"消息是正确的。
