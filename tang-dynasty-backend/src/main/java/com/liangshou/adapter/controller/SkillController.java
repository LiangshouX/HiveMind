package com.liangshou.adapter.controller;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.SkillPO;
import com.liangshou.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;

    @GetMapping
    public Result<List<SkillPO>> listSkills() {
        return Result.success(skillService.list());
    }

    @PostMapping
    public Result<Boolean> saveSkill(@RequestBody SkillPO skill) {
        if (skill.getId() == null) {
            skill.setCreateTime(LocalDateTime.now());
        }
        skill.setUpdateTime(LocalDateTime.now());
        return Result.success(skillService.saveOrUpdate(skill));
    }
    
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSkill(@PathVariable Long id) {
        return Result.success(skillService.removeById(id));
    }
}
