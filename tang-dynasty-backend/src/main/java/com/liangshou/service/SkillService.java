package com.liangshou.service;

import com.liangshou.infrastructure.datasource.po.SkillPO;

import java.util.List;
import java.util.Map;

/**
 * Skill 服务接口
 * 
 * 纯业务逻辑接口，不包含任何数据库操作方法
 */
public interface SkillService {
    
    /**
     * 保存技能
     */
    boolean save(SkillPO entity);
    
    /**
     * 更新技能
     */
    boolean updateById(SkillPO entity);
    
    /**
     * 根据 ID 删除技能
     */
    boolean removeById(Long id);
    
    /**
     * 执行技能
     */
    Object executeSkill(String skillName, Map<String, Object> context) throws Exception;
    
    /**
     * 获取所有已加载的技能
     */
    List<String> getLoadedSkills();
    
    /**
     * 启用技能
     */
    void enableSkill(Long id);
    
    /**
     * 禁用技能
     */
    void disableSkill(Long id);
}
