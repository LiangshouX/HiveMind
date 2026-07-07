package com.liangshou.infrastructure.mongo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * 任务模板文档 - MongoDB 中存储任务模板的持久化对象。
 *
 * <p>支持系统内置模板和用户自建模板，系统内置模板不可编辑/删除。</p>
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "task_templates")
public class TaskTemplateDocument {

    @Id
    private String id;

    /**
     * 模板唯一标识（系统内置：tpl_xxx，用户自建：user_tpl_xxx）
     */
    @Field("template_id")
    @Indexed(unique = true)
    private String templateId;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板分类（前端筛选用）
     */
    @Indexed
    private String category;

    /**
     * 图标 emoji
     */
    private String icon;

    /**
     * 命令模板（含 {param} 占位符）
     */
    private String command;

    /**
     * 参数定义列表
     */
    private List<TemplateParam> params;

    /**
     * 承办部门列表
     */
    private List<String> depts;

    /**
     * 预估时间
     */
    private String est;

    /**
     * 预估耗资
     */
    private String cost;

    /**
     * 模板类型（SYSTEM=系统内置，USER=用户自建）
     */
    @Indexed
    private TemplateType type;

    /**
     * 创建者用户ID（系统内置为空）
     */
    @Field("user_id")
    private String userId;

    /**
     * 创建时间
     */
    @Field("created_at")
    private Instant createdAt;

    /**
     * 更新时间
     */
    @Field("updated_at")
    private Instant updatedAt;

    /**
     * 模板参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateParam {
        private String key;
        private String label;
        private String type; // input, textarea, select
        private boolean required;
        private String defaultValue;
        private List<String> options;
    }

    /**
     * 模板类型枚举
     */
    public enum TemplateType {
        SYSTEM,
        USER
    }
}
