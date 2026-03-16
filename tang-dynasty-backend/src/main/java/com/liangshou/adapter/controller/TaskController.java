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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskLogService taskLogService;

    // --- Create Task ---
    @PostMapping("/create-task")
    public Result<Map<String, String>> createTask(@RequestBody Map<String, Object> payload) {
        TaskPO task = new TaskPO();
        String taskId = "JJC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 4);
        task.setTaskId(taskId);
        task.setTitle((String) payload.getOrDefault("title", "New Task"));
        task.setDescription((String) payload.get("description"));
        task.setStatus("Pending");
        task.setPriority("normal");
        
        // Handle payload
        task.setPayload(payload);
        
        task.setCreateTime(LocalDateTime.now());
        taskService.save(task);
        
        Map<String, String> res = new HashMap<>();
        res.put("taskId", taskId);
        return Result.success(res);
    }

    // --- Task Action ---
    @PostMapping("/task-action")
    public Result<Boolean> taskAction(@RequestBody Map<String, String> payload) {
        String taskId = payload.get("taskId");
        String action = payload.get("action");
        String reason = payload.get("reason");

        TaskPO task = taskService.getOne(new LambdaQueryWrapper<TaskPO>().eq(TaskPO::getTaskId, taskId));
        if (task != null) {
            // Update status based on action
            if ("cancel".equalsIgnoreCase(action)) {
                task.setStatus("Cancelled");
            } else if ("retry".equalsIgnoreCase(action)) {
                task.setStatus("Pending"); // Retry usually resets to pending
            } else if ("complete".equalsIgnoreCase(action)) {
                task.setStatus("Done");
            }
            task.setUpdateTime(LocalDateTime.now());
            taskService.updateById(task);
            
            // Log action
            TaskLogPO log = new TaskLogPO();
            log.setTaskId(taskId);
            log.setType("action");
            Map<String, Object> content = new HashMap<>();
            content.put("action", action);
            content.put("reason", reason);
            log.setContent(content);
            log.setCreateTime(LocalDateTime.now());
            taskLogService.save(log);
            
            return Result.success(true);
        }
        return Result.error("Task not found");
    }

    // --- Task Activity ---
    @GetMapping("/task-activity/{taskId}")
    public Result<List<TaskLogPO>> getTaskActivity(@PathVariable String taskId) {
        return Result.success(taskLogService.list(new LambdaQueryWrapper<TaskLogPO>()
                .eq(TaskLogPO::getTaskId, taskId)
                .orderByDesc(TaskLogPO::getCreateTime)));
    }
    
    // --- Archive Task ---
    @PostMapping("/archive-task")
    public Result<Boolean> archiveTask(@RequestBody Map<String, Object> payload) {
        String taskId = (String) payload.get("taskId");
        Boolean archived = (Boolean) payload.get("archived");
        
        if (taskId != null) {
             TaskPO task = taskService.getOne(new LambdaQueryWrapper<TaskPO>().eq(TaskPO::getTaskId, taskId));
             if (task != null) {
                 task.setStatus("Archived");
                 taskService.updateById(task);
             }
        }
        return Result.success(true);
    }
    
    // --- Scheduler Stubs ---
    @GetMapping("/scheduler-state/{taskId}")
    public Result<Map<String, Object>> getSchedulerState(@PathVariable String taskId) {
        return Result.success(new HashMap<>());
    }
    
    @PostMapping("/scheduler-scan")
    public Result<Boolean> schedulerScan(@RequestBody Map<String, Object> payload) {
        return Result.success(true);
    }
    
    @PostMapping("/scheduler-retry")
    public Result<Boolean> schedulerRetry(@RequestBody Map<String, Object> payload) {
        return taskAction(Map.of("taskId", (String)payload.get("taskId"), "action", "retry", "reason", (String)payload.get("reason")));
    }
}
