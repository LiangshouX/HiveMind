package com.liangshou.tangdynasty.agentic.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 工具调用审批操作请求对象，用于处理用户对待审批工具调用的批准或拒绝操作。
 * <p>
 * 包含以下字段：
 * <ul>
 *   <li>userId - 用户标识（必填）</li>
 *   <li>sessionId - 会话标识（必填）</li>
 *   <li>approvalIds - 要处理的审批记录 ID 列表（必填，非空）</li>
 *   <li>title - 会话标题（可选）</li>
 *   <li>comment - 审批意见或备注（可选）</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
public class ToolApprovalActionRequest {

    private String userId;

    @NotBlank
    private String sessionId;

    @NotEmpty
    private List<String> approvalIds;

    private String title;

    private String comment;
}
