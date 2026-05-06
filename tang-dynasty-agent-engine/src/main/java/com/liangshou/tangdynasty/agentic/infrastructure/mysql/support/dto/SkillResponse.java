package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto;

import com.liangshou.tangdynasty.agentic.common.enums.TdAgentSkillSource;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Skill 响应 DTO
 */
@Data
@Builder
public class SkillResponse {

    private String skillId;
    private String userId;
    private String name;
    private String description;
    private String currentVersion;
    private String status;
    private List<String> tags;
    private Map<String, Object> dependencies;
    private Map<String, String> executionEnv;
    private Map<String, Object> fileManifest;
    /** 技能来源：BUILTIN(系统内置) 或 CUSTOMIZED(用户自定义) */
    private TdAgentSkillSource source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 当前版本的下载 URL（如果有） */
    private String downloadUrl;
}
