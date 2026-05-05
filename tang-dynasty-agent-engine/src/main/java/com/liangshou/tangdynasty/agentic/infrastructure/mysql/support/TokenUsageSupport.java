package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.po.TokenUsagePO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Token 使用量 Support 接口
 */
public interface TokenUsageSupport extends IService<TokenUsagePO> {

    /**
     * 按用户和时间范围查询汇总统计
     */
    Map<String, Object> getSummary(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按模型分组统计
     */
    List<Map<String, Object>> getByModel(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按供应商分组统计
     */
    List<Map<String, Object>> getByProvider(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按日期分组统计（趋势）
     */
    List<Map<String, Object>> getByDate(String userId, LocalDate startDate, LocalDate endDate);
}
