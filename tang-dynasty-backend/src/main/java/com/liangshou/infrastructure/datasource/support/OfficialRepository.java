package com.liangshou.infrastructure.datasource.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.OfficialMapper;
import com.liangshou.infrastructure.datasource.po.OfficialPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Official 数据访问层
 * 
 * 所有 Official 相关的 CRUD 操作必须通过此类进行
 */
@Repository
public class OfficialRepository extends ServiceImpl<OfficialMapper, OfficialPO> {
    
    /**
     * 根据 ID 查询官员
     */
    public Optional<OfficialPO> findById(Long id) {
        return Optional.ofNullable(getById(id));
    }
    
    /**
     * 根据名称查询官员
     */
    public Optional<OfficialPO> findByName(String name) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<OfficialPO>()
                .eq(OfficialPO::getName, name)));
    }
    
    /**
     * 根据部门查询官员
     */
    public List<OfficialPO> findByDepartment(String department) {
        return list(new LambdaQueryWrapper<OfficialPO>()
                .eq(OfficialPO::getDepartment, department));
    }
    
    /**
     * 查询所有官员
     */
    public List<OfficialPO> findAll() {
        return list();
    }
    
    /**
     * 保存官员
     */
    public OfficialPO saveOfficial(OfficialPO official) {
        save(official);
        return official;
    }
    
    /**
     * 更新官员
     */
    public OfficialPO updateOfficial(OfficialPO official) {
        updateById(official);
        return official;
    }
    
    /**
     * 删除官员
     */
    public boolean deleteOfficial(Long id) {
        return removeById(id);
    }
}
