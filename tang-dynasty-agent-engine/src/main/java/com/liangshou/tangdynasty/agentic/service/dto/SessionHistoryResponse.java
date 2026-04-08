package com.liangshou.tangdynasty.agentic.service.dto;

import com.liangshou.tangdynasty.agentic.domain.document.ConversationViewDocument;
import com.liangshou.tangdynasty.agentic.domain.document.StoredMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 会话历史响应对象，用于返回完整的会话信息和消息列表。
 * <p>
 * 包含以下字段：
 * <ul>
 *   <li>session - 会话视图信息，包括会话 ID、标题、创建时间等元数据</li>
 *   <li>messages - 会话中的消息列表，按时间顺序排列，包含用户消息和 Agent 回复</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
public class SessionHistoryResponse {

    private ConversationViewDocument session;

    private List<StoredMessage> messages;
}
