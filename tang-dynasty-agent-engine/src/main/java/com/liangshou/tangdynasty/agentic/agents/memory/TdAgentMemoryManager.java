package com.liangshou.tangdynasty.agentic.agents.memory;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 记忆管理器 - 管理对话记忆的压缩和摘要注入。
 *
 * <p>该管理器负责智能控制对话历史的压缩策略，主要功能包括：</p>
 * <ul>
 *     <li><b>自动压缩触发</b>：根据消息数量或字符数阈值判断是否需要压缩历史</li>
 *     <li><b>压缩执行</b>：调用 ReMe 服务对旧消息进行压缩，保留最近 N 条消息不压缩</li>
 *     <li><b>摘要注入</b>：将压缩后的历史摘要作为 System Message 注入到当前对话上下文中</li>
 *     <li><b>配置驱动</b>：通过 {@link com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties.Compaction} 控制压缩行为</li>
 * </ul>
 *
 * <p>压缩触发条件（满足任一即可）：</p>
 * <ul>
 *     <li>历史消息数超过 {@code triggerMessageCount}（默认 20 条）</li>
 *     <li>总字符数超过 {@code triggerCharacterCount}（默认 24000 字符）</li>
 * </ul>
 *
 * <p>压缩策略：</p>
 * <ul>
 *     <li>保留最近的 {@code keepRecentMessages} 条消息（默认 8 条）不被压缩</li>
 *     <li>将更早的消息压缩为摘要，最大长度不超过 {@code maxSummaryCharacters}（默认 2400 字符）</li>
 *     <li>压缩后的摘要会累积更新，融合新旧信息</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Service
public class TdAgentMemoryManager {

    private final TdAgentProperties properties;
    private final TdAgentReMeService reMeService;

    /**
     * 构造器
     *
     * @param properties  外部化配置
     * @param reMeService ReMe 集成服务
     */
    public TdAgentMemoryManager(TdAgentProperties properties, TdAgentReMeService reMeService) {
        this.properties = properties;
        this.reMeService = reMeService;
    }

    /**
     * 执行 injectCompressedSummary 操作。
     *
     * @param originalInput 原始输入消息
     * @param memory        记忆实例
     * @return 返回结果
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

    /**
     * 执行 maybeCompact 操作。
     *
     * @param context      会话上下文
     * @param memory       记忆实例
     * @param currentInput 当前输入消息
     * @return 返回结果
     */
    public boolean maybeCompact(
            ConversationSessionContext context,
            MongoConversationMemory memory,
            List<Msg> currentInput) {
        if (!properties.getCompaction().isEnabled() || !properties.getReme().isEnabled()) {
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
        String summary =
                reMeService.compactSessionHistory(
                        context, compactCandidates, memory.getCompressedSummary());
        if (summary == null || summary.isBlank()) {
            return false;
        }
        memory.applyCompaction(remaining, summary);
        return true;
    }

    private int length(List<Msg> messages) {
        return messages.stream()
                .map(Msg::getTextContent)
                .mapToInt(text -> text == null ? 0 : text.length())
                .sum();
    }
}
