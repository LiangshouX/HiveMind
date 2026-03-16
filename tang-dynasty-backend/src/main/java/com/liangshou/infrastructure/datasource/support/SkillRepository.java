package com.liangshou.infrastructure.datasource.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.SkillMapper;
import com.liangshou.infrastructure.datasource.po.SkillPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Skill 数据访问层
 * 
 * 所有 Skill 相关的 CRUD 操作必须通过此类进行，禁止在 Service 层直接操作数据库
 */
@Repository
public class SkillRepository extends ServiceImpl<SkillMapper, SkillPO> {
    
    /**
     * 根据 ID 查询技能
     */
    public Optional<SkillPO> findById(Long id) {
        return Optional.ofNullable(getById(id));
    }
    
    /**
     * 根据名称查询技能
     */
    public Optional<SkillPO> findByName(String name) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SkillPO>()
                .eq(SkillPO::getName, name)));
    }
    
    /**
     * 查询所有启用的技能
     */
    public List<SkillPO> findAllEnabled() {
        return list(new LambdaQueryWrapper<SkillPO>()
                .eq(SkillPO::getEnabled, true));
    }
    
    /**
     * 查询所有技能
     */
    public List<SkillPO> findAll() {
        return list();
    }
    
    /**
     * 保存技能
     */
    public SkillPO saveSkill(SkillPO skill) {
        save(skill);
        return skill;
    }
    
    /**
     * 更新技能
     */
    public SkillPO updateSkill(SkillPO skill) {
        updateById(skill);
        return skill;
    }
    
    /**
     * 删除技能
     */
    public boolean deleteSkill(Long id) {
        return removeById(id);
    }
    
    /**
     * 启用技能
     */
    public void enableSkill(Long id) {
        SkillPO skill = getById(id);
        if (skill != null) {
            skill.setEnabled(true);
            updateById(skill);
        }
    }
    
    /**
     * 禁用技能
     */
    public void disableSkill(Long id) {
        SkillPO skill = getById(id);
        if (skill != null) {
            skill.setEnabled(false);
            updateById(skill);
        }
    }
}
