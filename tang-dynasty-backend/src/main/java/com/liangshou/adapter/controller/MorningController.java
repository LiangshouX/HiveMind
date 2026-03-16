package com.liangshou.adapter.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.ConfigPO;
import com.liangshou.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MorningController {

    private final ConfigService configService;

    // --- Morning Brief ---
    @GetMapping("/morning-brief")
    public Result<Map<String, Object>> getMorningBrief() {
        ConfigPO config = configService.getOne(new LambdaQueryWrapper<ConfigPO>().eq(ConfigPO::getKeyName, "morning_brief_data"));
        if (config != null) {
            return Result.success(JSON.parseObject(config.getValue()));
        }
        return Result.success(new HashMap<>());
    }
    
    @PostMapping("/morning-brief/refresh")
    public Result<Boolean> refreshMorningBrief() {
        // Logic to generate brief
        return Result.success(true);
    }

    // --- Morning Config ---
    @GetMapping("/morning-config")
    public Result<Map<String, Object>> getMorningConfig() {
        ConfigPO config = configService.getOne(new LambdaQueryWrapper<ConfigPO>().eq(ConfigPO::getKeyName, "morning_config"));
        if (config != null) {
            return Result.success(JSON.parseObject(config.getValue()));
        }
        return Result.success(new HashMap<>());
    }

    @PostMapping("/morning-config")
    public Result<Boolean> saveMorningConfig(@RequestBody Map<String, Object> payload) {
        String json = JSON.toJSONString(payload);
        ConfigPO config = configService.getOne(new LambdaQueryWrapper<ConfigPO>().eq(ConfigPO::getKeyName, "morning_config"));
        if (config == null) {
            config = new ConfigPO();
            config.setKeyName("morning_config");
            config.setCreateTime(LocalDateTime.now());
        }
        config.setValue(json);
        config.setUpdateTime(LocalDateTime.now());
        configService.saveOrUpdate(config);
        return Result.success(true);
    }
}
