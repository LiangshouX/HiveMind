package com.liangshou.agentic.agents.hooks;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.agentic.agents.memory.TdAgentMemoryManager;
import com.liangshou.agentic.application.IConversationPersistenceService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.state.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link TdAgentMemoryCompactionMiddleware} 回归测试。
 *
 * <p>覆盖代码审查修复点 B3：</p>
 * <ul>
 *     <li>压缩后推理输入真正收缩（系统消息 + 保留的近期消息），而非完整历史上叠加摘要</li>
 *     <li>压缩结果回写 call-scoped AgentState，使框架持久化压缩后的状态</li>
 *     <li>框架注入的系统消息不参与压缩、不被写入状态</li>
 * </ul>
 */
class TdAgentMemoryCompactionMiddlewareTest {

    private ConversationSessionContext context;
    private MongoConversationMemory memory;
    private TdAgentMemoryManager memoryManager;
    private TdAgentMemoryCompactionMiddleware middleware;
    private AgentState agentState;

    private Msg sysMsg;
    private Msg m1;
    private Msg m2;
    private Msg m3;

    @BeforeEach
    void setUp() {
        context = ConversationSessionContext.builder()
                .userId("u1")
                .sessionId("s1")
                .build();
        IConversationPersistenceService persistence = mock(IConversationPersistenceService.class);
        when(persistence.loadMessages(any())).thenReturn(List.of());
        when(persistence.loadCompressedSummary(any())).thenReturn("");
        memory = new MongoConversationMemory(context, persistence, "system-prompt");
        memoryManager = mock(TdAgentMemoryManager.class);
        middleware = new TdAgentMemoryCompactionMiddleware(context, memory, memoryManager);

        agentState = AgentState.builder()
                .userId("u1")
                .sessionId("s1")
                .build();
        // 模拟调用入口：状态中已含完整历史
        sysMsg = msg(MsgRole.SYSTEM, "sys");
        m1 = msg(MsgRole.USER, "m1");
        m2 = msg(MsgRole.ASSISTANT, "m2");
        m3 = msg(MsgRole.USER, "m3");
        agentState.contextMutable().add(m1);
        agentState.contextMutable().add(m2);
        agentState.contextMutable().add(m3);
    }

    private Msg msg(MsgRole role, String text) {
        return Msg.builder().role(role).textContent(text).build();
    }

    private ReasoningInput run(List<Msg> inputMessages) {
        RuntimeContext ctx = RuntimeContext.builder()
                .userId("u1")
                .sessionId("s1")
                .agentState(agentState)
                .build();
        AtomicReference<ReasoningInput> captured = new AtomicReference<>();
        Function<ReasoningInput, Flux<AgentEvent>> next = input -> {
            captured.set(input);
            return Flux.empty();
        };
        middleware.onReasoning(null, ctx,
                new ReasoningInput(new ArrayList<>(inputMessages), List.of(), null),
                next).blockLast();
        assertNotNull(captured.get(), "next 应被调用");
        return captured.get();
    }

    @Test
    @DisplayName("压缩后：推理输入收缩为'系统消息 + 保留消息'，状态同步回写 (B3)")
    void compactionShrinksInputAndWritesBackState() {
        // 压缩器保留最近一条消息并产生摘要
        doAnswer(inv -> {
            memory.applyCompaction(List.of(m3), "摘要");
            return true;
        }).when(memoryManager).maybeCompact(eq(context), eq(memory), any());
        when(memoryManager.injectCompressedSummary(any(), eq(memory)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReasoningInput result = run(List.of(sysMsg, m1, m2, m3));

        // 推理输入 = 头部系统消息 + 保留的 m3（m1/m2 已被压缩移除）
        assertEquals(2, result.messages().size());
        assertSame(sysMsg, result.messages().get(0), "框架系统消息必须保留");
        assertSame(m3, result.messages().get(1));

        // AgentState 回写：仅保留压缩后的对话消息，不含系统消息
        assertEquals(1, agentState.getContext().size());
        assertSame(m3, agentState.getContext().get(0));
    }

    @Test
    @DisplayName("未压缩时：推理输入保持原样，状态不被改写")
    void noCompactionKeepsInputAndState() {
        when(memoryManager.maybeCompact(eq(context), eq(memory), any())).thenReturn(false);
        when(memoryManager.injectCompressedSummary(any(), eq(memory)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<Msg> original = List.of(sysMsg, m1, m2, m3);
        ReasoningInput result = run(original);

        assertEquals(4, result.messages().size());
        // 状态保持完整历史
        assertEquals(3, agentState.getContext().size());
    }

    @Test
    @DisplayName("系统消息不进入对话记忆参与压缩度量")
    void systemMessagesAreExcludedFromMemory() {
        when(memoryManager.maybeCompact(eq(context), eq(memory), any())).thenReturn(false);
        when(memoryManager.injectCompressedSummary(any(), eq(memory)))
                .thenAnswer(inv -> inv.getArgument(0));

        run(List.of(sysMsg, m1, m2, m3));

        // 同步到记忆用于压缩判断的消息不含 SYSTEM
        assertTrue(memory.getMessages().stream()
                .noneMatch(m -> m.getRole() == MsgRole.SYSTEM));
        assertEquals(3, memory.getMessages().size());
    }
}
