package com.liangshou.agentic.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 记忆文件编辑请求 DTO。
 *
 * @author LiangshouX
 */
@Data
public class ReMeMemoryRequest {

    /**
     * 文件路径。
     */
    @NotBlank(message = "文件路径不能为空")
    private String path;

    /**
     * 原始文本（需要被替换的内容）。
     */
    @NotBlank(message = "原始文本不能为空")
    private String oldText;

    /**
     * 新文本（替换后的内容）。
     */
    @NotBlank(message = "新文本不能为空")
    private String newText;
}
