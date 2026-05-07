package com.liangshou.service.dto;

import com.liangshou.infrastructure.mongo.domain.EdictTemplateDocument.TemplateParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新任务模板命令。
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdictTemplateUpdateCommand {

    private String name;
    private String description;
    private String category;
    private String icon;
    private String command;
    private List<TemplateParam> params;
    private List<String> depts;
    private String est;
    private String cost;
}
