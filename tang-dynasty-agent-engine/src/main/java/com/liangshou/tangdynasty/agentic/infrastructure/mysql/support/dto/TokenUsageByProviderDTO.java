package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto;

import lombok.Data;

/**
 * 按供应商统计的 Token 使用量 VO
 */
@Data
public class TokenUsageByProviderDTO {
    private String modelProvider;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private Long callCount;
    private Long modelCount;
}
