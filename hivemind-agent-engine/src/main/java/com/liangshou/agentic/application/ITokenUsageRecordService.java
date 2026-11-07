package com.liangshou.agentic.application;

import com.liangshou.agentic.application.dto.SessionTokenUsageDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Token 使用量记录与查询服务
 */
public interface ITokenUsageRecordService {

    /**
     * 记录 Token 使用量
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param messageId 消息ID
     * @param modelConfig 模型配置 JSON
     * @param metadata 消息 metadata（包含 _chat_usage）
     */
    void record(String userId, String sessionId, String messageId, 
                String modelConfig, Map<String, Object> metadata);

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

    /**
     * 汇总指定会话的 Token 用量。
     *
     * <p>供"上下文面板"展示缓存命中、未缓存输入、缓存读取与输出。</p>
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 会话维度的 Token 用量汇总（无记录时返回全 0）
     */
    SessionTokenUsageDTO summarizeSession(String userId, String sessionId);
}
