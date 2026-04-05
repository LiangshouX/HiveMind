package com.liangshou.tangdynasty.agentic.agents.memory;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.MongoConversationMemory;
import com.liangshou.tangdynasty.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 记忆管理器
 *
 * <p>负责管理 Agent 的对话记忆，包括添加消息、自动压缩等功能。</p>
 *
 * <h3>功能说明：</h3>
 * <ul>
 *     <li>添加消息到记忆</li>
 *     <li>获取最近的消息</li>
 *     <li>自动压缩（当记忆超过阈值时）</li>
 *     <li>手动触发压缩</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Service
public class TdAgentMemoryManager {

    private final TdAgentProperties properties;
    private final TdAgentReMeService reMeService;

    /**
     * 执行相关操作。
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
