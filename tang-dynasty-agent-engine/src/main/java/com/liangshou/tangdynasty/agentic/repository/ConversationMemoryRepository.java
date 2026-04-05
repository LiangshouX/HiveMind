package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.domain.document.ConversationMemoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * ConversationMemoryRepository 对话记录存储库
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
