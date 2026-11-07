package com.liangshou.agentic.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 会话上下文占用情况响应。
 *
 * <p>用于聊天页底部实时展示"上下文窗口占用比例"，并在悬浮提示中展示
 * 已占用/总量、思考次数与工具调用次数。</p>
 *
 * @author LiangshouX
 */
@Getter
@Builder
public class SessionContextUsageResponse {

    /** 当前上下文已占用 token 数（估算） */
    private final int usedTokens;

    /** 上下文窗口总量（tokens） */
    private final int contextWindow;

    /** 占用比例（0~1） */
    private final double usageRatio;

    /** 压缩触发阈值比例（0~1） */
    private final double thresholdRatio;

    /** 压缩触发阈值（tokens） */
    private final int thresholdTokens;

    /** 输出预留 token 数 */
    private final int outputReserveTokens;

    /** 系统提示词固定部分占用的 token 数（每次调用都会占用上下文窗口） */
    private final int systemPromptTokens;

    /** 工具定义（Tool Schema）占用的 token 数（每次调用都会占用上下文窗口） */
    private final int toolDefinitionTokens;

    /** 对话消息（历史 + 压缩摘要）占用的 token 数 */
    private final int conversationTokens;

    /** 当前会话消息条数 */
    private final int messageCount;

    /** 思考（推理）次数 */
    private final int reasoningCount;

    /** 工具调用次数 */
    private final int toolCallCount;

    /** 已执行压缩次数 */
    private final long compactionCount;

    /** 压缩摘要字符数 */
    private final int compressedSummaryChars;

    /** 会话使用的供应商 ID */
    private final String providerId;

    /** 会话使用的模型 ID */
    private final String modelId;

    /** 是否已接近压缩阈值 */
    private final boolean nearThreshold;

    /** 本轮模型总耗时（毫秒） */
    private final long modelDurationMs;

    /** 首 token 延迟（毫秒，Time To First Token） */
    private final long ttftMs;

    /** 输出速度（tokens / 秒） */
    private final double tokensPerSecond;

    /** 缓存命中率（0~1） */
    private final double cacheHitRatio;

    /** 未命中缓存、真实计算的输入 token 数 */
    private final long uncachedInputTokens;

    /** 缓存读取（命中）的 token 数 */
    private final long cachedInputTokens;

    /** 输出 token 数 */
    private final long outputTokens;

    /** 模型调用次数 */
    private final int modelCalls;

    /** 当前会话使用的模型展示名 */
    private final String modelDisplayName;
}
