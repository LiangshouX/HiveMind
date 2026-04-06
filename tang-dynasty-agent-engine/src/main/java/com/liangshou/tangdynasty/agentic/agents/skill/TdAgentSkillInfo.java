package com.liangshou.tangdynasty.agentic.agents.skill;

import com.liangshou.tangdynasty.agentic.common.enums.TdAgentSkillSource;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Skill 详情视图对象，用于向客户端展示 Skill 的完整信息。
 * <p>
 * 包含以下字段：
 * <ul>
 *   <li>userId - 用户标识</li>
 *   <li>name - Skill 名称</li>
 *   <li>description - Skill 描述</li>
 *   <li>skillMarkdown - Skill 的 Markdown 定义内容</li>
 *   <li>resources - Skill 依赖的资源文件映射</li>
 *   <li>source - Skill 来源类型（BUILTIN 或 CUSTOMIZED）</li>
 *   <li>enabled - 是否启用</li>
 *   <li>overriddenBuiltin - 是否为覆盖内置 Skill 的自定义 Skill</li>
 *   <li>updatedAt - 最后更新时间</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Builder
public class TdAgentSkillInfo {

    private final String userId;

    private final String name;

    private final String description;

    private final String skillMarkdown;

    private final Map<String, String> resources;

    private final TdAgentSkillSource source;

    private final boolean enabled;

    private final boolean overriddenBuiltin;

    private final Instant updatedAt;
}
