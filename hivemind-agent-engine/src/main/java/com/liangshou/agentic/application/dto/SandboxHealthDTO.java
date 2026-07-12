package com.liangshou.agentic.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 沙箱健康摘要 DTO - 提供沙箱服务整体状态概览。
 *
 * <p>用于前端沙箱面板顶部的健康摘要展示。</p>
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxHealthDTO {

    /**
     * 沙箱功能是否启用
     */
    private boolean sandboxEnabled;

    /**
     * Docker 容器运行时是否可连接
     */
    private boolean dockerConnected;

    /**
     * 不可用时的错误信息（如 Docker 未启动）
     */
    private String errorMessage;

    /**
     * 沙箱容器总数
     */
    private int totalSandboxes;

    /**
     * 运行中的沙箱数
     */
    private int runningCount;

    /**
     * 已停止的沙箱数
     */
    private int stoppedCount;
}
