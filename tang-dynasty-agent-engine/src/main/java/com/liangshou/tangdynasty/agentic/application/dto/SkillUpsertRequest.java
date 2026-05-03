package com.liangshou.tangdynasty.agentic.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 自定义 Skill 保存请求对象，用于创建或更新用户的自定义 Skill。
 * <p>
 * 包含以下字段：
 * <ul>
 *   <li>userId - 用户标识（必填）</li>
 *   <li>skillMarkdown - Skill 的 Markdown 定义内容（必填），包含 SKILL.md 的完整内容</li>
 *   <li>resources - Skill 依赖的资源文件映射（可选），键为文件名，值为文件内容</li>
 *   <li>enabled - 是否启用该 Skill（可选），默认为系统配置的默认值</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
public class SkillUpsertRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String skillMarkdown;

    private Map<String, String> resources;

    private Boolean enabled;
}
