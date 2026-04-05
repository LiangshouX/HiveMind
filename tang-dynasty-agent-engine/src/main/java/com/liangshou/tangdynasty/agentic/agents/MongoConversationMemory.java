package com.liangshou.tangdynasty.agentic.agents;

import com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;

/**
 * 封装基于 MongoDB 存储的用于 AgentScope ReAct Agent 会话记忆的实现。
 *
 * @author LiangshouX
 */
public class MongoConversationMemory extends InMemoryMemory {

    private final ConversationSessionContext context;
    private final ConversationPersistenceService persistenceService;
    private final String systemPrompt;
    private String compressedSummary;

    /**
     * 执行相关操作。
     *
     * @param context            会话上下文
     * @param persistenceService 持久化服务
     * @param systemPrompt       system prompt
     */
    public MongoConversationMemory(
            ConversationSessionContext context,
            ConversationPersistenceService persistenceService,
            String systemPrompt) {
        this.context = context;
        this.persistenceService = persistenceService;
        this.systemPrompt = systemPrompt;
        this.compressedSummary = persistenceService.loadCompressedSummary(context);
        persistenceService.loadMessages(context).forEach(super::addMessage);
    }

    /**
     * 执行 addMessage 操作。
     *
     * @param message 消息
     */
    @Override
    public void addMessage(Msg message) {
        super.addMessage(message);
        flush();
    }

    /**
     * 执行 deleteMessage 操作。
     *
     * @param index 索引
     */
    @Override
    public void deleteMessage(int index) {
        super.deleteMessage(index);
        flush();
    }

    /**
     * 清理当前数据。
     */
    @Override
    public void clear() {
        super.clear();
        compressedSummary = "";
        persistenceService.clearSession(context);
    }

    /**
     * 执行 getCompressedSummary 操作。
     *
     * @return 返回结果
     */
    public String getCompressedSummary() {
        return compressedSummary == null ? "" : compressedSummary;
    }

    /**
     * 执行 applyCompaction 操作。
     *
     * @param remainingMessages 压缩后保留的消息
     * @param summary           更新后的摘要
     */
    public void applyCompaction(Iterable<Msg> remainingMessages, String summary) {
        super.clear();
        for (Msg remainingMessage : remainingMessages) {
            super.addMessage(remainingMessage);
        }
        this.compressedSummary = summary;
        persistenceService.replaceMessages(
                context, getMessages(), systemPrompt, getCompressedSummary(), true);
    }

    private void flush() {
        persistenceService.replaceMessages(
                context, getMessages(), systemPrompt, getCompressedSummary(), false);
    }
}

