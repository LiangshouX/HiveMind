package com.liangshou.adapter.controller;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.ModelPO;
import com.liangshou.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {
    private final ModelService modelService;

    @GetMapping
    public Result<List<ModelPO>> listModels() {
        return Result.success(modelService.list());
    }

    @PostMapping
    public Result<Boolean> saveModel(@RequestBody ModelPO model) {
        if (model.getId() == null) {
            model.setCreateTime(LocalDateTime.now());
        }
        model.setUpdateTime(LocalDateTime.now());
        return Result.success(modelService.saveOrUpdate(model));
    }
    
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteModel(@PathVariable Long id) {
        return Result.success(modelService.removeById(id));
    }
}
