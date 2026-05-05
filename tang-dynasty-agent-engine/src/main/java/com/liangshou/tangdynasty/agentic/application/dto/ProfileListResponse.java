package com.liangshou.tangdynasty.agentic.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Profile 列表响应 DTO - 用于返回用户的 Profile 文件列表。
 *
 * @author LiangshouX
 */
@Data
@Builder
public class ProfileListResponse {

    /**
     * 文件名（SOUL.md, AGENTS.md, PROFILE.md）
     */
    private String filename;

    /**
     * 文件内容（Markdown 格式）
     */
    private String content;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 来源（DEFAULT 或 USER_CUSTOMIZED）
     */
    private String source;

    /**
     * 文件大小（人类可读格式，如 "1.6 KB"）
     */
    private String size;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
