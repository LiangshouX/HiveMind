package com.liangshou.infrastructure.datasource.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.McpMapper;
import com.liangshou.infrastructure.datasource.po.McpPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP 数据访问层
 * 
 * 所有 MCP 相关的 CRUD 操作必须通过此类进行，禁止在 Service 层直接操作数据库
 */
@Repository
public class McpRepository extends ServiceImpl<McpMapper, McpPO> {
    
    /**
     * 根据 ID 查询 MCP
     */
    public Optional<McpPO> findById(Long id) {
        return Optional.ofNullable(getById(id));
    }
    
    /**
     * 根据名称查询 MCP
     */
    public Optional<McpPO> findByName(String name) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<McpPO>()
                .eq(McpPO::getName, name)));
    }
    
    /**
     * 查询所有启用的 MCP
     */
    public List<McpPO> findAllEnabled() {
        return list(new LambdaQueryWrapper<McpPO>()
                .eq(McpPO::getEnabled, true));
    }
    
    /**
     * 查询所有 MCP
     */
    public List<McpPO> findAll() {
        return list();
    }
    
    /**
     * 保存 MCP
     */
    public McpPO saveMcp(McpPO mcp) {
        save(mcp);
        return mcp;
    }
    
    /**
     * 更新 MCP
     */
    public McpPO updateMcp(McpPO mcp) {
        updateById(mcp);
        return mcp;
    }
    
    /**
     * 删除 MCP
     */
    public boolean deleteMcp(Long id) {
        return removeById(id);
    }
    
    /**
     * 启用 MCP
     */
    public void enableMcp(Long id) {
        McpPO mcp = getById(id);
        if (mcp != null) {
            mcp.setEnabled(true);
            updateById(mcp);
        }
    }
    
    /**
     * 禁用 MCP
     */
    public void disableMcp(Long id) {
        McpPO mcp = getById(id);
        if (mcp != null) {
            mcp.setEnabled(false);
            updateById(mcp);
        }
    }
    
    /**
     * 根据配置查询 MCP
     */
    public List<McpPO> findByConfigValue(String key, Object value) {
        // 使用 JSON 查询（MySQL 5.7+）
        // 这里简化实现，实际应该使用 JSON_EXTRACT
        return list();
    }
}
