package com.liangshou.tangdynasty.agentic.infrastructure.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent Skill 元数据实体
 * 注意：需配合 MetaObjectHandler 实现自动填充
 */
@Data
@Accessors(chain = true)
// autoResultMap 必须开启，否则 TypeHandler 不生效
@TableName(value = "skills_meta_manage", autoResultMap = true)
public class SkillMetaManagePO {

    @TableId(type = IdType.ASSIGN_UUID)
    private String skillId;

    private String userId;

    private String name;

    private String description;

    private String currentVersion;

    /** 状态枚举: draft/published/deprecated/archived */
    private String status;

    /** 标签列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 外部依赖 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> dependencies;

    /** 运行环境 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> executionEnv;

    /** 文件清单映射 (逻辑路径 -> S3元信息) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> fileManifest;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
