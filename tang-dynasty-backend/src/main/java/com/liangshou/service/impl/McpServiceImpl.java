package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.agentsupport.mcp.McpClientManager;
import com.liangshou.infrastructure.datasource.mapper.McpMapper;
import com.liangshou.infrastructure.datasource.po.McpPO;
import com.liangshou.service.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * MCP服务实现类
 * 
 * 职责：业务逻辑编排 + 数据访问（使用 MyBatis Plus IService）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpServiceImpl extends ServiceImpl<McpMapper, McpPO> implements McpService {
    
    private final McpClientManager mcpClientManager;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(McpPO entity) {
        log.info("Saving MCP: {}", entity.getName());
        
        // 业务验证
        validateMcp(entity);
        
        // 使用 MyBatis Plus 的 save 方法
        boolean saved = super.save(entity);
        
        if (saved && entity.getEnabled()) {
            // 启动 MCP 客户端
            try {
                mcpClientManager.startClient(entity);
            } catch (Exception e) {
                log.error("Failed to start MCP client after saving: {}", entity.getName(), e);
                throw new RuntimeException("Failed to start MCP client", e);
            }
        }
        
        return saved;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(McpPO entity) {
        log.info("Updating MCP: {}", entity.getName());
        
        // 业务验证
        validateMcp(entity);
        
        // 使用 MyBatis Plus 的 updateById 方法
        boolean updated = super.updateById(entity);
        
        if (updated) {
            // 重新加载 MCP 客户端配置
            try {
                mcpClientManager.reloadClient(entity);
            } catch (Exception e) {
                log.error("Failed to reload MCP client after updating: {}", entity.getName(), e);
                throw new RuntimeException("Failed to reload MCP client", e);
            }
        }
        
        return updated;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Long id) {
        McpPO mcp = getById(id);
        if (mcp != null) {
            log.info("Removing MCP: {}", mcp.getName());
            
            // 停止 MCP 客户端
            mcpClientManager.stopClient(mcp.getName());
            
            // 使用 MyBatis Plus 的 removeById 方法
            return super.removeById(id);
        }
        return false;
    }
    
    /**
     * 调用 MCP 工具（业务方法）
     */
    @Override
    public Object callTool(String mcpName, String toolName, Map<String, Object> arguments) throws Exception {
        log.info("Calling MCP tool: {}.{}", mcpName, toolName);
        
        // 业务验证：检查 MCP 是否存在且已启用
        McpPO mcp = getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<McpPO>()
                .eq(McpPO::getName, mcpName));
        
        if (mcp == null) {
            throw new IllegalArgumentException("MCP not found: " + mcpName);
        }
        
        if (!mcp.getEnabled()) {
            throw new IllegalStateException("MCP is disabled: " + mcpName);
        }
        
        return mcpClientManager.callTool(mcpName, toolName, arguments);
    }
    
    /**
     * 获取所有活跃的 MCP 客户端（查询方法）
     */
    @Override
    public List<String> getActiveClients() {
        return mcpClientManager.getActiveClients();
    }
    
    /**
     * 启用 MCP（业务方法）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableMcp(Long id) {
        McpPO mcp = getById(id);
        if (mcp == null) {
            throw new IllegalArgumentException("MCP not found: " + id);
        }
        
        // 业务规则：检查配置是否完整
        if (mcp.getConfig() == null || mcp.getConfig().isEmpty()) {
            throw new IllegalArgumentException("MCP configuration is required");
        }
        
        mcp.setEnabled(true);
        updateById(mcp);
        
        // 启动客户端
        try {
            mcpClientManager.startClient(mcp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start MCP client after enabling", e);
        }
        
        log.info("MCP enabled: {}", mcp.getName());
    }
    
    /**
     * 禁用 MCP（业务方法）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableMcp(Long id) {
        McpPO mcp = getById(id);
        if (mcp == null) {
            throw new IllegalArgumentException("MCP not found: " + id);
        }
        
        mcp.setEnabled(false);
        updateById(mcp);
        
        // 停止客户端
        mcpClientManager.stopClient(mcp.getName());
        
        log.info("MCP disabled: {}", mcp.getName());
    }
    
    /**
     * 业务验证方法
     */
    private void validateMcp(McpPO mcp) {
        if (mcp == null) {
            throw new IllegalArgumentException("MCP cannot be null");
        }
        
        if (mcp.getName() == null || mcp.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("MCP name is required");
        }
        
        if (mcp.getConfig() == null) {
            throw new IllegalArgumentException("MCP configuration is required");
        }
    }
}
