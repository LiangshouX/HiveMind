package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.agentsupport.tools.SkillExecutor;
import com.liangshou.infrastructure.datasource.mapper.SkillMapper;
import com.liangshou.infrastructure.datasource.po.SkillPO;
import com.liangshou.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Skill 服务实现类
 * 
 * 职责：业务逻辑编排 + 数据访问（使用 MyBatis Plus IService）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillServiceImpl extends ServiceImpl<SkillMapper, SkillPO> implements SkillService {
    
    private final SkillExecutor skillExecutor;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SkillPO entity) {
        log.info("Saving skill: {}", entity.getName());
        
        // 业务验证
        validateSkill(entity);
        
        // 使用 MyBatis Plus 的 save 方法
        boolean saved = super.save(entity);
        
        if (saved && entity.getEnabled()) {
            // 加载技能到执行器
            try {
                skillExecutor.loadSkill(entity);
            } catch (Exception e) {
                log.error("Failed to load skill after saving: {}", entity.getName(), e);
                throw new RuntimeException("Failed to load skill", e);
            }
        }
        
        return saved;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SkillPO entity) {
        log.info("Updating skill: {}", entity.getName());
        
        // 业务验证
        validateSkill(entity);
        
        // 使用 MyBatis Plus 的 updateById 方法
        boolean updated = super.updateById(entity);
        
        if (updated) {
            // 重新加载技能
            try {
                skillExecutor.reloadSkill(entity);
            } catch (Exception e) {
                log.error("Failed to reload skill after updating: {}", entity.getName(), e);
                throw new RuntimeException("Failed to reload skill", e);
            }
        }
        
        return updated;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Long id) {
        SkillPO skill = getById(id);
        if (skill != null) {
            log.info("Removing skill: {}", skill.getName());
            
            // 从执行器卸载技能
            skillExecutor.unloadSkill(skill.getName());
            
            // 使用 MyBatis Plus 的 removeById 方法
            return super.removeById(id);
        }
        return false;
    }
    
    /**
     * 执行技能（业务方法）
     */
    @Override
    public Object executeSkill(String skillName, Map<String, Object> context) throws Exception {
        log.info("Executing skill: {}", skillName);
        
        // 业务验证：检查技能是否存在且已启用
        SkillPO skill = getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillPO>()
                .eq(SkillPO::getName, skillName));
        
        if (skill == null) {
            throw new IllegalArgumentException("Skill not found: " + skillName);
        }
        
        if (!skill.getEnabled()) {
            throw new IllegalStateException("Skill is disabled: " + skillName);
        }
        
        return skillExecutor.executeSkill(skillName, context);
    }
    
    /**
     * 获取所有已加载的技能（查询方法）
     */
    @Override
    public List<String> getLoadedSkills() {
        return skillExecutor.getLoadedSkills();
    }
    
    /**
     * 启用技能（业务方法）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableSkill(Long id) {
        SkillPO skill = getById(id);
        if (skill == null) {
            throw new IllegalArgumentException("Skill not found: " + id);
        }
        
        // 业务规则：检查技能名称是否合法
        if (skill.getName() == null || skill.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name cannot be empty");
        }
        
        skill.setEnabled(true);
        updateById(skill);
        
        // 加载到执行器
        try {
            skillExecutor.loadSkill(skill);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load skill after enabling", e);
        }
        
        log.info("Skill enabled: {}", skill.getName());
    }
    
    /**
     * 禁用技能（业务方法）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableSkill(Long id) {
        SkillPO skill = getById(id);
        if (skill == null) {
            throw new IllegalArgumentException("Skill not found: " + id);
        }
        
        skill.setEnabled(false);
        updateById(skill);
        
        // 从执行器卸载
        skillExecutor.unloadSkill(skill.getName());
        
        log.info("Skill disabled: {}", skill.getName());
    }
    
    /**
     * 业务验证方法
     */
    private void validateSkill(SkillPO skill) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill cannot be null");
        }
        
        if (skill.getName() == null || skill.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name is required");
        }
        
        if (skill.getScript() == null || skill.getScript().trim().isEmpty()) {
            throw new IllegalArgumentException("Skill script is required");
        }
    }
}
