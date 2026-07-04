package com.liangshou.agentic.agents.memory;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.compaction.CompactionResult;
import com.liangshou.agentic.agents.memory.compaction.ContextCompressor;
import com.liangshou.agentic.agents.memory.compaction.ContextWindowManager;
import com.liangshou.agentic.agents.memory.compaction.TokenMeter;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 记忆管理器 - 管理对话记忆的压缩和摘要注入。
 *
 * <p>支持两种压缩触发模式：</p>
 * <ul>
 *     <li><b>TOKEN 模式</b>（默认）：基于 token 计量，当上下文达到模型窗口的 85% 时触发压缩</li>
 *     <li><b>LEGACY 模式</b>：基于字符数/消息数，兼容旧的压缩逻辑</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Service
public class TdAgentMemoryManager {

    private static final Logger log = LoggerFactory.getLogger(TdAgentMemoryManager.class);

    private final TdAgentProperties properties;
    private final TdAgentReMeService reMeService;
    private final ContextCompressor compressor;
    private final TokenMeter tokenMeter;
    private final ContextWindowManager windowManager;

    public TdAgentMemoryManager(
            TdAgentProperties properties,
            TdAgentReMeService reMeService,
            ContextCompressor compressor,
            TokenMeter tokenMeter,
            ContextWindowManager windowManager) {
        this.properties = properties;
        this.reMeService = reMeService;
        this.compressor = compressor;
        this.tokenMeter = tokenMeter;
        this.windowManager = windowManager;
    }

    /**
     * 判断并执行压缩。
     *
     * <p>根据配置的 triggerMode 选择压缩策略：</p>
     * <ul>
     *     <li>TOKEN：基于 token 计量判断</li>
     *     <li>LEGACY：基于字符数/消息数判断（向后兼容）</li>
     * </ul>
     */
    public boolean maybeCompact(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            List<Msg> currentInput) {

        if (!properties.getCompaction().isEnabled()) {
            return false;
        }

        String mode = properties.getCompaction().getTriggerMode();
        if ("LEGACY".equalsIgnoreCase(mode)) {
            return maybeCompactLegacy(context, memory, currentInput);
        }

        return maybeCompactToken(context, memory);
    }

    /**
     * Token 模式压缩（新版本）。
     */
    private boolean maybeCompactToken(
            ConversationSessionContext context,
            MongoConversationMemory memory) {

        List<Msg> history = memory.getMessages();
        int totalTokens = tokenMeter.countTotalTokens(history)
                + tokenMeter.countTokens(memory.getCompressedSummary());

        if (!windowManager.needsCompaction(totalTokens)) {
            log.debug("[TdAgentMemoryManager] 未达到压缩阈值 - 当前: {}, 阈值: {}",
                    totalTokens, windowManager.getCompactionThreshold());
            return false;
        }

        // 检查压缩间隔
        int minInterval = properties.getCompaction().getMinMessagesSinceCompaction();
        long compactionCount = memory.getCompressedSummary().isEmpty() ? 0 : 1;
        // 简化：通过历史消息数判断是否有足够新消息
        if (history.size() <= minInterval) {
            log.debug("[TdAgentMemoryManager] 消息数不足最小压缩间隔 - 当前: {}, 最小间隔: {}",
                    history.size(), minInterval);
            return false;
        }

        log.info("[TdAgentMemoryManager] 触发 TOKEN 模式压缩 - 当前tokens: {}, 阈值: {}",
                totalTokens, windowManager.getCompactionThreshold());

        CompactionResult result = compressor.compress(context, memory, history);

        if (result.isCompacted()) {
            log.info("[TdAgentMemoryManager] 压缩成功 - tokens: {} → {} (减少 {:.1f}%), " +
                            "messages: {} → {}, 策略: {}",
                    result.getTokensBefore(), result.getTokensAfter(),
                    result.compressionRatio() * 100,
                    result.getMessagesBefore(), result.getMessagesAfter(),
                    result.getStrategy());
        }

        return result.isCompacted();
    }

    /**
     * LEGACY 模式压缩（向后兼容）。
     */
    private boolean maybeCompactLegacy(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            List<Msg> currentInput) {

        if (!properties.getReme().isEnabled()) {
            return false;
        }

        List<Msg> history = memory.getMessages();
        int keepRecent = Math.max(1, properties.getCompaction().getKeepRecentMessages());

        if (history.size() <= Math.max(keepRecent, properties.getCompaction().getTriggerMessageCount())) {
            return false;
        }

        int totalCharacters = length(history) + length(currentInput) + memory.getCompressedSummary().length();
        if (totalCharacters < properties.getCompaction().getTriggerCharacterCount()
                && history.size() < properties.getCompaction().getTriggerMessageCount()) {
            return false;
        }

        int compactSize = Math.max(0, history.size() - keepRecent);
        if (compactSize == 0) {
            return false;
        }

        List<Msg> compactCandidates = new ArrayList<>(history.subList(0, compactSize));
        List<Msg> remaining = new ArrayList<>(history.subList(compactSize, history.size()));

        String summary = reMeService.compactSessionHistory(
                context, compactCandidates, memory.getCompressedSummary());

        if (summary == null || summary.isBlank()) {
            return false;
        }

        memory.applyCompaction(remaining, summary);
        return true;
    }

    /**
     * 将压缩摘要注入到推理上下文中。
     */
    public List<Msg> injectCompressedSummary(List<Msg> originalInput, MongoConversationMemory memory) {
        String compressedSummary = memory.getCompressedSummary();
        if (compressedSummary == null || compressedSummary.isBlank()) {
            return originalInput;
        }
        List<Msg> updated = new ArrayList<>();
        updated.add(
                Msg.builder()
                        .role(MsgRole.SYSTEM)
                        .textContent(
                                """
                                        <compressed_history>
                                        以下为已自动压缩的历史上下文摘要，请将其作为当前会话的延续依据：
                                        %s
                                        </compressed_history>
                                        """
                                        .formatted(compressedSummary))
                        .build());
        updated.addAll(originalInput);
        return updated;
    }

    private int length(List<Msg> messages) {
        return messages.stream()
                .map(Msg::getTextContent)
                .mapToInt(text -> text == null ? 0 : text.length())
                .sum();
    }
}
