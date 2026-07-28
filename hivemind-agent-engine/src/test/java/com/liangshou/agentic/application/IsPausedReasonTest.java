package com.liangshou.agentic.application;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.application.dto.ChatRequest;
import com.liangshou.agentic.application.dto.ChatResponse;
import com.liangshou.agentic.application.dto.SessionHistoryResponse;
import com.liangshou.agentic.domain.memory.model.ConversationViewDocument;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ITdAgentChatService#isPaused(Msg)} 回归测试。
 *
 * <p>覆盖代码审查修复点 B2：2.0 中间件（Tool Guard）发出的 RequestStopEvent
 * 会产生 {@link GenerateReason#MIDDLEWARE_STOP_REQUESTED}，必须被识别为暂停。</p>
 */
class IsPausedReasonTest {

    /** 仅用于测试 default isPaused 的最小实现 */
    private final ITdAgentChatService service = new ITdAgentChatService() {
        @Override public ChatResponse chat(ChatRequest request) { return null; }
        @Override public List<ConversationViewDocument> listSessions(String userId) { return List.of(); }
        @Override public SessionHistoryResponse getSessionHistory(String userId, String sessionId) { return null; }
        @Override public void deleteSession(String userId, String sessionId) { }
        @Override public ConversationSessionContext buildContext(ChatRequest request) { return null; }
        @Override public ConversationSessionContext buildContext(String userId, String sessionId, String title) { return null; }
        @Override public String generateSessionTitle(String userId, String sessionId, String providerId, String modelId) { return null; }
    };

    private Msg msgWith(GenerateReason reason) {
        Msg msg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("stopped")
                .build();
        return reason == null ? msg : msg.withGenerateReason(reason);
    }

    @Test
    @DisplayName("MIDDLEWARE_STOP_REQUESTED 被识别为暂停 (B2)")
    void middlewareStopRequestedIsPaused() {
        assertTrue(service.isPaused(msgWith(GenerateReason.MIDDLEWARE_STOP_REQUESTED)));
    }

    @Test
    @DisplayName("其余停止原因仍被识别为暂停")
    void otherStopReasonsArePaused() {
        assertTrue(service.isPaused(msgWith(GenerateReason.REASONING_STOP_REQUESTED)));
        assertTrue(service.isPaused(msgWith(GenerateReason.ACTING_STOP_REQUESTED)));
        assertTrue(service.isPaused(msgWith(GenerateReason.TOOL_SUSPENDED)));
    }

    @Test
    @DisplayName("null 消息与无停止原因的消息不视为暂停")
    void normalMessagesAreNotPaused() {
        assertFalse(service.isPaused(null));
        assertFalse(service.isPaused(msgWith(null)));
    }
}
