package com.liangshou.agentic.agents.memory.compaction;

import lombok.Builder;
import lombok.Getter;

/**
 * 压缩结果 - 记录单次压缩操作的详细统计信息。
 *
 * @author LiangshouX
 */
@Getter
@Builder
public class CompactionResult {

    /**
     * 是否执行了压缩（false 表示不需要压缩）。
     */
    private final boolean compacted;

    /**
     * 压缩前总 token 数。
     */
    private final int tokensBefore;

    /**
     * 压缩后总 token 数。
     */
    private final int tokensAfter;

    /**
     * 压缩前消息数。
     */
    private final int messagesBefore;

    /**
     * 压缩后消息数（保留的消息）。
     */
    private final int messagesAfter;

    /**
     * 被压缩的中间层消息数。
     */
    private final int middleCompressed;

    /**
     * 压缩耗时（毫秒）。
     */
    private final long durationMs;

    /**
     * 使用的压缩策略（REME / LOCAL）。
     */
    private final String strategy;

    /**
     * 压缩率（token 减少百分比）。
     */
    public double compressionRatio() {
        if (tokensBefore == 0) return 0;
        return 1.0 - (double) tokensAfter / tokensBefore;
    }
}
