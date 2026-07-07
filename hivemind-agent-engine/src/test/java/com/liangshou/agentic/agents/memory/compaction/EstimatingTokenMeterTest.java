package com.liangshou.agentic.agents.memory.compaction;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstimatingTokenMeterTest {

    private EstimatingTokenMeter meter;

    @BeforeEach
    void setUp() {
        meter = new EstimatingTokenMeter();
    }

    @Test
    void countTokens_null_returnsZero() {
        assertEquals(0, meter.countTokens(null));
    }

    @Test
    void countTokens_empty_returnsZero() {
        assertEquals(0, meter.countTokens(""));
    }

    @Test
    void countTokens_englishText_returnsReasonableEstimate() {
        // "hello world" = 11 ASCII chars → 11 * 0.25 = 2.75 → ceil = 3
        int tokens = meter.countTokens("hello world");
        assertTrue(tokens >= 2 && tokens <= 5, "Expected ~3 tokens for 'hello world', got " + tokens);
    }

    @Test
    void countTokens_chineseText_returnsReasonableEstimate() {
        // "你好世界" = 4 CJK chars → 4 * 1.5 = 6
        int tokens = meter.countTokens("你好世界");
        assertTrue(tokens >= 5 && tokens <= 8, "Expected ~6 tokens for '你好世界', got " + tokens);
    }

    @Test
    void countTokens_mixedText_returnsReasonableEstimate() {
        String text = "Hello 你好 World 世界";
        int tokens = meter.countTokens(text);
        assertTrue(tokens >= 6 && tokens <= 15, "Mixed text tokens: " + tokens);
    }

    @Test
    void countTokens_longChineseText_scalesLinearly() {
        // 100 Chinese chars → ~150 tokens
        String text = "你".repeat(100);
        int tokens = meter.countTokens(text);
        assertTrue(tokens >= 140 && tokens <= 160, "100 CJK chars → ~150 tokens, got " + tokens);
    }

    @Test
    void countMessageTokens_null_returnsZero() {
        assertEquals(0, meter.countMessageTokens(null));
    }

    @Test
    void countMessageTokens_textMessage_includesOverhead() {
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("hello")
                .build();
        int tokens = meter.countMessageTokens(msg);
        // 4 (overhead) + 2 (block overhead) + countTokens("hello") ≈ 4 + 2 + 2 = 8
        assertTrue(tokens >= 5 && tokens <= 12, "Text message tokens: " + tokens);
    }

    @Test
    void countMessageTokens_thinkingMessage_countsThinkingContent() {
        Msg msg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .content(List.of(ThinkingBlock.builder().thinking("Let me think...").build()))
                .build();
        int tokens = meter.countMessageTokens(msg);
        assertTrue(tokens > 4, "Thinking message should have tokens > overhead");
    }

    @Test
    void countMessageTokens_toolUseMessage_countsNameAndInput() {
        Msg msg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .content(List.of(ToolUseBlock.builder()
                        .id("tool-1")
                        .name("read_file")
                        .input(Map.of("path", "/tmp/test.txt"))
                        .content("{\"path\": \"/tmp/test.txt\"}")
                        .build()))
                .build();
        int tokens = meter.countMessageTokens(msg);
        assertTrue(tokens > 6, "Tool use message tokens: " + tokens);
    }

    @Test
    void countTotalTokens_null_returnsZero() {
        assertEquals(0, meter.countTotalTokens(null));
    }

    @Test
    void countTotalTokens_emptyList_returnsZero() {
        assertEquals(0, meter.countTotalTokens(List.of()));
    }

    @Test
    void countTotalTokens_multipleMessages_sumsCorrectly() {
        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.USER).textContent("hello").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("hi there").build()
        );
        int total = meter.countTotalTokens(messages);
        assertTrue(total > 10, "Total tokens for 2 messages: " + total);
    }
}
