package com.liangshou.tangdynasty.agentic.repository;


import com.liangshou.tangdynasty.agentic.domain.document.ConversationViewDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * ConversationViewRepository 对话记忆视图存储库
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
