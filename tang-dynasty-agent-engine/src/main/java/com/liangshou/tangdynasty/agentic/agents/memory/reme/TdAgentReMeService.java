package com.liangshou.tangdynasty.agentic.agents.memory.reme;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import io.agentscope.core.memory.reme.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolUseBlock;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ReMe 记忆服务 - 集成 ReMe (Retrieval-enhanced Memory) 实现长期记忆管理。
 *
 * <p>该服务利用 ReMe 框架提供以下核心功能：</p>
 * <ul>
 *     <li><b>记忆存储</b>：将对话消息添加到指定的 workspace 中，用于后续检索</li>
 *     <li><b>语义检索</b>：根据查询内容从历史记忆中检索相关信息，支持 Top-K 排序</li>
 *     <li><b>会话压缩</b>：对长对话历史进行智能压缩，生成包含关键事实、任务状态和未完成事项的摘要</li>
 *     <li><b>Workspace 管理</b>：为用户和会话生成独立的 workspace ID，实现记忆的隔离和组织</li>
 * </ul>
 *
 * <p>工作流程：</p>
 * <ol>
 *     <li>当对话历史超过阈值时，调用 {@link #compactSessionHistory} 方法</li>
 *     <li>将待压缩的消息添加到 ReMe workspace 中</li>
 *     <li>使用提示词向 ReMe 请求生成会话摘要</li>
 *     <li>返回融合后的压缩摘要，用于替换原始历史消息</li>
 * </ol>
 *
 * <p>配置项通过 {@link com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties.ReMe} 管理，
 * 包括 baseUrl、topK、timeout 等参数。</p>
 *
 * @author LiangshouX
 */
@Service
public class TdAgentReMeService {

    private final TdAgentProperties properties;
    private final ReMeClient reMeClient;

    /**
     * 构造器
     *
     * @param properties 外部化配置
     */
    public TdAgentReMeService(TdAgentProperties properties) {
        this.properties = properties;
        this.reMeClient = new ReMeClient(properties.getReme().getBaseUrl());
    }

    /**
     * 添加数据。
     *
     * @param workspaceId workspace 标识
     * @param messages    消息列表
     */
    public void add(String workspaceId, List<Msg> messages) {
        if (!properties.getReme().isEnabled()) {
            return;
        }
        List<ReMeMessage> remeMessages = toReMeMessages(messages);
        if (remeMessages.isEmpty()) {
            return;
        }
        ReMeAddRequest request =
                ReMeAddRequest.builder()
                        .workspaceId(workspaceId)
                        .trajectories(List.of(ReMeTrajectory.builder().messages(remeMessages).build()))
                        .build();
        reMeClient.add(request).block();
    }

    /**
     * 检索内容。
     *
     * @param workspaceId workspace 标识
     * @param query       查询内容
     * @return 返回结果
     */
    public String retrieve(String workspaceId, String query) {
        if (!properties.getReme().isEnabled() || query == null || query.isBlank()) {
            return "";
        }
        ReMeSearchResponse response =
                reMeClient.search(
                                ReMeSearchRequest.builder()
                                        .workspaceId(workspaceId)
                                        .query(query)
                                        .topK(properties.getReme().getTopK())
                                        .build())
                        .block();
        if (response == null) {
            return "";
        }
        if (response.getAnswer() != null && !response.getAnswer().isBlank()) {
            return truncate(response.getAnswer());
        }
        return truncate(
                response.getMemories().stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(System.lineSeparator())));
    }

    /**
     * 压缩会话历史。
     *
     * @param context         会话上下文
     * @param messages        消息列表
     * @param previousSummary 参数
     * @return 返回结果
     */
    public String compactSessionHistory(
            ConversationSessionContext context, List<Msg> messages, String previousSummary) {
        if (messages == null || messages.isEmpty()) {
            return previousSummary == null ? "" : previousSummary;
        }
        String workspaceId = sessionWorkspaceId(context);
        add(workspaceId, messages);
        String summary =
                retrieve(
                        workspaceId,
                        """
                                请总结该会话已压缩的历史上下文，输出后续继续对话所需的关键事实、任务状态、重要工具结果、用户偏好与未完成事项。
                                如果已有历史摘要，请融合去重并输出一份更新后的摘要。
                                现有摘要：
                                %s
                                """
                                .formatted(previousSummary == null ? "" : previousSummary));
        return summary == null || summary.isBlank() ? previousSummary : summary;
    }

    /**
     * 返回用户 workspace 标识。
     *
     * @param context 会话上下文
     * @return 返回结果
     */
    public String userWorkspaceId(ConversationSessionContext context) {
        return context.getUserId();
    }

    /**
     * 返回会话 workspace 标识。
     *
     * @param context 会话上下文
     * @return 返回结果
     */
    public String sessionWorkspaceId(ConversationSessionContext context) {
        return context.getUserId() + "::" + context.getSessionId();
    }

    private List<ReMeMessage> toReMeMessages(List<Msg> messages) {
        return messages.stream()
                .filter(Objects::nonNull)
                .filter(msg -> msg.getRole() == MsgRole.USER || msg.getRole() == MsgRole.ASSISTANT)
                .filter(msg -> !msg.hasContentBlocks(ToolUseBlock.class))
                .map(
                        msg -> {
                            String content = msg.getTextContent();
                            if (content == null || content.isBlank() || content.contains("<compressed_history>")) {
                                return null;
                            }
                            return ReMeMessage.builder()
                                    .role(msg.getRole() == MsgRole.USER ? "user" : "assistant")
                                    .content(content)
                                    .build();
                        })
                .filter(Objects::nonNull)
                .toList();
    }

    private String truncate(String summary) {
        if (summary == null) {
            return "";
        }
        int maxChars = properties.getCompaction().getMaxSummaryCharacters();
        return summary.length() <= maxChars ? summary : summary.substring(0, maxChars);
    }

    @PreDestroy
    void destroy() {
        reMeClient.shutdown();
    }
}

