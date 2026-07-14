package com.liangshou.adapter.controller;

import com.liangshou.service.ISysMcpService;
import com.liangshou.service.dto.SysMcpDTO;
import com.liangshou.service.vo.SysMcpVO;
import com.liangshou.agentic.common.utils.Result;
import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * SysMcp 控制器
 * <p>
 * 提供 SysMcp 相关的 RESTful API 接口
 */
@Tag(name = "SysMcp API", description = "SysMcp 管理相关接口")
@RestController
@RequestMapping("/api/sys-mcps")
@RequiredArgsConstructor
public class SysMcpController {

    private final ISysMcpService service;

    /**
     * 根据主键查询 SysMcp 详情
     *
     * @param id 主键ID
     * @return 包含 SysMcpVO 详情的 Result 对象
     */
    @Operation(summary = "查询详情", description = "根据主键获取单条记录")
    @GetMapping("/{id}")
    public Result<SysMcpVO> getById(@PathVariable Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.getById(userId, id));
    }

    @Operation(summary = "分页查询", description = "分页获取数据列表")
    @GetMapping
    public Result<PageResult<SysMcpVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.page(userId, current, size));
    }

    /**
     * 新增 SysMcp 记录
     *
     * @param dto 包含新增数据的 SysMcpDTO 对象
     * @return 包含保存结果(true/false)的 Result 对象
     */
    @Operation(summary = "新增记录", description = "创建一条新的数据记录")
    @PostMapping
    public Result<Boolean> save(@RequestBody SysMcpDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.save(userId, dto));
    }

    /**
     * 根据主键更新 SysMcp 记录
     *
     * @param id  主键ID
     * @param dto 包含更新数据的 SysMcpDTO 对象
     * @return 包含更新结果(true/false)的 Result 对象
     */
    @Operation(summary = "更新记录", description = "根据主键更新已有记录")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysMcpDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        dto.setId(id);
        return Result.success(service.update(userId, dto));
    }

    /**
     * 根据主键删除 SysMcp 记录
     *
     * @param id 主键ID
     * @return 包含删除结果(true/false)的 Result 对象
     */
    @Operation(summary = "删除记录", description = "根据主键逻辑删除一条记录")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.delete(userId, id));
    }
}
