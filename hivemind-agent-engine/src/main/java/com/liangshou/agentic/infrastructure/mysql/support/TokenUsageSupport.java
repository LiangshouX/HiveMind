package com.liangshou.agentic.infrastructure.mysql.support;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liangshou.agentic.infrastructure.mysql.po.TokenUsagePO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByDateDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByModelDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByProviderDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageSummaryDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Token 使用量 Support 接口
 */
public interface TokenUsageSupport extends IService<TokenUsagePO> {

    /**
     * 按用户和时间范围查询汇总统计
     */
    TokenUsageSummaryDTO getSummary(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按模型分组统计
     */
    List<TokenUsageByModelDTO> getByModel(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按供应商分组统计
     */
    List<TokenUsageByProviderDTO> getByProvider(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按日期分组统计（趋势）
     */
    List<TokenUsageByDateDTO> getByDate(String userId, LocalDate startDate, LocalDate endDate);
}
