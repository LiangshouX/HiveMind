package com.liangshou.tangdynasty.agentic.agents.guard.approval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.guard.ToolGuardDecision;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.common.enums.ToolApprovalStatus;
import com.liangshou.tangdynasty.agentic.domain.document.ToolApprovalDocument;
import com.liangshou.tangdynasty.agentic.repository.ToolApprovalRepository;
import io.agentscope.core.message.ToolUseBlock;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * ToolApprovalService 提供工具执行审批操作 Service
 *
 * @author LiangshouX
 */
@Service
public class ToolApprovalService {

    private final ToolApprovalRepository toolApprovalRepository;
    private final TdAgentProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 执行相关操作。
     *
     * @param toolApprovalRepository 审批 Repository
     * @param properties             外部化配置
     * @param objectMapper           ObjectMapper
     */
    public ToolApprovalService(
            ToolApprovalRepository toolApprovalRepository,
            TdAgentProperties properties,
            ObjectMapper objectMapper) {
        this.toolApprovalRepository = toolApprovalRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 createPendingApprovals 操作。
     *
     * @param context          会话上下文
     * @param guardedTools     待审批的工具调用
     * @param decisionProvider 决策提供器
     * @return 返回结果
     */
    public List<ToolApprovalDocument> createPendingApprovals(
            ConversationSessionContext context,
            List<ToolUseBlock> guardedTools,
            Function<ToolUseBlock, ToolGuardDecision> decisionProvider) {
        Instant now = Instant.now();
        return guardedTools.stream()
                .map(
                        toolUse -> {
                            ToolGuardDecision decision = decisionProvider.apply(toolUse);
                            return ToolApprovalDocument.builder()
                                    .id(UUID.randomUUID().toString())
                                    .sessionId(context.getSessionId())
                                    .userId(context.getUserId())
                                    .toolCallId(toolUse.getId())
                                    .toolName(toolUse.getName())
                                    .toolInputJson(toJson(toolUse.getInput()))
                                    .riskLevel(decision.getRiskLevel().name())
                                    .reason(decision.getReason())
                                    .status(ToolApprovalStatus.PENDING)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .expiresAt(
                                            now.plusSeconds(
                                                    properties
                                                            .getToolGuard()
                                                            .getPendingExpireMinutes()
                                                            * 60L))
                                    .build();
                        })
                .map(toolApprovalRepository::save)
                .toList();
    }

    /**
     * 执行 listPending 操作。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    public List<ToolApprovalDocument> listPending(String userId, String sessionId) {
        expireIfNecessary(userId, sessionId);
        return toolApprovalRepository.findByUserIdAndSessionIdAndStatusOrderByCreatedAtAsc(
                userId, sessionId, ToolApprovalStatus.PENDING);
    }

    /**
     * 执行 approve 操作。
     *
     * @param approvalIds 审批记录标识
     * @param comment     备注信息
     * @return 返回结果
     */
    public List<ToolApprovalDocument> approve(Collection<String> approvalIds, String comment) {
        return updateStatus(approvalIds, ToolApprovalStatus.APPROVED, comment);
    }

    /**
     * 执行 reject 操作。
     *
     * @param approvalIds 审批记录标识
     * @param comment     备注信息
     * @return 返回结果
     */
    public List<ToolApprovalDocument> reject(Collection<String> approvalIds, String comment) {
        return updateStatus(approvalIds, ToolApprovalStatus.REJECTED, comment);
    }

    /**
     * 执行 isApproved 操作。
     *
     * @param context    会话上下文
     * @param toolCallId 工具调用标识
     * @return 返回结果
     */
    public boolean isApproved(ConversationSessionContext context, String toolCallId) {
        return toolApprovalRepository
                .findTopByUserIdAndSessionIdAndToolCallIdOrderByCreatedAtDesc(
                        context.getUserId(), context.getSessionId(), toolCallId)
                .filter(document -> document.getStatus() == ToolApprovalStatus.APPROVED)
                .filter(document -> document.getExpiresAt() == null || document.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    /**
     * 执行 markExecuted 操作。
     *
     * @param context    会话上下文
     * @param toolCallId 工具调用标识
     */
    public void markExecuted(ConversationSessionContext context, String toolCallId) {
        toolApprovalRepository
                .findTopByUserIdAndSessionIdAndToolCallIdOrderByCreatedAtDesc(
                        context.getUserId(), context.getSessionId(), toolCallId)
                .ifPresent(
                        document -> {
                            document.setStatus(ToolApprovalStatus.EXECUTED);
                            document.setUpdatedAt(Instant.now());
                            toolApprovalRepository.save(document);
                        });
    }

    private List<ToolApprovalDocument> updateStatus(
            Collection<String> approvalIds, ToolApprovalStatus status, String comment) {
        Instant now = Instant.now();
        return toolApprovalRepository.findByIdIn(approvalIds).stream()
                .map(
                        document -> {
                            document.setStatus(status);
                            document.setReviewComment(comment);
                            document.setUpdatedAt(now);
                            return toolApprovalRepository.save(document);
                        })
                .toList();
    }

    private void expireIfNecessary(String userId, String sessionId) {
        Instant now = Instant.now();
        toolApprovalRepository.findByUserIdAndSessionIdAndStatusOrderByCreatedAtAsc(
                        userId, sessionId, ToolApprovalStatus.PENDING)
                .stream()
                .filter(document -> document.getExpiresAt() != null && document.getExpiresAt().isBefore(now))
                .forEach(
                        document -> {
                            document.setStatus(ToolApprovalStatus.EXPIRED);
                            document.setUpdatedAt(now);
                            toolApprovalRepository.save(document);
                        });
    }

    private String toJson(Map<String, Object> input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}

