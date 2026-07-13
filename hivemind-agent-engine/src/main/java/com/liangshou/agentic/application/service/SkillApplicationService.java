package com.liangshou.agentic.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import com.liangshou.agentic.infrastructure.mysql.po.SkillMetaManagePO;
import com.liangshou.agentic.infrastructure.mysql.support.SkillMetaManageSupport;
import com.liangshou.agentic.infrastructure.mysql.support.dto.*;
import com.liangshou.agentic.infrastructure.storage.OssProperties;
import com.liangshou.agentic.infrastructure.storage.SkillFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.*;

/**
 * Skill 应用层服务
 * 负责编排元数据管理和文件存储的完整业务流程
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "tdagent.skill.storage.oss.enabled", havingValue = "true")
public class SkillApplicationService {

    private final SkillMetaManageSupport skillMetaSupport;
    private final SkillFileStorageService fileStorageService;
    private final OssProperties ossProperties;

    @Autowired
    public SkillApplicationService(
            SkillMetaManageSupport skillMetaSupport,
            SkillFileStorageService fileStorageService,
            OssProperties ossProperties) {
        this.skillMetaSupport = skillMetaSupport;
        this.fileStorageService = fileStorageService;
        this.ossProperties = ossProperties;
    }

    /**
     * 创建 Skill（云端存储模式）
     *
     * @param userId  用户 ID
     * @param request 创建请求
     * @return Skill 响应信息
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse createSkill(String userId, SkillCreateRequest request) {
        // 1. 检查同名 Skill 是否已存在
        String skillName = request.getName();
        SkillMetaManagePO existing = skillMetaSupport.findByUserIdAndName(userId, skillName);
        if (existing != null) {
            throw new BizException(HmeErrorCode.SKILL_ALREADY_EXISTS, "同名 Skill 已存在: " + skillName);
        }

        // 2. 确定版本号
        String version = request.getVersion() != null ? request.getVersion() : "1.0.0";

        // 3. 创建元数据记录
        SkillMetaManagePO skill = skillMetaSupport.createSkill(userId, skillName, request.getDescription(), version);

        try {
            // 4. 上传文件到 OSS
            uploadSkillVersion(skill, version, request.getSkillMarkdown(), request.getResources());

            // 5. 更新标签和依赖信息
            skill.setTags(request.getTags() != null ? List.of(request.getTags()) : Collections.emptyList());
            skill.setDependencies(request.getDependencies());
            skill.setExecutionEnv(request.getExecutionEnv());
            skillMetaSupport.updateById(skill);

            // 6. 如果需要立即发布
            if (request.isPublish()) {
                skillMetaSupport.publishSkill(skill.getSkillId(), version);
                skill.setStatus("published");
            }

            log.info("Skill 创建成功: userId={}, skillId={}, name={}", userId, skill.getSkillId(), skillName);
            return buildSkillResponse(skill);
        } catch (Exception e) {
            log.error("Skill 创建失败，回滚元数据: userId={}, name={}", userId, skillName, e);
            skillMetaSupport.removeById(skill.getSkillId());
            throw new BizException(HmeErrorCode.SKILL_CREATE_ERROR, e);
        }
    }

    /**
     * 更新 Skill 并创建新版本
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @param request 版本请求
     * @return Skill 响应信息
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse updateSkill(String userId, String skillId, SkillVersionRequest request) {
        // 1. 查询并校验权限
        SkillMetaManagePO skill = skillMetaSupport.getById(skillId);
        if (skill == null || !skill.getUserId().equals(userId)) {
            throw new BizException(HmeErrorCode.SKILL_NOT_FOUND_OR_NO_PERMISSION);
        }

        String version = request.getVersion();

        // 2. 检查版本是否已存在
        if (fileStorageService.versionExists(userId, skillId, version)) {
            throw new BizException(HmeErrorCode.SKILL_VERSION_ALREADY_EXISTS, "版本已存在: " + version);
        }

        try {
            // 3. 上传新版本
            uploadSkillVersion(skill, version, request.getSkillMarkdown(), request.getResources());

            // 4. 更新当前版本号和文件清单
            skillMetaSupport.updateCurrentVersion(skillId, version);
            updateFileManifest(skill, version);

            log.info("Skill 更新成功: userId={}, skillId={}, version={}", userId, skillId, version);
            return buildSkillResponse(skillMetaSupport.getById(skillId));
        } catch (Exception e) {
            log.error("Skill 更新失败: userId={}, skillId={}", userId, skillId, e);
            throw new BizException(HmeErrorCode.SKILL_UPDATE_ERROR, e);
        }
    }

    /**
     * 发布 Skill
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @return Skill 响应信息
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse publishSkill(String userId, String skillId) {
        SkillMetaManagePO skill = skillMetaSupport.getById(skillId);
        if (skill == null || !skill.getUserId().equals(userId)) {
            throw new BizException(HmeErrorCode.SKILL_NOT_FOUND_OR_NO_PERMISSION);
        }

        boolean success = skillMetaSupport.publishSkill(skillId, skill.getCurrentVersion());
        if (!success) {
            throw new BizException(HmeErrorCode.SKILL_PUBLISH_ERROR);
        }

        log.info("Skill 发布成功: userId={}, skillId={}", userId, skillId);
        return buildSkillResponse(skillMetaSupport.getById(skillId));
    }

    /**
     * 归档/下架 Skill
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void archiveSkill(String userId, String skillId) {
        SkillMetaManagePO skill = skillMetaSupport.getById(skillId);
        if (skill == null || !skill.getUserId().equals(userId)) {
            throw new BizException(HmeErrorCode.SKILL_NOT_FOUND_OR_NO_PERMISSION);
        }

        skillMetaSupport.archiveSkill(skillId);
        log.info("Skill 归档成功: userId={}, skillId={}", userId, skillId);
    }

    /**
     * 删除 Skill（软删除）
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkill(String userId, String skillId) {
        SkillMetaManagePO skill = skillMetaSupport.getById(skillId);
        if (skill == null || !skill.getUserId().equals(userId)) {
            throw new BizException(HmeErrorCode.SKILL_NOT_FOUND_OR_NO_PERMISSION);
        }

        skillMetaSupport.removeById(skillId);
        log.info("Skill 删除成功: userId={}, skillId={}", userId, skillId);
    }

    /**
     * 获取 Skill 详情
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @return Skill 响应信息
     */
    public SkillResponse getSkill(String userId, String skillId) {
        SkillMetaManagePO skill = skillMetaSupport.getById(skillId);
        if (skill == null || !skill.getUserId().equals(userId)) {
            throw new BizException(HmeErrorCode.SKILL_NOT_FOUND_OR_NO_PERMISSION);
        }
        return buildSkillResponse(skill);
    }

    /**
     * 分页查询 Skills
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public IPage<SkillMetaManagePO> pageSkills(SkillPageQuery query) {
        return skillMetaSupport.pageSkills(query);
    }

    /**
     * 获取 Skill 下载 URL
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @return 预签名下载 URL
     */
    public String getDownloadUrl(String userId, String skillId) {
        SkillMetaManagePO skill = skillMetaSupport.getById(skillId);
        if (skill == null || !skill.getUserId().equals(userId)) {
            throw new BizException(HmeErrorCode.SKILL_NOT_FOUND_OR_NO_PERMISSION);
        }

        URL url = fileStorageService.generateDownloadUrl(userId, skillId, "v" + skill.getCurrentVersion());
        return url.toString();
    }

    /**
     * 上传 Skill 版本到 OSS
     */
    private void uploadSkillVersion(SkillMetaManagePO skill, String version,
                                    String skillMarkdown, Map<String, String> resources) {
        fileStorageService.uploadSkillVersion(
                skill.getUserId(),
                skill.getSkillId(),
                "v" + version,
                skillMarkdown,
                resources != null ? resources : Collections.emptyMap()
        );
    }

    /**
     * 更新文件清单
     */
    private void updateFileManifest(SkillMetaManagePO skill, String version) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("version", version);
        manifest.put("objectKey", buildVersionPath(skill.getUserId(), skill.getSkillId(), version));
        manifest.put("updatedAt", new Date().toString());
        skillMetaSupport.updateFileManifest(skill.getSkillId(), manifest);
    }

    /**
     * 构建版本路径
     */
    private String buildVersionPath(String userId, String skillId, String version) {
        return userId + "/" + skillId + "/v" + version + "/skill.tar.gz";
    }

    /**
     * 构建响应对象
     */
    private SkillResponse buildSkillResponse(SkillMetaManagePO skill) {
        SkillResponse.SkillResponseBuilder builder = SkillResponse.builder()
                .skillId(skill.getSkillId())
                .userId(skill.getUserId())
                .name(skill.getName())
                .description(skill.getDescription())
                .currentVersion(skill.getCurrentVersion())
                .status(skill.getStatus())
                .tags(skill.getTags())
                .dependencies(skill.getDependencies())
                .executionEnv(skill.getExecutionEnv())
                .fileManifest(skill.getFileManifest())
                .createdAt(skill.getCreatedAt())
                .updatedAt(skill.getUpdatedAt());

        // 添加下载 URL
        if (ossProperties.isEnabled()) {
            try {
                builder.downloadUrl(fileStorageService.generateDownloadUrl(
                        skill.getUserId(), skill.getSkillId(), "v" + skill.getCurrentVersion()).toString());
            } catch (Exception e) {
                log.warn("生成下载 URL 失败: {}", skill.getSkillId(), e);
            }
        }

        return builder.build();
    }
}
