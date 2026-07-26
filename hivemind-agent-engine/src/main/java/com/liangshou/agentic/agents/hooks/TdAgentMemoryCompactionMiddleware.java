package com.liangshou.agentic.agents.hooks;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.agentic.agents.memory.TdAgentMemoryManager;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
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

        try {
            // 同步输入消息到 MongoConversationMemory（用于压缩判断）
            syncMessagesToMemory(messages);

            // 判断并执行压缩
            memoryManager.maybeCompact(context, memory, messages);

            // 注入压缩摘要
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
     */
    private void syncMessagesToMemory(List<Msg> inputMessages) {
        memory.clear();
        for (Msg msg : inputMessages) {
            memory.addMessage(msg);
        }
    }
}
