package com.liangshou.agentic.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 中断请求对象，用于向正在执行的流式会话发送中断信号。
 * <p>
 * 包含以下字段：
 * <ul>
 *   <li>userId - 用户标识（必填）</li>
 *   <li>sessionId - 会话标识（必填）</li>
 * </ul>
 * </p>
 * <p>
 * 当客户端需要停止正在进行的 Agent 任务时（如长时间运行的工具调用），
 * 可以发送此请求来中断执行。
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
public class InterruptRequest {

    private String userId;

    @NotBlank
    private String sessionId;
}
