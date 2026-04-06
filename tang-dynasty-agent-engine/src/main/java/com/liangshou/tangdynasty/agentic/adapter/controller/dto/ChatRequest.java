package com.liangshou.tangdynasty.agentic.adapter.controller.dto;

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

    @NotBlank
    private String userId;

    @NotBlank
    private String sessionId;

    private String userName;

    private String title;

    @NotBlank
    private String message;
}
