package com.liangshou.tangdynasty.agentic.common.enums;

/**
 * ToolApprovalStatus，工具审批状态枚举
 *
 * @author LiangshouX
 */
public enum ToolApprovalStatus {

    /**
     * 等待审批
     */
    PENDING,

    /**
     * 审批通过
     */
    APPROVED,

    /**
     * 审批拒绝
     */
    REJECTED,

    /**
     * 执行完成
     */
    EXECUTED,

    /**
     * 审批过期
     */
    EXPIRED
}
