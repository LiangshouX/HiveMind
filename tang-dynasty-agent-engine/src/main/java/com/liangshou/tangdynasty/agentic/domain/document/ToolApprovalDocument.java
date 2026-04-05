package com.liangshou.tangdynasty.agentic.domain.document;

import com.liangshou.tangdynasty.agentic.common.enums.ToolApprovalStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * ToolApprovalDocument 组件，用于存储工具审批信息
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
