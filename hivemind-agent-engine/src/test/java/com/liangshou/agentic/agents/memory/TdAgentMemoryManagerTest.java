package com.liangshou.agentic.agents.memory;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.compaction.ContextCompressor;
import com.liangshou.agentic.agents.memory.compaction.ContextWindowManager;
import com.liangshou.agentic.agents.memory.compaction.EstimatingTokenMeter;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TdAgentMemoryManagerTest {

    private TdAgentProperties properties;
    private TdAgentReMeService reMeService;
    private ContextCompressor compressor;
    private EstimatingTokenMeter tokenMeter;
    private ContextWindowManager windowManager;
    private TdAgentMemoryManager manager;

    private ConversationSessionContext context;

    @BeforeEach
    void setUp() {
        properties = new TdAgentProperties();
        properties.getModel().setModelId("qwen3-max");
        reMeService = mock(TdAgentReMeService.class);
        tokenMeter = new EstimatingTokenMeter();
        windowManager = new ContextWindowManager(properties);
        compressor = spy(new ContextCompressor(properties, reMeService, tokenMeter, windowManager));
        manager = new TdAgentMemoryManager(properties, reMeService, compressor, tokenMeter, windowManager);

        context = ConversationSessionContext.builder()
                .userId("user1")
                .sessionId("session1")
                .build();
    }

    @Test
    void maybeCompact_disabled_returnsFalse() {
        properties.getCompaction().setEnabled(false);
        MongoConversationMemory memory = mock(MongoConversationMemory.class);

        assertFalse(manager.maybeCompact(context, memory, List.of()));
    }

    @Test
    void maybeCompact_tokenMode_belowThreshold_returnsFalse() {
        MongoConversationMemory memory = mock(MongoConversationMemory.class);
        when(memory.getMessages()).thenReturn(new ArrayList<>());
        when(memory.getCompressedSummary()).thenReturn("");

        assertFalse(manager.maybeCompact(context, memory, List.of()));
    }

    @Test
    void injectCompressedSummary_emptySummary_returnsOriginal() {
        MongoConversationMemory memory = mock(MongoConversationMemory.class);
        when(memory.getCompressedSummary()).thenReturn("");

        List<Msg> original = List.of(
                Msg.builder().role(MsgRole.USER).textContent("Hello").build()
        );

        List<Msg> result = manager.injectCompressedSummary(original, memory);
        assertEquals(1, result.size());
    }

    @Test
    void injectCompressedSummary_withSummary_prependsSystemMessage() {
        MongoConversationMemory memory = mock(MongoConversationMemory.class);
        when(memory.getCompressedSummary()).thenReturn("Previous conversation summary");

        List<Msg> original = List.of(
                Msg.builder().role(MsgRole.USER).textContent("Hello").build()
        );

        List<Msg> result = manager.injectCompressedSummary(original, memory);
        assertEquals(2, result.size());
        assertEquals(MsgRole.SYSTEM, result.get(0).getRole());
        assertTrue(result.get(0).getTextContent().contains("compressed_history"));
        assertTrue(result.get(0).getTextContent().contains("Previous conversation summary"));
    }

    @Test
    void maybeCompact_legacyMode_respectsCharacterThreshold() {
        properties.getCompaction().setTriggerMode("LEGACY");
        properties.getReme().setEnabled(true);
        properties.getCompaction().setTriggerCharacterCount(100);
        properties.getCompaction().setTriggerMessageCount(100);

        MongoConversationMemory memory = mock(MongoConversationMemory.class);
        when(memory.getMessages()).thenReturn(new ArrayList<>());
        when(memory.getCompressedSummary()).thenReturn("");

        // 空消息列表不触发
        assertFalse(manager.maybeCompact(context, memory, List.of()));
    }

    @Test
    void maybeCompact_legacyMode_disabledReme_returnsFalse() {
        properties.getCompaction().setTriggerMode("LEGACY");
        properties.getReme().setEnabled(false);

        MongoConversationMemory memory = mock(MongoConversationMemory.class);

        assertFalse(manager.maybeCompact(context, memory, List.of()));
    }

    @Test
    void injectCompressedSummary_nullSummary_returnsOriginal() {
        MongoConversationMemory memory = mock(MongoConversationMemory.class);
        when(memory.getCompressedSummary()).thenReturn(null);

        List<Msg> original = List.of(
                Msg.builder().role(MsgRole.USER).textContent("Hello").build()
        );

        List<Msg> result = manager.injectCompressedSummary(original, memory);
        assertEquals(1, result.size());
    }
}
