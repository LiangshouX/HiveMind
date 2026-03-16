package com.liangshou.adapter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.SkillPO;
import com.liangshou.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    // --- List Skills ---
    @GetMapping("/skills")
    public Result<List<SkillPO>> listSkills() {
        return Result.success(skillService.list());
    }

    // --- Remote Skills ---
    @GetMapping("/remote-skills")
    public Result<List<Map<String, Object>>> listRemoteSkills() {
        // Mock remote skills for now
        List<Map<String, Object>> skills = new ArrayList<>();
        // In a real app, fetch from a remote repo or DB
        return Result.success(skills);
    }
    
    // --- Add Remote Skill ---
    @PostMapping("/add-remote-skill")
    public Result<Boolean> addRemoteSkill(@RequestBody Map<String, String> payload) {
        String skillName = payload.get("skillName");
        String sourceUrl = payload.get("sourceUrl");
        String description = payload.get("description");
        
        SkillPO skill = new SkillPO();
        skill.setName(skillName);
        skill.setDescription(description + " (Source: " + sourceUrl + ")");
        skill.setScript("# Loaded from " + sourceUrl);
        skill.setEnabled(true);
        skill.setCreateTime(LocalDateTime.now());
        
        skillService.save(skill);
        return Result.success(true);
    }
    
    // --- Update/Remove Remote Skill ---
    @PostMapping("/update-remote-skill")
    public Result<Boolean> updateRemoteSkill(@RequestBody Map<String, String> payload) {
        // Stub
        return Result.success(true);
    }

    @PostMapping("/remove-remote-skill")
    public Result<Boolean> removeRemoteSkill(@RequestBody Map<String, String> payload) {
        String skillName = payload.get("skillName");
        skillService.remove(new LambdaQueryWrapper<SkillPO>().eq(SkillPO::getName, skillName));
        return Result.success(true);
    }

    // --- Skill Content ---
    @GetMapping("/skill-content/{agentId}/{skillName}")
    public Result<Map<String, String>> getSkillContent(@PathVariable String agentId, @PathVariable String skillName) {
        SkillPO skill = skillService.getOne(new LambdaQueryWrapper<SkillPO>().eq(SkillPO::getName, skillName));
        Map<String, String> res = new HashMap<>();
        if (skill != null) {
            res.put("script", skill.getScript());
        } else {
            res.put("script", "# Skill not found");
        }
        return Result.success(res);
    }
}
