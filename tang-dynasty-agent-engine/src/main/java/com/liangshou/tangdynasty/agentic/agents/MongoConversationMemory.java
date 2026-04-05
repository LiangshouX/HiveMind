package com.liangshou.tangdynasty.agentic.agents;

import com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;

/**
 * MongoDB 对话记忆 - 封装基于 MongoDB 存储的 AgentScope ReAct Agent 会话记忆实现。
 *
 * <p>该类扩展了 AgentScope 的 {@link io.agentscope.core.memory.InMemoryMemory}，提供以下增强功能：</p>
 * <ul>
 *     <li><b>自动持久化</b>：每次添加或删除消息时，自动将更新后的消息列表保存到 MongoDB</li>
 *     <li><b>历史加载</b>：在初始化时从 MongoDB 加载之前的对话历史，恢复会话上下文</li>
 *     <li><b>压缩摘要</b>：维护一个压缩的历史摘要字符串，用于在长对话中保留关键信息</li>
 *     <li><b>智能压缩</b>：通过 {@link #applyCompaction} 方法应用消息压缩，替换旧消息为摘要</li>
 * </ul>
 *
 * <p>工作流程：</p>
 * <ol>
 *     <li>创建实例时，从 {@link ConversationPersistenceService} 加载历史消息和压缩摘要</li>
 *     <li>Agent 添加新消息时，调用 {@link #addMessage} 并触发 {@link #flush} 持久化</li>
 *     <li>当需要压缩时，调用 {@link #applyCompaction} 清除旧消息、保留最近消息、更新摘要</li>
 *     <li>清理会话时，调用 {@link #clear} 清空内存和数据库中的所有消息</li>
 * </ol>
 *
 * <p>该实现确保了对话历史的持久性和一致性，支持会话中断后恢复和长对话的上下文管理。</p>
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

