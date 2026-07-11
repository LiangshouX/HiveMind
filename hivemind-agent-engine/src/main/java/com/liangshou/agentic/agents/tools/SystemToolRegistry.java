package com.liangshou.agentic.agents.tools;

import com.liangshou.agentic.domain.shared.enums.RunEnvironment;
import com.liangshou.agentic.domain.shared.enums.ToolCategory;
import com.liangshou.agentic.domain.shared.enums.ToolRiskLevel;
import com.liangshou.agentic.domain.tool.model.ToolExample;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统工具注册表 - 统一管理所有系统默认工具的定义和元数据。
 *
 * <p>该组件负责：</p>
 * <ul>
 *     <li>注册所有系统工具，包括内置工具、沙盒工具、浏览器工具</li>
 *     <li>提供系统默认配置，供用户首次使用时同步到 MongoDB</li>
 *     <li>为 ToolGuardEngine 和 TdAgentToolkitFactory 提供工具元数据查询</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Component
@Slf4j
public class SystemToolRegistry {

    private final Map<String, SystemToolDefinition> tools = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // ========== 内置工具 ==========
        register(SystemToolDefinition.builder()
                .toolName("get_session_id")
                .description("获取当前会话 ID，用于会话级别的资源隔离")
                .category(ToolCategory.BUILTIN)
                .runEnvironment(RunEnvironment.SYSTEM)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("获取会话 ID", "{}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("get_history_preview")
                .description("获取历史对话预览，用于展示最近的对话内容")
                .category(ToolCategory.BUILTIN)
                .runEnvironment(RunEnvironment.SYSTEM)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("获取最近 12 条历史对话", "{}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("search_memory")
                .description("搜索长期记忆（ReMe），查找与当前任务相关的历史信息")
                .category(ToolCategory.BUILTIN)
                .runEnvironment(RunEnvironment.SYSTEM)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("搜索相关记忆", "{\"query\": \"之前的项目进度\"}")
                ))
                .build());

        // ========== 沙盒工具 - 代码执行 ==========
        register(SystemToolDefinition.builder()
                .toolName("run_shell_command")
                .description("执行 Shell 命令，用于运行脚本、安装依赖、系统管理等操作")
                .category(ToolCategory.SANDBOX)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.HIGH)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of("rm -rf", "del /f", "format ", "shutdown ", "reboot ", "remove-item -recurse -force"))
                .examples(List.of(
                        new ToolExample("安装 Python 包", "{\"command\": \"pip install requests\"}"),
                        new ToolExample("查看当前目录", "{\"command\": \"ls -la\"}"),
                        new ToolExample("检查 Python 版本", "{\"command\": \"python --version\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("run_ipython_cell")
                .description("执行 Python 代码，用于数据分析、可视化、算法实现等")
                .category(ToolCategory.SANDBOX)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.HIGH)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of("os.system(", "subprocess.", "__import__('os')", "__import__('subprocess')"))
                .examples(List.of(
                        new ToolExample("打印 Hello World", "{\"code\": \"print('Hello World')\"}"),
                        new ToolExample("计算 1+1", "{\"code\": \"1 + 1\"}")
                ))
                .build());

        // ========== 沙盒工具 - 文件系统 ==========
        register(SystemToolDefinition.builder()
                .toolName("fs_read_file")
                .description("读取文件内容，支持查看代码、文档、日志等文件")
                .category(ToolCategory.SANDBOX)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of("/etc/shadow", "/etc/passwd", "id_rsa"))
                .examples(List.of(
                        new ToolExample("读取 Python 文件", "{\"file_path\": \"main.py\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("fs_write_file")
                .description("写入文件内容，用于创建或修改代码、配置文件等")
                .category(ToolCategory.SANDBOX)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.HIGH)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of("/etc/shadow", "/etc/passwd", ".env", "/etc/nginx/"))
                .examples(List.of(
                        new ToolExample("创建 Python 脚本", "{\"file_path\": \"hello.py\", \"content\": \"print('Hello')\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("edit_file")
                .description("编辑文件（查找替换），用于精确修改文件中的部分内容")
                .category(ToolCategory.SANDBOX)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.HIGH)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("替换文件中的文本", "{\"file_path\": \"config.py\", \"old_str\": \"DEBUG = False\", \"new_str\": \"DEBUG = True\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("move_file")
                .description("移动或重命名文件")
                .category(ToolCategory.SANDBOX)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.MEDIUM)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("重命名文件", "{\"source\": \"old_name.txt\", \"destination\": \"new_name.txt\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("list_directory")
                .description("列出目录内容，查看文件和子目录")
                .category(ToolCategory.SANDBOX)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("查看当前目录", "{\"path\": \".\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("search_files")
                .description("搜索文件，按名称或模式查找")
                .category(ToolCategory.SANDBOX)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("查找所有 Python 文件", "{\"path\": \".\", \"pattern\": \"*.py\"}")
                ))
                .build());

        // ========== 浏览器工具 ==========
        register(SystemToolDefinition.builder()
                .toolName("browser_navigate")
                .description("导航到指定网页，用于访问网站")
                .category(ToolCategory.BROWSER)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.HIGH)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("打开百度", "{\"url\": \"https://www.baidu.com\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("browser_snapshot")
                .description("获取当前网页的文本快照，用于查看页面内容")
                .category(ToolCategory.BROWSER)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("获取页面文本", "{}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("browser_click")
                .description("点击网页元素，用于与页面交互")
                .category(ToolCategory.BROWSER)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.HIGH)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("点击按钮", "{\"index\": 1, \"ref\": \"点击搜索按钮\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("browser_type")
                .description("在网页输入框中输入文本，用于表单填写等")
                .category(ToolCategory.BROWSER)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.HIGH)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("输入搜索内容", "{\"index\": 2, \"ref\": \"搜索框\", \"text\": \"AgentScope\"}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("browser_wait_for")
                .description("等待页面元素加载完成")
                .category(ToolCategory.BROWSER)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("等待 3 秒", "{\"timeout\": 3000}")
                ))
                .build());

        register(SystemToolDefinition.builder()
                .toolName("browser_take_screenshot")
                .description("截取当前网页的截图，用于查看页面渲染效果")
                .category(ToolCategory.BROWSER)
                .runEnvironment(RunEnvironment.SANDBOX)
                .riskLevel(ToolRiskLevel.LOW)
                .enabled(true)
                .approvalRequired(false)
                .denyPatterns(List.of())
                .examples(List.of(
                        new ToolExample("截取网页", "{}")
                ))
                .build());

        log.info("[SystemToolRegistry] 已注册 {} 个系统工具", tools.size());
    }

    /**
     * 注册系统工具。
     *
     * @param definition 工具定义
     */
    private void register(SystemToolDefinition definition) {
        tools.put(definition.getToolName(), definition);
    }

    /**
     * 获取指定工具的定义。
     *
     * @param toolName 工具名称
     * @return 工具定义；若不存在返回 null
     */
    public SystemToolDefinition getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * 获取所有系统工具。
     *
     * @return 所有工具定义的集合
     */
    public Collection<SystemToolDefinition> getAllTools() {
        return tools.values();
    }

    /**
     * 检查工具是否已注册。
     *
     * @param toolName 工具名称
     * @return 是否已注册
     */
    public boolean isRegistered(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * 系统工具定义。
     */
    @Data
    @Builder
    public static class SystemToolDefinition {
        /** 工具名称 */
        private String toolName;
        /** 工具描述 */
        private String description;
        /** 工具分类 */
        private ToolCategory category;
        /** 运行环境 */
        private RunEnvironment runEnvironment;
        /** 风险等级 */
        private ToolRiskLevel riskLevel;
        /** 是否启用 */
        private Boolean enabled;
        /** 是否需要人工审批 */
        private Boolean approvalRequired;
        /** 拒绝执行的命令模式 */
        private List<String> denyPatterns;
        /** 使用示例 */
        private List<ToolExample> examples;
    }
}
