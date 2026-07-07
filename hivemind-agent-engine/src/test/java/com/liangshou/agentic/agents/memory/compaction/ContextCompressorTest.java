package com.liangshou.agentic.agents.memory.compaction;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContextCompressorTest {

    private TdAgentProperties properties;
    private TdAgentReMeService reMeService;
    private EstimatingTokenMeter tokenMeter;
    private ContextWindowManager windowManager;
    private ContextCompressor compressor;

    private ConversationSessionContext context;

    @BeforeEach
    void setUp() {
        properties = new TdAgentProperties();
        properties.getModel().setModelId("qwen3-max");
        reMeService = mock(TdAgentReMeService.class);
        tokenMeter = new EstimatingTokenMeter();
        windowManager = new ContextWindowManager(properties);
        compressor = new ContextCompressor(properties, reMeService, tokenMeter, windowManager);

        context = ConversationSessionContext.builder()
                .userId("user1")
                .sessionId("session1")
                .build();
    }

    @Test
    void splitLayers_systemMessages_preserved() {
        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.SYSTEM).textContent("You are a helpful assistant.").build(),
                Msg.builder().role(MsgRole.USER).textContent("Hello").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("Hi there!").build()
        );

        var layers = compressor.splitLayers(messages);

        assertEquals(1, layers.systemMessages().size());
        assertEquals("You are a helpful assistant.", layers.systemMessages().get(0).getTextContent());
    }

    @Test
    void splitLayers_compressedHistory_skipped() {
        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.SYSTEM).textContent("You are a helpful assistant.").build(),
                Msg.builder().role(MsgRole.SYSTEM).textContent("<compressed_history>old summary</compressed_history>").build(),
                Msg.builder().role(MsgRole.USER).textContent("Hello").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("Hi!").build()
        );

        var layers = compressor.splitLayers(messages);

        assertEquals(1, layers.systemMessages().size());
        assertFalse(layers.systemMessages().get(0).getTextContent().contains("compressed_history"));
    }

    @Test
    void splitLayers_emptyMessages_skippedInHead() {
        List<Msg> messages = new ArrayList<>();
        // 添加空消息和有内容的消息
        messages.add(Msg.builder().role(MsgRole.USER).textContent("").build());
        messages.add(Msg.builder().role(MsgRole.USER).textContent("ok").build()); // < 10 chars
        messages.add(Msg.builder().role(MsgRole.USER).textContent("Real first message with enough content").build());
        messages.add(Msg.builder().role(MsgRole.ASSISTANT).textContent("Response 1 with enough content").build());
        messages.add(Msg.builder().role(MsgRole.USER).textContent("Second message with enough content").build());
        messages.add(Msg.builder().role(MsgRole.ASSISTANT).textContent("Response 2 with enough content").build());

        var layers = compressor.splitLayers(messages);

        // Head 区应跳过空消息
        assertFalse(layers.headMessages().isEmpty());
        for (Msg msg : layers.headMessages()) {
            assertTrue(msg.getTextContent().length() >= 10 || layers.headMessages().size() == 1);
        }
    }

    @Test
    void mergeSummary_bothEmpty_returnsEmpty() {
        assertEquals("", compressor.mergeSummary("", ""));
    }

    @Test
    void mergeSummary_onlyNew_returnsNew() {
        assertEquals("new summary", compressor.mergeSummary("", "new summary"));
    }

    @Test
    void mergeSummary_onlyExisting_returnsExisting() {
        assertEquals("old summary", compressor.mergeSummary("old summary", null));
    }

    @Test
    void mergeSummary_bothPresent_concatenates() {
        String result = compressor.mergeSummary("old", "new");
        assertTrue(result.contains("old"));
        assertTrue(result.contains("new"));
        assertTrue(result.contains("---"));
    }

    @Test
    void localCompress_userMessages_extractsPreviews() {
        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.USER).textContent("What is the meaning of life?").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("The meaning of life is...").build()
        );

        String summary = compressor.localCompress(messages);

        assertTrue(summary.contains("用户"));
        assertTrue(summary.contains("What is the meaning"));
        assertTrue(summary.contains("统计"));
    }

    @Test
    void localCompress_toolMessages_summarizes() {
        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.USER).textContent("Read the file").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("I'll read the file for you.").build()
        );

        String summary = compressor.localCompress(messages);

        assertNotNull(summary);
        assertFalse(summary.isBlank());
    }

    @Test
    void compressMiddleLayer_remeUnavailable_fallsBackToLocal() {
        when(reMeService.compactSessionHistory(any(), any(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

        List<Msg> middleMessages = List.of(
                Msg.builder().role(MsgRole.USER).textContent("Test message").build()
        );

        String result = compressor.compressMiddleLayer(context, middleMessages, "");

        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void compressMiddleLayer_emptyMiddle_returnsNull() {
        String result = compressor.compressMiddleLayer(context, List.of(), "");
        assertNull(result);
    }

    @Test
    void compressMiddleLayer_remeReturnsSummary_usesIt() {
        when(reMeService.compactSessionHistory(any(), any(), anyString()))
                .thenReturn("ReMe generated summary");

        List<Msg> middleMessages = List.of(
                Msg.builder().role(MsgRole.USER).textContent("Test message").build()
        );

        String result = compressor.compressMiddleLayer(context, middleMessages, "");

        assertEquals("ReMe generated summary", result);
    }

    @Test
    void splitLayers_headPlusTail_notExceedHalf() {
        // 当消息很少时，head + tail 不应超过总数
        List<Msg> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(Msg.builder().role(MsgRole.USER).textContent("Message " + i + " with enough content").build());
        }

        var layers = compressor.splitLayers(messages);

        int headTail = layers.headMessages().size() + layers.tailMessages().size();
        assertTrue(headTail <= messages.size(),
                "Head(" + layers.headMessages().size() + ") + Tail(" + layers.tailMessages().size()
                        + ") should not exceed total(" + messages.size() + ")");
    }

    @Test
    void localCompress_truncatesToMaxLength() {
        properties.getCompaction().setMaxSummaryCharacters(50);

        List<Msg> messages = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            messages.add(Msg.builder().role(MsgRole.USER)
                    .textContent("This is a fairly long message number " + i + " with enough content to be meaningful")
                    .build());
        }

        String summary = compressor.localCompress(messages);
        assertTrue(summary.length() <= 50, "Summary should be truncated to max length, got " + summary.length());
    }
}
