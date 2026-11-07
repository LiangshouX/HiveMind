package com.liangshou.agentic.agents.memory.compaction;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文压缩器 - 实现智能分层压缩策略。
 *
 * <p>压缩策略（参考 Claude Code）：</p>
 * <ol>
 *     <li><b>System Prompt</b>：永不压缩，始终完整保留</li>
 *     <li><b>前 10% 消息</b>：保留对话开头的上下文建立阶段</li>
 *     <li><b>最近 N 条消息</b>：保留最近的交互，确保对话连贯性</li>
 *     <li><b>中间消息</b>：压缩为摘要</li>
 * </ol>
 *
 * @author LiangshouX
 */
@Component
public class ContextCompressor {

    private static final Logger log = LoggerFactory.getLogger(ContextCompressor.class);

    /**
     * 前区保留比例：保留对话开头的 10% 消息。
     */
    private static final double HEAD_RATIO = 0.10;

    /**
     * 默认保留最近消息数。
     */
    private static final int DEFAULT_KEEP_RECENT = 6;

    /**
     * 空消息判定阈值（字符数）。
     */
    private static final int EMPTY_MESSAGE_THRESHOLD = 10;

    /**
     * 超大消息截断阈值比例（相对于压缩目标）。
     */
    private static final double OVERSIZED_RATIO = 0.50;

    /**
     * 超大消息截断保留字符数。
     */
    private static final int TRUNCATE_CHARS = 500;

    /**
     * 工具消息概要保留字符数。
     */
    private static final int TOOL_SUMMARY_CHARS = 100;

    /**
     * 用户消息预览保留字符数。
     */
    private static final int USER_PREVIEW_CHARS = 200;

    /**
     * 新摘要允许占用的最小 token 预算（避免摘要被压成空串）。
     */
    private static final int MIN_SUMMARY_TOKEN_BUDGET = 120;

    /**
     * 新摘要相对被压缩内容的目标 token 比例。
     *
     * <p>摘要只有明显小于被替换的内容，压缩才有意义；否则压缩反而会让上下文变大。</p>
     */
    private static final double SUMMARY_BUDGET_RATIO = 0.6;

    private final TdAgentProperties properties;
    private final TdAgentReMeService reMeService;
    private final TokenMeter tokenMeter;
    private final ContextWindowManager windowManager;

    public ContextCompressor(
            TdAgentProperties properties,
            TdAgentReMeService reMeService,
            TokenMeter tokenMeter,
            ContextWindowManager windowManager) {
        this.properties = properties;
        this.reMeService = reMeService;
        this.tokenMeter = tokenMeter;
        this.windowManager = windowManager;
    }

    /**
     * 执行上下文压缩。
     *
     * @param context  会话上下文
     * @param memory   对话记忆
     * @param messages 当前完整消息列表
     * @return 压缩结果
     */
    public CompactionResult compress(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            List<Msg> messages) {

        long startTime = System.currentTimeMillis();
        String previousSummary = memory.getCompressedSummary();
        int totalTokensBefore = tokenMeter.countTotalTokens(messages)
                + tokenMeter.countTokens(previousSummary);

        log.info("[ContextCompressor] 开始压缩 - sessionId: {}, 总tokens: {}, 阈值: {}",
                context.getSessionId(), totalTokensBefore, windowManager.getCompactionThreshold(context));

        // 步骤1: 分层分割消息
        MessageLayers layers = splitLayers(messages);

        log.debug("[ContextCompressor] 分层结果 - system: {}, head: {}, middle: {}, tail: {}",
                layers.systemMessages.size(),
                layers.headMessages.size(),
                layers.middleMessages.size(),
                layers.tailMessages.size());

        // 步骤2: 压缩中间层
        MiddleCompression outcome =
                compressMiddleLayer(context, layers.middleMessages, previousSummary);
        String newSummary = outcome.summary();

        // 步骤2.5: 摘要预算裁剪
        // 摘要必须明显小于它所替代的内容，否则"压缩"反而会让上下文变大。
        int middleTokens = tokenMeter.countTotalTokens(layers.middleMessages);
        if (newSummary != null && !newSummary.isBlank()) {
            newSummary = trimSummaryToBudget(newSummary, middleTokens);
        }

        // 步骤3: 计算保留消息 = system + head + tail
        List<Msg> retainedMessages = new ArrayList<>();
        retainedMessages.addAll(layers.systemMessages);
        retainedMessages.addAll(layers.headMessages);
        retainedMessages.addAll(layers.tailMessages);

        // 步骤3.5: 截断保留区中的超大消息
        int compactionTarget = windowManager.getCompactionTarget(context);
        retainedMessages = truncateOversizedMessages(retainedMessages, compactionTarget);

        // 步骤4: 融合摘要
        String mergedSummary = mergeSummary(previousSummary, newSummary);

        // 步骤5: 应用压缩
        memory.applyCompaction(retainedMessages, mergedSummary);

        int totalTokensAfter = tokenMeter.countTotalTokens(retainedMessages)
                + tokenMeter.countTokens(mergedSummary);
        long duration = System.currentTimeMillis() - startTime;

        String strategy = outcome.reme() ? "REME" : "LOCAL";

        String ratio = String.format("%.1f",
                totalTokensBefore > 0 ? (1.0 - (double) totalTokensAfter / totalTokensBefore) * 100 : 0);
        log.info("[ContextCompressor] 压缩完成 - sessionId: {}, tokens: {} → {} (减少 {}%), " +
                        "保留: system({}) + head({}) + tail({}), 压缩: middle({}), 策略: {}, 耗时: {}ms",
                context.getSessionId(),
                totalTokensBefore, totalTokensAfter,
                ratio,
                layers.systemMessages.size(),
                layers.headMessages.size(),
                layers.tailMessages.size(),
                layers.middleMessages.size(),
                strategy,
                duration);

        return CompactionResult.builder()
                .compacted(true)
                .tokensBefore(totalTokensBefore)
                .tokensAfter(totalTokensAfter)
                .messagesBefore(messages.size())
                .messagesAfter(retainedMessages.size())
                .middleCompressed(layers.middleMessages.size())
                .durationMs(duration)
                .strategy(strategy)
                .build();
    }

    /**
     * 按 token 预算裁剪摘要。
     *
     * <p>摘要的 token 数被限制在"被压缩内容 token 数"的一定比例以内（并保留一个下限），
     * 从而保证压缩后上下文不会比压缩前更大。</p>
     *
     * @param summary      原始摘要
     * @param middleTokens 被压缩内容的 token 数
     * @return 裁剪后的摘要
     */
    private String trimSummaryToBudget(String summary, int middleTokens) {
        int budget = (int) Math.max(MIN_SUMMARY_TOKEN_BUDGET, middleTokens * SUMMARY_BUDGET_RATIO);
        int current = tokenMeter.countTokens(summary);
        if (current <= budget) {
            return summary;
        }

        // 二分查找可以保留的最大字符数
        int low = 0;
        int high = summary.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (tokenMeter.countTokens(summary.substring(0, mid)) <= budget) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        String trimmed = summary.substring(0, low).trim();
        if (trimmed.isEmpty()) {
            // 极端情况下至少保留一段可读文本
            return summary.substring(0, Math.min(summary.length(), 120));
        }
        log.debug("[ContextCompressor] 摘要超出预算，已裁剪 - 原始tokens: {}, 预算: {}, 保留字符: {}",
                current, budget, trimmed.length());
        return trimmed;
    }

    /**
     * 分层分割消息。
     */
    MessageLayers splitLayers(List<Msg> messages) {
        List<Msg> systemMessages = new ArrayList<>();
        List<Msg> nonSystemMessages = new ArrayList<>();

        for (Msg msg : messages) {
            // 跳过已注入的压缩摘要
            if (msg.getRole() == MsgRole.SYSTEM
                    && msg.getTextContent() != null
                    && msg.getTextContent().contains("<compressed_history>")) {
                continue;
            }
            if (msg.getRole() == MsgRole.SYSTEM) {
                systemMessages.add(msg);
            } else {
                nonSystemMessages.add(msg);
            }
        }

        int total = nonSystemMessages.size();
        if (total == 0) {
            return new MessageLayers(systemMessages, List.of(), List.of(), List.of());
        }

        // 计算前区大小：至少 1 条，按 headRatio 计算
        double headRatio = properties.getCompaction().getHeadRatio();
        if (headRatio <= 0 || headRatio > 0.5) {
            headRatio = HEAD_RATIO;
        }
        int headSize = Math.max(1, (int) (total * headRatio));

        // 计算尾区大小
        int tailSize = calculateTailSize(nonSystemMessages);

        // 确保 head + tail 不超过总数（至少留 1 条给 middle）
        if (headSize + tailSize >= total) {
            // 消息太少，不需要压缩
            return new MessageLayers(systemMessages, nonSystemMessages, List.of(), List.of());
        }

        int tailStart = total - tailSize;

        // Head 区：跳过空消息，顺延选取
        List<Msg> headMessages = selectHeadMessages(nonSystemMessages, headSize);
        int headActualEnd = headSize;

        // 重新计算 head 实际结束位置（考虑空消息跳过）
        int headIndex = 0;
        int selected = 0;
        for (int i = 0; i < tailStart && selected < headSize; i++) {
            if (!isEmptyMessage(nonSystemMessages.get(i))) {
                selected++;
                headIndex = i + 1;
            }
        }
        headActualEnd = Math.min(headIndex, tailStart);

        List<Msg> middleMessages = new ArrayList<>(nonSystemMessages.subList(headActualEnd, tailStart));
        List<Msg> tailMessages = nonSystemMessages.subList(tailStart, total);

        return new MessageLayers(systemMessages, headMessages, middleMessages, tailMessages);
    }

    /**
     * 选取 Head 区消息，跳过空消息。
     */
    private List<Msg> selectHeadMessages(List<Msg> nonSystemMessages, int headSize) {
        List<Msg> head = new ArrayList<>();
        for (Msg msg : nonSystemMessages) {
            if (head.size() >= headSize) break;
            if (!isEmptyMessage(msg)) {
                head.add(msg);
            }
        }
        // 如果跳过空消息后不足，补充原始消息
        if (head.isEmpty() && !nonSystemMessages.isEmpty()) {
            head.add(nonSystemMessages.get(0));
        }
        return head;
    }

    /**
     * 判断消息是否为空或极短。
     */
    private boolean isEmptyMessage(Msg msg) {
        if (msg == null) return true;
        String text = msg.getTextContent();
        return text == null || text.length() < EMPTY_MESSAGE_THRESHOLD;
    }

    /**
     * 计算尾区保留消息数。
     */
    private int calculateTailSize(List<Msg> nonSystemMessages) {
        int keepRecent = properties.getCompaction().getKeepRecentMessages();
        int tailSize = Math.max(DEFAULT_KEEP_RECENT, keepRecent);
        int maxTail = nonSystemMessages.size() / 2;
        return Math.min(tailSize, maxTail);
    }

    /**
     * 压缩中间层消息。
     */
    MiddleCompression compressMiddleLayer(
            ConversationSessionContext context,
            List<Msg> middleMessages,
            String existingSummary) {

        if (middleMessages.isEmpty()) {
            return new MiddleCompression(null, false);
        }

        // 策略1: 尝试 ReMe 智能压缩
        if (properties.getReme().isEnabled()) {
            try {
                String summary = reMeService.compactSessionHistory(
                        context, middleMessages, existingSummary);
                if (summary != null && !summary.isBlank()) {
                    log.debug("[ContextCompressor] ReMe 压缩成功 - 摘要长度: {}", summary.length());
                    return new MiddleCompression(summary, true);
                }
            } catch (Exception e) {
                log.warn("[ContextCompressor] ReMe 压缩失败，降级为本地压缩 - error: {}",
                        e.getMessage());
            }
        }

        // 策略2: 本地降级压缩
        return new MiddleCompression(localCompress(middleMessages), false);
    }

    /**
     * 中间层压缩结果。
     *
     * @param summary 生成的摘要，可为 null
     * @param reme    是否由 ReMe 生成（用于标注压缩策略）
     */
    record MiddleCompression(String summary, boolean reme) {
    }

    /**
     * 本地降级压缩。
     */
    String localCompress(List<Msg> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("[自动压缩的对话历史摘要]\n");

        int userCount = 0;
        int assistantCount = 0;
        int toolCount = 0;

        for (Msg msg : messages) {
            if (msg.getRole() == MsgRole.USER) {
                userCount++;
                String text = msg.getTextContent();
                if (text != null && !text.isBlank()) {
                    String preview = text.length() > USER_PREVIEW_CHARS
                            ? text.substring(0, USER_PREVIEW_CHARS) + "..."
                            : text;
                    sb.append("- 用户: ").append(preview).append("\n");
                }
            } else if (msg.getRole() == MsgRole.ASSISTANT) {
                assistantCount++;
                // 检查是否包含工具调用
                boolean hasToolUse = msg.getContent().stream()
                        .anyMatch(block -> block.getClass().getSimpleName().contains("ToolUse"));
                if (hasToolUse) {
                    toolCount++;
                    // 工具消息仅保留概要
                    msg.getContent().stream()
                            .filter(block -> block.getClass().getSimpleName().contains("ToolUse"))
                            .forEach(block -> {
                                String blockName = block.getClass().getSimpleName();
                                if (block instanceof io.agentscope.core.message.ToolUseBlock toolUse) {
                                    sb.append("- 工具调用: ").append(toolUse.getName());
                                    String input = toolUse.getContent();
                                    if (input != null && input.length() > TOOL_SUMMARY_CHARS) {
                                        sb.append(" (参数: ").append(input, 0, TOOL_SUMMARY_CHARS).append("...)");
                                    }
                                    sb.append("\n");
                                }
                            });
                } else {
                    String text = msg.getTextContent();
                    if (text != null && text.length() > 100) {
                        sb.append("- 助手回复 (").append(text.length()).append("字)\n");
                    }
                }
            } else if (msg.getRole() == MsgRole.TOOL) {
                toolCount++;
            }
        }

        sb.append(String.format("\n[统计: 用户消息 %d 条, 助手回复 %d 条, 工具调用 %d 条]",
                userCount, assistantCount, toolCount));

        String result = sb.toString();
        int maxLen = properties.getCompaction().getMaxSummaryCharacters();
        return result.length() <= maxLen ? result : result.substring(0, maxLen);
    }

    /**
     * 截断保留区中的超大消息。
     *
     * <p>如果单条消息 token 数超过压缩目标的 50%，截断到 TRUNCATE_CHARS 字符并添加 [已截断] 标记。</p>
     */
    private List<Msg> truncateOversizedMessages(List<Msg> messages, int compactionTarget) {
        int oversizedThreshold = (int) (compactionTarget * OVERSIZED_RATIO);
        List<Msg> result = new ArrayList<>();
        for (Msg msg : messages) {
            int msgTokens = tokenMeter.countMessageTokens(msg);
            if (msgTokens > oversizedThreshold && msg.getRole() != MsgRole.SYSTEM) {
                String text = msg.getTextContent();
                if (text != null && text.length() > TRUNCATE_CHARS) {
                    String truncated = text.substring(0, TRUNCATE_CHARS) + " [已截断]";
                    log.debug("[ContextCompressor] 截断超大消息 - 原始tokens: {}, 截断后长度: {}",
                            msgTokens, truncated.length());
                    result.add(Msg.builder()
                            .id(msg.getId())
                            .name(msg.getName())
                            .role(msg.getRole())
                            .textContent(truncated)
                            .timestamp(msg.getTimestamp())
                            .build());
                    continue;
                }
            }
            result.add(msg);
        }
        return result;
    }

    /**
     * 融合新旧摘要。
     *
     * <p>多次压缩会让摘要不断累加，因此这里按 {@code maxSummaryCharacters} 硬上限截断，
     * 避免摘要无限增长。</p>
     */
    String mergeSummary(String existingSummary, String newSummary) {
        if (newSummary == null || newSummary.isBlank()) {
            return existingSummary == null ? "" : existingSummary;
        }
        if (existingSummary == null || existingSummary.isBlank()) {
            return capSummary(newSummary);
        }

        int maxLen = properties.getCompaction().getMaxSummaryCharacters();
        if (maxLen <= 0) {
            return existingSummary + "\n---\n" + newSummary;
        }

        // 最新的摘要必须完整保留：先为它预留额度，剩余额度再用于旧摘要的尾部
        String separator = "\n---\n";
        if (newSummary.length() + separator.length() >= maxLen) {
            // 新摘要本身就超长：直接截断新摘要（它代表最新的上下文）
            return newSummary.substring(0, Math.min(newSummary.length(), maxLen));
        }
        int oldBudget = maxLen - newSummary.length() - separator.length();
        String trimmedExisting = existingSummary;
        if (trimmedExisting.length() > oldBudget) {
            // 保留旧摘要的尾部（更接近当前上下文的部分）
            trimmedExisting = trimmedExisting.substring(trimmedExisting.length() - oldBudget);
        }
        return trimmedExisting + separator + newSummary;
    }

    /**
     * 按配置的字符上限截断摘要。
     */
    private String capSummary(String summary) {
        int maxLen = properties.getCompaction().getMaxSummaryCharacters();
        if (maxLen <= 0 || summary.length() <= maxLen) {
            return summary;
        }
        return summary.substring(0, maxLen);
    }

    /**
     * 消息分层结果。
     */
    record MessageLayers(
            List<Msg> systemMessages,
            List<Msg> headMessages,
            List<Msg> middleMessages,
            List<Msg> tailMessages) {
    }
}
