package com.liangshou.agentic.infrastructure.mysql.support;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liangshou.agentic.infrastructure.mysql.po.SkillMetaManagePO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.SkillPageQuery;

import java.util.List;
import java.util.Map;

public interface SkillMetaManageSupport extends IService<SkillMetaManagePO> {

    /** 分页查询（支持用户隔离、关键词、标签、状态过滤） */
    IPage<SkillMetaManagePO> pageSkills(SkillPageQuery query);

    /** 公开技能关键词检索（用于全局搜索/推荐） */
    List<SkillMetaManagePO> searchPublicSkills(String keyword, int limit);

    /** 按标签匹配（支持 OR/AND 策略切换） */
    List<SkillMetaManagePO> findByTags(List<String> tags, boolean matchAny);

    /** 状态机：发布技能 */
    boolean publishSkill(String skillId, String targetVersion);

    /** 状态机：下架/归档技能 */
    boolean archiveSkill(String skillId);

    /** 更新文件清单（原子替换 manifest） */
    boolean updateFileManifest(String skillId, Map<String, Object> manifest);

    /** 创建技能元数据记录 */
    SkillMetaManagePO createSkill(String userId, String name, String description, String version);

    /** 更新当前版本号 */
    boolean updateCurrentVersion(String skillId, String version);

    /** 根据用户ID和名称查询技能 */
    SkillMetaManagePO findByUserIdAndName(String userId, String name);

    /** 根据用户ID查询技能列表 */
    List<SkillMetaManagePO> findByUserId(String userId);
}
