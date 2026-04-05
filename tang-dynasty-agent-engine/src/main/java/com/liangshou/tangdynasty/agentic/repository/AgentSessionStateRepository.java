package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.domain.document.AgentSessionStateDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Agent 会话状态存储库 - 提供 AgentSessionStateDocument 的 MongoDB 数据访问接口。
 *
 * <p>该 Repository 继承自 Spring Data MongoDB 的 MongoRepository，提供：</p>
 * <ul>
 *     <li>标准的 CRUD 操作（保存、查询、删除等）</li>
 *     <li>自定义查询方法 {@link #findByUserIdAndSessionId} 用于根据用户和会话ID精确查找状态</li>
 * </ul>
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
