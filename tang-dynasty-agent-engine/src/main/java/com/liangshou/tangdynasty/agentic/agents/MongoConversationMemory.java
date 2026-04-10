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
     * 构造 MongoDB 对话记忆实例，并在初始化时加载历史对话数据。
     *
     * <p>构造函数执行以下操作：</p>
     * <ol>
     *     <li>保存会话上下文和持久化服务引用</li>
     *     <li>从 MongoDB 加载压缩摘要（如果有）</li>
     *     <li>从 MongoDB 加载历史消息并添加到内存中，恢复会话上下文</li>
     * </ol>
     *
     * @param context            会话上下文，包含用户ID、会话ID等元数据
     * @param persistenceService 对话持久化服务，用于读写 MongoDB 中的对话数据
     * @param systemPrompt       系统提示词模板，用于后续消息持久化时保持一致性
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
     * 添加消息到对话记忆中，并自动触发持久化操作。
     *
     * <p>该方法重写了父类的 {@code addMessage}，在添加消息后立即调用 {@link #flush()}
     * 将更新后的消息列表同步到 MongoDB，确保对话历史的实时持久化。</p>
     *
     * <p>典型使用场景：</p>
     * <ul>
     *     <li>Agent 接收到用户输入消息时</li>
     *     <li>Agent 生成回复消息后</li>
     *     <li>工具调用产生中间结果时</li>
     * </ul>
     *
     * @param message 要添加的消息对象，包含角色、内容、元数据等信息
     */
    @Override
    public void addMessage(Msg message) {
        super.addMessage(message);
        flush();
    }

    /**
     * 删除指定索引位置的消息，并自动触发持久化操作。
     *
     * <p>该方法重写了父类的 {@code deleteMessage}，在删除消息后立即调用 {@link #flush()}
     * 将更新后的消息列表同步到 MongoDB，保持数据库与内存的一致性。</p>
     *
     * <p>典型使用场景：</p>
     * <ul>
     *     <li>用户请求删除某条错误的消息</li>
     *     <li>消息压缩时移除旧消息</li>
     *     <li>清理无效或过期的对话内容</li>
     * </ul>
     *
     * @param index 要删除的消息在列表中的索引位置（从0开始）
     * @throws IndexOutOfBoundsException 如果索引超出消息列表范围
     */
    @Override
    public void deleteMessage(int index) {
        super.deleteMessage(index);
        flush();
    }

    /**
     * 清空当前会话的所有对话记忆，包括内存和数据库中的数据。
     *
     * <p>该方法执行以下清理操作：</p>
     * <ol>
     *     <li>调用父类 {@code clear()} 清空内存中的消息列表</li>
     *     <li>重置压缩摘要为空字符串</li>
     *     <li>调用持久化服务清除 MongoDB 中该会话的所有数据</li>
     * </ol>
     *
     * <p><strong>注意：</strong>此操作不可逆，调用后将永久删除该会话的所有历史记录。</p>
     *
     * <p>典型使用场景：</p>
     * <ul>
     *     <li>用户主动清空会话历史</li>
     *     <li>创建新会话前清理旧数据</li>
     *     <li>测试环境中重置会话状态</li>
     * </ul>
     */
    @Override
    public void clear() {
        super.clear();
        compressedSummary = "";
        persistenceService.clearSession(context);
    }

    /**
     * 获取当前的压缩历史摘要字符串。
     *
     * <p>该方法返回经过压缩处理的对话历史摘要，用于在长对话中保留关键上下文信息。
     * 当原始对话消息过多时，Agent 可以依赖此摘要而非完整历史来理解之前的交流内容。</p>
     *
     * <p>摘要内容由记忆压缩 Hook 定期生成和更新，通常包含：</p>
     * <ul>
     *     <li>对话的主要主题和结论</li>
     *     <li>重要的技术决策或代码片段</li>
     *     <li>用户的关键需求和偏好</li>
     * </ul>
     *
     * @return 压缩摘要字符串；若尚未生成摘要则返回空字符串
     */
    public String getCompressedSummary() {
        return compressedSummary == null ? "" : compressedSummary;
    }

    /**
     * 应用消息压缩策略，替换旧消息为压缩摘要并持久化。
     *
     * <p>该方法执行以下操作：</p>
     * <ol>
     *     <li>清空内存中的所有消息</li>
     *     <li>重新添加压缩后保留的最近消息</li>
     *     <li>更新压缩摘要为新生成的摘要内容</li>
     *     <li>调用持久化服务将压缩后的消息和摘要保存到 MongoDB</li>
     * </ol>
     *
     * <p>压缩策略的目的是在长对话中控制上下文长度，避免超出模型的 token 限制，
     * 同时通过摘要保留关键历史信息，确保 Agent 能够理解完整的对话脉络。</p>
     *
     * <p>典型使用场景：</p>
     * <ul>
     *     <li>对话消息数量超过预设阈值时自动触发</li>
     *     <li>用户手动请求压缩历史以优化性能</li>
     *     <li>检测到上下文即将超出模型限制时</li>
     * </ul>
     *
     * @param remainingMessages 压缩后需要保留的最近消息集合，通常是最后几条关键消息
     * @param summary           新生成的压缩摘要，概括被移除的历史消息的核心内容
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

