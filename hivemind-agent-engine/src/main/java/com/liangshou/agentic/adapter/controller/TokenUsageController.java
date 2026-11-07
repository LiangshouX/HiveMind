package com.liangshou.agentic.adapter.controller;

import com.liangshou.agentic.application.ITokenUsageRecordService;
import com.liangshou.agentic.common.util.SecurityUtils;
import com.liangshou.agentic.common.utils.Result;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByDateDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByModelDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageByProviderDTO;
import com.liangshou.agentic.infrastructure.mysql.support.dto.TokenUsageSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Token 使用量查询 API
 *
 * <p>提供按用户、模型、供应商等维度的 Token 使用量查询功能。</p>
 *
 * <p>所有端点统一返回 {@link Result} 包装结构，与项目其余 REST 接口保持一致。</p>
 */
@RestController
@RequestMapping("/api/agent/token-usage")
@RequiredArgsConstructor
public class TokenUsageController {

    /** 默认查询窗口：最近 30 天。 */
    private static final int DEFAULT_RANGE_DAYS = 30;

    private final ITokenUsageRecordService tokenUsageService;

    /**
     * 获取汇总统计
     */
    @GetMapping("/summary")
    public Result<TokenUsageSummaryDTO> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        DateRange range = DateRange.resolve(startDate, endDate);
        return Result.success(tokenUsageService.getSummary(userId, range.startDate(), range.endDate()));
    }

    /**
     * 按模型统计
     */
    @GetMapping("/by-model")
    public Result<List<TokenUsageByModelDTO>> getByModel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        DateRange range = DateRange.resolve(startDate, endDate);
        return Result.success(tokenUsageService.getByModel(userId, range.startDate(), range.endDate()));
    }

    /**
     * 按供应商统计
     */
    @GetMapping("/by-provider")
    public Result<List<TokenUsageByProviderDTO>> getByProvider(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        DateRange range = DateRange.resolve(startDate, endDate);
        return Result.success(tokenUsageService.getByProvider(userId, range.startDate(), range.endDate()));
    }

    /**
     * 按日期统计（趋势）
     */
    @GetMapping("/by-date")
    public Result<List<TokenUsageByDateDTO>> getByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        DateRange range = DateRange.resolve(startDate, endDate);
        return Result.success(tokenUsageService.getByDate(userId, range.startDate(), range.endDate()));
    }

    /**
     * 查询时间范围解析：缺省时回退到最近 {@value #DEFAULT_RANGE_DAYS} 天，
     * 并保证 startDate 不晚于 endDate。
     */
    private record DateRange(LocalDate startDate, LocalDate endDate) {

        static DateRange resolve(LocalDate startDate, LocalDate endDate) {
            LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
            LocalDate resolvedStart = startDate != null
                    ? startDate
                    : resolvedEnd.minusDays(DEFAULT_RANGE_DAYS - 1);
            if (resolvedStart.isAfter(resolvedEnd)) {
                resolvedStart = resolvedEnd;
            }
            return new DateRange(resolvedStart, resolvedEnd);
        }
    }
}
