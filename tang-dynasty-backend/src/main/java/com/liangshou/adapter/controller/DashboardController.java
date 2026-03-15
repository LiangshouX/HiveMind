package com.liangshou.adapter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.TaskLogPO;
import com.liangshou.service.TaskLogService;
import com.liangshou.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final TaskService taskService;
    private final TaskLogService taskLogService;

    @GetMapping("/live-status")
    public Result<Map<String, Object>> liveStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("tasks", taskService.list());
        data.put("syncStatus", Map.of("ok", true));
        return Result.success(data);
    }

    @GetMapping("/task-activity/{taskId}")
    public Result<List<TaskLogPO>> getTaskActivity(@PathVariable String taskId) {
        return Result.success(taskLogService.list(new LambdaQueryWrapper<TaskLogPO>()
                .eq(TaskLogPO::getTaskId, taskId)
                .orderByAsc(TaskLogPO::getCreateTime)));
    }
}
