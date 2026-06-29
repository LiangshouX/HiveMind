package com.liangshou.agentic.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 聊天响应对象，用于返回 Agent 对话的结果。
 * <p>
 * 包含以下字段：
 * <ul>
 *   <li>success - 请求是否成功执行</li>
 *   <li>commandHandled - 是否为命令响应（如 /clear、/history 等）</li>
 *   <li>userId - 用户标识</li>
 *   <li>sessionId - 会话标识</li>
 *   <li>title - 会话标题</li>
 *   <li>message - Agent 的回复消息内容</li>
 *   <li>messageCount - 当前会话的消息总数</li>
 *   <li>timestamp - 响应时间戳（ISO 8601 格式）</li>
 *   <li>metadata - 元数据映射，包含额外信息如 agentName、generateReason、paused、pendingApprovals 等</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
public class ChatResponse {

    private boolean success;

    private boolean commandHandled;

    private String userId;

    private String sessionId;

    private String title;

    private String message;

    private long messageCount;

    private String timestamp;

    private Map<String, Object> metadata;
}
