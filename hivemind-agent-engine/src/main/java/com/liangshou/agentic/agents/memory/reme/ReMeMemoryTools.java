package com.liangshou.agentic.agents.memory.reme;

import com.liangshou.agentic.agents.ConversationSessionContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ReMe 长期记忆工具 — 供 Agent 通过 Tool Call 主动管理长期记忆。
 *
 * <p>替代已废弃的 {@code LongTermMemoryTools}（依赖废弃的 {@code LongTermMemory} 接口），
 * 直接使用 {@link TdAgentReMeService} 实现记忆的记录与检索。</p>
 *
 * <p>提供两个工具：</p>
 * <ul>
 *     <li>{@code record_to_memory} — 将重要信息记录到长期记忆</li>
 *     <li>{@code retrieve_from_memory} — 从长期记忆中检索相关信息</li>
 * </ul>
 */
@Slf4j
public class ReMeMemoryTools {

    private final TdAgentReMeService reMeService;
    private final ConversationSessionContext sessionContext;

    public ReMeMemoryTools(TdAgentReMeService reMeService, ConversationSessionContext sessionContext) {
        this.reMeService = reMeService;
        this.sessionContext = sessionContext;
    }

    @Tool(name = "record_to_memory", description = "将重要信息或对话记录到长期记忆中，以便将来检索")
    public String recordToMemory(
            @ToolParam(name = "content", description = "需要记住的内容") String content) {
        if (content == null || content.isBlank()) {
            return "内容为空，未记录";
        }
        try {
            Msg msg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(List.of(TextBlock.builder().text(content).build()))
                    .build();
            String workspaceId = reMeService.userWorkspaceId(sessionContext);
            reMeService.add(workspaceId, List.of(msg));
            log.info("[ReMeMemoryTools] 已记录到长期记忆, workspace={}, contentLength={}",
                    workspaceId, content.length());
            return "已成功记录到长期记忆";
        } catch (Exception e) {
            log.error("[ReMeMemoryTools] 记录到长期记忆失败", e);
            return "记录失败: " + e.getMessage();
        }
    }

    @Tool(name = "retrieve_from_memory", description = "从长期记忆中检索与查询相关的信息")
    public String retrieveFromMemory(
            @ToolParam(name = "query", description = "检索查询内容") String query) {
        if (query == null || query.isBlank()) {
            return "查询内容为空";
        }
        try {
            String workspaceId = reMeService.userWorkspaceId(sessionContext);
            String result = reMeService.retrieve(workspaceId, query);
            if (result == null || result.isBlank()) {
                return "未找到相关记忆";
            }
            log.info("[ReMeMemoryTools] 从长期记忆检索到结果, workspace={}, queryLength={}, resultLength={}",
                    workspaceId, query.length(), result.length());
            return result;
        } catch (Exception e) {
            log.error("[ReMeMemoryTools] 从长期记忆检索失败", e);
            return "检索失败: " + e.getMessage();
        }
    }
}
