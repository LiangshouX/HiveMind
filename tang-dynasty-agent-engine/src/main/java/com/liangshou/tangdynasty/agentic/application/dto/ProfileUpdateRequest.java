package com.liangshou.tangdynasty.agentic.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Profile 更新请求 DTO - 用于接收用户上传的 Profile 文件内容。
 *
 * @author LiangshouX
 */
@Data
public class ProfileUpdateRequest {

    /**
     * 文件名（SOUL.md, AGENTS.md, PROFILE.md）
     */
    @NotBlank(message = "filename 不能为空")
    private String filename;

    /**
     * 文件内容（Markdown 格式）
     */
    @NotBlank(message = "content 不能为空")
    private String content;

    /**
     * 是否启用（可选，默认为 true）
     */
    private boolean enabled = true;
}
