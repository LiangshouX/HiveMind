package com.liangshou.agentic.agents.memory.compaction;

import com.liangshou.agentic.common.config.TdAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextWindowManagerTest {

    private TdAgentProperties properties;
    private ContextWindowManager manager;

    @BeforeEach
    void setUp() {
        properties = new TdAgentProperties();
        manager = new ContextWindowManager(properties);
    }

    @Test
    void getContextWindowSize_defaultModel_returns32768() {
        properties.getModel().setModelId("qwen-max");
        assertEquals(32768, manager.getContextWindowSize());
    }

    @Test
    void getContextWindowSize_qwen3max_returns131072() {
        properties.getModel().setModelId("qwen3-max");
        assertEquals(131072, manager.getContextWindowSize());
    }

    @Test
    void getContextWindowSize_deepseek_returns65536() {
        properties.getModel().setModelId("deepseek-chat");
        assertEquals(65536, manager.getContextWindowSize());
    }

    @Test
    void getContextWindowSize_unknownModel_returnsDefault() {
        properties.getModel().setModelId("unknown-model");
        assertEquals(32768, manager.getContextWindowSize());
    }

    @Test
    void getContextWindowSize_configOverride_takesPrecedence() {
        properties.getModel().setModelId("qwen-max");
        properties.getCompaction().setContextWindowSize(65536);
        assertEquals(65536, manager.getContextWindowSize());
    }

    @Test
    void getCompactionThreshold_defaultRatio_returns85Percent() {
        properties.getModel().setModelId("qwen3-max");
        // 131072 * 0.85 = 111411
        int threshold = manager.getCompactionThreshold();
        assertEquals((int) (131072 * 0.85), threshold);
    }

    @Test
    void getCompactionThreshold_customRatio_respectsConfig() {
        properties.getModel().setModelId("qwen3-max");
        properties.getCompaction().setThresholdRatio(0.90);
        assertEquals((int) (131072 * 0.90), manager.getCompactionThreshold());
    }

    @Test
    void getCompactionTarget_returns70PercentOfThreshold() {
        properties.getModel().setModelId("qwen3-max");
        int threshold = manager.getCompactionThreshold();
        int target = manager.getCompactionTarget();
        assertEquals((int) (threshold * 0.7), target);
    }

    @Test
    void needsCompaction_belowThreshold_returnsFalse() {
        properties.getModel().setModelId("qwen3-max");
        assertFalse(manager.needsCompaction(100000));
    }

    @Test
    void needsCompaction_atThreshold_returnsTrue() {
        properties.getModel().setModelId("qwen3-max");
        int threshold = manager.getCompactionThreshold();
        assertTrue(manager.needsCompaction(threshold));
    }

    @Test
    void needsCompaction_aboveThreshold_returnsTrue() {
        properties.getModel().setModelId("qwen3-max");
        assertTrue(manager.needsCompaction(120000));
    }

    @Test
    void getOutputReserve_default_returns8192() {
        assertEquals(8192, manager.getOutputReserve());
    }

    @Test
    void getOutputReserve_configOverride_returnsConfigured() {
        properties.getCompaction().setOutputReserveTokens(16384);
        assertEquals(16384, manager.getOutputReserve());
    }
}
