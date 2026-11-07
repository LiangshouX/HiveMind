package com.liangshou.agentic.infrastructure.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liangshou.agentic.infrastructure.mysql.po.TokenUsagePO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByDateDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByModelDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByProviderDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Token 使用量 Mapper
 *
 * <p>聚合查询使用显式 SQL 并映射到 DTO，避免 {@code selectMaps} 返回的
 * Map key 大小写与列别名不一致导致字段丢失。</p>
 */
@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsagePO> {

    /**
     * 按用户与时间范围汇总 Token 用量。
     */
    @Select("""
            SELECT
                COALESCE(SUM(input_tokens), 0)  AS totalInputTokens,
                COALESCE(SUM(output_tokens), 0) AS totalOutputTokens,
                COALESCE(SUM(total_tokens), 0)   AS totalTokens,
                COUNT(*)                        AS totalCalls
            FROM sys_token_usage
            WHERE user_id = #{userId}
              AND usage_time >= #{startTime}
              AND usage_time < #{endTime}
            """)
    TokenUsageSummaryDTO selectSummary(@Param("userId") String userId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 按模型分组统计 Token 用量。
     */
    @Select("""
            SELECT
                model_provider            AS modelProvider,
                model_name                AS modelName,
                COALESCE(SUM(input_tokens), 0)  AS inputTokens,
                COALESCE(SUM(output_tokens), 0) AS outputTokens,
                COALESCE(SUM(total_tokens), 0)   AS totalTokens,
                COUNT(*)                        AS callCount
            FROM sys_token_usage
            WHERE user_id = #{userId}
              AND usage_time >= #{startTime}
              AND usage_time < #{endTime}
            GROUP BY model_provider, model_name
            ORDER BY SUM(total_tokens) DESC
            """)
    List<TokenUsageByModelDTO> selectByModel(@Param("userId") String userId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 按供应商分组统计 Token 用量。
     */
    @Select("""
            SELECT
                model_provider                AS modelProvider,
                COALESCE(SUM(input_tokens), 0)  AS inputTokens,
                COALESCE(SUM(output_tokens), 0) AS outputTokens,
                COALESCE(SUM(total_tokens), 0)   AS totalTokens,
                COUNT(*)                        AS callCount,
                COUNT(DISTINCT model_name)      AS modelCount
            FROM sys_token_usage
            WHERE user_id = #{userId}
              AND usage_time >= #{startTime}
              AND usage_time < #{endTime}
            GROUP BY model_provider
            ORDER BY SUM(total_tokens) DESC
            """)
    List<TokenUsageByProviderDTO> selectByProvider(@Param("userId") String userId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 按日期分组统计 Token 用量（趋势图数据源）。
     */
    @Select("""
            SELECT
                DATE(usage_time)             AS usageDate,
                COALESCE(SUM(input_tokens), 0)  AS inputTokens,
                COALESCE(SUM(output_tokens), 0) AS outputTokens,
                COALESCE(SUM(total_tokens), 0)   AS totalTokens,
                COUNT(*)                        AS callCount
            FROM sys_token_usage
            WHERE user_id = #{userId}
              AND usage_time >= #{startTime}
              AND usage_time < #{endTime}
            GROUP BY DATE(usage_time)
            ORDER BY DATE(usage_time)
            """)
    List<TokenUsageByDateDTO> selectByDate(@Param("userId") String userId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
}
