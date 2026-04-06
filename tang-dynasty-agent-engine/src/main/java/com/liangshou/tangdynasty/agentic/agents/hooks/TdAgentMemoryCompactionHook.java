package com.liangshou.tangdynasty.agentic.agents.hooks;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.MongoConversationMemory;
import com.liangshou.tangdynasty.agentic.agents.memory.TdAgentMemoryManager;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import reactor.core.publisher.Mono;

/**
 * @author LiangshouX
 */
public class TdAgentMemoryCompactionHook implements Hook {

    private final ConversationSessionContext context;
    private final MongoConversationMemory memory;
    private final TdAgentMemoryManager memoryManager;

    /**
     * 执行相关操作。
     * @param context 会话上下文
     * @param memory 记忆实例
     * @param memoryManager 记忆管理器
     */
    public TdAgentMemoryCompactionHook(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            TdAgentMemoryManager memoryManager) {
        this.context = context;
        this.memory = memory;
        this.memoryManager = memoryManager;
    }

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

    @Override
    public int priority() {
        return 10;
    }
}

