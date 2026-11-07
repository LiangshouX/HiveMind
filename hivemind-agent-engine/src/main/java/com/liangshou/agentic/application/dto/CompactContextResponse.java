package com.liangshou.agentic.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 手动压缩上下文的结果响应。
 *
 * @author LiangshouX
 */
@Getter
@Builder
public class CompactContextResponse {

    /** 是否实际执行了压缩 */
    private final boolean compacted;

    /** 压缩前 token 数 */
    private final int tokensBefore;

    /** 压缩后 token 数 */
    private final int tokensAfter;

    /** 压缩前消息数 */
    private final int messagesBefore;

    /** 压缩后消息数 */
    private final int messagesAfter;

    /** 被压缩成摘要的消息数 */
    private final int middleCompressed;

    /** 压缩策略（REME / LOCAL） */
    private final String strategy;

    /** 耗时（毫秒） */
    private final long durationMs;

    /** 压缩率（0~1） */
    private final double compressionRatio;

    /** 人类可读的结果描述 */
    private final String summary;
}
