package com.liangshou.agentic.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 聊天请求对象，用于向 Agent 发送用户消息。
 * <p>
 * 包含以下字段：
 * <ul>
 *   <li>userId - 用户标识（必填），用于区分不同用户</li>
 *   <li>sessionId - 会话标识（必填），用于关联同一会话的多轮对话</li>
 *   <li>userName - 用户名（可选），用于显示和个性化</li>
 *   <li>title - 会话标题（可选），用于创建新会话时设置标题</li>
 *   <li>message - 用户消息内容（必填），支持普通文本和特殊命令（以 / 开头）</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
public class ChatRequest {

    private String userId;

    @NotBlank
    private String sessionId;

    private String userName;

    private String title;

    @NotBlank
    private String message;

    /**
     * 请求级别指定的模型 ID（可选）。
     * <p>如果提供，则使用该模型而非全局默认模型。</p>
     */
    private String modelId;

    /**
     * 请求级别指定的供应商 ID（可选）。
     * <p>如果提供，则使用该供应商的配置而非全局默认供应商。</p>
     */
    private String providerId;
}
