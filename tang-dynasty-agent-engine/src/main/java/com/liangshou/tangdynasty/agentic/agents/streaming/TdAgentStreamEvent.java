package com.liangshou.tangdynasty.agentic.agents.streaming;

import com.liangshou.tangdynasty.agentic.common.enums.TdAgentStreamEventType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Agent 流式事件数据传输对象，用于封装 SSE 推送的单个事件。
 * <p>
 * 包含以下信息：
 * <ul>
 *   <li>事件类型（MESSAGE、REASONING、TOOL_RESULT、RESULT、ERROR、APPROVAL_REQUIRED、DONE）</li>
 *   <li>会话 ID 和用户 ID，用于标识事件来源</li>
 *   <li>消息 ID，关联特定的 Agent 消息</li>
 *   <li>事件内容，通常是文本消息或状态描述</li>
 *   <li>是否为最后一个事件的标志</li>
 *   <li>元数据映射，包含额外的上下文信息</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
public class TdAgentStreamEvent {

    private TdAgentStreamEventType type;

    private String sessionId;

    private String userId;

    private String messageId;

    private String content;

    private boolean last;

    private Map<String, Object> metadata;
}
