package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class SkillPageQuery {

    /** 归属用户ID（可选，为空则查公开技能） */
    private String userId;

    /** 关键词（匹配 name / description） */
    private String keyword;

    /** 状态过滤：draft/published/deprecated */
    private String status;

    /** 标签过滤（交集匹配：必须同时包含所有传入标签） */
    private List<String> tags;

    /** 分页参数 */
    @Min(1) private Integer pageNum = 1;
    @Min(1) @Max(100) private Integer pageSize = 20;

    /** 排序字段：updated_at / created_at / name */
    private String orderBy = "updated_at";
    /** 排序方向：asc / desc */
    private String orderDir = "desc";
}
