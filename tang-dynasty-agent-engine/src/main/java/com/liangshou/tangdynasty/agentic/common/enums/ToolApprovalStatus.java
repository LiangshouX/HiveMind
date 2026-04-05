package com.liangshou.tangdynasty.agentic.common.enums;

/**
 * 工具审批状态枚举 - 定义工具调用审批的生命周期状态。
 *
 * <p>状态流转：</p>
 * <pre>{@code
 * PENDING → APPROVED → EXECUTED
 *       ↘ REJECTED
 *       ↘ EXPIRED
 * }</pre>
 *
 * <ul>
 *     <li><b>PENDING</b> - 等待审批：工具调用已创建，等待用户确认</li>
 *     <li><b>APPROVED</b> - 审批通过：用户已批准，可以执行工具</li>
 *     <li><b>REJECTED</b> - 审批拒绝：用户拒绝了该工具调用</li>
 *     <li><b>EXECUTED</b> - 执行完成：工具已成功执行</li>
 *     <li><b>EXPIRED</b> - 审批过期：超过有效期未处理，自动失效</li>
 * </ul>
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
