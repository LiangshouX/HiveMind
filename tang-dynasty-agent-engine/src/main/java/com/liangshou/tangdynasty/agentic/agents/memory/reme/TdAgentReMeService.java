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
 * @author LiangshouX
 */
@Service
public class TdAgentReMeService {

    private final TdAgentProperties properties;
    private final ReMeClient reMeClient;

    /**
     * 执行相关操作。
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

