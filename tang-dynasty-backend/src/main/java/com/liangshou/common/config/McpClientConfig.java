package com.liangshou.common.config;

import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.transport.McpClientTransport;
import org.springframework.ai.mcp.client.transport.StdioClientTransport;
import org.springframework.ai.mcp.client.transport.SseClientTransport;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI Alibaba 官方 MCP Client 配置
 * 
 * 基于官方文档的标准实现
 */
@Configuration
@ConfigurationProperties(prefix = "spring.ai.mcp")
public class McpClientConfig {
    
    private List<McpConnection> connections = new java.util.ArrayList<>();
    
    /**
     * 创建 MCP Client Bean
     */
    @Bean
    public Map<String, McpClient> mcpClients() {
        Map<String, McpClient> clients = new HashMap<>();
        
        for (McpConnection connection : connections) {
            if (connection.isEnabled()) {
                try {
                    McpClient client = createClient(connection);
                    clients.put(connection.getName(), client);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create MCP client: " + connection.getName(), e);
                }
            }
        }
        
        return clients;
    }
    
    /**
     * 创建单个 MCP Client
     */
    private McpClient createClient(McpConnection connection) {
        McpClientTransport transport = switch (connection.getType()) {
            case STDIO -> createStdioTransport(connection.getStdio());
            case SSE -> createSseTransport(connection.getSse());
            default -> throw new IllegalArgumentException("Unknown MCP connection type: " + connection.getType());
        };
        
        return McpClient.builder()
                .transport(transport)
                .build();
    }
    
    /**
     * 创建 Stdio 传输
     */
    private McpClientTransport createStdioTransport(StdioConfig stdio) {
        ProcessBuilder processBuilder = new ProcessBuilder(stdio.getCommand());
        processBuilder.command().addAll(stdio.getArgs());
        processBuilder.environment().putAll(stdio.getEnv());
        
        return new StdioClientTransport(processBuilder);
    }
    
    /**
     * 创建 SSE 传输
     */
    private McpClientTransport createSseTransport(SseConfig sse) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(sse.getTimeout()));
        
        return new SseClientTransport(httpClient, sse.getUrl());
    }
    
    // Getters and Setters
    
    public List<McpConnection> getConnections() {
        return connections;
    }
    
    public void setConnections(List<McpConnection> connections) {
        this.connections = connections;
    }
    
    /**
     * MCP 连接配置
     */
    public static class McpConnection {
        private String name;
        private ConnectionType type = ConnectionType.STDIO;
        private boolean enabled = true;
        private StdioConfig stdio;
        private SseConfig sse;
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public ConnectionType getType() {
            return type;
        }
        
        public void setType(ConnectionType type) {
            this.type = type;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public StdioConfig getStdio() {
            return stdio;
        }
        
        public void setStdio(StdioConfig stdio) {
            this.stdio = stdio;
        }
        
        public SseConfig getSse() {
            return sse;
        }
        
        public void setSse(SseConfig sse) {
            this.sse = sse;
        }
    }
    
    /**
     * 连接类型枚举
     */
    public enum ConnectionType {
        STDIO,
        SSE
    }
    
    /**
     * Stdio 配置
     */
    public static class StdioConfig {
        private String command;
        private List<String> args = new java.util.ArrayList<>();
        private Map<String, String> env = new HashMap<>();
        
        // Getters and Setters
        public String getCommand() {
            return command;
        }
        
        public void setCommand(String command) {
            this.command = command;
        }
        
        public List<String> getArgs() {
            return args;
        }
        
        public void setArgs(List<String> args) {
            this.args = args;
        }
        
        public Map<String, String> getEnv() {
            return env;
        }
        
        public void setEnv(Map<String, String> env) {
            this.env = env;
        }
    }
    
    /**
     * SSE 配置
     */
    public static class SseConfig {
        private String url;
        private int timeout = 30;
        
        // Getters and Setters
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
}
