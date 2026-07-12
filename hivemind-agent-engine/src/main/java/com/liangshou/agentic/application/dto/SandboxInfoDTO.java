package com.liangshou.agentic.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 沙箱信息 DTO - 表示一个活跃沙箱实例的运行时信息。
 *
 * <p>用于沙箱管理面板展示，包含容器标识、运行状态、端口映射、
 * 网络端点以及该沙箱提供的工具列表。</p>
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxInfoDTO {

    /**
     * 容器 ID（短格式）
     */
    private String containerId;

    /**
     * 容器名称
     */
    private String containerName;

    /**
     * 推断的沙箱类型：base / browser / filesystem / gui / mobile / unknown
     */
    private String sandboxType;

    /**
     * 运行状态：running / stopped / created / unknown
     */
    private String status;

    /**
     * 端口映射列表
     */
    private String[] ports;

    /**
     * 沙箱 HTTP 端点
     */
    private String baseUrl;

    /**
     * 浏览器访问 URL（仅 browser 类型沙箱有值）
     */
    private String browserUrl;

    /**
     * 宿主机挂载目录
     */
    private String mountDir;

    /**
     * Docker 镜像名
     */
    private String version;

    /**
     * 当前活跃引用计数
     */
    private long refCount;

    /**
     * 此沙箱类型提供的工具名称列表
     */
    private List<String> providedTools;
}
