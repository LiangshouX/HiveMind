package com.liangshou.agentic.domain.memory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * 会话上下文统计快照。
 *
 * <p>记录最近一轮对话的运行时指标，供"上下文面板"展示：</p>
 * <ul>
 *   <li>{@code toolDefinitionTokens} — 工具定义（Tool Schema）占用的 token 数，
 *       这部分每次请求都会随提示词一起送给模型，属于上下文窗口的固定开销。</li>
 *   <li>{@code modelDurationMs} — 本轮从发起请求到拿到完整结果的总耗时。</li>
 *   <li>{@code ttftMs} — 首 token 延迟（Time To First Token）。</li>
 *   <li>{@code outputTokens} — 本轮输出 token 数。</li>
 *   <li>{@code tokensPerSecond} — 输出速度（TPS）。</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContextStats {

    /** 工具定义占用的 token 数 */
    @Field("tool_definition_tokens")
    private Integer toolDefinitionTokens;

    /** 本轮模型总耗时（毫秒） */
    @Field("model_duration_ms")
    private Long modelDurationMs;

    /** 首 token 延迟（毫秒） */
    @Field("ttft_ms")
    private Long ttftMs;

    /** 本轮输出 token 数 */
    @Field("output_tokens")
    private Integer outputTokens;

    /** 输出速度（tokens / 秒） */
    @Field("tokens_per_second")
    private Double tokensPerSecond;

    /** 统计时间 */
    @Field("measured_at")
    private Instant measuredAt;
}
