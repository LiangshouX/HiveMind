package com.liangshou.agentic.agents.memory;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.application.IConversationPersistenceService;
import io.agentscope.core.message.Msg;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB 对话记忆辅助类 - 管理对话历史和压缩摘要。
 *
 * <p>在 AgentScope 2.0 中，对话上下文由 {@link io.agentscope.core.state.AgentState} 管理。
 * 此类不再继承已废弃的 {@code InMemoryMemory}，而是作为独立的辅助对象，
 * 供记忆压缩中间件使用。</p>
 *
 * @author LiangshouX
 */
public class MongoConversationMemory {

    private final ConversationSessionContext context;
    private final IConversationPersistenceService persistenceService;
    private final String systemPrompt;
    private final List<Msg> messages = new ArrayList<>();
    private String compressedSummary;

    public MongoConversationMemory(
            ConversationSessionContext context,
            IConversationPersistenceService persistenceService,
            String systemPrompt) {
        this.context = context;
        this.persistenceService = persistenceService;
        this.systemPrompt = systemPrompt;
        this.compressedSummary = persistenceService.loadCompressedSummary(context);
        persistenceService.loadMessages(context).forEach(messages::add);
    }

    /**
     * 添加消息到对话记忆中。
     */
    public void addMessage(Msg message) {
        messages.add(message);
    }

    /**
     * 获取所有消息。
     */
    public List<Msg> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * 清空所有消息。
     */
    public void clear() {
        messages.clear();
    }

    /**
     * 获取当前的压缩历史摘要字符串。
     */
    public String getCompressedSummary() {
        return compressedSummary == null ? "" : compressedSummary;
    }

    /**
     * 应用消息压缩策略，替换旧消息为压缩摘要并持久化。
     */
    public void applyCompaction(Iterable<Msg> remainingMessages, String summary) {
        messages.clear();
        for (Msg remainingMessage : remainingMessages) {
            messages.add(remainingMessage);
        }
        this.compressedSummary = summary;
        persistenceService.replaceMessages(
                context, getMessages(), systemPrompt, getCompressedSummary(), true);
    }
}
