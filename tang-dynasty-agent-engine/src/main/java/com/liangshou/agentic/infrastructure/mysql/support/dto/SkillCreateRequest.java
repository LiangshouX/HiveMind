package com.liangshou.agentic.infrastructure.mysql.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

/**
 * Skill 创建请求 DTO
 */
@Data
public class SkillCreateRequest {

    /** Skill 名称 */
    @NotBlank(message = "Skill 名称不能为空")
    private String name;

    /** Skill 描述 */
    private String description;

    /** SKILL.md 内容 */
    @NotBlank(message = "SKILL.md 内容不能为空")
    private String skillMarkdown;

    /** 资源文件映射 (相对路径 -> 内容) */
    private Map<String, String> resources;

    /** 标签列表 */
    private String[] tags;

    /** 运行环境配置 */
    private Map<String, String> executionEnv;

    /** 依赖配置 */
    private Map<String, Object> dependencies;

    /** 是否立即发布 */
    private boolean publish = false;

    /** 目标版本号（如果不为空，则创建指定版本） */
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "版本号格式必须为 X.Y.Z")
    private String version;
}
