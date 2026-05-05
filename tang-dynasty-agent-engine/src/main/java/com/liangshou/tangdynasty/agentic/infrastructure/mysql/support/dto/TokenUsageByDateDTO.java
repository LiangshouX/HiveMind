package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 按日期统计的 Token 使用量 VO（用于趋势图）
 */
@Data
public class TokenUsageByDateDTO {
    private LocalDate usageDate;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private Long callCount;
}
