package com.liangshou.common.config;

import org.springframework.ai.model.client.ModelClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP 客户端配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "spring.ai.mcp")
public class McpClientProperties {
    
    /**
     * MCP 客户端配置列表
     */
    private List<ClientConfig> clients;
    
    public List<ClientConfig> getClients() {
        return clients;
    }
    
    public void setClients(List<ClientConfig> clients) {
        this.clients = clients;
    }
    
    /**
     * 单个 MCP 客户端配置
     */
    public static class ClientConfig {
        /**
         * 客户端名称
         */
        private String name;
        
        /**
         * 客户端类型：stdio, sse, websocket
         */
        private String type = "stdio";
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * stdio 类型配置
         */
        private StdioConfig stdio;
        
        /**
         * SSE 类型配置
         */
        private SseConfig sse;
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
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
     * stdio 类型 MCP 配置
     */
    public static class StdioConfig {
        private String command;
        private List<String> args;
        
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
    }
    
    /**
     * SSE 类型 MCP 配置
     */
    public static class SseConfig {
        private String url;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
}
