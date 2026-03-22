package com.liangshou.adapter.controller;

import com.liangshou.service.ISysModelsService;
import com.liangshou.service.dto.SysModelsDTO;
import com.liangshou.service.vo.SysModelsVO;
import com.liangshou.common.utils.Result;
import com.liangshou.common.utils.PageResult;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * SysModels 控制器
 *
 * 提供 SysModels 相关的 RESTful API 接口
 */
@Tag(name = "SysModels API", description = "SysModels 管理相关接口")
@RestController
@RequestMapping("/api/sys-modelss")
@RequiredArgsConstructor
public class SysModelsController {

    private final ISysModelsService service;

    /**
     * 根据主键查询 SysModels 详情
     *
     * @param id 主键ID
     * @return 包含 SysModelsVO 详情的 Result 对象
     */
    @Operation(summary = "查询详情", description = "根据主键获取单条记录")
    @GetMapping("/{id}")
    public Result<SysModelsVO> getById(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @Operation(summary = "分页查询", description = "分页获取数据列表")
    @GetMapping
    public Result<PageResult<SysModelsVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.page(current, size));
    }

    /**
     * 新增 SysModels 记录
     *
     * @param dto 包含新增数据的 SysModelsDTO 对象
     * @return 包含保存结果(true/false)的 Result 对象
     */
    @Operation(summary = "新增记录", description = "创建一条新的数据记录")
    @PostMapping
    public Result<Boolean> save(@RequestBody SysModelsDTO dto) {
        return Result.success(service.save(dto));
    }

    /**
     * 根据主键更新 SysModels 记录
     *
     * @param id  主键ID
     * @param dto 包含更新数据的 SysModelsDTO 对象
     * @return 包含更新结果(true/false)的 Result 对象
     */
    @Operation(summary = "更新记录", description = "根据主键更新已有记录")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysModelsDTO dto) {
        dto.setId(id);
        return Result.success(service.update(dto));
    }

    /**
     * 根据主键删除 SysModels 记录
     *
     * @param id 主键ID
     * @return 包含删除结果(true/false)的 Result 对象
     */
    @Operation(summary = "删除记录", description = "根据主键逻辑删除一条记录")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.delete(id));
    }
}
