package com.liangshou.infrastructure.datasource.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.TaskMapper;
import com.liangshou.infrastructure.datasource.po.TaskPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Task 数据访问层
 * 
 * 所有 Task 相关的 CRUD 操作必须通过此类进行
 */
@Repository
public class TaskRepository extends ServiceImpl<TaskMapper, TaskPO> {
    
    /**
     * 根据 ID 查询任务
     */
    public Optional<TaskPO> findById(Long id) {
        return Optional.ofNullable(getById(id));
    }
    
    /**
     * 根据任务 ID 查询
     */
    public Optional<TaskPO> findByTaskId(String taskId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<TaskPO>()
                .eq(TaskPO::getTaskId, taskId)));
    }
    
    /**
     * 根据状态查询任务
     */
    public List<TaskPO> findByStatus(String status) {
        return list(new LambdaQueryWrapper<TaskPO>()
                .eq(TaskPO::getStatus, status));
    }
    
    /**
     * 查询所有任务
     */
    public List<TaskPO> findAll() {
        return list();
    }
    
    /**
     * 保存任务
     */
    public TaskPO saveTask(TaskPO task) {
        save(task);
        return task;
    }
    
    /**
     * 更新任务
     */
    public TaskPO updateTask(TaskPO task) {
        updateById(task);
        return task;
    }
    
    /**
     * 删除任务
     */
    public boolean deleteTask(Long id) {
        return removeById(id);
    }
    
    /**
     * 更新任务状态
     */
    public void updateStatus(Long id, String status) {
        TaskPO task = getById(id);
        if (task != null) {
            task.setStatus(status);
            updateById(task);
        }
    }
}
