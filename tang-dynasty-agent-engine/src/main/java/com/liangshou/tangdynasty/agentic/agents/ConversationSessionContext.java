package com.liangshou.tangdynasty.agentic.agents;

import lombok.Builder;
import lombok.Getter;

/**
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
