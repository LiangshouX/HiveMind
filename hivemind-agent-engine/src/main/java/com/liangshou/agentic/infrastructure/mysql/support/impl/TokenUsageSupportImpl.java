package com.liangshou.agentic.infrastructure.mysql.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.agentic.infrastructure.mysql.mapper.TokenUsageMapper;
import com.liangshou.agentic.infrastructure.mysql.po.TokenUsagePO;
import com.liangshou.agentic.infrastructure.mysql.support.TokenUsageSupport;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByDateDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByModelDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByProviderDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageSummaryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Token 使用量 Support 实现
 *
 * <p>聚合查询直接映射到 DTO（见 {@link TokenUsageMapper} 中的显式 SQL），
 * 避免 {@code selectMaps} 的 Map key 大小写不确定问题。</p>
 */
@Service
@Slf4j
public class TokenUsageSupportImpl extends ServiceImpl<TokenUsageMapper, TokenUsagePO> implements TokenUsageSupport {

    @Override
    public TokenUsageSummaryDTO getSummary(String userId, LocalDate startDate, LocalDate endDate) {
        TokenUsageSummaryDTO dto = getBaseMapper().selectSummary(
                userId, startOfDay(startDate), startOfNextDay(endDate));
        if (dto == null) {
            dto = new TokenUsageSummaryDTO();
        }
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        return dto;
    }

    @Override
    public List<TokenUsageByModelDTO> getByModel(String userId, LocalDate startDate, LocalDate endDate) {
        List<TokenUsageByModelDTO> list = getBaseMapper().selectByModel(
                userId, startOfDay(startDate), startOfNextDay(endDate));
        return list == null ? List.of() : list;
    }

    @Override
    public List<TokenUsageByProviderDTO> getByProvider(String userId, LocalDate startDate, LocalDate endDate) {
        List<TokenUsageByProviderDTO> list = getBaseMapper().selectByProvider(
                userId, startOfDay(startDate), startOfNextDay(endDate));
        return list == null ? List.of() : list;
    }

    @Override
    public List<TokenUsageByDateDTO> getByDate(String userId, LocalDate startDate, LocalDate endDate) {
        List<TokenUsageByDateDTO> list = getBaseMapper().selectByDate(
                userId, startOfDay(startDate), startOfNextDay(endDate));
        return list == null ? List.of() : list;
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime startOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }
}
