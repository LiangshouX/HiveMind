package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Token 使用量汇总统计 VO
 */
@Data
public class TokenUsageSummaryDTO {
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private Long totalTokens;
    private Long totalCalls;
    private BigDecimal estimatedCost;
    private LocalDate startDate;
    private LocalDate endDate;
}
