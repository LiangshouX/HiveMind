# 上下文压缩（Context Compression）设计方案

> 文档版本: v1.0 
> 
> 日   期: 2026-07-04 
> 
> 作   者: Claude Code

---

## 目录

- [一、现状分析](#一现状分析)
- [二、问题诊断](#二问题诊断)
- [三、业界参考](#三业界参考)
- [四、设计方案](#四设计方案)
- [五、详细设计](#五详细设计)
- [六、配置设计](#六配置设计)
- [七、与现有系统的集成](#七与现有系统的集成)
- [八、测试策略](#八测试策略)
- [九、实施计划](#九实施计划)

---

## 一、现状分析

### 1.1 Agent 系统整体架构

HiveMind 的 Agent 系统基于 **AgentScope-Java** 框架构建，采用 ReAct（Reasoning + Acting）模式运行。核心流程为：用户消息 → Hook 预处理 → LLM 推理 → 工具调用 → Hook 后处理 → 响应输出。

**关键组件调用链：**

```
用户请求
  → TdAgentStreamingServiceImpl.stream()
    → TdAgentFactory.createAgent(context)
      → MongoConversationMemory 初始化（加载历史消息）
      → TdAgentMemoryCompactionHook 注册
      → ReActAgent 构建
    → agent.stream(userMessage, streamOptions())
      → PreReasoningEvent 触发
        → TdAgentMemoryCompactionHook.onEvent()
          → TdAgentMemoryManager.maybeCompact()  // 判断是否压缩
          → TdAgentMemoryManager.injectCompressedSummary()  // 注入摘要
      → LLM 推理
      → 工具调用（如有）
      → Agent 结果返回
    → AgentSessionStateService.save()  // 持久化状态
```

### 1.2 消息存储模型

对话历史存储在 MongoDB 的 `conversations_memory` 集合中，核心文档结构：

```java
@Document(collection = "conversations_memory")
public class ConversationMemoryDocument {
    @Id
    private String id;                    // 格式: "{userId}:{sessionId}"
    private String sessionId;
    private String userId;
    private List<StoredMessage> messages; // 完整消息列表
    private Long roundCount;              // 对话轮次计数
    private String sysPrompt;             // 系统提示词
    private String compressedSummary;     // 压缩摘要
    private Long compactionCount;         // 压缩次数
    private Instant summaryUpdatedAt;     // 摘要更新时间
    @Version
    private Long version;                 // 乐观锁
    // ... 其他时间戳字段
}
```

每条消息（`StoredMessage`）的结构：

```java
public class StoredMessage {
    private String msgId;                         // 消息唯一标识
    private String name;                          // 发送者名称
    private String role;                          // USER / ASSISTANT / SYSTEM / TOOL
    private List<StoredMessageContent> content;   // 内容块列表
    private String metadata;                      // 元数据 JSON
    private String timestamp;                     // 时间戳
}
```

内容块（`StoredMessageContent`）支持四种类型：

| type | 说明 | 关键字段 |
|------|------|----------|
| `text` | 普通文本 | `text` |
| `thinking` | Agent 思考过程（思维链） | `text` |
| `tool_use` | 工具调用请求 | `name`, `input` (JSON), `inputRaw`, `id` |
| `tool_result` | 工具执行结果 | `name`, `text`, `id` |

### 1.3 现有压缩机制详解

当前系统已实现一套基于 **消息数量/字符数** 的压缩机制，完整调用链如下：

#### 1.3.1 触发时机

压缩在 `PreReasoningEvent`（LLM 推理前）通过 Hook 触发：

```java
// TdAgentMemoryCompactionHook.java
public <T extends HookEvent> Mono<T> onEvent(T event) {
    if (!(event instanceof PreReasoningEvent preReasoningEvent)) {
        return Mono.just(event);
    }
    // 步骤1: 判断是否需要压缩
    memoryManager.maybeCompact(context, memory, preReasoningEvent.getInputMessages());
    // 步骤2: 注入压缩摘要到输入消息
    preReasoningEvent.setInputMessages(
            memoryManager.injectCompressedSummary(preReasoningEvent.getInputMessages(), memory));
    return Mono.just(event);
}
```

#### 1.3.2 触发条件（TdAgentMemoryManager.maybeCompact）

```java
// 当前触发逻辑（简化）
boolean shouldCompact = false;
List<Msg> history = memory.getMessages();

// 条件1: 消息数量超过阈值（默认 20 条）
if (history.size() > triggerMessageCount) shouldCompact = true;

// 条件2: 总字符数超过阈值（默认 24000 字符）
int totalChars = length(history) + length(currentInput) + summary.length();
if (totalChars >= triggerCharacterCount) shouldCompact = true;
```

#### 1.3.3 压缩执行流程

```
1. 计算保留消息数: keepRecent = max(1, config.keepRecentMessages)  // 默认 8
2. 分割消息:
   - compactCandidates = history[0 .. history.size()-keepRecent]  // 前面的旧消息
   - remaining = history[history.size()-keepRecent .. end]         // 最近 8 条
3. 调用 ReMe 服务压缩:
   - 将 compactCandidates 发送到 ReMe workspace
   - ReMe 检索并生成摘要（融合已有摘要）
4. 应用压缩:
   - 清空内存中的所有消息
   - 重新添加 remaining（最近 8 条）
   - 更新 compressedSummary
   - 持久化到 MongoDB
```

#### 1.3.4 摘要注入

```java
// TdAgentMemoryManager.injectCompressedSummary()
// 在每次推理前，将压缩摘要作为 System Message 注入到消息列表最前面
<compressed_history>
以下为已自动压缩的历史上下文摘要，请将其作为当前会话的延续依据：
{摘要内容}
</compressed_history>
```

### 1.4 现有配置项

```yaml
tdagent:
  compaction:
    enabled: true                    # 是否启用自动压缩
    trigger-message-count: 20        # 触发压缩的消息数量阈值
    trigger-character-count: 24000   # 触发压缩的字符数阈值
    keep-recent-messages: 8          # 保留不压缩的最近消息数
    max-summary-characters: 2400     # 摘要最大长度
  reme:
    enabled: true                    # 是否启用 ReMe 服务（压缩依赖此服务）
    base-url: http://localhost:8002  # ReMe 服务地址
    timeout-seconds: 60              # 超时时间
    top-k: 5                        # 检索 Top-K
```

### 1.5 模型上下文窗口

当前支持的模型及其上下文窗口：

| 模型 | 上下文窗口 | 最大输出 |
|------|-----------|---------|
| qwen-max (DashScope) | 32,768 tokens | 8,192 tokens |
| qwen3-max (DashScope) | 131,072 tokens | 8,192 tokens |
| qwen3.5-plus (DashScope) | 131,072 tokens | 8,192 tokens |
| qwen3.5 (DashScope) | 131,072 tokens | 8,192 tokens |
| qwen-plus (DashScope) | 131,072 tokens | 8,192 tokens |
| deepseek-chat (DeepSeek) | 65,536 tokens | 65,536 tokens |
| deepseek-reasoner (DeepSeek) | 65,536 tokens | 65,536 tokens |

### 1.6 对话生命周期

```
会话创建
  ↓
用户发送消息 → TdAgentChatServiceImpl.chat() / TdAgentStreamingServiceImpl.stream()
  ↓
构建 ConversationSessionContext（userId, sessionId, title）
  ↓
TdAgentFactory.createAgent()
  ├── 构建 System Prompt（TdAgentPromptService.buildPrompt）
  ├── 创建 MongoConversationMemory（从 MongoDB 加载历史消息 + 压缩摘要）
  ├── 创建 Toolkit（注册工具）
  ├── 创建 SkillBox（激活 Skills）
  ├── 注册 TdAgentMemoryCompactionHook（压缩 Hook）
  ├── 注册 TdAgentToolGuardHook（工具防护 Hook）
  └── 可选集成 ReMe 长期记忆
  ↓
AgentSessionStateService.restore()（恢复上次状态）
  ↓
agent.call(userMessage) / agent.stream(userMessage)
  ↓
[PreReasoningEvent] → 压缩 Hook 检查 → 注入摘要
  ↓
LLM 推理 → 工具调用循环 → 最终结果
  ↓
AgentSessionStateService.save()（保存状态）
  ↓
MongoDB 自动持久化（每次 addMessage 触发 flush）
```

---

## 二、问题诊断

### 2.1 当前方案的核心缺陷

#### 缺陷 1：触发条件与模型实际限制脱节

当前使用 **字符数**（24000）和 **消息条数**（20）作为触发条件，但 LLM 的上下文窗口以 **token** 计量。字符数与 token 的映射关系因语言和内容而异：

- 英文：约 1 token ≈ 4 字符
- 中文：约 1 token ≈ 1.5-2 字符
- 代码/JSON：约 1 token ≈ 3-4 字符

当前 24000 字符的阈值，在中文场景下约 12,000-16,000 tokens，在英文场景下约 6,000 tokens。对于 131K 上下文窗口的 qwen3-max，这意味着仅使用了约 **9-12%** 的上下文就开始压缩，**过于激进**。

#### 缺陷 2：压缩粒度粗糙

当前压缩策略是"保留最近 N 条 + 其余全部压缩"，没有区分消息类型：

- **System Prompt** 被压缩后可能丢失关键指令
- **工具调用结果**（可能包含代码、数据）被一视同仁地压缩
- **思考过程**（thinking blocks）可能包含有价值的推理链
- 没有保留对话开头的上下文建立消息

#### 缺陷 3：无法感知模型的 max_tokens 配置

当前配置中 `max_tokens: 8192`（DashScope）/ `65536`（DeepSeek），但压缩逻辑完全不考虑输出预留空间。如果输入上下文已接近窗口上限，留给输出的空间不足，会导致截断或错误。

#### 缺陷 4：压缩摘要的质量不可控

ReMe 服务的压缩质量依赖于：
- 提示词的表述（当前为固定模板）
- ReMe 服务的可用性和响应时间（timeout 60s）
- 网络延迟

如果 ReMe 服务不可用或响应慢，压缩会静默失败（`maybeCompact` 返回 false），对话继续增长直到超出模型限制。

#### 缺陷 5：缺乏压缩历史的可观测性

虽然 `ConversationMemoryDocument` 记录了 `compactionCount` 和 `summaryUpdatedAt`，但：
- 没有记录每次压缩移除了多少消息
- 没有记录压缩前后的 token 使用量
- 没有记录压缩耗时
- 无法判断压缩是否有效降低了上下文长度

### 2.2 对话场景分析

| 场景 | 当前行为 | 问题 |
|------|---------|------|
| 短对话（<10条消息） | 不触发压缩 | ✅ 正常 |
| 中等对话（10-20条） | 不触发压缩 | ⚠️ 如果消息含大量代码/工具结果，可能已接近上下文限制 |
| 长对话（>20条） | 每 20 条触发一次 | ⚠️ 字符数阈值可能过早触发，丢失有用的早期上下文 |
| 工具密集型对话 | 工具结果与普通消息同等对待 | ❌ 工具输出（如代码、JSON）通常很长，压缩后丢失关键信息 |
| 多轮推理对话 | thinking blocks 被压缩 | ❌ 思考过程包含推理链，压缩后 Agent 可能重复推理 |

---

## 三、业界参考

### 3.1 Claude Code 的上下文管理策略

Claude Code 采用以下策略管理长对话上下文：

1. **Token 精确计量**：使用 tiktoken 精确计算每条消息的 token 数
2. **85% 阈值预警**：当上下文达到模型窗口的 85% 时触发压缩
3. **分层保留策略**：
   - **System Prompt**：始终保留，永不压缩
   - **前 10% 消息**：保留对话开头的上下文建立阶段
   - **最近几条消息**：保留最近的交互，确保连贯性
   - **中间消息**：压缩为摘要
4. **渐进式压缩**：不是一次性压缩所有中间消息，而是分批压缩
5. **压缩质量保证**：使用强模型生成高质量摘要

### 3.2 Cursor / Windsurf 的做法

- 使用基于 token 的精确计量
- 保留 system prompt + 最近 N 轮对话
- 中间部分按重要性加权保留（工具结果 > 用户消息 > 助手回复）
- 支持用户手动触发压缩

### 3.3 ChatGPT / Gemini 的做法

- ChatGPT：基于 token 的滑动窗口 + 关键信息提取到 system message
- Gemini：超长上下文（1M+ tokens），但仍有压缩机制避免成本过高

### 3.4 核心启示

1. **Token 为王**：必须基于 token 计量，而非字符数
2. **85% 阈值**：业界共识的压缩触发点
3. **分层保留**：不是简单的"保留最近 N 条"
4. **System Prompt 不可压缩**：这是 Agent 的核心指令
5. **早期上下文有价值**：对话开头通常包含任务定义和关键约束

---

## 四、设计方案

### 4.1 设计目标

| 目标 | 说明 |
|------|------|
| **精确感知** | 基于 token 计量上下文长度，精确匹配模型窗口 |
| **智能触发** | 达到上下文窗口 85% 时自动触发压缩 |
| **分层保留** | System Prompt 全保留、前 10% 消息保留、最近几条保留、中间压缩 |
| **平滑降级** | ReMe 不可用时有本地降级压缩方案 |
| **可观测** | 压缩过程全链路可观测，便于调优 |
| **向后兼容** | 不破坏现有的 `conversations_memory` MongoDB 存储结构 |

### 4.2 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    TdAgentMemoryCompactionHook               │
│                    (PreReasoningEvent 触发)                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────┐    ┌───────────────────────────┐  │
│  │ ContextWindowManager │    │ TokenMeter                │  │
│  │ · 模型窗口配置管理    │    │ · 消息 token 计量         │  │
│  │ · 阈值计算            │    │ · 累计上下文 token 计算   │  │
│  │ · 压缩决策            │    │ · 多种计量策略            │  │
│  └──────────┬───────────┘    └─────────────┬─────────────┘  │
│             │                               │                │
│             ▼                               ▼                │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              ContextCompressor                           ││
│  │  · 分层消息分割（System / Head / Tail / Middle）         ││
│  │  · 压缩策略选择（ReMe / Local / Hybrid）                ││
│  │  · 摘要生成与融合                                        ││
│  │  · 压缩结果应用                                          ││
│  └─────────────────────────────────────────────────────────┘│
│             │                                               │
│             ▼                                               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              CompactionMetrics                           ││
│  │  · 压缩前 token 统计                                     ││
│  │  · 压缩后 token 统计                                     ││
│  │  · 压缩耗时                                              ││
│  │  · 压缩策略                                              ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 4.3 核心流程

```
PreReasoningEvent 触发
  ↓
1. TokenMeter 计算当前上下文总 token 数
   - systemPrompt tokens
   - compressedSummary tokens
   - 所有消息 tokens
   - 当前输入 tokens
  ↓
2. ContextWindowManager 判断是否需要压缩
   - 总 tokens ≥ 模型窗口 × 85%？
   - 否 → 跳过，返回原消息
   - 是 → 进入压缩流程
  ↓
3. ContextCompressor 分层分割消息
   - systemPrompt → 永不压缩（标记为 IMMUTABLE）
   - messages[0 .. headEnd] → 保留区（前 10%）
   - messages[tailStart .. end] → 保留区（最近 N 条）
   - messages[headEnd .. tailStart] → 压缩区
  ↓
4. 压缩区消息 → ReMe 服务生成摘要
   - ReMe 不可用？→ 降级为本地截断压缩
  ↓
5. 融合摘要
   - 新摘要 = merge(已有 compressedSummary, 本次压缩摘要)
  ↓
6. 应用压缩
   - MongoConversationMemory.applyCompaction(保留消息, 融合摘要)
   - 更新 MongoDB
  ↓
7. 注入摘要到推理上下文
   - 在消息列表最前面注入 <compressed_history> System Message
```

---

## 五、详细设计

### 5.1 Token 计量模块

#### 5.1.1 TokenMeter 接口

```java
package com.liangshou.agentic.agents.memory.compaction;

/**
 * Token 计量器 - 计算消息和文本的 token 数量。
 *
 * <p>不同模型使用不同的 tokenizer，该接口抽象了计量逻辑，
 * 允许根据模型类型选择最优的计量策略。</p>
 */
public interface TokenMeter {

    /**
     * 计算文本的 token 数量。
     *
     * @param text 文本内容
     * @return token 数量
     */
    int countTokens(String text);

    /**
     * 计算单条消息的 token 数量（包含角色开销）。
     *
     * @param message 消息对象
     * @return token 数量
     */
    int countMessageTokens(Msg message);

    /**
     * 批量计算消息列表的总 token 数量。
     *
     * @param messages 消息列表
     * @return 总 token 数量
     */
    int countTotalTokens(List<Msg> messages);
}
```

#### 5.1.2 估算型 TokenMeter 实现

考虑到 Java 生态中缺乏成熟的 tokenizer 库（tiktoken 是 Python），且引入 JNI 或 native 库会增加部署复杂度，**第一阶段采用基于规则的估算策略**：

```java
package com.liangshou.agentic.agents.memory.compaction;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.ToolResultBlock;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于规则的 Token 估算器。
 *
 * <p>估算策略：</p>
 * <ul>
 *     <li>中文字符：约 1.5 tokens/字符</li>
 *     <li>英文/数字/标点：约 0.25 tokens/字符（4 字符 ≈ 1 token）</li>
 *     <li>每条消息的固定开销：约 4 tokens（role, separators）</li>
 *     <li>每个 ContentBlock 的结构开销：约 2 tokens</li>
 * </ul>
 *
 * <p>该估算器的误差范围约 ±15%，对于压缩触发判断已足够精确。
 * 后续可替换为基于 tiktoken-jni 或远程 tokenizer 的精确实现。</p>
 */
@Component
public class EstimatingTokenMeter implements TokenMeter {

    // 中文字符的平均 token 密度
    private static final double CJK_TOKEN_RATIO = 1.5;
    // ASCII 字符的平均 token 密度
    private static final double ASCII_TOKEN_RATIO = 0.25;
    // 每条消息的固定开销（role 标记、分隔符等）
    private static final int MESSAGE_OVERHEAD = 4;
    // 每个内容块的结构开销
    private static final int CONTENT_BLOCK_OVERHEAD = 2;

    @Override
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int tokens = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCJK(c)) {
                tokens += CJK_TOKEN_RATIO;  // 向上取整在最终结果中处理
            } else {
                tokens += ASCII_TOKEN_RATIO;
            }
        }
        return Math.max(1, (int) Math.ceil(tokens));
    }

    @Override
    public int countMessageTokens(Msg message) {
        if (message == null) {
            return 0;
        }
        int tokens = MESSAGE_OVERHEAD;
        for (ContentBlock block : message.getContent()) {
            tokens += CONTENT_BLOCK_OVERHEAD;
            tokens += countBlockTokens(block);
        }
        return tokens;
    }

    @Override
    public int countTotalTokens(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return messages.stream().mapToInt(this::countMessageTokens).sum();
    }

    private int countBlockTokens(ContentBlock block) {
        if (block instanceof TextBlock textBlock) {
            return countTokens(textBlock.getText());
        }
        if (block instanceof ThinkingBlock thinkingBlock) {
            return countTokens(thinkingBlock.getThinking());
        }
        if (block instanceof ToolUseBlock toolUseBlock) {
            int nameTokens = countTokens(toolUseBlock.getName());
            int inputTokens = countTokens(toolUseBlock.getContent());
            return nameTokens + inputTokens;
        }
        if (block instanceof ToolResultBlock toolResultBlock) {
            int nameTokens = countTokens(toolResultBlock.getName());
            int outputTokens = countTokens(toolResultBlock.getOutput().stream()
                    .filter(TextBlock.class::isInstance)
                    .map(TextBlock.class::cast)
                    .map(TextBlock::getText)
                    .reduce((a, b) -> a + b)
                    .orElse(""));
            return nameTokens + outputTokens;
        }
        return 0;
    }

    private boolean isCJK(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
```

### 5.2 上下文窗口管理模块

```java
package com.liangshou.agentic.agents.memory.compaction;

import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.agents.provider.TdAgentResolvedModelConfig;
import org.springframework.stereotype.Component;

/**
 * 上下文窗口管理器 - 管理模型上下文窗口配置和压缩阈值计算。
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>维护每个模型的上下文窗口大小配置</li>
 *     <li>计算压缩触发阈值（默认 85%）</li>
 *     <li>计算输出预留空间</li>
 *     <li>判断当前上下文是否需要压缩</li>
 * </ul>
 */
@Component
public class ContextWindowManager {

    /**
     * 压缩触发阈值：上下文窗口的 85%。
     */
    private static final double COMPACTION_THRESHOLD_RATIO = 0.85;

    /**
     * 默认上下文窗口大小（当模型未配置时使用）。
     */
    private static final int DEFAULT_CONTEXT_WINDOW = 32768;

    private final TdAgentProperties properties;

    public ContextWindowManager(TdAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取当前模型的上下文窗口大小。
     *
     * <p>从配置中读取，如果未配置则使用默认值。
     * 支持通过 tdagent.compaction.context-window-size 覆盖。</p>
     *
     * @return 上下文窗口大小（tokens）
     */
    public int getContextWindowSize() {
        // 优先使用显式配置的窗口大小
        int configured = properties.getCompaction().getContextWindowSize();
        if (configured > 0) {
            return configured;
        }
        // 根据模型 ID 选择已知的窗口大小
        String modelId = properties.getModel().getModelId();
        return resolveContextWindow(modelId);
    }

    /**
     * 获取压缩触发阈值（token 数）。
     *
     * @return 触发压缩所需的最小 token 数
     */
    public int getCompactionThreshold() {
        return (int) (getContextWindowSize() * COMPACTION_THRESHOLD_RATIO);
    }

    /**
     * 获取输出预留空间（token 数）。
     *
     * <p>为 LLM 的输出预留足够的空间，避免输入+输出超出窗口限制。</p>
     *
     * @return 输出预留 token 数
     */
    public int getOutputReserve() {
        int configured = properties.getCompaction().getOutputReserveTokens();
        if (configured > 0) {
            return configured;
        }
        // 默认预留 max_tokens 的 1.2 倍，留有余量
        return (int) (properties.getModel().getMaxIters() * 100); // 粗略估算
    }

    /**
     * 判断当前上下文是否需要压缩。
     *
     * @param currentTokens 当前上下文总 token 数
     * @return true 如果需要压缩
     */
    public boolean needsCompaction(int currentTokens) {
        return currentTokens >= getCompactionThreshold();
    }

    /**
     * 计算压缩目标 token 数。
     *
     * <p>压缩后应达到的目标 token 数，为阈值的 70%，
     * 避免压缩后很快又触发下一次压缩。</p>
     *
     * @return 压缩目标 token 数
     */
    public int getCompactionTarget() {
        return (int) (getCompactionThreshold() * 0.7);
    }

    /**
     * 根据模型 ID 解析上下文窗口大小。
     */
    private int resolveContextWindow(String modelId) {
        if (modelId == null) {
            return DEFAULT_CONTEXT_WINDOW;
        }
        return switch (modelId.toLowerCase()) {
            case "qwen-max" -> 32768;
            case "qwen3-max", "qwen3.5-plus", "qwen3.5", "qwen-plus" -> 131072;
            case "deepseek-chat", "deepseek-reasoner" -> 65536;
            default -> DEFAULT_CONTEXT_WINDOW;
        };
    }
}
```

### 5.3 上下文压缩器（核心）

```java
package com.liangshou.agentic.agents.memory.compaction;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文压缩器 - 实现智能分层压缩策略。
 *
 * <p>压缩策略（参考 Claude Code）：</p>
 * <ol>
 *     <li><b>System Prompt</b>：永不压缩，始终完整保留</li>
 *     <li><b>前 10% 消息</b>：保留对话开头的上下文建立阶段</li>
 *     <li><b>最近 N 条消息</b>：保留最近的交互，确保对话连贯性</li>
 *     <li><b>中间消息</b>：压缩为摘要</li>
 * </ol>
 *
 * <p>压缩质量保证：</p>
 * <ul>
 *     <li>优先使用 ReMe 服务进行智能压缩</li>
 *     <li>ReMe 不可用时降级为本地截断压缩</li>
 *     <li>压缩后验证 token 数是否达到目标</li>
 * </ul>
 */
@Component
public class ContextCompressor {

    private static final Logger log = LoggerFactory.getLogger(ContextCompressor.class);

    /**
     * 前区保留比例：保留对话开头的 10% 消息。
     */
    private static final double HEAD_RATIO = 0.10;

    /**
     * 默认保留最近消息数（当无法按 token 精确计算时使用）。
     */
    private static final int DEFAULT_KEEP_RECENT = 6;

    private final TdAgentProperties properties;
    private final TdAgentReMeService reMeService;
    private final TokenMeter tokenMeter;
    private final ContextWindowManager windowManager;

    public ContextCompressor(
            TdAgentProperties properties,
            TdAgentReMeService reMeService,
            TokenMeter tokenMeter,
            ContextWindowManager windowManager) {
        this.properties = properties;
        this.reMeService = reMeService;
        this.tokenMeter = tokenMeter;
        this.windowManager = windowManager;
    }

    /**
     * 执行上下文压缩。
     *
     * @param context  会话上下文
     * @param memory   对话记忆
     * @param messages 当前完整消息列表（包含 system prompt 注入等）
     * @return 压缩结果，包含是否执行了压缩及统计信息
     */
    public CompactionResult compress(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            List<Msg> messages) {

        long startTime = System.currentTimeMillis();
        int totalTokensBefore = tokenMeter.countTotalTokens(messages)
                + tokenMeter.countTokens(memory.getCompressedSummary());

        log.info("[ContextCompressor] 开始压缩 - sessionId: {}, 总tokens: {}, 阈值: {}",
                context.getSessionId(), totalTokensBefore, windowManager.getCompactionThreshold());

        // 步骤1: 分层分割消息
        MessageLayers layers = splitLayers(messages);

        log.debug("[ContextCompressor] 分层结果 - system: {}, head: {}, middle: {}, tail: {}",
                layers.systemMessages.size(),
                layers.headMessages.size(),
                layers.middleMessages.size(),
                layers.tailMessages.size());

        // 步骤2: 压缩中间层
        String newSummary = compressMiddleLayer(
                context, layers.middleMessages, memory.getCompressedSummary());

        // 步骤3: 计算保留消息 = system + head + tail
        List<Msg> retainedMessages = new ArrayList<>();
        retainedMessages.addAll(layers.systemMessages);
        retainedMessages.addAll(layers.headMessages);
        retainedMessages.addAll(layers.tailMessages);

        // 步骤4: 融合摘要
        String mergedSummary = mergeSummary(memory.getCompressedSummary(), newSummary);

        // 步骤5: 应用压缩
        memory.applyCompaction(retainedMessages, mergedSummary);

        int totalTokensAfter = tokenMeter.countTotalTokens(retainedMessages)
                + tokenMeter.countTokens(mergedSummary);
        long duration = System.currentTimeMillis() - startTime;

        log.info("[ContextCompressor] 压缩完成 - sessionId: {}, tokens: {} → {}, " +
                        "保留: system({}) + head({}) + tail({}), 压缩: middle({}), 耗时: {}ms",
                context.getSessionId(),
                totalTokensBefore, totalTokensAfter,
                layers.systemMessages.size(),
                layers.headMessages.size(),
                layers.tailMessages.size(),
                layers.middleMessages.size(),
                duration);

        return CompactionResult.builder()
                .compacted(true)
                .tokensBefore(totalTokensBefore)
                .tokensAfter(totalTokensAfter)
                .messagesBefore(messages.size())
                .messagesAfter(retainedMessages.size())
                .middleCompressed(layers.middleMessages.size())
                .durationMs(duration)
                .strategy(newSummary != null ? "REME" : "LOCAL")
                .build();
    }

    /**
     * 分层分割消息。
     *
     * <p>将消息列表分为四层：</p>
     * <ul>
     *     <li>systemMessages: 角色为 SYSTEM 的消息（不含压缩摘要注入）</li>
     *     <li>headMessages: 前 10% 的非系统消息</li>
     *     <li>tailMessages: 最近的保留消息</li>
     *     <li>middleMessages: 中间待压缩的消息</li>
     * </ul>
     */
    private MessageLayers splitLayers(List<Msg> messages) {
        List<Msg> systemMessages = new ArrayList<>();
        List<Msg> nonSystemMessages = new ArrayList<>();

        for (Msg msg : messages) {
            // 跳过已注入的压缩摘要（会在后续重新注入）
            if (msg.getRole() == MsgRole.SYSTEM
                    && msg.getTextContent() != null
                    && msg.getTextContent().contains("<compressed_history>")) {
                continue;
            }
            if (msg.getRole() == MsgRole.SYSTEM) {
                systemMessages.add(msg);
            } else {
                nonSystemMessages.add(msg);
            }
        }

        int total = nonSystemMessages.size();

        // 计算前区大小：至少 1 条，最多 10%
        int headSize = Math.max(1, (int) (total * HEAD_RATIO));

        // 计算尾区大小：基于 token 预算动态计算
        int tailSize = calculateTailSize(nonSystemMessages);

        // 确保 head + tail 不超过总数
        if (headSize + tailSize >= total) {
            // 消息太少，不需要压缩
            return new MessageLayers(systemMessages, nonSystemMessages, List.of(), List.of());
        }

        int tailStart = total - tailSize;

        List<Msg> headMessages = nonSystemMessages.subList(0, headSize);
        List<Msg> middleMessages = new ArrayList<>(nonSystemMessages.subList(headSize, tailStart));
        List<Msg> tailMessages = nonSystemMessages.subList(tailStart, total);

        return new MessageLayers(systemMessages, headMessages, middleMessages, tailMessages);
    }

    /**
     * 计算尾区保留消息数。
     *
     * <p>基于压缩目标 token 数和消息的实际 token 分布，
     * 从后向前计算需要保留多少条消息才能确保压缩后不丢失近期上下文。</p>
     */
    private int calculateTailSize(List<Msg> nonSystemMessages) {
        int target = windowManager.getCompactionTarget();
        int keepRecent = properties.getCompaction().getKeepRecentMessages();

        // 至少保留配置的最近消息数
        int tailSize = Math.max(DEFAULT_KEEP_RECENT, keepRecent);

        // 不超过总数的一半（否则中间区太小，压缩意义不大）
        int maxTail = nonSystemMessages.size() / 2;
        return Math.min(tailSize, maxTail);
    }

    /**
     * 压缩中间层消息。
     *
     * <p>策略：</p>
     * <ol>
     *     <li>如果中间层为空，返回 null</li>
     *     <li>优先调用 ReMe 服务进行智能压缩</li>
     *     <li>ReMe 失败时降级为本地压缩</li>
     * </ol>
     */
    private String compressMiddleLayer(
            ConversationSessionContext context,
            List<Msg> middleMessages,
            String existingSummary) {

        if (middleMessages.isEmpty()) {
            return null;
        }

        // 策略1: 尝试 ReMe 智能压缩
        if (properties.getReme().isEnabled()) {
            try {
                String summary = reMeService.compactSessionHistory(
                        context, middleMessages, existingSummary);
                if (summary != null && !summary.isBlank()) {
                    log.debug("[ContextCompressor] ReMe 压缩成功 - 摘要长度: {}", summary.length());
                    return summary;
                }
            } catch (Exception e) {
                log.warn("[ContextCompressor] ReMe 压缩失败，降级为本地压缩 - error: {}",
                        e.getMessage());
            }
        }

        // 策略2: 本地降级压缩
        return localCompress(middleMessages);
    }

    /**
     * 本地降级压缩 - 当 ReMe 不可用时的后备方案。
     *
     * <p>提取中间层消息的关键信息，生成简单的结构化摘要。</p>
     */
    private String localCompress(List<Msg> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("[自动压缩的对话历史摘要]\n");

        int userCount = 0;
        int assistantCount = 0;
        int toolCount = 0;

        for (Msg msg : messages) {
            if (msg.getRole() == MsgRole.USER) {
                userCount++;
                // 提取用户消息的关键内容（截取前 200 字符）
                String text = msg.getTextContent();
                if (text != null && !text.isBlank()) {
                    String preview = text.length() > 200
                            ? text.substring(0, 200) + "..."
                            : text;
                    sb.append("- 用户: ").append(preview).append("\n");
                }
            } else if (msg.getRole() == MsgRole.ASSISTANT) {
                assistantCount++;
                // 助手消息只保留概要
                String text = msg.getTextContent();
                if (text != null && text.length() > 100) {
                    sb.append("- 助手回复 (").append(text.length()).append("字)\n");
                }
            } else if (msg.getRole() == MsgRole.TOOL) {
                toolCount++;
            }
        }

        sb.append(String.format("\n[统计: 用户消息 %d 条, 助手回复 %d 条, 工具调用 %d 条]",
                userCount, assistantCount, toolCount));

        // 截断到最大摘要长度
        String result = sb.toString();
        int maxLen = properties.getCompaction().getMaxSummaryCharacters();
        return result.length() <= maxLen ? result : result.substring(0, maxLen);
    }

    /**
     * 融合新旧摘要。
     */
    private String mergeSummary(String existingSummary, String newSummary) {
        if (newSummary == null || newSummary.isBlank()) {
            return existingSummary == null ? "" : existingSummary;
        }
        if (existingSummary == null || existingSummary.isBlank()) {
            return newSummary;
        }
        // 融合策略：旧摘要 + 新摘要
        return existingSummary + "\n---\n" + newSummary;
    }

    /**
     * 消息分层结果。
     */
    private record MessageLayers(
            List<Msg> systemMessages,
            List<Msg> headMessages,
            List<Msg> middleMessages,
            List<Msg> tailMessages) {
    }
}
```

### 5.4 压缩结果模型

```java
package com.liangshou.agentic.agents.memory.compaction;

import lombok.Builder;
import lombok.Getter;

/**
 * 压缩结果 - 记录单次压缩操作的详细统计信息。
 */
@Getter
@Builder
public class CompactionResult {

    /**
     * 是否执行了压缩（false 表示不需要压缩）。
     */
    private final boolean compacted;

    /**
     * 压缩前总 token 数。
     */
    private final int tokensBefore;

    /**
     * 压缩后总 token 数。
     */
    private final int tokensAfter;

    /**
     * 压缩前消息数。
     */
    private final int messagesBefore;

    /**
     * 压缩后消息数（保留的消息）。
     */
    private final int messagesAfter;

    /**
     * 被压缩的中间层消息数。
     */
    private final int middleCompressed;

    /**
     * 压缩耗时（毫秒）。
     */
    private final long durationMs;

    /**
     * 使用的压缩策略（REME / LOCAL）。
     */
    private final String strategy;

    /**
     * 压缩率（token 减少百分比）。
     */
    public double compressionRatio() {
        if (tokensBefore == 0) return 0;
        return 1.0 - (double) tokensAfter / tokensBefore;
    }
}
```

### 5.5 重构后的 TdAgentMemoryManager

```java
// 重构后的 TdAgentMemoryManager - 集成新的压缩逻辑

@Service
public class TdAgentMemoryManager {

    private final TdAgentProperties properties;
    private final TdAgentReMeService reMeService;
    private final ContextCompressor compressor;
    private final TokenMeter tokenMeter;
    private final ContextWindowManager windowManager;

    public TdAgentMemoryManager(
            TdAgentProperties properties,
            TdAgentReMeService reMeService,
            ContextCompressor compressor,
            TokenMeter tokenMeter,
            ContextWindowManager windowManager) {
        this.properties = properties;
        this.reMeService = reMeService;
        this.compressor = compressor;
        this.tokenMeter = tokenMeter;
        this.windowManager = windowManager;
    }

    /**
     * 判断并执行压缩（新版本）。
     *
     * <p>基于 token 计量判断是否需要压缩，替代原有的字符数/消息数判断。</p>
     */
    public boolean maybeCompact(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            List<Msg> currentInput) {

        if (!properties.getCompaction().isEnabled()) {
            return false;
        }

        // 计算当前总 token 数
        List<Msg> allMessages = new ArrayList<>(memory.getMessages());
        // currentInput 是即将发送给 LLM 的消息（已包含历史），不需要重复计算

        int totalTokens = tokenMeter.countTotalTokens(allMessages)
                + tokenMeter.countTokens(memory.getCompressedSummary());

        // 判断是否达到压缩阈值
        if (!windowManager.needsCompaction(totalTokens)) {
            log.debug("[TdAgentMemoryManager] 未达到压缩阈值 - 当前: {}, 阈值: {}",
                    totalTokens, windowManager.getCompactionThreshold());
            return false;
        }

        log.info("[TdAgentMemoryManager] 触发压缩 - 当前tokens: {}, 阈值: {}",
                totalTokens, windowManager.getCompactionThreshold());

        // 执行压缩
        CompactionResult result = compressor.compress(context, memory, allMessages);

        if (result.isCompacted()) {
            log.info("[TdAgentMemoryManager] 压缩成功 - tokens: {} → {} (减少 {:.1f}%), " +
                            "messages: {} → {}, 策略: {}",
                    result.getTokensBefore(), result.getTokensAfter(),
                    result.compressionRatio() * 100,
                    result.getMessagesBefore(), result.getMessagesAfter(),
                    result.getStrategy());
        }

        return result.isCompacted();
    }

    // injectCompressedSummary 方法保持不变
    public List<Msg> injectCompressedSummary(List<Msg> originalInput, MongoConversationMemory memory) {
        String compressedSummary = memory.getCompressedSummary();
        if (compressedSummary == null || compressedSummary.isBlank()) {
            return originalInput;
        }
        List<Msg> updated = new ArrayList<>();
        updated.add(
                Msg.builder()
                        .role(MsgRole.SYSTEM)
                        .textContent(
                                """
                                        <compressed_history>
                                        以下为已自动压缩的历史上下文摘要，请将其作为当前会话的延续依据：
                                        %s
                                        </compressed_history>
                                        """
                                        .formatted(compressedSummary))
                        .build());
        updated.addAll(originalInput);
        return updated;
    }
}
```

---

## 六、配置设计

### 6.1 新增配置项

```yaml
tdagent:
  compaction:
    enabled: true
    # === 新增配置 ===
    # 压缩触发模式：TOKEN（基于token计量）| LEGACY（兼容旧的字符数/消息数模式）
    trigger-mode: TOKEN
    # 模型上下文窗口大小（tokens），0 表示自动检测
    context-window-size: 0
    # 压缩触发阈值比例（上下文窗口的百分比），默认 0.85
    threshold-ratio: 0.85
    # 输出预留 token 数，0 表示自动计算
    output-reserve-tokens: 0
    # 前区保留比例（消息列表前 N% 的消息不压缩），默认 0.10
    head-ratio: 0.10
    # 尾区保留的最近消息数（覆盖 keep-recent-messages）
    keep-recent-messages: 6

    # === 保留旧配置（向后兼容） ===
    trigger-message-count: 20        # LEGACY 模式使用
    trigger-character-count: 24000   # LEGACY 模式使用
    max-summary-characters: 2400
```

### 6.2 配置属性类扩展

```java
// TdAgentProperties.Compaction 扩展
@Getter
@Setter
public static class Compaction {
    private boolean enabled = true;

    // 新增: 压缩触发模式
    private String triggerMode = "TOKEN";

    // 新增: 模型上下文窗口大小（tokens）
    private int contextWindowSize = 0;

    // 新增: 压缩触发阈值比例
    private double thresholdRatio = 0.85;

    // 新增: 输出预留 token 数
    private int outputReserveTokens = 0;

    // 新增: 前区保留比例
    private double headRatio = 0.10;

    // 保留: 保留最近消息数
    private int keepRecentMessages = 6;

    // 保留: 旧配置（LEGACY 模式）
    private int triggerMessageCount = 20;
    private int triggerCharacterCount = 24000;
    private int maxSummaryCharacters = 2400;
}
```

---

## 七、与现有系统的集成

### 7.1 MongoDB 存储兼容性

**核心原则：不改变 `conversations_memory` 集合的文档结构。**

现有的 `ConversationMemoryDocument` 已包含压缩所需的所有字段：

| 字段 | 用途 | 新方案中的使用方式 |
|------|------|-------------------|
| `messages` | 消息列表 | 保留区消息（system + head + tail） |
| `compressedSummary` | 压缩摘要 | 融合后的摘要（旧摘要 + 新摘要） |
| `compactionCount` | 压缩次数 | 每次压缩 +1（已实现） |
| `summaryUpdatedAt` | 摘要更新时间 | 每次压缩更新（已实现） |
| `sysPrompt` | 系统提示词 | 永不压缩，保持不变 |

**无需新增 MongoDB 字段。** 压缩统计信息（token 数、耗时等）仅在日志中记录，不持久化到 MongoDB，避免增加存储开销。

### 7.2 Hook 集成

`TdAgentMemoryCompactionHook` 无需修改结构，只需替换内部调用的 `maybeCompact` 方法：

```java
// TdAgentMemoryCompactionHook.onEvent() - 无需修改
// 因为它委托给 TdAgentMemoryManager.maybeCompact()
// 我们只修改 TdAgentMemoryManager 的内部实现
```

### 7.3 向后兼容策略

```
trigger-mode: TOKEN   → 使用新的 token-based 压缩（推荐）
trigger-mode: LEGACY  → 使用旧的字符数/消息数压缩（兼容）
```

当 `trigger-mode: LEGACY` 时，`maybeCompact` 方法回退到原有的判断逻辑，确保：
- 已有的 `compressedSummary` 格式不变
- MongoDB 中的数据结构不变
- 现有的 ReMe 压缩流程不变

### 7.4 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `agents/memory/compaction/TokenMeter.java` | **新增** | Token 计量接口 |
| `agents/memory/compaction/EstimatingTokenMeter.java` | **新增** | 基于规则的估算实现 |
| `agents/memory/compaction/ContextWindowManager.java` | **新增** | 上下文窗口管理器 |
| `agents/memory/compaction/ContextCompressor.java` | **新增** | 核心压缩器 |
| `agents/memory/compaction/CompactionResult.java` | **新增** | 压缩结果模型 |
| `agents/memory/TdAgentMemoryManager.java` | **修改** | 集成新的压缩逻辑，保留旧逻辑兼容 |
| `common/config/TdAgentProperties.java` | **修改** | Compaction 内部类新增配置字段 |
| `application-agentic.yaml` | **修改** | 新增压缩配置项 |

---

## 八、测试策略

### 8.1 单元测试

| 测试类 | 测试目标 |
|--------|---------|
| `EstimatingTokenMeterTest` | Token 计量准确性（中英文、代码、工具消息） |
| `ContextWindowManagerTest` | 窗口大小解析、阈值计算、目标计算 |
| `ContextCompressorTest` | 分层分割、压缩执行、摘要融合、降级策略 |
| `TdAgentMemoryManagerTest` | 新旧模式切换、压缩触发判断、摘要注入 |

### 8.2 集成测试

| 场景 | 验证点 |
|------|--------|
| 短对话不触发压缩 | < 85% 阈值时不做任何操作 |
| 长对话触发压缩 | ≥ 85% 阈值时正确分层、压缩、应用 |
| System Prompt 保留 | 压缩后 system prompt 完整不变 |
| 前 10% 消息保留 | 压缩后对话开头的消息完整保留 |
| 最近消息保留 | 压缩后最近 N 条消息完整保留 |
| 中间消息压缩 | 中间消息被正确压缩为摘要 |
| ReMe 降级 | ReMe 不可用时使用本地压缩 |
| 多次压缩累积 | 多次压缩后摘要正确融合 |
| LEGACY 模式兼容 | 旧配置下行为不变 |

### 8.3 性能测试

| 指标 | 目标 |
|------|------|
| Token 计量延迟 | < 10ms / 100 条消息 |
| 压缩总耗时（ReMe） | < 5s |
| 压缩总耗时（本地降级） | < 100ms |
| 压缩后 token 减少率 | ≥ 30% |

---

## 九、实施计划

### Phase 1: 基础设施（预计 2 天）

- [ ] 创建 `compaction` 包
- [ ] 实现 `TokenMeter` 接口和 `EstimatingTokenMeter`
- [ ] 实现 `ContextWindowManager`
- [ ] 实现 `CompactionResult` 模型
- [ ] 编写单元测试

### Phase 2: 核心压缩器（预计 2 天）

- [ ] 实现 `ContextCompressor` 的分层分割逻辑
- [ ] 实现 ReMe 压缩集成
- [ ] 实现本地降级压缩
- [ ] 实现摘要融合逻辑
- [ ] 编写单元测试

### Phase 3: 集成与配置（预计 1 天）

- [ ] 扩展 `TdAgentProperties.Compaction` 配置类
- [ ] 修改 `application-agentic.yaml` 配置
- [ ] 重构 `TdAgentMemoryManager`，集成新压缩逻辑
- [ ] 保留 LEGACY 模式兼容
- [ ] 编写集成测试

### Phase 4: 验证与调优（预计 1 天）

- [ ] 端到端测试（长对话场景）
- [ ] Token 估算精度验证
- [ ] 压缩质量验证（摘要是否保留关键信息）
- [ ] 性能基准测试
- [ ] 调整默认参数

**总计：约 6 个工作日**
