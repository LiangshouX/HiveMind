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
        int totalTokensBefore = tokenMeter.countTotalTokens(messages)
                + tokenMeter.countTokens(memory.getCompressedSummary());

        log.info("[ContextCompressor] 开始压缩 - sessionId: {}, 总tokens: {}, 阈值: {}",
                context.getSessionId(), totalTokensBefore, windowManager.getCompactionThreshold());

        // 步骤1: 分层分割消息
        MessageLayers layers = splitLayers(messages);

        log.debug("[ContextCompressor] 分层结果 - system: {}, head: {}, middle: {}, tail: {}",
                layers.systemMessages.size(),
                layers.headMessages.size(),
                layers.middleMessages.size(),
                layers.tailMessages.size());

        // 步骤2: 压缩中间层
        String newSummary = compressMiddleLayer(
                context, layers.middleMessages, memory.getCompressedSummary());

        // 步骤3: 计算保留消息 = system + head + tail
        List<Msg> retainedMessages = new ArrayList<>();
        retainedMessages.addAll(layers.systemMessages);
        retainedMessages.addAll(layers.headMessages);
        retainedMessages.addAll(layers.tailMessages);

        // 步骤3.5: 截断保留区中的超大消息
        int compactionTarget = windowManager.getCompactionTarget();
        retainedMessages = truncateOversizedMessages(retainedMessages, compactionTarget);

        // 步骤4: 融合摘要
        String mergedSummary = mergeSummary(memory.getCompressedSummary(), newSummary);

        // 步骤5: 应用压缩
        memory.applyCompaction(retainedMessages, mergedSummary);

        int totalTokensAfter = tokenMeter.countTotalTokens(retainedMessages)
                + tokenMeter.countTokens(mergedSummary);
        long duration = System.currentTimeMillis() - startTime;

        String strategy = newSummary != null
                && !newSummary.equals(memory.getCompressedSummary()) ? "REME" : "LOCAL";

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
    String compressMiddleLayer(
            ConversationSessionContext context,
            List<Msg> middleMessages,
            String existingSummary) {

        if (middleMessages.isEmpty()) {
            return null;
        }

        // 策略1: 尝试 ReMe 智能压缩
        if (properties.getReme().isEnabled()) {
            try {
                String summary = reMeService.compactSessionHistory(
                        context, middleMessages, existingSummary);
                if (summary != null && !summary.isBlank()) {
                    log.debug("[ContextCompressor] ReMe 压缩成功 - 摘要长度: {}", summary.length());
                    return summary;
                }
            } catch (Exception e) {
                log.warn("[ContextCompressor] ReMe 压缩失败，降级为本地压缩 - error: {}",
                        e.getMessage());
            }
        }

        // 策略2: 本地降级压缩
        return localCompress(middleMessages);
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
     */
    String mergeSummary(String existingSummary, String newSummary) {
        if (newSummary == null || newSummary.isBlank()) {
            return existingSummary == null ? "" : existingSummary;
        }
        if (existingSummary == null || existingSummary.isBlank()) {
            return newSummary;
        }
        return existingSummary + "\n---\n" + newSummary;
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
