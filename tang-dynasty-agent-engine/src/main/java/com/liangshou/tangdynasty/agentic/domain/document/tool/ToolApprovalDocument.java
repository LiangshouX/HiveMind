package com.liangshou.tangdynasty.agentic.domain.document.tool;

import com.liangshou.tangdynasty.agentic.common.enums.ToolApprovalStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * 工具审批文档 - MongoDB 中存储工具调用审批记录的持久化对象。
 *
 * <p>该文档包含以下字段：</p>
 * <ul>
 *     <li>{@code id} - 主键，UUID 随机生成</li>
 *     <li>{@code sessionId} / {@code userId} - 会话和用户的唯一标识</li>
 *     <li>{@code toolCallId} - 工具调用的唯一标识，用于关联具体的工具执行</li>
 *     <li>{@code toolName} - 被调用的工具名称</li>
 *     <li>{@code toolInputJson} - 工具输入参数的 JSON 字符串</li>
 *     <li>{@code riskLevel} - 风险等级字符串（LOW/MEDIUM/HIGH/CRITICAL）</li>
 *     <li>{@code reason} - 需要审批或拒绝的原因说明</li>
 *     <li>{@code status} - 审批状态（PENDING/APPROVED/REJECTED/EXECUTED/EXPIRED）</li>
 *     <li>{@code reviewComment} - 用户审批时填写的备注信息</li>
 *     <li>{@code createdAt} / {@code updatedAt} - 创建和更新时间</li>
 *     <li>{@code expiresAt} - 审批过期时间，超时后自动失效</li>
 * </ul>
 *
 * <p>索引策略：</p>
 * <ul>
 *     <li>复合索引 (userId, session_id, status) 优化待审批列表查询</li>
 * </ul>
 *
 * <p>该文档由 {@link com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService} 管理，
 * 支持工具调用的审批流程控制。</p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tool_approvals")
@CompoundIndex(name = "idx_approval_user_session_status", def = "{'userId': 1, 'session_id': 1, 'status': 1}")
public class ToolApprovalDocument {

    @Id
    private String id;

    @Field("session_id")
    private String sessionId;

    private String userId;

    @Field("tool_call_id")
    private String toolCallId;

    @Field("tool_name")
    private String toolName;

    @Field("tool_input_json")
    private String toolInputJson;

    @Field("risk_level")
    private String riskLevel;

    private String reason;

    private ToolApprovalStatus status;

    @Field("review_comment")
    private String reviewComment;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    @Field("expires_at")
    private Instant expiresAt;
}
