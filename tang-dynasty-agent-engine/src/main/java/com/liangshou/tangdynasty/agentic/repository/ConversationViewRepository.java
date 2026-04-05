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
     * 执行相关操作。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    Optional<ConversationViewDocument> findByUserIdAndSessionId(String userId, String sessionId);

    /**
     * 执行相关操作。
     *
     * @param userId 用户标识
     * @return 返回结果
     */
    List<ConversationViewDocument> findByUserIdOrderByUpdatedAtDesc(String userId);
}
