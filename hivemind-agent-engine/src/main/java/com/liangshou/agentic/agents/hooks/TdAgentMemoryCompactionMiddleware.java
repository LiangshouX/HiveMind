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

        // AgentScope 在推理前会把 Agent 的 sysPrompt 前置到消息列表里（name="system"）。
        // 这条系统消息是"每次调用重新生成"的瞬态内容，绝不能进入记忆或 Agent 状态：
        //  1) 进入记忆会被压缩器当作"永不压缩的 system 层"保留并落库；
        //  2) 进入 Agent 状态会每轮累积一条，导致上下文无限膨胀；
        //  3) 系统提示词内含"最近会话预览"，而预览又来自已落库的历史，
        //     会形成"系统提示词 -> 预览 -> 历史 -> 系统提示词"的正反馈，指数级膨胀。
        // 因此这里把它拆出来：只让它流向下发给 LLM 的消息列表，不进记忆、不进状态。
        List<Msg> systemMessages = new ArrayList<>();
        List<Msg> conversationMessages = new ArrayList<>();
        for (Msg msg : messages) {
            if (isFrameworkSystemMessage(msg)) {
                systemMessages.add(msg);
            } else {
                conversationMessages.add(msg);
            }
        }

        try {
            // 同步对话消息到 MongoConversationMemory（用于压缩判断）
            syncMessagesToMemory(conversationMessages);

            // 判断并执行压缩
            boolean compacted = memoryManager.maybeCompact(context, memory, conversationMessages);

            if (compacted) {
                // 关键：压缩后必须用"压缩后的消息集合"替换完整历史。
                // 否则 memory 虽然已被替换，但送给 LLM 的仍然是未压缩的完整历史，
                // 压缩对 token 完全无效（且流末持久化时又会把完整历史写回 MongoDB）。
                conversationMessages = new ArrayList<>(memory.getMessages());
            }

            // 注入压缩摘要
            conversationMessages =
                    new ArrayList<>(memoryManager.injectCompressedSummary(conversationMessages, memory));

            // 压缩成功后同步 Agent 状态上下文。
            // 否则流式结束时持久化的是 Agent 里未压缩的完整历史，
            // 会把刚刚压缩掉的中间消息重新写回 MongoDB，压缩等于白做。
            if (compacted) {
                syncAgentContext(agent, conversationMessages);
            }
        } catch (Exception e) {
            log.warn("[记忆压缩] 压缩失败，继续使用原始消息: {}", e.getMessage());
        }

        // 重新组装下发给 LLM 的消息：系统消息在前，对话消息在后
        List<Msg> outgoing = new ArrayList<>(systemMessages);
        outgoing.addAll(conversationMessages);

        // 创建新的 ReasoningInput（messages 可能已被修改）
        ReasoningInput modifiedInput = new ReasoningInput(outgoing, input.tools(), input.options());
        return next.apply(modifiedInput);
    }

    /**
     * 判断是否为 AgentScope 框架注入的系统消息（Agent 的 sysPrompt）。
     */
    private boolean isFrameworkSystemMessage(Msg msg) {
        return msg != null
                && msg.getRole() == MsgRole.SYSTEM
                && "system".equals(msg.getName());
    }

    /**
     * 将压缩后的消息同步到 Agent 状态上下文。
     */
    private void syncAgentContext(Agent agent, List<Msg> messages) {
        try {
            var state = agent.getAgentState();
            if (state == null) {
                return;
            }
            List<Msg> mutable = state.contextMutable();
            mutable.clear();
            mutable.addAll(messages);
            log.info("[记忆压缩] Agent 上下文已同步为压缩后版本 - 消息数: {}", mutable.size());
        } catch (Exception e) {
            log.warn("[记忆压缩] 同步 Agent 上下文失败: {}", e.getMessage());
        }
    }

    /**
     * 将输入消息同步到 MongoConversationMemory。
     */
    private void syncMessagesToMemory(List<Msg> inputMessages) {
        memory.clear();
        for (Msg msg : inputMessages) {
            memory.addMessage(msg);
        }
    }
}
