package com.liangshou.adapter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.ToolPO;
import com.liangshou.service.ToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService toolService;

    @GetMapping
    public Result<List<ToolPO>> listTools() {
        return Result.success(toolService.list());
    }

    @PostMapping
    public Result<Boolean> saveTool(@RequestBody ToolPO tool) {
        if (tool.getId() == null) {
            tool.setCreateTime(LocalDateTime.now());
        }
        tool.setUpdateTime(LocalDateTime.now());
        return Result.success(toolService.saveOrUpdate(tool));
    }
    
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTool(@PathVariable Long id) {
        return Result.success(toolService.removeById(id));
    }
}
