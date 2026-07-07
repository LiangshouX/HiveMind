package com.liangshou.agentic.agents;

import lombok.Builder;
import lombok.Getter;

/**
 * 会话上下文 - 封装当前对话会话的关键信息。
 *
 * <p>该对象包含以下核心信息：</p>
 * <ul>
 *     <li>{@code userId} - 用户唯一标识，用于区分不同用户的会话</li>
 *     <li>{@code sessionId} - 会话唯一标识，用于区分同一用户的不同对话</li>
 *     <li>{@code userName} - 用户显示名称，用于个性化交互</li>
 *     <li>{@code sessionTitle} - 会话标题，用于在会话列表中标识该对话</li>
 * </ul>
 *
 * <p>该上下文对象在整个会话生命周期中传递，为以下组件提供必要的身份信息：</p>
 * <ul>
 *     <li>记忆管理服务（加载和保存对话历史）</li>
 *     <li>工具审批服务（关联审批记录到具体会话）</li>
 *     <li>状态持久化服务（保存和恢复 Agent 状态）</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Getter
@Builder
public class ConversationSessionContext {

    private final String userId;

    private final String sessionId;

    private final String userName;

    private final String sessionTitle;

    /**
     * 执行 documentId 操作。
     *
     * @return 返回结果
     */
    public String documentId() {
        return userId + ":" + sessionId;
    }
}
