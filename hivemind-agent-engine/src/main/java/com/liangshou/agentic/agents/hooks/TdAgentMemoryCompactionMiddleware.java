package com.liangshou.agentic.agents.hooks;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.agentic.agents.memory.TdAgentMemoryManager;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.state.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Agent 记忆压缩中间件 - 在推理前自动管理对话上下文的长度。
 *
 * <p>使用 AgentScope 2.0 推荐的 {@link MiddlewareBase} 接口，
 * 通过 {@link #onReasoning} 拦截推理阶段，在 LLM 调用前执行压缩。</p>
 *
 * @author LiangshouX
 */
public class TdAgentMemoryCompactionMiddleware implements MiddlewareBase {

    private static final Logger log = LoggerFactory.getLogger(TdAgentMemoryCompactionMiddleware.class);

    private final ConversationSessionContext context;
    private final MongoConversationMemory memory;
    private final TdAgentMemoryManager memoryManager;

    public TdAgentMemoryCompactionMiddleware(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            TdAgentMemoryManager memoryManager) {
        this.context = context;
        this.memory = memory;
        this.memoryManager = memoryManager;
    }

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext ctx,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {

        List<Msg> messages = new ArrayList<>(input.messages());

        try {
            // 保留框架在推理输入头部注入的系统消息（sysPrompt 等），
            // 这类消息不属于对话记忆，压缩与状态回写都不应处理它们
            List<Msg> leadingSystemMessages = new ArrayList<>();
            for (Msg msg : messages) {
                if (msg.getRole() == MsgRole.SYSTEM) {
                    leadingSystemMessages.add(msg);
                } else {
                    break;
                }
            }

            // 同步对话消息到 MongoConversationMemory（用于压缩判断，不含系统消息）
            syncMessagesToMemory(messages);

            // 判断并执行压缩
            boolean compacted = memoryManager.maybeCompact(context, memory, messages);

            if (compacted) {
                // 压缩发生：推理输入改为"系统消息 + 保留的近期消息"，真正收缩上下文，
                // 而不是在完整历史上再叠加摘要
                messages = new ArrayList<>(leadingSystemMessages);
                messages.addAll(memory.getMessages());

                // 将压缩结果回写到 call-scoped AgentState，框架随后会自动持久化压缩后的状态，
                // 避免持久化上下文无界增长、每次请求重复触发 LLM 摘要
                AgentState agentState = ctx.getAgentState();
                if (agentState != null) {
                    List<Msg> stateContext = agentState.contextMutable();
                    stateContext.clear();
                    stateContext.addAll(memory.getMessages());
                } else {
                    log.warn("[记忆压缩] 无法获取 call-scoped AgentState，压缩结果仅对本次调用生效");
                }
            }

            // 注入压缩摘要（存在历史摘要时前置摘要系统消息）
            messages = new ArrayList<>(memoryManager.injectCompressedSummary(messages, memory));
        } catch (Exception e) {
            log.warn("[记忆压缩] 压缩失败，继续使用原始消息: {}", e.getMessage());
        }

        // 创建新的 ReasoningInput（messages 可能已被修改）
        ReasoningInput modifiedInput = new ReasoningInput(messages, input.tools(), input.options());
        return next.apply(modifiedInput);
    }

    /**
     * 将输入消息同步到 MongoConversationMemory。
     *
     * <p>过滤系统消息：框架会在推理输入头部注入 sysPrompt 等系统消息，
     * 它们不属于对话记忆（与 1.x 时代从持久层加载的语义一致）。</p>
     */
    private void syncMessagesToMemory(List<Msg> inputMessages) {
        memory.clear();
        for (Msg msg : inputMessages) {
            if (msg.getRole() == MsgRole.SYSTEM) {
                continue;
            }
            memory.addMessage(msg);
        }
    }
}
