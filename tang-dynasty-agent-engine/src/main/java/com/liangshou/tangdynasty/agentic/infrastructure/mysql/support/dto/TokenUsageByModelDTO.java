package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按模型统计的 Token 使用量 VO
 */
@Data
public class TokenUsageByModelDTO {
    private String modelName;
    private String modelProvider;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private Long callCount;
    private BigDecimal estimatedCost;
}
