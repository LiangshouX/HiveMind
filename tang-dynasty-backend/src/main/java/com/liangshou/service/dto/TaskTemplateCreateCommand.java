package com.liangshou.service.dto;

import com.liangshou.infrastructure.mongo.domain.TaskTemplateDocument.TemplateParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建任务模板命令。
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateCreateCommand {

    @NotBlank(message = "模板名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "分类不能为空")
    private String category;

    private String icon;

    @NotBlank(message = "命令模板不能为空")
    private String command;

    @NotEmpty(message = "参数定义不能为空")
    private List<TemplateParam> params;

    private List<String> depts;

    private String est;

    private String cost;
}
