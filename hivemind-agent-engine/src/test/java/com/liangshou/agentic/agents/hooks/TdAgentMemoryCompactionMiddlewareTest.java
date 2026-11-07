package com.liangshou.agentic.agents.hooks;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.agentic.agents.memory.TdAgentMemoryManager;
import com.liangshou.agentic.application.IConversationPersistenceService;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.state.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 记忆压缩中间件回归测试。
 *
 * <p>覆盖核心缺陷：压缩成功后，送往 LLM 的消息与写回 Agent 状态的上下文
 * 必须替换为"压缩后的保留集"，而不是未压缩的完整历史。
 * 否则压缩对 token 完全无效，且流末持久化会把完整历史写回数据库。</p>
 *
 * @author LiangshouX
 */
class TdAgentMemoryCompactionMiddlewareTest {

    private ConversationSessionContext context;
    private IConversationPersistenceService persistenceService;
    private MongoConversationMemory memory;
    private TdAgentMemoryManager memoryManager;
    private Agent agent;
    private AgentState agentState;

    @BeforeEach
    void setUp() {
        context = ConversationSessionContext.builder()
                .userId("user1")
                .sessionId("session1")
                .build();

        persistenceService = mock(IConversationPersistenceService.class);
        when(persistenceService.loadCompressedSummary(any())).thenReturn("");
        when(persistenceService.loadMessages(any())).thenReturn(List.of());

        memory = new MongoConversationMemory(context, persistenceService, "");
        memoryManager = mock(TdAgentMemoryManager.class);
        when(memoryManager.injectCompressedSummary(any(), any()))
                .thenAnswer(invocation -> {
                    List<Msg> original = invocation.getArgument(0);
                    MongoConversationMemory mem = invocation.getArgument(1);
                    String summary = mem.getCompressedSummary();
                    if (summary == null || summary.isBlank()) {
                        return original;
                    }
                    List<Msg> updated = new ArrayList<>();
                    updated.add(Msg.builder()
                            .role(MsgRole.SYSTEM)
                            .textContent("<compressed_history>\n" + summary + "\n</compressed_history>")
                            .build());
                    updated.addAll(original);
                    return updated;
                });

        agentState = AgentState.builder()
                .sessionId("session1")
                .userId("user1")
                .build();
        agent = mock(Agent.class);
        when(agent.getAgentState()).thenReturn(agentState);
    }

    private List<Msg> buildHistory() {
        List<Msg> messages = new ArrayList<>();
        messages.add(userMsg("问题 1"));
        messages.add(assistantMsg("回答 1"));
        messages.add(userMsg("问题 2"));
        messages.add(assistantMsg("回答 2"));
        messages.add(userMsg("问题 3"));
        return messages;
    }

    private Msg userMsg(String text) {
        return Msg.builder().role(MsgRole.USER).textContent(text).build();
    }

    private Msg assistantMsg(String text) {
        return Msg.builder().role(MsgRole.ASSISTANT).textContent(text).build();
    }

    private List<Msg> invoke(List<Msg> messages) {
        TdAgentMemoryCompactionMiddleware middleware =
                new TdAgentMemoryCompactionMiddleware(context, memory, memoryManager);
        AtomicReference<ReasoningInput> captured = new AtomicReference<>();
        Function<ReasoningInput, Flux<io.agentscope.core.event.AgentEvent>> next = input -> {
            captured.set(input);
            return Flux.empty();
        };
        middleware.onReasoning(agent, null, new ReasoningInput(messages, List.of(), null), next)
                .blockLast();
        return captured.get().messages();
    }

    @Test
    void onReasoning_compacted_usesCompressedMessagesInsteadOfFullHistory() {
        List<Msg> history = buildHistory();
        List<Msg> retained = List.of(userMsg("问题 1"), userMsg("问题 3"));

        when(memoryManager.maybeCompact(any(), any(), any())).thenAnswer(invocation -> {
            MongoConversationMemory mem = invocation.getArgument(1);
            mem.applyCompaction(retained, "压缩后的历史摘要");
            return true;
        });

        List<Msg> result = invoke(history);

        // 完整历史是 5 条；压缩后应当只剩摘要 + 保留集（3 条）
        assertEquals(3, result.size());
        assertTrue(result.get(0).getTextContent().contains("<compressed_history>"));
        assertTrue(result.get(0).getTextContent().contains("压缩后的历史摘要"));
        assertEquals("问题 1", result.get(1).getTextContent());
        assertEquals("问题 3", result.get(2).getTextContent());

        // 被压缩掉的中间消息不应再出现
        assertFalse(result.stream().anyMatch(m -> "回答 1".equals(m.getTextContent())));
        assertFalse(result.stream().anyMatch(m -> "问题 2".equals(m.getTextContent())));
        assertFalse(result.stream().anyMatch(m -> "回答 2".equals(m.getTextContent())));
    }

    @Test
    void onReasoning_compacted_syncsAgentContextToCompressedVersion() {
        List<Msg> history = buildHistory();
        List<Msg> retained = List.of(userMsg("问题 1"), userMsg("问题 3"));

        when(memoryManager.maybeCompact(any(), any(), any())).thenAnswer(invocation -> {
            MongoConversationMemory mem = invocation.getArgument(1);
            mem.applyCompaction(retained, "压缩后的历史摘要");
            return true;
        });

        invoke(history);

        // Agent 状态必须同步为压缩后版本，否则流末持久化会把完整历史写回数据库
        List<Msg> agentContext = agentState.getContext();
        assertEquals(3, agentContext.size());
        assertFalse(agentContext.stream().anyMatch(m -> "回答 2".equals(m.getTextContent())));
    }

    @Test
    void onReasoning_notCompacted_keepsOriginalMessages() {
        List<Msg> history = buildHistory();
        when(memoryManager.maybeCompact(any(), any(), any())).thenReturn(false);

        List<Msg> result = invoke(history);

        assertEquals(5, result.size());
        assertEquals("问题 1", result.get(0).getTextContent());
        assertEquals("问题 3", result.get(4).getTextContent());
    }

    @Test
    void onReasoning_frameworkSystemMessage_isForwardedButNotRemembered() {
        Msg systemMessage = Msg.builder()
                .role(MsgRole.SYSTEM)
                .name("system")
                .textContent("# SOUL.md 这是一段系统提示词")
                .build();
        List<Msg> history = new ArrayList<>();
        history.add(systemMessage);
        history.addAll(buildHistory());

        List<Msg> retained = List.of(userMsg("问题 1"));
        when(memoryManager.maybeCompact(any(), any(), any())).thenAnswer(invocation -> {
            MongoConversationMemory mem = invocation.getArgument(1);
            mem.applyCompaction(retained, "摘要");
            return true;
        });

        List<Msg> result = invoke(history);

        // 系统消息仍要送给 LLM（否则 Agent 会失去人设与工具规范）
        assertEquals("system", result.get(0).getName());
        assertTrue(result.get(0).getTextContent().contains("SOUL.md"));

        // 但绝不能进入 Agent 状态：否则每轮累积一条，且系统提示词里的
        // "最近会话预览"会把上一轮的系统消息再包回来，形成正反馈膨胀
        List<Msg> agentContext = agentState.getContext();
        assertFalse(agentContext.stream().anyMatch(m -> "system".equals(m.getName())));
        assertFalse(agentContext.stream().anyMatch(m -> "SOUL.md".equals(m.getTextContent())));

        // 系统消息也不应进入记忆（否则会被压缩器当作"永不压缩的 system 层"保留并落库）
        assertFalse(memory.getMessages().stream().anyMatch(m -> "system".equals(m.getName())));
    }
}
