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
     * 根据用户ID和会话ID查询 Agent 会话状态。
     *
     * <p>该方法用于获取指定用户在特定会话中的运行时状态信息，包括会话配置、上下文数据等，
     * 通常在以下场景使用：</p>
     * <ul>
     *     <li>恢复中断的会话，加载之前的对话状态</li>
     *     <li>检查会话是否仍然活跃或已过期</li>
     *     <li>获取会话级别的个性化配置</li>
     * </ul>
     *
     * @param userId    用户唯一标识
     * @param sessionId 会话唯一标识
     * @return 包含会话状态文档的 Optional，若该会话不存在或已过期则返回 empty
     */
    Optional<AgentSessionStateDocument> findByUserIdAndSessionId(String userId, String sessionId);
}
