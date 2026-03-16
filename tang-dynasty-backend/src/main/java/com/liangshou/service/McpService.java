package com.liangshou.service;

import com.liangshou.infrastructure.datasource.po.McpPO;

import java.util.List;
import java.util.Map;

/**
 * MCP服务接口
 * 
 * 纯业务逻辑接口，不包含任何数据库操作方法
 */
public interface McpService {
    
    /**
     * 保存 MCP
     */
    boolean save(McpPO entity);
    
    /**
     * 更新 MCP
     */
    boolean updateById(McpPO entity);
    
    /**
     * 根据 ID 删除 MCP
     */
    boolean removeById(Long id);
    
    /**
     * 调用 MCP 工具
     */
    Object callTool(String mcpName, String toolName, Map<String, Object> arguments) throws Exception;
    
    /**
     * 获取所有活跃的 MCP 客户端
     */
    List<String> getActiveClients();
    
    /**
     * 启用 MCP
     */
    void enableMcp(Long id);
    
    /**
     * 禁用 MCP
     */
    void disableMcp(Long id);
}
