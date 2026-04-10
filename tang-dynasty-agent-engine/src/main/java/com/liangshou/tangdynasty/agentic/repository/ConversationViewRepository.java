package com.liangshou.tangdynasty.agentic.repository;


import com.liangshou.tangdynasty.agentic.domain.document.ConversationViewDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 对话视图存储库 - 提供 ConversationViewDocument 的 MongoDB 数据访问接口。
 *
 * <p>该 Repository 继承自 Spring Data MongoDB 的 MongoRepository，提供：</p>
 * <ul>
 *     <li>标准的 CRUD 操作（保存、查询、删除等）</li>
 *     <li>{@link #findByUserIdAndSessionId} - 查询指定会话的视图信息</li>
 *     <li>{@link #findByUserIdOrderByUpdatedAtDesc} - 按更新时间倒序查询用户的所有会话，用于会话列表展示</li>
 * </ul>
 *
 * @author LiangshouX
 */
public interface ConversationViewRepository extends MongoRepository<ConversationViewDocument, String> {

    /**
     * 根据用户ID和会话ID查询对话视图信息。
     *
     * <p>该方法用于获取指定会话的摘要信息，包括会话标题、最后一条消息预览、更新时间等，
     * 通常在以下场景使用：</p>
     * <ul>
     *     <li>在会话列表中展示单个会话的卡片信息</li>
     *     <li>检查会话是否存在及基本属性</li>
     *     <li>快速定位特定会话而不加载完整对话历史</li>
     * </ul>
     *
     * @param userId    用户唯一标识
     * @param sessionId 会话唯一标识
     * @return 包含对话视图文档的 Optional，若该会话不存在则返回 empty
     */
    Optional<ConversationViewDocument> findByUserIdAndSessionId(String userId, String sessionId);

    /**
     * 查询指定用户的所有对话视图，按更新时间降序排列。
     *
     * <p>该方法用于获取用户的所有会话列表，最新活跃的会话排在前面，
     * 通常用于以下场景：</p>
     * <ul>
     *     <li>在侧边栏或主页展示用户的会话历史记录</li>
     *     <li>实现"最近对话"功能，方便用户快速恢复之前的交流</li>
     *     <li>统计用户的会话数量和使用频率</li>
     * </ul>
     *
     * @param userId 用户唯一标识
     * @return 该用户的所有对话视图列表，按更新时间从新到旧排序；若无会话则返回空列表
     */
    List<ConversationViewDocument> findByUserIdOrderByUpdatedAtDesc(String userId);
}
