package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.mapper.SkillMetaManageMapper;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.po.SkillMetaManagePO;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.SkillMetaManageSupport;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto.SkillPageQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class SkillMetaManageSupportImpl extends ServiceImpl<SkillMetaManageMapper, SkillMetaManagePO>
        implements SkillMetaManageSupport {
    @Override
    public IPage<SkillMetaManagePO> pageSkills(SkillPageQuery query) {
        Page<SkillMetaManagePO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SkillMetaManagePO> wrapper = buildQueryWrapper(query);

        // 动态排序
        if ("created_at".equalsIgnoreCase(query.getOrderBy())) {
            wrapper.orderBy(true, "desc".equalsIgnoreCase(query.getOrderDir()), SkillMetaManagePO::getCreatedAt);
        } else if ("name".equalsIgnoreCase(query.getOrderBy())) {
            wrapper.orderBy(true, "desc".equalsIgnoreCase(query.getOrderDir()), SkillMetaManagePO::getName);
        } else {
            wrapper.orderByDesc(SkillMetaManagePO::getUpdatedAt); // 默认按更新时间降序
        }

        return this.page(page, wrapper);
    }

    @Override
    public List<SkillMetaManagePO> searchPublicSkills(String keyword, int limit) {
        return this.list(new LambdaQueryWrapper<SkillMetaManagePO>()
                .eq(SkillMetaManagePO::getStatus, "published")
                .and(StringUtils.hasText(keyword), w ->
                        w.like(SkillMetaManagePO::getName, keyword)
                                .or()
                                .like(SkillMetaManagePO::getDescription, keyword))
                .orderByDesc(SkillMetaManagePO::getUpdatedAt)
                .last("LIMIT " + Math.min(limit, 50)));
    }

    @Override
    public List<SkillMetaManagePO> findByTags(List<String> tags, boolean matchAny) {
        if (CollectionUtils.isEmpty(tags)) return List.of();

        LambdaQueryWrapper<SkillMetaManagePO> wrapper = new LambdaQueryWrapper<>();
        if (matchAny) {
            // MySQL 8.0.30+ 支持 JSON_OVERLAPS，性能更优
            wrapper.apply("JSON_OVERLAPS(tags, {0})", tags.stream().map(t -> "\"" + t + "\"").collect(java.util.stream.Collectors.joining(",", "[", "]")));
        } else {
            // 交集匹配：必须包含所有传入标签
            for (String tag : tags) {
                wrapper.apply("JSON_CONTAINS(tags, {0})", "\"" + tag + "\"");
            }
        }
        wrapper.eq(SkillMetaManagePO::getStatus, "published").orderByDesc(SkillMetaManagePO::getUpdatedAt);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishSkill(String skillId, String targetVersion) {
        SkillMetaManagePO skill = this.getById(skillId);
        Objects.requireNonNull(skill, "Skill not found");

        if ("published".equals(skill.getStatus())) {
            log.warn("Skill {} is already published", skillId);
            return true;
        }

        skill.setStatus("published");
        if (StringUtils.hasText(targetVersion)) {
            // 可在此校验 targetVersion 是否存在于 fileManifest 中
            skill.setCurrentVersion(targetVersion);
        }
        return this.updateById(skill);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean archiveSkill(String skillId) {
        return this.update(new SkillMetaManagePO().setStatus("deprecated"),
                new LambdaQueryWrapper<SkillMetaManagePO>()
                        .eq(SkillMetaManagePO::getSkillId, skillId)
                        .ne(SkillMetaManagePO::getStatus, "deprecated")); // 避免重复更新
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFileManifest(String skillId, Map<String, Object> manifest) {
        Objects.requireNonNull(manifest, "Manifest cannot be null");
        return this.update(new SkillMetaManagePO().setFileManifest(manifest),
                new LambdaQueryWrapper<SkillMetaManagePO>().eq(SkillMetaManagePO::getSkillId, skillId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SkillMetaManagePO createSkill(String userId, String name, String description, String version) {
        SkillMetaManagePO skill = new SkillMetaManagePO();
        skill.setUserId(userId);
        skill.setName(name);
        skill.setDescription(description);
        skill.setCurrentVersion(version != null ? version : "1.0.0");
        skill.setStatus("draft");
        this.save(skill);
        log.info("创建 Skill 成功: userId={}, name={}, skillId={}", userId, name, skill.getSkillId());
        return skill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCurrentVersion(String skillId, String version) {
        Objects.requireNonNull(version, "Version cannot be null");
        return this.update(new SkillMetaManagePO().setCurrentVersion(version),
                new LambdaQueryWrapper<SkillMetaManagePO>().eq(SkillMetaManagePO::getSkillId, skillId));
    }

    @Override
    public SkillMetaManagePO findByUserIdAndName(String userId, String name) {
        return this.getOne(new LambdaQueryWrapper<SkillMetaManagePO>()
                .eq(SkillMetaManagePO::getUserId, userId)
                .eq(SkillMetaManagePO::getName, name)
                .last("LIMIT 1"));
    }

    @Override
    public List<SkillMetaManagePO> findByUserId(String userId) {
        return this.list(new LambdaQueryWrapper<SkillMetaManagePO>()
                .eq(SkillMetaManagePO::getUserId, userId)
                .orderByDesc(SkillMetaManagePO::getUpdatedAt));
    }

    /**
     * 构建通用查询条件（复用核心过滤逻辑）
     */
    private LambdaQueryWrapper<SkillMetaManagePO> buildQueryWrapper(SkillPageQuery query) {
        LambdaQueryWrapper<SkillMetaManagePO> wrapper = new LambdaQueryWrapper<>();

        // 1. 用户隔离
        if (StringUtils.hasText(query.getUserId())) {
            wrapper.eq(SkillMetaManagePO::getUserId, query.getUserId());
        } else {
            // 未传 userId 时仅返回已发布的公开技能
            wrapper.eq(SkillMetaManagePO::getStatus, "published");
        }

        // 2. 状态过滤
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(SkillMetaManagePO::getStatus, query.getStatus());
        }

        // 3. 关键词模糊匹配（name / description）
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(SkillMetaManagePO::getName, query.getKeyword())
                    .or()
                    .like(SkillMetaManagePO::getDescription, query.getKeyword()));
        }

        // 4. 标签交集过滤
        if (!CollectionUtils.isEmpty(query.getTags())) {
            for (String tag : query.getTags()) {
                wrapper.apply("JSON_CONTAINS(tags, {0})", "\"" + tag + "\"");
            }
        }

        return wrapper;
    }
}
