package com.liangshou.agentic.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话维度的 Token 用量汇总。
 *
 * <p>用于"上下文面板"的 Token 用量区块：缓存命中、未缓存输入、缓存读取、输出。</p>
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionTokenUsageDTO {

    /** 调用次数 */
    private int calls;

    /** 输入 token 合计 */
    private long inputTokens;

    /** 输出 token 合计 */
    private long outputTokens;

    /** 总 token 合计 */
    private long totalTokens;

    /** 缓存命中（读取）token 合计 */
    private long cachedTokens;

    /**
     * 缓存命中率（0~1）。命中 token / 输入 token。
     */
    public double cacheHitRatio() {
        if (inputTokens <= 0) {
            return 0d;
        }
        return Math.min(1d, (double) cachedTokens / (double) inputTokens);
    }

    /**
     * 未命中缓存、需要真实计算的输入 token 数。
     */
    public long uncachedInputTokens() {
        return Math.max(0L, inputTokens - cachedTokens);
    }
}
