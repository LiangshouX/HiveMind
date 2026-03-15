package com.liangshou.adapter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.TaskLogPO;
import com.liangshou.infrastructure.datasource.po.TaskPO;
import com.liangshou.service.TaskLogService;
import com.liangshou.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskLogService taskLogService;

    @PostMapping
    public Result<Boolean> createTask(@RequestBody TaskPO task) {
        if (task.getCreateTime() == null) {
            task.setCreateTime(LocalDateTime.now());
        }
        return Result.success(taskService.save(task));
    }

    @GetMapping
    public Result<List<TaskPO>> listTasks() {
        return Result.success(taskService.list());
    }

    @GetMapping("/{taskId}")
    public Result<TaskPO> getTask(@PathVariable String taskId) {
        return Result.success(taskService.getOne(new LambdaQueryWrapper<TaskPO>().eq(TaskPO::getTaskId, taskId)));
    }

    @PutMapping("/{taskId}")
    public Result<Boolean> updateTask(@PathVariable String taskId, @RequestBody TaskPO task) {
        task.setTaskId(taskId);
        task.setUpdateTime(LocalDateTime.now());
        return Result.success(taskService.update(task, new LambdaQueryWrapper<TaskPO>().eq(TaskPO::getTaskId, taskId)));
    }

    @GetMapping("/{taskId}/logs")
    public Result<List<TaskLogPO>> getTaskLogs(@PathVariable String taskId) {
        return Result.success(taskLogService.list(new LambdaQueryWrapper<TaskLogPO>()
                .eq(TaskLogPO::getTaskId, taskId)
                .orderByAsc(TaskLogPO::getCreateTime)));
    }
}
