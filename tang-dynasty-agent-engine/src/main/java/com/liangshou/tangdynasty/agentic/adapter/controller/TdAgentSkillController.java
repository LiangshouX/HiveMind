package com.liangshou.tangdynasty.agentic.adapter.controller;

import com.liangshou.tangdynasty.agentic.service.dto.SkillUpsertRequest;
import com.liangshou.tangdynasty.agentic.agents.skill.TdAgentSkillInfo;
import com.liangshou.tangdynasty.agentic.agents.skill.TdAgentSkillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Skill 管理 REST API 控制器，提供完整的 Skill CRUD 操作接口。
 * <p>
 * 提供的 API 端点包括：
 * <ul>
 *   <li>GET /api/v1/tdagent/skills/users/{userId} - 列出用户的所有 Skills</li>
 *   <li>GET /api/v1/tdagent/skills/users/{userId}/{skillName} - 获取 Skill 详情</li>
 *   <li>POST /api/v1/tdagent/skills/custom - 创建或更新自定义 Skill</li>
 *   <li>POST /api/v1/tdagent/skills/users/{userId}/{skillName}/enable - 启用 Skill</li>
 *   <li>POST /api/v1/tdagent/skills/users/{userId}/{skillName}/disable - 停用 Skill</li>
 *   <li>DELETE /api/v1/tdagent/skills/custom/{userId}/{skillName} - 删除自定义 Skill</li>
 *   <li>POST /api/v1/tdagent/skills/catalog/reload - 重新加载内置 Skills 目录</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/v1/tdagent/skills")
public class TdAgentSkillController {

    private final TdAgentSkillService skillService;

    /**
     * 执行相关操作。
     * @param skillService skill 服务
     */
    public TdAgentSkillController(TdAgentSkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * 列出用户 Skill。
     * @param userId 用户标识
     * @return 返回结果
     */
    @GetMapping("/users/{userId}")
    public List<TdAgentSkillInfo> listSkills(@PathVariable String userId) {
        return skillService.listSkills(userId);
    }

    /**
     * 获取 Skill 详情。
     * @param userId 用户标识
     * @param skillName skill 名称
     * @return 返回结果
     */
    @GetMapping("/users/{userId}/{skillName}")
    public TdAgentSkillInfo getSkill(@PathVariable String userId, @PathVariable String skillName) {
        return skillService.getSkill(userId, skillName);
    }

    /**
     * 保存自定义 Skill。
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping("/custom")
    public TdAgentSkillInfo saveCustomSkill(@Valid @RequestBody SkillUpsertRequest request) {
        return skillService.saveCustomSkill(
                request.getUserId(),
                request.getSkillMarkdown(),
                request.getResources(),
                request.getEnabled());
    }

    /**
     * 启用 Skill。
     * @param userId 用户标识
     * @param skillName skill 名称
     * @return 返回结果
     */
    @PostMapping("/users/{userId}/{skillName}/enable")
    public TdAgentSkillInfo enableSkill(
            @PathVariable String userId, @PathVariable String skillName) {
        return skillService.setSkillEnabled(userId, skillName, true);
    }

    /**
     * 停用 Skill。
     * @param userId 用户标识
     * @param skillName skill 名称
     * @return 返回结果
     */
    @PostMapping("/users/{userId}/{skillName}/disable")
    public TdAgentSkillInfo disableSkill(
            @PathVariable String userId, @PathVariable String skillName) {
        return skillService.setSkillEnabled(userId, skillName, false);
    }

    /**
     * 删除自定义 Skill。
     * @param userId 用户标识
     * @param skillName skill 名称
     * @return 返回结果
     */
    @DeleteMapping("/custom/{userId}/{skillName}")
    public Map<String, Object> deleteCustomSkill(
            @PathVariable String userId, @PathVariable String skillName) {
        skillService.deleteCustomSkill(userId, skillName);
        return Map.of("success", true, "userId", userId, "skillName", skillName);
    }

    /**
     * 重新加载内置 Skill。
     * @return 返回结果
     */
    @PostMapping("/catalog/reload")
    public Map<String, Object> reloadCatalog() {
        skillService.reloadBuiltinSkills();
        return Map.of("success", true);
    }
}
