package com.liangshou.tangdynasty.agentic.domain.tool.model;

import com.liangshou.tangdynasty.agentic.domain.shared.enums.RunEnvironment;
import com.liangshou.tangdynasty.agentic.domain.shared.enums.ToolCategory;
import com.liangshou.tangdynasty.agentic.domain.shared.enums.ToolRiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * 工具配置文档 - MongoDB 中存储用户工具配置的持久化对象。
 *
 * <p>该文档记录每个用户对各个工具的配置信息，包括：</p>
 * <ul>
 *     <li>风险等级（LOW/MEDIUM/HIGH/CRITICAL）</li>
 *     <li>启用状态（enabled）</li>
 *     <li>是否需要人工审批（approvalRequired）</li>
 *     <li>拒绝执行的命令模式（denyPatterns）</li>
 *     <li>工具描述、分类、运行环境等元数据</li>
 * </ul>
 *
 * <p>索引策略：</p>
 * <ul>
 *     <li>复合唯一索引 (userId, toolName) 确保同一用户下工具名称唯一</li>
 *     <li>索引 (userId, enabled) 优化按用户查询和启用状态过滤</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tool_configs")
@CompoundIndex(name = "idx_tool_config_user_tool", def = "{'userId': 1, 'toolName': 1}", unique = true)
@CompoundIndex(name = "idx_tool_config_user_enabled", def = "{'userId': 1, 'enabled': 1}")
public class ToolConfigDocument {

    /**
     * 主键，UUID 随机生成
     */
    @Id
    private String id;

    /**
     * 用户唯一标识
     */
    @Field("user_id")
    private String userId;

    /**
     * 工具名称（在用户级别唯一）
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 风险等级
     */
    @Field("risk_level")
    private ToolRiskLevel riskLevel;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 工具用途描述
     */
    private String description;

    /**
     * 工具分类（builtin/sandbox/browser/custom）
     */
    private ToolCategory category;

    /**
     * 运行环境（system/sandbox）
     */
    @Field("run_environment")
    private RunEnvironment runEnvironment;

    /**
     * 使用示例列表
     */
    private List<ToolExample> examples;

    /**
     * 是否需要人工审批
     */
    @Field("approval_required")
    private Boolean approvalRequired;

    /**
     * 拒绝执行的命令模式列表
     */
    @Field("deny_patterns")
    private List<String> denyPatterns;

    /**
     * 是否用户自定义（false=系统同步的默认配置）
     */
    private Boolean customized;

    /**
     * 创建时间
     */
    @Field("created_at")
    private Instant createdAt;

    /**
     * 更新时间
     */
    @Field("updated_at")
    private Instant updatedAt;
}
