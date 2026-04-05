package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.domain.document.AgentSessionStateDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * AgentSessionStateRepository 会话状态存储库
 *
 * @author LiangshouX
 */
public interface AgentSessionStateRepository extends MongoRepository<AgentSessionStateDocument, String> {

    /**
     * 根据用户标识和会话标识查询会话状态
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    Optional<AgentSessionStateDocument> findByUserIdAndSessionId(String userId, String sessionId);
}
