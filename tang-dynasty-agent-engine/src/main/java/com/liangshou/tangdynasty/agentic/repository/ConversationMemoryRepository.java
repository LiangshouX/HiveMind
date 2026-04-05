package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.domain.document.ConversationMemoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 对话记忆存储库 - 提供 ConversationMemoryDocument 的 MongoDB 数据访问接口。
 *
 * <p>该 Repository 继承自 Spring Data MongoDB 的 MongoRepository，提供：</p>
 * <ul>
 *     <li>标准的 CRUD 操作（保存、查询、删除等）</li>
 *     <li>自定义查询方法 {@link #findByUserIdAndSessionId} 用于加载指定会话的完整对话历史</li>
 * </ul>
 *
 * @author LiangshouX
 */
public interface ConversationMemoryRepository extends MongoRepository<ConversationMemoryDocument, String> {

    /**
     * 执行相关操作。
     * @param userId 用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    Optional<ConversationMemoryDocument> findByUserIdAndSessionId(String userId, String sessionId);
}
