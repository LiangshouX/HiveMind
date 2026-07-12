package com.liangshou.agentic.agents.memory.reme;

import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpSyncClientWrapper;
import io.agentscope.core.tool.mcp.McpTool;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReMe MCP 客户端 - 通过 MCP SSE 协议与 ReMe 服务通信。
 *
 * <p>封装 AgentScope-Java 的 McpSyncClientWrapper，提供类型安全的工具调用接口。
 * 支持自动初始化、错误降级、优雅关闭，以及基于用户 ID 的 workspace 隔离。</p>
 *
 * <p><b>用户隔离机制：</b></p>
 * <p>通过 {@link #callToolForUser} 方法，文件操作类工具（read、write、edit、list）
 * 的 path 参数会自动添加用户 ID 前缀，实现用户间的数据隔离。</p>
 * <p>例如：用户 "user-001" 调用 read(path="daily/2025-07-12.md")
 * 实际访问的是 "user-001/daily/2025-07-12.md"</p>
 *
 * @author LiangshouX
 */
public class McpReMeClient {

    private static final Logger log = LoggerFactory.getLogger(McpReMeClient.class);

    /** 需要添加用户路径前缀的文件操作工具 */
    private static final Set<String> FILE_PATH_TOOLS = Set.of(
            "read", "write", "edit", "list", "move", "delete",
            "read_image", "daily_write", "daily_list",
            "frontmatter_read", "frontmatter_update", "frontmatter_delete"
    );

    /**
     * -- GETTER --
     *  获取 SSE URL。
     *
     */
    @Getter
    private final String sseUrl;
    private final Duration timeout;
    private McpSyncClientWrapper clientWrapper;
    /**
     * -- GETTER --
     *  检查客户端是否已初始化。
     *
     */
    @Getter
    private boolean initialized = false;

    /**
     * 构造 MCP 客户端。
     *
     * @param sseUrl         MCP SSE 端点 URL
     * @param timeoutSeconds 超时时间（秒）
     */
    public McpReMeClient(String sseUrl, int timeoutSeconds) {
        this.sseUrl = sseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    /**
     * 初始化 MCP 连接。应在应用启动时调用。
     *
     * @return 是否初始化成功
     */
    public synchronized boolean initialize() {
        if (initialized) return true;
        try {
            clientWrapper = (McpSyncClientWrapper) McpClientBuilder.create("reme-memory")
                    .sseTransport(sseUrl)
                    .timeout(timeout)
                    .buildSync();
            clientWrapper.initialize().block();
            initialized = true;
            log.info("ReMe MCP client initialized: {}", sseUrl);
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize ReMe MCP client", e);
            initialized = false;
            return false;
        }
    }

    /**
     * 列出 ReMe 暴露的所有 MCP 工具。
     *
     * @return 工具列表
     */
    public List<McpSchema.Tool> listTools() {
        ensureInitialized();
        return clientWrapper.listTools().block();
    }

    /**
     * 将 ReMe MCP 工具转换为 AgentScope AgentTool 实例，可直接注册到 Toolkit。
     *
     * <p>每个 MCP Tool 会被包装为 {@link McpTool}，Agent 可以像使用普通工具一样调用它们。
     * 工具名称直接使用 ReMe MCP 服务器的原始名称（search、read、write、edit 等）。</p>
     *
     * @return AgentTool 列表（无用户隔离）
     */
    public List<McpTool> toAgentTools() {
        return toAgentTools(null);
    }

    /**
     * 将 ReMe MCP 工具转换为 AgentScope AgentTool 实例，带用户隔离。
     *
     * <p>当指定 userId 时，文件操作类工具会自动添加用户路径前缀，
     * 实现用户间的数据隔离。</p>
     *
     * @param userId 用户 ID，用于 workspace 隔离；为 null 时不隔离
     * @return AgentTool 列表
     */
    public List<McpTool> toAgentTools(String userId) {
        List<McpSchema.Tool> tools = listTools();
        return tools.stream()
                .map(tool -> {
                    McpTool mcpTool = new McpTool(
                            tool.name(),
                            tool.description() != null ? tool.description() : "ReMe MCP tool: " + tool.name(),
                            tool.inputSchema() != null ? McpTool.convertMcpSchemaToParameters(tool.inputSchema(), null) : Map.of(),
                            clientWrapper
                    );
                    // 如果指定了 userId，包装为用户隔离的工具
                    if (userId != null && !userId.isBlank()) {
                        return (McpTool) new UserScopedMcpTool(mcpTool, userId);
                    }
                    return mcpTool;
                })
                .collect(Collectors.toList());
    }

    /**
     * 调用 ReMe MCP 工具（无用户隔离）。
     *
     * @param toolName  工具名称 (search, auto_memory, read, write, edit, ...)
     * @param arguments 工具参数
     * @return 工具调用结果
     */
    public McpSchema.CallToolResult callTool(String toolName, Map<String, Object> arguments) {
        ensureInitialized();
        log.debug("Calling ReMe MCP tool: {} with args: {}", toolName, arguments);
        return clientWrapper.callTool(toolName, arguments).block();
    }

    /**
     * 为指定用户调用 ReMe MCP 工具（带用户隔离）。
     *
     * <p>对于文件操作类工具，自动将 path 参数添加用户 ID 前缀，
     * 实现用户间的数据隔离。</p>
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @param userId    用户 ID，用作 workspace 隔离前缀
     * @return 工具调用结果
     */
    public McpSchema.CallToolResult callToolForUser(String toolName, Map<String, Object> arguments, String userId) {
        ensureInitialized();
        Map<String, Object> userArgs = applyUserScope(toolName, arguments, userId);
        log.debug("Calling ReMe MCP tool for user {}: {} with args: {}", userId, toolName, userArgs);
        return clientWrapper.callTool(toolName, userArgs).block();
    }

    /**
     * 为指定用户调用 ReMe MCP 工具并提取文本结果。
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @param userId    用户 ID
     * @return 工具调用结果的文本内容
     */
    public String callToolTextForUser(String toolName, Map<String, Object> arguments, String userId) {
        McpSchema.CallToolResult result = callToolForUser(toolName, arguments, userId);
        return extractText(result);
    }

    /**
     * 便捷方法：调用工具并提取文本结果。
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @return 工具调用结果的文本内容
     */
    public String callToolText(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult result = callTool(toolName, arguments);
        return extractText(result);
    }

    /**
     * 对文件操作类工具的参数应用用户隔离。
     *
     * <p>如果工具是文件操作类（read、write、edit、list 等），
     * 且参数中包含 path，则自动添加 userId/ 前缀。</p>
     *
     * @param toolName  工具名称
     * @param arguments 原始参数
     * @param userId    用户 ID
     * @return 添加用户隔离后的参数
     */
    private Map<String, Object> applyUserScope(String toolName, Map<String, Object> arguments, String userId) {
        if (!FILE_PATH_TOOLS.contains(toolName) || userId == null || userId.isBlank()) {
            return arguments;
        }

        Map<String, Object> scoped = new HashMap<>(arguments);
        String path = (String) scoped.get("path");
        if (path != null && !path.isBlank()) {
            // 添加用户 ID 前缀，避免路径穿越
            String userPath = userId + "/" + path;
            scoped.put("path", userPath);
            log.debug("User scope applied: {} -> {}", path, userPath);
        }
        return scoped;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ReMe MCP client not initialized");
        }
    }

    private static String extractText(McpSchema.CallToolResult result) {
        if (result.content() == null) return "";
        for (McpSchema.Content block : result.content()) {
            if (block instanceof McpSchema.TextContent tc) {
                return tc.text();
            }
        }
        return result.content().toString();
    }

    @PreDestroy
    public void close() {
        if (clientWrapper != null) {
            try {
                clientWrapper.close();
            } catch (Exception e) {
                log.warn("Error closing ReMe MCP client", e);
            }
        }
    }
}
