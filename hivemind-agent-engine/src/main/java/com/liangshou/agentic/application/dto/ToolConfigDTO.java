package com.liangshou.agentic.application.dto;

import com.liangshou.agentic.domain.shared.enums.RunEnvironment;
import com.liangshou.agentic.domain.shared.enums.ToolCategory;
import com.liangshou.agentic.domain.shared.enums.ToolRiskLevel;
import com.liangshou.agentic.domain.tool.model.ToolExample;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 工具配置数据传输对象 - 用于在 Service 层和 Controller 层之间传输工具配置信息。
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolConfigDTO {

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具分类
     */
    private ToolCategory category;

    /**
     * 运行环境
     */
    private RunEnvironment runEnvironment;

    /**
     * 风险等级
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
    private List<String> denyPatterns;

    /**
     * 使用示例列表
     */
    private List<ToolExample> examples;

    /**
     * 是否用户自定义
     */
    private Boolean customized;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
