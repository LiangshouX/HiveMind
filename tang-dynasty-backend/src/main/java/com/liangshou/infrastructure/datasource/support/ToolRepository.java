package com.liangshou.infrastructure.datasource.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.ToolMapper;
import com.liangshou.infrastructure.datasource.po.ToolPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tool 数据访问层
 * 
 * 所有 Tool 相关的 CRUD 操作必须通过此类进行，禁止在 Service 层直接操作数据库
 */
@Repository
public class ToolRepository extends ServiceImpl<ToolMapper, ToolPO> {
    
    /**
     * 根据 ID 查询工具
     */
    public Optional<ToolPO> findById(Long id) {
        return Optional.ofNullable(getById(id));
    }
    
    /**
     * 根据名称查询工具
     */
    public Optional<ToolPO> findByName(String name) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<ToolPO>()
                .eq(ToolPO::getName, name)));
    }
    
    /**
     * 查询所有启用的工具
     */
    public List<ToolPO> findAllEnabled() {
        return list(new LambdaQueryWrapper<ToolPO>()
                .eq(ToolPO::getEnabled, true));
    }
    
    /**
     * 查询所有工具
     */
    public List<ToolPO> findAll() {
        return list();
    }
    
    /**
     * 保存工具
     */
    public ToolPO saveTool(ToolPO tool) {
        save(tool);
        return tool;
    }
    
    /**
     * 更新工具
     */
    public ToolPO updateTool(ToolPO tool) {
        updateById(tool);
        return tool;
    }
    
    /**
     * 删除工具
     */
    public boolean deleteTool(Long id) {
        return removeById(id);
    }
    
    /**
     * 启用工具
     */
    public void enableTool(Long id) {
        ToolPO tool = getById(id);
        if (tool != null) {
            tool.setEnabled(true);
            updateById(tool);
        }
    }
    
    /**
     * 禁用工具
     */
    public void disableTool(Long id) {
        ToolPO tool = getById(id);
        if (tool != null) {
            tool.setEnabled(false);
            updateById(tool);
        }
    }
}
