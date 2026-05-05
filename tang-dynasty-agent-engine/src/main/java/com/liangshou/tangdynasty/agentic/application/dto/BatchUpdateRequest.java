package com.liangshou.tangdynasty.agentic.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量更新 Profile 请求 DTO - 用于接收用户上传的多个 Profile 文件内容。
 *
 * @author LiangshouX
 */
@Data
public class BatchUpdateRequest {

    /**
     * Profile 更新请求列表
     */
    @Valid
    @NotEmpty(message = "profiles 列表不能为空")
    private List<ProfileUpdateRequest> profiles;
}
