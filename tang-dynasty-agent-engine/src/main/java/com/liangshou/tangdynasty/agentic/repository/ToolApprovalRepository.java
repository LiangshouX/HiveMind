package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.common.enums.ToolApprovalStatus;
import com.liangshou.tangdynasty.agentic.domain.document.ToolApprovalDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 工具审批存储库 - 提供 ToolApprovalDocument 的 MongoDB 数据访问接口。
 *
 * <p>该 Repository 继承自 Spring Data MongoDB 的 MongoRepository，提供：</p>
 * <ul>
 *     <li>标准的 CRUD 操作（保存、查询、删除等）</li>
 *     <li>{@link #findByUserIdAndSessionIdAndStatusOrderByCreatedAtAsc} - 查询指定会话中某状态的所有审批记录，按创建时间升序</li>
 *     <li>{@link #findByIdIn} - 批量查询多个审批记录，用于批量审批操作</li>
 *     <li>{@link #findTopByUserIdAndSessionIdAndToolCallIdOrderByCreatedAtDesc} - 查询特定工具调用的最新审批记录</li>
 * </ul>
 *
 * @author LiangshouX
 */
public interface ToolApprovalRepository extends MongoRepository<ToolApprovalDocument, String> {

    /**
     * 执行相关操作。
     * @param userId 用户标识
     * @param sessionId 会话标识
     * @param status 状态
     * @return 返回结果
     */
    List<ToolApprovalDocument> findByUserIdAndSessionIdAndStatusOrderByCreatedAtAsc(
            String userId, String sessionId, ToolApprovalStatus status);

    /**
     * 执行相关操作。
     * @param ids 标识列表
     * @return 返回结果
     */
    List<ToolApprovalDocument> findByIdIn(Collection<String> ids);

    /**
     * 执行相关操作。
     * @param userId 用户标识
     * @param sessionId 会话标识
     * @param toolCallId 工具调用标识
     * @return 返回结果
     */
    Optional<ToolApprovalDocument> findTopByUserIdAndSessionIdAndToolCallIdOrderByCreatedAtDesc(
            String userId, String sessionId, String toolCallId);
}
