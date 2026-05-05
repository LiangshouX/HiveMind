package com.liangshou.tangdynasty.agentic.domain.tool.model;

import com.liangshou.tangdynasty.agentic.domain.shared.enums.ToolRiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具配置更新命令 - 用于接收前端传来的工具配置修改请求。
 *
 * <p>所有字段均为可选，仅更新非 null 的字段。</p>
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolConfigUpdateCommand {

    /**
     * 风险等级（LOW/MEDIUM/HIGH/CRITICAL）
     */
    private ToolRiskLevel riskLevel;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否需要人工审批
     */
    private Boolean approvalRequired;

    /**
     * 拒绝执行的命令模式列表
     */
    private java.util.List<String> denyPatterns;
}
