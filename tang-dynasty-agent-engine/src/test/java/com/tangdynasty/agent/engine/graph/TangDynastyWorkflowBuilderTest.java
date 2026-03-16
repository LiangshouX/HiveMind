package com.tangdynasty.agent.engine.graph;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 三省六部工作流构建器测试
 */
@DisplayName("三省六部工作流测试")
class TangDynastyWorkflowBuilderTest {
    
    @Mock
    private DashScopeChatModel chatModel;
    
    private TangDynastyWorkflowBuilder workflowBuilder;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        workflowBuilder = new TangDynastyWorkflowBuilder(chatModel);
    }
    
    @Test
    @DisplayName("应该成功构建工作流图")
    void shouldBuildWorkflowSuccessfully() throws Exception {
        // When
        CompiledGraph graph = workflowBuilder.buildWorkflow();
        
        // Then
        assertNotNull(graph, "工作流图不应为空");
    }
    
    @Test
    @DisplayName("闲聊消息应该直接返回回复")
    void shouldHandleChatMessage() throws Exception {
        // Given
        CompiledGraph graph = workflowBuilder.buildWorkflow();
        OverAllState initialState = new OverAllState();
        initialState.updateState(Map.of(
            "content", "你好，请问最近有什么新闻？"
        ));
        
        // Mock AI response for chat classification
        when(chatModel.call(any())).thenAnswer(invocation -> {
            var prompt = invocation.getArgument(0);
            return new com.alibaba.cloud.ai.dashscope.chat.DashScopeChatResponse(
                new com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion(
                    "CHAT", null, null
                )
            );
        });
        
        // When
        OverAllState result = graph.invoke(initialState);
        
        // Then
        assertEquals("__END__", result.value("nextNode").orElse(null), "闲聊应该结束流程");
        assertNotNull(result.value("response").orElse(null), "应该有回复内容");
    }
    
    @Test
    @DisplayName("任务消息应该进入中书省规划")
    void shouldHandleTaskMessage() throws Exception {
        // Given
        CompiledGraph graph = workflowBuilder.buildWorkflow();
        OverAllState initialState = new OverAllState();
        initialState.updateState(Map.of(
            "content", "帮我创建一个新的用户管理系统"
        ));
        
        // Mock AI response for task classification
        when(chatModel.call(any())).thenAnswer(invocation -> {
            return new com.alibaba.cloud.ai.dashscope.chat.DashScopeChatResponse(
                new com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion(
                    "EDICT", null, null
                )
            );
        });
        
        // When
        OverAllState result = graph.invoke(initialState);
        
        // Then
        assertEquals("zhongshu", result.value("nextNode").orElse(null), "任务应该进入中书省");
        assertEquals("EDICT", result.value("taskType").orElse(null));
    }
}
