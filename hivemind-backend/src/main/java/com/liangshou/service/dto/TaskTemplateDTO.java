package com.liangshou.service.dto;

import com.liangshou.infrastructure.mongo.domain.TaskTemplateDocument.TemplateParam;
import com.liangshou.infrastructure.mongo.domain.TaskTemplateDocument.TemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 任务模板数据传输对象。
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateDTO {

    private String templateId;
    private String name;
    private String description;
    private String category;
    private String icon;
    private String command;
    private List<TemplateParam> params;
    private List<String> depts;
    private String est;
    private String cost;
    private TemplateType type;
    private String userId;
    private Instant createdAt;
    private Instant updatedAt;
}
