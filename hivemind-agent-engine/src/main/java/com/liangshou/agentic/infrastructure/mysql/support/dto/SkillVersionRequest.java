package com.liangshou.agentic.infrastructure.mysql.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

/**
 * Skill 版本创建请求 DTO
 */
@Data
public class SkillVersionRequest {

    /** SKILL.md 内容 */
    @NotBlank(message = "SKILL.md 内容不能为空")
    private String skillMarkdown;

    /** 资源文件映射 */
    private Map<String, String> resources;

    /** 目标版本号 */
    @NotBlank(message = "版本号不能为空")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "版本号格式必须为 X.Y.Z")
    private String version;
}
