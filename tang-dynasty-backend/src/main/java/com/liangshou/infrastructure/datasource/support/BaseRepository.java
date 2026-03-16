package com.liangshou.infrastructure.datasource.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

/**
 * 基础 Repository 接口
 * 
 * 所有数据访问对象必须继承此接口，确保 CRUD 操作集中在 infrastructure 层
 * 
 * @param <M> Mapper 类型
 * @param <T> 实体类型
 */
public interface BaseRepository<M extends BaseMapper<T>, T> extends IService<T> {
    
    /**
     * 根据 ID 查询实体
     */
    default Optional<T> findById(Long id) {
        return Optional.ofNullable(getById(id));
    }
    
    /**
     * 保存或更新实体
     */
    default T saveOrUpdate(T entity) {
        if (entity instanceof AuditableEntity auditable) {
            auditable.preUpdate();
        }
        boolean success = IService.super.saveOrUpdate(entity);
        return success ? entity : null;
    }
    
    /**
     * 根据条件查询列表
     */
    default List<T> list(LambdaQueryWrapper<T> wrapper) {
        return list(wrapper);
    }
    
    /**
     * 根据 ID 删除
     */
    default boolean deleteById(Long id) {
        return removeById(id);
    }
}
