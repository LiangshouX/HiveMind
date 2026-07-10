package com.liangshou.agentic.application;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.domain.memory.model.ConversationMemoryDocument;
import com.liangshou.agentic.domain.memory.model.ConversationViewDocument;
import com.liangshou.agentic.domain.tool.model.ToolApprovalDocument;
import io.agentscope.core.message.Msg;

import java.util.List;
import java.util.Optional;

/**
 * 对话持久化服务接口 - 定义会话历史在 MongoDB 中存储和检索的操作契约。
 *
 * <p>该接口定义以下核心操作：</p>
 * <ul>
 *     <li>{@link #loadMessages} - 加载会话历史消息并转换为 AgentScope Msg 对象</li>
 *     <li>{@link #replaceMessages} - 保存或更新会话的所有消息，同时维护会话视图和压缩摘要</li>
 *     <li>{@link #clearSession} - 清空指定会话的所有消息和摘要</li>
 *     <li>{@link #deleteSession} - 删除会话（包括会话视图和历史记录）</li>
 *     <li>{@link #loadCompressedSummary} - 加载压缩后的历史摘要</li>
 *     <li>{@link #listSessions} - 查询用户的所有会话，按更新时间倒序排列</li>
 *     <li>{@link #getSessionView} - 获取会话视图摘要</li>
 *     <li>{@link #getSessionHistory} - 获取完整会话历史记录</li>
 *     <li>{@link #searchSessionMemory} - 在会话历史中搜索包含关键词的消息</li>
 *     <li>{@link #countMessages} - 统计会话的消息数量</li>
 *     <li>{@link #buildRecentPreview} - 构建最近消息预览</li>
 * </ul>
 *
 * <p>数据一致性保证：</p>
 * <ul>
 *     <li>每次消息变更都会同步更新 ConversationMemoryDocument 和 ConversationViewDocument</li>
 *     <li>会话标题自动从第一条用户消息提取，或使用默认标题</li>
 *     <li>使用乐观锁（version 字段）防止并发更新冲突</li>
 * </ul>
 *
 * @author LiangshouX
 */
public interface IConversationPersistenceService {

    /**
     * 加载历史消息。
     *
     * @param context 会话上下文，包含 userId 和 sessionId
     * @return 历史消息列表，已转换为 AgentScope Msg 对象；如果会话不存在则返回空列表
     */
    List<Msg> loadMessages(ConversationSessionContext context);

    /**
     * 替换消息列表。
     *
     * <p>该方法会保存或更新会话的所有消息，并同步更新会话视图和压缩摘要。</p>
     *
     * @param context           会话上下文
     * @param messages          新的消息列表
     * @param systemPrompt      系统提示词
     * @param compressedSummary 压缩后的历史摘要
     * @param compactionUpdated 是否更新了压缩摘要（影响 compactionCount 计数）
     * @return 保存后的 ConversationMemoryDocument 对象
     */
    ConversationMemoryDocument replaceMessages(
            ConversationSessionContext context,
            List<Msg> messages,
            String systemPrompt,
            String compressedSummary,
            boolean compactionUpdated);

    /**
     * 清空指定会话的所有消息和摘要。
     *
     * <p>该方法会保留会话视图，但清空所有消息内容和压缩摘要。</p>
     *
     * @param context 会话上下文
     */
    void clearSession(ConversationSessionContext context);

    /**
     * 删除会话（包括会话视图和历史记录）。
     *
     * <p>该方法会永久删除会话的所有数据，包括：</p>
     * <ul>
     *     <li>ConversationMemoryDocument（完整历史）</li>
     *     <li>ConversationViewDocument（会话视图）</li>
     * </ul>
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     */
    void deleteSession(String userId, String sessionId);

    /**
     * 加载压缩摘要。
     *
     * @param context 会话上下文
     * @return 压缩后的历史摘要；如果会话不存在或无摘要则返回空字符串
     */
    String loadCompressedSummary(ConversationSessionContext context);

    /**
     * 列出会话列表。
     *
     * @param userId 用户标识
     * @return 该用户的所有会话视图列表，按更新时间倒序排列
     */
    List<ConversationViewDocument> listSessions(String userId);

    /**
     * 获取会话视图摘要。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 会话视图的 Optional，如果不存在则返回 empty
     */
    Optional<ConversationViewDocument> getSessionView(String userId, String sessionId);

    /**
     * 获取完整会话历史记录。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 会话历史的 Optional，如果不存在则返回 empty
     */
    Optional<ConversationMemoryDocument> getSessionHistory(String userId, String sessionId);

    /**
     * 在会话历史中搜索包含关键词的消息。
     *
     * <p>搜索范围包括消息的角色、名称和内容（文本、输入等）。</p>
     *
     * @param context 会话上下文
     * @param query   搜索关键词（不区分大小写）
     * @param limit   返回结果数量限制
     * @return 匹配的消息格式化列表，按时间戳倒序排列
     */
    List<String> searchSessionMemory(ConversationSessionContext context, String query, int limit);

    /**
     * 统计会话的消息数量。
     *
     * @param context 会话上下文
     * @return 消息总数；如果会话不存在则返回 0
     */
    long countMessages(ConversationSessionContext context);

    /**
     * 构建最近消息预览。
     *
     * <p>该方法提取会话末尾的指定数量消息，格式化为可读的预览文本。</p>
     *
     * @param context 会话上下文
     * @param limit   预览消息数量
     * @return 格式化的预览文本，每行格式为：[timestamp] role: text
     */
    String buildRecentPreview(ConversationSessionContext context, int limit);

    /**
     * 更新会话标题。
     *
     * <p>该方法同时更新 ConversationViewDocument 和 ConversationMemoryDocument 中的标题字段。</p>
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @param title     新的会话标题
     */
    void updateSessionTitle(String userId, String sessionId, String title);

    /**
     * 追加审批消息到对话历史。
     *
     * <p>审批完成后，将审批记录作为 approval 类型的消息内容追加到 conversations_memory，
     * 确保刷新加载时能够正确展示审批状态。审批消息会被插入到最后一条 TOOL_RESULT 或 REASONING 之后。</p>
     *
     * @param context   会话上下文
     * @param approvals 审批记录列表
     */
    void appendApprovalMessages(ConversationSessionContext context, List<ToolApprovalDocument> approvals);
}
