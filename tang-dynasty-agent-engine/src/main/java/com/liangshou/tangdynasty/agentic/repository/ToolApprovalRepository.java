package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.common.enums.ToolApprovalStatus;
import com.liangshou.tangdynasty.agentic.domain.document.ToolApprovalDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * ToolApprovalRepository 工具审批存储库接口。
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
