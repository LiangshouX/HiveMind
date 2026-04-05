package com.liangshou.tangdynasty.agentic.common.enums;

/**
 * 工具风险等级枚举 - 定义 Agent 工具调用的风险程度。
 *
 * <p>风险等级从低到高：</p>
 * <ul>
 *     <li><b>LOW</b> - 低风险：普通查询类工具，如获取时间、搜索记忆等</li>
 *     <li><b>MEDIUM</b> - 中风险：需要谨慎使用的工具</li>
 *     <li><b>HIGH</b> - 高风险：可能修改系统状态的工具，如文件写入、代码执行，需要人工审批</li>
 *     <li><b>CRITICAL</b> - 严重风险：危险操作，如删除文件、格式化磁盘、关机等，直接拒绝执行</li>
 * </ul>
 *
 * <p>风险等级由 {@link com.liangshou.tangdynasty.agentic.agents.guard.ToolGuardEngine} 评估生成，
 * 用于决定工具调用是否需要审批或直接拒绝。</p>
 *
 * @author LiangshouX
 */
public enum ToolRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
