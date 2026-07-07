package com.liangshou.agentic.infrastructure.mysql.support.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 版本信息 DTO
 */
@Data
@Builder
public class SkillVersionInfo {

    /** 版本号 */
    private String version;

    /** OSS 对象键 */
    private String objectKey;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 是否当前版本 */
    private boolean current;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 下载 URL（预签名） */
    private String downloadUrl;
}
