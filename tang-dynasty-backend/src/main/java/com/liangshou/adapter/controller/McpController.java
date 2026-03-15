package com.liangshou.adapter.controller;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.McpPO;
import com.liangshou.service.McpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {
    private final McpService mcpService;

    @GetMapping
    public Result<List<McpPO>> listMcps() {
        return Result.success(mcpService.list());
    }

    @PostMapping
    public Result<Boolean> saveMcp(@RequestBody McpPO mcp) {
        if (mcp.getId() == null) {
            mcp.setCreateTime(LocalDateTime.now());
        }
        mcp.setUpdateTime(LocalDateTime.now());
        return Result.success(mcpService.saveOrUpdate(mcp));
    }
    
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteMcp(@PathVariable Long id) {
        return Result.success(mcpService.removeById(id));
    }
}
