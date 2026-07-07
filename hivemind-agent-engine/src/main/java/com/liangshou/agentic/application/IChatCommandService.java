package com.liangshou.agentic.application;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.application.dto.ChatResponse;

import java.time.Instant;
import java.util.Map;

/**
 * @author LiangshouX
 */
public interface IChatCommandService {
    ChatResponse handleCommand(ConversationSessionContext context, String rawMessage);

    default ChatResponse buildResponse(
            ConversationSessionContext context,
            String message,
            long messageCount,
            Map<String, Object> metadata) {
        return ChatResponse.builder()
                .success(true)
                .commandHandled(true)
                .userId(context.getUserId())
                .sessionId(context.getSessionId())
                .title(context.getSessionTitle())
                .message(message)
                .messageCount(messageCount)
                .timestamp(Instant.now().toString())
                .metadata(metadata)
                .build();
    }
}
