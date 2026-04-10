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
     * 根据用户ID、会话ID和审批状态查询工具调用审批记录列表，按创建时间升序排列。
     *
     * <p>该方法用于获取指定会话中处于特定审批状态（如待审批、已批准、已拒绝）的所有工具调用记录，
     * 通常用于展示用户的审批历史或待处理队列。</p>
     *
     * @param userId    用户唯一标识
     * @param sessionId 会话唯一标识
     * @param status    审批状态枚举值（PENDING/APPROVED/REJECTED）
     * @return 符合条件的审批记录列表，按创建时间从早到晚排序；若无匹配记录则返回空列表
     */
    List<ToolApprovalDocument> findByUserIdAndSessionIdAndStatusOrderByCreatedAtAsc(
            String userId, String sessionId, ToolApprovalStatus status);

    /**
     * 根据ID集合批量查询工具调用审批记录。
     *
     * <p>该方法用于一次性获取多个审批记录的详细信息，常用于批量审批操作场景，
     * 例如用户同时批准或拒绝多个待处理的工具调用请求。</p>
     *
     * @param ids 审批记录ID集合，不能为null但可以为空集合
     * @return 匹配的审批记录列表；若集合为空或无匹配记录则返回空列表
     */
    List<ToolApprovalDocument> findByIdIn(Collection<String> ids);

    /**
     * 查询指定工具调用的最新一条审批记录。
     *
     * <p>该方法用于获取某个工具调用（toolCallId）的最新审批状态，当同一工具调用可能被多次审批时
     * （例如用户先拒绝后重新审批），此方法可确保获取最新的审批决策。</p>
     *
     * @param userId     用户唯一标识
     * @param sessionId  会话唯一标识
     * @param toolCallId 工具调用的唯一标识符
     * @return 包含最新审批记录的 Optional，若不存在则返回 empty
     */
    Optional<ToolApprovalDocument> findTopByUserIdAndSessionIdAndToolCallIdOrderByCreatedAtDesc(
            String userId, String sessionId, String toolCallId);
}
