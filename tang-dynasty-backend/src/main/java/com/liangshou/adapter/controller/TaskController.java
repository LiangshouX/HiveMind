package com.liangshou.adapter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.TaskPO;
import com.liangshou.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "旨意任务接口", description = "用于皇上下发旨意、任务查询与流转控制")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "分页获取任务列表")
    public Result<Page<TaskPO>> listTasks(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String state) {
        
        LambdaQueryWrapper<TaskPO> queryWrapper = new LambdaQueryWrapper<>();
        if (state != null && !state.isEmpty()) {
            queryWrapper.eq(TaskPO::getState, state);
        }
        queryWrapper.orderByDesc(TaskPO::getCreateTime);
        
        Page<TaskPO> page = taskService.page(new Page<>(current, size), queryWrapper);
        return Result.success(page);
    }

    @PostMapping
    @Operation(summary = "创建新任务(下旨)")
    public Result<Map<String, String>> createTask(@RequestBody Map<String, Object> payload) {
        TaskPO task = new TaskPO();
        String taskId = "JJC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 4);
        task.setId(taskId);
        task.setTitle((String) payload.getOrDefault("title", "New Task"));
        task.setState("Pending");
        task.setPriority("normal");
        
        taskService.save(task);
        
        Map<String, String> res = new HashMap<>();
        res.put("taskId", taskId);
        return Result.success(res);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    public Result<TaskPO> getTask(@PathVariable String id) {
        return Result.success(taskService.getById(id));
    }

    @PostMapping("/{id}/action")
    @Operation(summary = "任务操作(取消/重试/归档)")
    public Result<Boolean> taskAction(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        String action = (String) payload.get("action");
        
        TaskPO task = taskService.getById(id);
        if (task != null) {
            if ("cancel".equalsIgnoreCase(action)) {
                task.setState("Cancelled");
            } else if ("retry".equalsIgnoreCase(action)) {
                task.setState("Pending");
            } else if ("archive".equalsIgnoreCase(action)) {
                task.setArchived(true);
                task.setArchivedAt(LocalDateTime.now());
            }
            taskService.updateById(task);
            return Result.success(true);
        }
        return Result.error("Task not found");
    }
}
