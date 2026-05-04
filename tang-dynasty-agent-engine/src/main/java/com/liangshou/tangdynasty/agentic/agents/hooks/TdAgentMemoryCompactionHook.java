package com.liangshou.tangdynasty.agentic.agents.hooks;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.tangdynasty.agentic.agents.memory.TdAgentMemoryManager;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import reactor.core.publisher.Mono;

/**
 * Agent 记忆压缩 Hook - 在推理前自动管理对话上下文的长度。
 *
 * <p><b>AgentScope Hook 机制说明：</b></p>
 * <p>根据 <a href="https://java.agentscope.io/zh/task/hook.html">AgentScope-Java Hook 官方文档</a>，
 * Hook 是 AgentScope 提供的生命周期拦截机制，允许开发者在 Agent 执行的关键节点插入自定义逻辑。
 * Hook 通过 {@link io.agentscope.core.hook.Hook#onEvent(HookEvent)} 方法监听特定事件，
 * 并可以修改事件中的数据或控制 Agent 的执行流程。</p>
 *
 * <p><b>本 Hook 的作用时机：</b></p>
 * <p>本 Hook 监听 {@link PreReasoningEvent} 事件，该事件在 Agent 进行推理（调用 LLM）之前触发。
 * 在这个时机点，Agent 已经收集了用户输入和历史消息，但尚未发送给大模型进行推理。
 * 这是优化上下文、控制 token 消耗的理想时机。</p>
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 *     <li><b>智能压缩触发</b>：通过 {@link TdAgentMemoryManager#maybeCompact} 判断是否需要压缩历史消息。
 *         触发条件包括：
 *         <ul>
 *             <li>历史消息数量超过配置的阈值（默认 20 条）</li>
 *             <li>总字符数超过配置的阈值（默认 24000 字符）</li>
 *         </ul>
 *     </li>
 *     <li><b>历史摘要注入</b>：通过 {@link TdAgentMemoryManager#injectCompressedSummary} 将压缩后的历史摘要
 *         作为 System Message 注入到当前推理上下文中，确保 Agent 不会丢失重要的历史信息。
 *         摘要格式为 XML 标签包裹的结构化文本：{@code <compressed_history>...摘要内容...</compressed_history>}
 *     </li>
 * </ul>
 *
 * <p><b>工作流程：</b></p>
 * <ol>
 *     <li>Agent 准备进行推理，触发 {@link PreReasoningEvent} 事件</li>
 *     <li>本 Hook 的 {@link #onEvent} 方法被调用</li>
 *     <li>检查当前记忆中的消息数量和字符数是否达到压缩阈值</li>
 *     <li>如果达到阈值，调用 ReMe 服务对旧消息进行压缩，保留最近 N 条消息不压缩</li>
 *     <li>将压缩后的摘要更新到 {@link MongoConversationMemory} 中</li>
 *     <li>将摘要作为 System Message 注入到推理输入消息列表的最前面</li>
 *     <li>Agent 使用优化后的消息列表进行推理</li>
 * </ol>
 *
 * <p><b>优先级设置：</b></p>
 * <p>本 Hook 的优先级为 10（通过 {@link #priority()} 方法返回）。在 AgentScope 中，
 * 优先级数值越小，Hook 越先执行。这个优先级确保了记忆压缩在其他高优先级 Hook（如工具防护 Hook，
 * 优先级 20）之前执行，保证推理时上下文已经是优化状态。</p>
 *
 * <p><b>配置驱动：</b></p>
 * <p>压缩行为由 {@link com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties.Compaction} 配置控制：
 * <ul>
 *     <li>{@code enabled} - 是否启用自动压缩（默认 true）</li>
 *     <li>{@code triggerMessageCount} - 触发压缩的消息数量阈值（默认 20）</li>
 *     <li>{@code triggerCharacterCount} - 触发压缩的字符数阈值（默认 24000）</li>
 *     <li>{@code keepRecentMessages} - 保留不压缩的最近消息数（默认 8）</li>
 *     <li>{@code maxSummaryCharacters} - 摘要最大长度（默认 2400）</li>
 * </ul>
 * </p>
 *
 * <p><b>使用场景：</b></p>
 * <ul>
 *     <li>长对话场景：避免上下文超出 LLM 的 token 限制</li>
 *     <li>成本控制：减少每次推理的 token 消耗，降低 API 调用成本</li>
 *     <li>性能优化：缩短推理时间，提高响应速度</li>
 *     <li>信息保留：通过摘要机制保留关键历史信息，避免简单截断导致的信息丢失</li>
 * </ul>
 *
 * @author LiangshouX
 * @see io.agentscope.core.hook.Hook
 * @see io.agentscope.core.hook.PreReasoningEvent
 * @see TdAgentMemoryManager
 * @see MongoConversationMemory
 */
public class TdAgentMemoryCompactionHook implements Hook {

    private final ConversationSessionContext context;
    private final MongoConversationMemory memory;
    private final TdAgentMemoryManager memoryManager;

    /**
     * 构造记忆压缩 Hook。
     *
     * @param context       会话上下文，包含用户 ID、会话 ID 等身份信息
     * @param memory        MongoDB 对话记忆实例，用于存储和加载历史消息及压缩摘要
     * @param memoryManager 记忆管理器，负责执行压缩逻辑和摘要注入
     */
    public TdAgentMemoryCompactionHook(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            TdAgentMemoryManager memoryManager) {
        this.context = context;
        this.memory = memory;
        this.memoryManager = memoryManager;
    }

    /**
     * 处理 Hook 事件，在推理前执行记忆压缩和摘要注入。
     *
     * <p>该方法实现了 {@link Hook#onEvent(HookEvent)} 接口，是 Hook 的核心逻辑入口。</p>
     *
     * <p><b>处理流程：</b></p>
     * <ol>
     *     <li>检查事件类型是否为 {@link PreReasoningEvent}，如果不是则直接透传事件</li>
     *     <li>调用 {@link TdAgentMemoryManager#maybeCompact} 判断是否需要压缩历史消息</li>
     *     <li>如果需要压缩，执行压缩逻辑并更新 {@link MongoConversationMemory} 中的摘要</li>
     *     <li>调用 {@link TdAgentMemoryManager#injectCompressedSummary} 将摘要作为 System Message 注入到输入消息中</li>
     *     <li>通过 {@link PreReasoningEvent#setInputMessages} 更新事件的输入消息列表</li>
     *     <li>返回修改后的事件，继续 Agent 的推理流程</li>
     * </ol>
     *
     * <p><b>响应式编程模型：</b></p>
     * <p>该方法返回 {@link Mono<T>}，符合 AgentScope 的响应式编程范式。
     * 由于记忆压缩是同步操作，直接使用 {@link Mono#just(Object)} 包装返回的事件即可。</p>
     *
     * @param event Hook 事件，可能是任意类型的 {@link HookEvent}
     * @param <T>   事件类型，由 AgentScope 框架推断
     * @return 修改后的事件，包裹在 {@link Mono} 中
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (!(event instanceof PreReasoningEvent preReasoningEvent)) {
            return Mono.just(event);
        }
        memoryManager.maybeCompact(context, memory, preReasoningEvent.getInputMessages());
        preReasoningEvent.setInputMessages(
                memoryManager.injectCompressedSummary(preReasoningEvent.getInputMessages(), memory));
        return Mono.just(event);
    }

    /**
     * 获取 Hook 的优先级。
     *
     * <p>在 AgentScope 中，Hook 按照优先级从低到高的顺序执行（数值越小，优先级越高）。</p>
     *
     * <p>本 Hook 返回优先级 10，确保在工具防护 Hook（优先级 20）之前执行，
     * 这样可以在推理前完成上下文优化，使后续的推理和工具调用都基于压缩后的上下文进行。</p>
     *
     * @return Hook 优先级，数值为 10
     */
    @Override
    public int priority() {
        return 10;
    }
}

