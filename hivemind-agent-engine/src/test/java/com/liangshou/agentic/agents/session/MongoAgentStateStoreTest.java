package com.liangshou.agentic.agents.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.domain.session.model.AgentSessionStateDocument;
import com.liangshou.agentic.infrastructure.mongo.repository.AgentSessionStateRepository;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.state.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link MongoAgentStateStore} 回归测试。
 *
 * <p>覆盖代码审查修复点：</p>
 * <ul>
 *     <li>B1 — (userId, sessionId) 寻址：文档 ID 与 userId 归一化</li>
 *     <li>I6 — save 单次读 + 单次写；exists/delete 走轻量操作</li>
 * </ul>
 */
class MongoAgentStateStoreTest {

    /** 测试用 State 实现，规避框架消息类型的序列化复杂度 */
    record TestState(String value) implements State {}

    private AgentSessionStateRepository repository;
    private MongoAgentStateStore store;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        repository = mock(AgentSessionStateRepository.class);
        mapper = new ObjectMapper().findAndRegisterModules();
        store = new MongoAgentStateStore(repository, mapper);
    }

    private ConversationSessionContext context(String userId, String sessionId) {
        return ConversationSessionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
    }

    @Test
    @DisplayName("save 按 (userId, sessionId, key) 三元组寻址创建文档")
    void saveCreatesDocumentWithTripleAddressedId() {
        when(repository.findById("u1:s1:state")).thenReturn(Optional.empty());

        store.save("u1", "s1", "k1", new TestState("v1"));

        ArgumentCaptor<AgentSessionStateDocument> captor =
                ArgumentCaptor.forClass(AgentSessionStateDocument.class);
        verify(repository, times(1)).save(captor.capture());
        AgentSessionStateDocument saved = captor.getValue();
        assertEquals("u1:s1:state", saved.getId());
        assertEquals("u1", saved.getUserId());
        assertEquals("s1", saved.getSessionId());
        assertTrue(saved.getStateJson().contains("k1"));
        // I6：单次读 + 单次写
        verify(repository, times(1)).findById("u1:s1:state");
    }

    @Test
    @DisplayName("save 合并进已有 payload 而非覆盖")
    void saveMergesIntoExistingPayload() throws Exception {
        AgentSessionStateDocument existing = AgentSessionStateDocument.builder()
                .id("u1:s1:state").userId("u1").sessionId("s1")
                .stateJson("{\"old\":{\"value\":\"x\"}}")
                .build();
        when(repository.findById("u1:s1:state")).thenReturn(Optional.of(existing));

        store.save("u1", "s1", "new", new TestState("y"));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = mapper.readValue(existing.getStateJson(), Map.class);
        assertTrue(payload.containsKey("old"), "旧键应保留");
        assertTrue(payload.containsKey("new"), "新键应写入");
    }

    @Test
    @DisplayName("get 往返读取已保存的 State")
    void getRoundTripsState() {
        AgentSessionStateDocument existing = AgentSessionStateDocument.builder()
                .id("u1:s1:state").userId("u1").sessionId("s1")
                .stateJson("{\"k1\":{\"value\":\"hello\"}}")
                .build();
        when(repository.findById("u1:s1:state")).thenReturn(Optional.of(existing));

        Optional<TestState> result = store.get("u1", "s1", "k1", TestState.class);

        assertTrue(result.isPresent());
        assertEquals("hello", result.get().value());
    }

    @Test
    @DisplayName("exists/delete 使用 existsById/deleteById 轻量操作 (I6)")
    void existsAndDeleteUseLightweightOperations() {
        store.exists("u1", "s1");
        store.delete("u1", "s1");

        verify(repository).existsById("u1:s1:state");
        verify(repository).deleteById("u1:s1:state");
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("userId 为 null 时归一化为空串命名空间")
    void nullUserIdMapsToEmptyNamespace() {
        when(repository.findById(":s1:state")).thenReturn(Optional.empty());

        store.save(null, "s1", "k", new TestState("v"));

        ArgumentCaptor<AgentSessionStateDocument> captor =
                ArgumentCaptor.forClass(AgentSessionStateDocument.class);
        verify(repository).save(captor.capture());
        assertEquals(":s1:state", captor.getValue().getId());
        assertEquals("", captor.getValue().getUserId());
    }

    @Test
    @DisplayName("prePopulate：状态已存在时跳过历史加载 (B1 寻址)")
    void prePopulateSkipsWhenStateExists() {
        when(repository.existsById("u1:s1:state")).thenReturn(true);
        IConversationPersistenceService persistence = mock(IConversationPersistenceService.class);

        store.prePopulateFromHistory(context("u1", "s1"), persistence);

        verify(persistence, never()).loadMessages(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("prePopulate：历史写入用户命名空间下的 agent_state 键 (B1 寻址)")
    void prePopulateWritesAgentStateKeyUnderUserNamespace() {
        when(repository.existsById("u1:s1:state")).thenReturn(false);
        when(repository.findById("u1:s1:state")).thenReturn(Optional.empty());
        IConversationPersistenceService persistence = mock(IConversationPersistenceService.class);
        Msg historyMessage = Msg.builder()
                .role(MsgRole.USER)
                .textContent("之前的对话")
                .build();
        when(persistence.loadMessages(any())).thenReturn(List.of(historyMessage));

        store.prePopulateFromHistory(context("u1", "s1"), persistence);

        ArgumentCaptor<AgentSessionStateDocument> captor =
                ArgumentCaptor.forClass(AgentSessionStateDocument.class);
        verify(repository).save(captor.capture());
        AgentSessionStateDocument saved = captor.getValue();
        assertEquals("u1:s1:state", saved.getId(), "必须写入真实用户命名空间，而非匿名 ':s1:state'");
        assertTrue(saved.getStateJson().contains("agent_state"),
                "必须使用框架约定的 agent_state 键");
    }
}
