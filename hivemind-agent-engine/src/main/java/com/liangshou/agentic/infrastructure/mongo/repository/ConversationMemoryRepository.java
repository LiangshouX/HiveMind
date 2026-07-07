package com.liangshou.agentic.infrastructure.mongo.repository;

import com.liangshou.agentic.domain.memory.model.ConversationMemoryDocument;
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
     * 根据用户ID和会话ID查询对话记忆文档。
     *
     * <p>该方法用于加载指定会话的完整对话历史，包括所有用户消息、Agent 回复及元数据，
     * 通常在以下场景使用：</p>
     * <ul>
     *     <li>恢复会话时重建对话上下文，使 Agent 能够理解之前的交流内容</li>
     *     <li>导出或备份用户的对话记录</li>
     *     <li>分析对话质量或进行用户行为研究</li>
     * </ul>
     *
     * @param userId    用户唯一标识
     * @param sessionId 会话唯一标识
     * @return 包含对话记忆文档的 Optional，若该会话不存在则返回 empty
     */
    Optional<ConversationMemoryDocument> findByUserIdAndSessionId(String userId, String sessionId);
}
