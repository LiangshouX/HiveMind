package com.liangshou.agentic.agents.memory.reme;

import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpSyncClientWrapper;
import io.agentscope.core.tool.mcp.McpTool;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReMe MCP 客户端 - 通过 MCP SSE 协议与 ReMe 服务通信。
 *
 * <p>封装 AgentScope-Java 的 McpSyncClientWrapper，提供类型安全的工具调用接口。
 * 支持自动初始化、错误降级和优雅关闭。</p>
 *
 * @author LiangshouX
 */
public class McpReMeClient {

    private static final Logger log = LoggerFactory.getLogger(McpReMeClient.class);

    private final String sseUrl;
    private final Duration timeout;
    private McpSyncClientWrapper clientWrapper;
    private boolean initialized = false;

    /**
     * 构造 MCP 客户端。
     *
     * @param sseUrl        MCP SSE 端点 URL
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
     * @return AgentTool 列表
     */
    public List<McpTool> toAgentTools() {
        List<McpSchema.Tool> tools = listTools();
        return tools.stream()
                .map(tool -> new McpTool(
                        tool.name(),
                        tool.description() != null ? tool.description() : "ReMe MCP tool: " + tool.name(),
                        tool.inputSchema() != null ? McpTool.convertMcpSchemaToParameters(tool.inputSchema(), null) : Map.of(),
                        clientWrapper
                ))
                .collect(Collectors.toList());
    }

    /**
     * 调用 ReMe MCP 工具。
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
     * 检查客户端是否已初始化。
     *
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取 SSE URL。
     *
     * @return SSE URL
     */
    public String getSseUrl() {
        return sseUrl;
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
