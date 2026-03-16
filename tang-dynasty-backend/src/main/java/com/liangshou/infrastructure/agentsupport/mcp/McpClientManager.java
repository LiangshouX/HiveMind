package com.liangshou.infrastructure.agentsupport.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.infrastructure.datasource.po.McpPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP (Model Context Protocol) 客户端管理器
 * 
 * 负责管理多个 MCP Client 连接，支持动态添加/删除/启用/禁用 MCP 服务
 */
@Component
@Slf4j
public class McpClientManager {
    
    private final Map<String, McpClientInstance> activeClients = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    /**
     * 启动 MCP 客户端
     */
    public synchronized McpClientInstance startClient(McpPO mcpConfig) {
        String name = mcpConfig.getName();
        
        if (activeClients.containsKey(name)) {
            log.warn("MCP client {} already started, stopping first", name);
            stopClient(name);
        }
        
        try {
            McpClientInstance client = createClient(mcpConfig);
            activeClients.put(name, client);
            log.info("MCP client {} started successfully", name);
            return client;
        } catch (Exception e) {
            log.error("Failed to start MCP client {}: {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to start MCP client: " + e.getMessage(), e);
        }
    }
    
    /**
     * 停止 MCP 客户端
     */
    public synchronized void stopClient(String name) {
        McpClientInstance client = activeClients.remove(name);
        if (client != null) {
            try {
                client.close();
                log.info("MCP client {} stopped", name);
            } catch (IOException e) {
                log.error("Error stopping MCP client {}: {}", name, e.getMessage(), e);
            }
        }
    }
    
    /**
     * 获取活跃的 MCP 客户端
     */
    public McpClientInstance getClient(String name) {
        return activeClients.get(name);
    }
    
    /**
     * 获取所有活跃的 MCP 客户端
     */
    public List<String> getActiveClients() {
        return new ArrayList<>(activeClients.keySet());
    }
    
    /**
     * 重新加载 MCP 客户端配置
     */
    public synchronized void reloadClient(McpPO mcpConfig) {
        String name = mcpConfig.getName();
        if (activeClients.containsKey(name)) {
            stopClient(name);
        }
        if (mcpConfig.getEnabled()) {
            startClient(mcpConfig);
        }
    }
    
    /**
     * 创建 MCP 客户端实例
     */
    private McpClientInstance createClient(McpPO mcpConfig) throws Exception {
        Map<String, Object> config = mcpConfig.getConfig();
        
        // 根据配置类型创建不同的客户端
        String type = (String) config.getOrDefault("type", "stdio");
        
        return switch (type) {
            case "stdio" -> createStdioClient(mcpConfig, config);
            case "sse" -> createSseClient(mcpConfig, config);
            case "websocket" -> createWebSocketClient(mcpConfig, config);
            default -> throw new IllegalArgumentException("Unknown MCP client type: " + type);
        };
    }
    
    /**
     * 创建 stdio 类型的 MCP 客户端
     */
    private McpClientInstance createStdioClient(McpPO mcpPO, Map<String, Object> config) throws Exception {
        String command = (String) config.get("command");
        List<String> args = (List<String>) config.getOrDefault("args", List.of());
        Map<String, String> env = (Map<String, String>) config.getOrDefault("env", Map.of());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.command().addAll(args);
        processBuilder.environment().putAll(env);
        
        Process process = processBuilder.start();
        
        return new McpClientInstance(mcpPO.getName(), process, null);
    }
    
    /**
     * 创建 SSE 类型的 MCP 客户端
     */
    private McpClientInstance createSseClient(McpPO mcpPO, Map<String, Object> config) throws Exception {
        String url = (String) config.get("url");
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("SSE URL is required");
        }
        
        // 验证 SSE 端点
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("SSE endpoint returned status code: " + response.statusCode());
        }
        
        log.info("SSE MCP client validated for URL: {}", url);
        return new McpClientInstance(mcpPO.getName(), null, url);
    }
    
    /**
     * 创建 WebSocket 类型的 MCP 客户端
     */
    private McpClientInstance createWebSocketClient(McpPO mcpPO, Map<String, Object> config) throws Exception {
        String url = (String) config.get("url");
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("WebSocket URL is required");
        }
        
        // TODO: 实现 WebSocket 客户端连接
        log.info("WebSocket MCP client configured for URL: {}", url);
        return new McpClientInstance(mcpPO.getName(), null, url);
    }
    
    /**
     * 调用 MCP 工具
     */
    public Object callTool(String clientName, String toolName, Map<String, Object> arguments) throws Exception {
        McpClientInstance client = getClient(clientName);
        if (client == null) {
            throw new IllegalStateException("MCP client not found: " + clientName);
        }
        
        // 构建工具调用请求
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", System.currentTimeMillis(),
            "method", "tools/call",
            "params", Map.of(
                "name", toolName,
                "arguments", arguments
            )
        );
        
        // 如果是 SSE 客户端，发送 HTTP 请求
        if (client.getSseUrl() != null) {
            return callSseTool(client, request);
        }
        
        // 如果是 stdio 客户端，通过进程通信
        if (client.getProcess() != null) {
            return callStdioTool(client, request);
        }
        
        throw new IllegalStateException("Unsupported MCP client type");
    }
    
    /**
     * 通过 SSE 调用 MCP 工具
     */
    private Object callSseTool(McpClientInstance client, Map<String, Object> request) throws Exception {
        String jsonRequest = objectMapper.writeValueAsString(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(client.getSseUrl() + "/call"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("MCP tool call failed with status: " + response.statusCode());
        }
        
        JsonNode jsonResponse = objectMapper.readTree(response.body());
        return jsonResponse.get("result");
    }
    
    /**
     * 通过 stdio 调用 MCP 工具
     */
    private Object callStdioTool(McpClientInstance client, Map<String, Object> request) throws Exception {
        String jsonRequest = objectMapper.writeValueAsString(request);
        
        // 写入请求到进程输入流
        var outputStream = client.getProcess().getOutputStream();
        outputStream.write((jsonRequest + "\n").getBytes());
        outputStream.flush();
        
        // 从进程输出流读取响应
        var inputStream = client.getProcess().getInputStream();
        String jsonResponse = new String(inputStream.readAllBytes());
        
        JsonNode jsonResponseNode = objectMapper.readTree(jsonResponse);
        return jsonResponseNode.get("result");
    }
    
    /**
     * MCP 客户端实例封装
     */
    public static class McpClientInstance implements AutoCloseable {
        private final String name;
        private final Process process;
        private final String sseUrl;
        private boolean closed = false;
        
        public McpClientInstance(String name, Process process, String sseUrl) {
            this.name = name;
            this.process = process;
            this.sseUrl = sseUrl;
        }
        
        public String getName() {
            return name;
        }
        
        public Process getProcess() {
            return process;
        }
        
        public String getSseUrl() {
            return sseUrl;
        }
        
        @Override
        public void close() throws IOException {
            if (!closed) {
                if (process != null) {
                    process.destroyForcibly();
                }
                closed = true;
            }
        }
        
        public boolean isClosed() {
            return closed;
        }
    }
}
