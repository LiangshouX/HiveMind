package com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.mapper.TokenUsageMapper;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.po.TokenUsagePO;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.TokenUsageSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Token 使用量 Support 实现
 */
@Service
@Slf4j
public class TokenUsageSupportImpl extends ServiceImpl<TokenUsageMapper, TokenUsagePO> implements TokenUsageSupport {

    @Override
    public Map<String, Object> getSummary(String userId, LocalDate startDate, LocalDate endDate) {
        // 使用原生 SQL 进行聚合查询
        String sql = "SELECT " +
                "COALESCE(SUM(input_tokens), 0) as total_input_tokens, " +
                "COALESCE(SUM(output_tokens), 0) as total_output_tokens, " +
                "COALESCE(SUM(total_tokens), 0) as total_tokens, " +
                "COUNT(*) as total_calls " +
                "FROM td_token_usage " +
                "WHERE user_id = ? AND usage_time >= ? AND usage_time < ?";
        
        List<Map<String, Object>> result = this.getBaseMapper().selectMaps(
                new QueryWrapper<TokenUsagePO>()
                        .select("COALESCE(SUM(input_tokens), 0) as totalInputTokens",
                                "COALESCE(SUM(output_tokens), 0) as totalOutputTokens",
                                "COALESCE(SUM(total_tokens), 0) as totalTokens",
                                "COUNT(*) as totalCalls")
                        .eq("user_id", userId)
                        .ge("usage_time", startDate.atStartOfDay())
                        .lt("usage_time", endDate.plusDays(1).atStartOfDay())
        );
        
        return result.isEmpty() ? Map.of() : result.get(0);
    }

    @Override
    public List<Map<String, Object>> getByModel(String userId, LocalDate startDate, LocalDate endDate) {
        return this.getBaseMapper().selectMaps(
                new QueryWrapper<TokenUsagePO>()
                        .select("model_name as modelName",
                                "model_provider as modelProvider",
                                "SUM(input_tokens) as inputTokens",
                                "SUM(output_tokens) as outputTokens",
                                "SUM(total_tokens) as totalTokens",
                                "COUNT(*) as callCount")
                        .eq("user_id", userId)
                        .ge("usage_time", startDate.atStartOfDay())
                        .lt("usage_time", endDate.plusDays(1).atStartOfDay())
                        .groupBy("model_name", "model_provider")
                        .orderByDesc("SUM(total_tokens)")
        );
    }

    @Override
    public List<Map<String, Object>> getByProvider(String userId, LocalDate startDate, LocalDate endDate) {
        return this.getBaseMapper().selectMaps(
                new QueryWrapper<TokenUsagePO>()
                        .select("model_provider as modelProvider",
                                "SUM(input_tokens) as inputTokens",
                                "SUM(output_tokens) as outputTokens",
                                "SUM(total_tokens) as totalTokens",
                                "COUNT(*) as callCount",
                                "COUNT(DISTINCT model_name) as modelCount")
                        .eq("user_id", userId)
                        .ge("usage_time", startDate.atStartOfDay())
                        .lt("usage_time", endDate.plusDays(1).atStartOfDay())
                        .groupBy("model_provider")
                        .orderByDesc("SUM(total_tokens)")
        );
    }

    @Override
    public List<Map<String, Object>> getByDate(String userId, LocalDate startDate, LocalDate endDate) {
        return this.getBaseMapper().selectMaps(
                new QueryWrapper<TokenUsagePO>()
                        .select("DATE(usage_time) as usageDate",
                                "SUM(input_tokens) as inputTokens",
                                "SUM(output_tokens) as outputTokens",
                                "SUM(total_tokens) as totalTokens",
                                "COUNT(*) as callCount")
                        .eq("user_id", userId)
                        .ge("usage_time", startDate.atStartOfDay())
                        .lt("usage_time", endDate.plusDays(1).atStartOfDay())
                        .groupBy("DATE(usage_time)")
                        .orderByAsc("DATE(usage_time)")
        );
    }
}
