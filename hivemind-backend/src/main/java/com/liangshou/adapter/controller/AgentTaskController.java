package com.liangshou.adapter.controller;

import com.liangshou.service.IAgentTaskService;
import com.liangshou.service.dto.AgentTaskDTO;
import com.liangshou.service.vo.AgentTaskVO;
import com.liangshou.common.utils.Result;
import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * AgentTask 控制器
 *
 * 提供 AgentTask 相关的 RESTful API 接口
 */
@Tag(name = "AgentTask API", description = "AgentTask 管理相关接口")
@RestController
@RequestMapping("/api/agent-tasks")
@RequiredArgsConstructor
public class AgentTaskController {

    private final IAgentTaskService service;

    /**
     * 根据主键查询 AgentTask 详情
     *
     * @return 包含 AgentTaskVO 详情的 Result 对象
     */
    @Operation(summary = "查询详情", description = "根据主键获取单条记录")
    @GetMapping("/{taskId}")
    public Result<AgentTaskVO> getById(@PathVariable String taskId) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.getById(userId, taskId));
    }

    @Operation(summary = "分页查询", description = "分页获取数据列表")
    @GetMapping
    public Result<PageResult<AgentTaskVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.page(userId, current, size));
    }

    /**
     * 新增 AgentTask 记录
     *
     * @param dto 包含新增数据的 AgentTaskDTO 对象
     * @return 包含保存结果(true/false)的 Result 对象
     */
    @Operation(summary = "新增记录", description = "创建一条新的数据记录")
    @PostMapping
    public Result<Boolean> save(@RequestBody AgentTaskDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.save(userId, dto));
    }

    /**
     * 根据主键更新 AgentTask 记录
     *
     * @param dto 包含更新数据的 AgentTaskDTO 对象
     * @return 包含更新结果(true/false)的 Result 对象
     */
    @Operation(summary = "更新记录", description = "根据主键更新已有记录")
    @PutMapping("/{taskId}")
    public Result<Boolean> update(@PathVariable String taskId, @RequestBody AgentTaskDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        dto.setTaskId(taskId);
        return Result.success(service.update(userId, dto));
    }

    /**
     * 根据主键删除 AgentTask 记录
     *
     * @return 包含删除结果(true/false)的 Result 对象
     */
    @Operation(summary = "删除记录", description = "根据主键逻辑删除一条记录")
    @DeleteMapping("/{taskId}")
    public Result<Boolean> delete(@PathVariable String taskId) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.delete(userId, taskId));
    }
}
