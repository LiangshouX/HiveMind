package com.liangshou.tangdynasty.agentic.domain.shared.enums;

/**
 * 工具运行环境枚举 - 定义工具的隔离运行方式。
 *
 * <p>运行环境决定了工具执行时的资源访问权限：</p>
 * <ul>
 *     <li><b>SYSTEM</b> - 系统环境：工具直接运行在宿主系统，访问系统资源</li>
 *     <li><b>SANDBOX</b> - 沙盒环境：工具隔离运行，只能访问沙盒分配的资源</li>
 * </ul>
 *
 * @author LiangshouX
 */
public enum RunEnvironment {

    /**
     * 系统环境：工具运行在宿主系统，如内置工具获取会话 ID、时间等
     */
    SYSTEM,

    /**
     * 沙盒环境：工具隔离运行，如代码执行、文件操作等
     */
    SANDBOX
}
