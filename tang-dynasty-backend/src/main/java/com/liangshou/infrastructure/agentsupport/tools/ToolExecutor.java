package com.liangshou.infrastructure.agentsupport.tools;

import com.liangshou.infrastructure.datasource.po.ToolPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 执行器
 * 
 * 负责注册和执行系统工具，支持动态添加自定义工具
 */
@Component
@Slf4j
public class ToolExecutor {
    
    private final Map<String, Object> toolInstances = new ConcurrentHashMap<>();
    private final Map<String, Method> toolMethods = new ConcurrentHashMap<>();
    
    /**
     * 注册工具实例
     */
    public void registerTool(String toolName, Object toolInstance) {
        toolInstances.put(toolName, toolInstance);
        
        // 扫描工具方法
        for (Method method : toolInstance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                String methodName = method.getAnnotation(Tool.class).name();
                if (methodName.isEmpty()) {
                    methodName = method.getName();
                }
                toolMethods.put(methodName, method);
                log.info("Registered tool method: {}", methodName);
            }
        }
        
        log.info("Tool {} registered successfully", toolName);
    }
    
    /**
     * 注销工具
     */
    public void unregisterTool(String toolName) {
        toolInstances.remove(toolName);
        
        // 移除相关方法
        toolMethods.entrySet().removeIf(entry -> 
            entry.getValue().getDeclaringClass().getSimpleName().equals(toolName)
        );
        
        log.info("Tool {} unregistered", toolName);
    }
    
    /**
     * 执行工具
     */
    public Object executeTool(String toolName, Map<String, Object> arguments) throws Exception {
        Object toolInstance = toolInstances.get(toolName);
        if (toolInstance == null) {
            throw new IllegalStateException("Tool not found: " + toolName);
        }
        
        Method method = toolMethods.get(toolName);
        if (method == null) {
            // 尝试查找同名的公共方法
            try {
                method = toolInstance.getClass().getMethod(toolName, Map.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Tool method not found: " + toolName);
            }
        }
        
        // 执行工具方法
        return method.invoke(toolInstance, arguments);
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolName) {
        return toolInstances.containsKey(toolName);
    }
    
    /**
     * 获取所有已注册的工具
     */
    public java.util.List<String> getRegisteredTools() {
        return new java.util.ArrayList<>(toolInstances.keySet());
    }
    
    /**
     * 从配置加载工具
     */
    public void loadToolFromConfig(ToolPO toolConfig) {
        String name = toolConfig.getName();
        
        // 根据工具类型创建相应的实例
        String type = toolConfig.getType();
        Object toolInstance = createToolInstance(type, toolConfig.getConfig());
        
        if (toolInstance != null) {
            registerTool(name, toolInstance);
        }
    }
    
    /**
     * 创建工具实例
     */
    private Object createToolInstance(String type, Map<String, Object> config) {
        return switch (type) {
            case "http" -> new HttpTool(config);
            case "shell" -> new ShellTool(config);
            case "database" -> new DatabaseTool(config);
            default -> {
                log.warn("Unknown tool type: {}, skipping", type);
                yield null;
            }
        };
    }
    
    /**
     * HTTP 工具实现
     */
    private static class HttpTool {
        private final Map<String, Object> config;
        
        public HttpTool(Map<String, Object> config) {
            this.config = config;
        }
        
        @Tool(name = "http_request")
        public String httpRequest(Map<String, Object> params) throws Exception {
            // TODO: 实现 HTTP 请求工具
            log.info("HTTP tool called with params: {}", params);
            return "{\"status\": \"success\"}";
        }
    }
    
    /**
     * Shell 命令工具实现
     */
    private static class ShellTool {
        private final Map<String, Object> config;
        
        public ShellTool(Map<String, Object> config) {
            this.config = config;
        }
        
        @Tool(name = "execute_shell")
        public String executeShell(Map<String, Object> params) throws Exception {
            // TODO: 实现 Shell 命令执行工具
            log.info("Shell tool called with params: {}", params);
            return "Command executed";
        }
    }
    
    /**
     * 数据库工具实现
     */
    private static class DatabaseTool {
        private final Map<String, Object> config;
        
        public DatabaseTool(Map<String, Object> config) {
            this.config = config;
        }
        
        @Tool(name = "execute_query")
        public String executeQuery(Map<String, Object> params) throws Exception {
            // TODO: 实现数据库查询工具
            log.info("Database tool called with params: {}", params);
            return "Query result";
        }
    }
}
