package com.liangshou.agentic.domain.shared.enums;

/**
 * 工具分类枚举 - 定义工具的类型类别。
 *
 * <p>分类用于前端展示分组和工具来源标识：</p>
 * <ul>
 *     <li><b>BUILTIN</b> - 内置工具：会话管理、历史查询等基础工具</li>
 *     <li><b>SANDBOX</b> - 沙盒工具：代码执行、文件系统操作等</li>
 *     <li><b>BROWSER</b> - 浏览器工具：网页导航、截图、交互等</li>
 *     <li><b>CUSTOM</b> - 自定义工具：用户上传或自定义的工具</li>
 * </ul>
 *
 * @author LiangshouX
 */
public enum ToolCategory {

    /**
     * 内置工具：会话管理、历史查询、记忆搜索等基础工具
     */
    BUILTIN,

    /**
     * 沙盒工具：Python/Shell 代码执行、文件读写等
     */
    SANDBOX,

    /**
     * 浏览器工具：网页导航、截图、点击、输入等
     */
    BROWSER,

    /**
     * 自定义工具：用户自定义或上传的扩展工具
     */
    CUSTOM
}
