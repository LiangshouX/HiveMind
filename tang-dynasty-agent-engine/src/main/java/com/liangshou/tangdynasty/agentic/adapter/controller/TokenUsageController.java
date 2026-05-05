package com.liangshou.tangdynasty.agentic.adapter.controller;

import com.liangshou.tangdynasty.agentic.application.ITokenUsageRecordService;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto.*;
import com.liangshou.tangdynasty.agentic.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Token 使用量查询 API
 * 
 * <p>提供按用户、模型、供应商等维度的 Token 使用量查询功能。</p>
 */
@RestController
@RequestMapping("/api/agent/token-usage")
@RequiredArgsConstructor
public class TokenUsageController {

    private final ITokenUsageRecordService tokenUsageService;

    /**
     * 获取汇总统计
     */
    @GetMapping("/summary")
    public TokenUsageSummaryDTO getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        return tokenUsageService.getSummary(userId, startDate, endDate);
    }

    /**
     * 按模型统计
     */
    @GetMapping("/by-model")
    public List<TokenUsageByModelDTO> getByModel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        return tokenUsageService.getByModel(userId, startDate, endDate);
    }

    /**
     * 按供应商统计
     */
    @GetMapping("/by-provider")
    public List<TokenUsageByProviderDTO> getByProvider(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        return tokenUsageService.getByProvider(userId, startDate, endDate);
    }

    /**
     * 按日期统计（趋势）
     */
    @GetMapping("/by-date")
    public List<TokenUsageByDateDTO> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        return tokenUsageService.getByDate(userId, startDate, endDate);
    }
}
