package com.liangshou.tangdynasty.agentic.adapter.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.liangshou.tangdynasty.agentic.application.dto.SkillUpsertRequest;
import com.liangshou.tangdynasty.agentic.agents.skill.TdAgentSkillInfo;
import com.liangshou.tangdynasty.agentic.agents.skill.TdAgentSkillService;
import com.liangshou.tangdynasty.agentic.application.service.SkillApplicationService;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.po.SkillMetaManagePO;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
@SuppressWarnings("unused")
public class TdAgentSkillController {

    private final TdAgentSkillService skillService;
    private final SkillApplicationService skillAppService;

    /**
     * 构造器
     * @param skillService skill 服务
     * @param skillAppService skill 应用服务（云端存储，可选）
     */
    public TdAgentSkillController(TdAgentSkillService skillService,
                                  @Autowired(required = false) SkillApplicationService skillAppService) {
        this.skillService = skillService;
        this.skillAppService = skillAppService;
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

    // ==================== 云端 Skill 管理 API（基于 OSS 存储） ====================

    /**
     * 检查云端服务是否可用
     */
    private void checkCloudServiceAvailable() {
        if (skillAppService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                    "云端 Skill 存储服务未启用，请在配置中启用 tdagent.skill.storage.oss.enabled=true");
        }
    }

    /**
     * 创建 Skill（云端存储模式）
     * @param userId 用户 ID
     * @param request 创建请求
     * @return Skill 响应信息
     */
    @PostMapping("/cloud/users/{userId}")
    public SkillResponse createCloudSkill(
            @PathVariable String userId,
            @Valid @RequestBody SkillCreateRequest request) {
        checkCloudServiceAvailable();
        return skillAppService.createSkill(userId, request);
    }

    /**
     * 更新 Skill 并创建新版本（云端存储模式）
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @param request 版本请求
     * @return Skill 响应信息
     */
    @PutMapping("/cloud/users/{userId}/{skillId}/versions")
    public SkillResponse updateCloudSkill(
            @PathVariable String userId,
            @PathVariable String skillId,
            @Valid @RequestBody SkillVersionRequest request) {
        checkCloudServiceAvailable();
        return skillAppService.updateSkill(userId, skillId, request);
    }

    /**
     * 发布 Skill
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @return Skill 响应信息
     */
    @PostMapping("/cloud/users/{userId}/{skillId}/publish")
    public SkillResponse publishCloudSkill(
            @PathVariable String userId,
            @PathVariable String skillId) {
        checkCloudServiceAvailable();
        return skillAppService.publishSkill(userId, skillId);
    }

    /**
     * 归档/下架 Skill
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @return 成功响应
     */
    @PostMapping("/cloud/users/{userId}/{skillId}/archive")
    public Map<String, Object> archiveCloudSkill(
            @PathVariable String userId,
            @PathVariable String skillId) {
        checkCloudServiceAvailable();
        skillAppService.archiveSkill(userId, skillId);
        return Map.of("success", true, "skillId", skillId);
    }

    /**
     * 删除 Skill（软删除）
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @return 成功响应
     */
    @DeleteMapping("/cloud/users/{userId}/{skillId}")
    public Map<String, Object> deleteCloudSkill(
            @PathVariable String userId,
            @PathVariable String skillId) {
        checkCloudServiceAvailable();
        skillAppService.deleteSkill(userId, skillId);
        return Map.of("success", true, "skillId", skillId);
    }

    /**
     * 获取 Skill 详情
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @return Skill 响应信息
     */
    @GetMapping("/cloud/users/{userId}/{skillId}")
    public SkillResponse getCloudSkill(
            @PathVariable String userId,
            @PathVariable String skillId) {
        checkCloudServiceAvailable();
        return skillAppService.getSkill(userId, skillId);
    }

    /**
     * 获取 Skill 下载 URL
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @return 下载 URL
     */
    @GetMapping("/cloud/users/{userId}/{skillId}/download")
    public Map<String, String> getCloudSkillDownloadUrl(
            @PathVariable String userId,
            @PathVariable String skillId) {
        checkCloudServiceAvailable();
        String url = skillAppService.getDownloadUrl(userId, skillId);
        return Map.of("downloadUrl", url);
    }

    /**
     * 分页查询 Skills（仅云端技能）
     * @param query 查询条件
     * @return 分页结果
     */
    @GetMapping("/cloud/page")
    public IPage<SkillMetaManagePO> pageCloudSkills(@Valid SkillPageQuery query) {
        checkCloudServiceAvailable();
        return skillAppService.pageSkills(query);
    }

    /**
     * 获取合并后的技能列表（系统内置 BUILTIN + 云端技能）
     * 系统内置技能优先展示，标记 source = BUILTIN
     * @param userId 用户 ID
     * @return 合并后的技能列表
     */
    @GetMapping("/cloud/all")
    public List<Map<String, Object>> listAllSkills(@RequestParam String userId) {
        // 1. 获取系统内置技能（BUILTIN）- 包含完整的 SKILL.md 内容
        List<TdAgentSkillInfo> builtinSkills = skillService.listSkills(userId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (TdAgentSkillInfo skill : builtinSkills) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("skillId", "builtin_" + skill.getName());
            map.put("userId", userId);
            map.put("name", skill.getName());
            map.put("description", skill.getDescription());
            map.put("currentVersion", "1.0.0");
            map.put("status", "published");
            map.put("tags", new java.util.ArrayList<String>());
            map.put("source", "BUILTIN");
            map.put("enabled", skill.isEnabled());
            map.put("createdAt", null);
            map.put("updatedAt", skill.getUpdatedAt() != null ? skill.getUpdatedAt().toString() : null);
            map.put("downloadUrl", null);
            // 系统预置SKILL直接返回 SKILL.md 内容和资源文件
            map.put("skillMarkdown", skill.getSkillMarkdown());
            map.put("resources", skill.getResources());
            map.put("fileManifest", null);
            result.add(map);
        }

        // 2. 获取云端技能（CUSTOMIZED）
        if (skillAppService != null) {
            SkillPageQuery query = new SkillPageQuery();
            query.setUserId(userId);
            query.setPageNum(1);
            query.setPageSize(100);
            var cloudPage = skillAppService.pageSkills(query);
            for (SkillMetaManagePO po : cloudPage.getRecords()) {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("skillId", po.getSkillId());
                map.put("userId", po.getUserId());
                map.put("name", po.getName());
                map.put("description", po.getDescription());
                map.put("currentVersion", po.getCurrentVersion());
                map.put("status", po.getStatus());
                map.put("tags", po.getTags());
                map.put("source", "CUSTOMIZED");
                map.put("enabled", true);
                map.put("createdAt", po.getCreatedAt());
                map.put("updatedAt", po.getUpdatedAt());
                map.put("fileManifest", po.getFileManifest());
                // 云端技能需要从 OSS 下载内容
                map.put("skillMarkdown", null);
                map.put("resources", null);

                // 生成下载 URL
                try {
                    String url = skillAppService.getDownloadUrl(userId, po.getSkillId());
                    map.put("downloadUrl", url);
                } catch (Exception e) {
                    map.put("downloadUrl", null);
                }

                result.add(map);
            }
        }

        return result;
    }
}
