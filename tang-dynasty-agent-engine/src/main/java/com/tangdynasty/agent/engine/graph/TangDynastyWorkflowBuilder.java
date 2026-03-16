package com.tangdynasty.agent.engine.graph;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphState;
import com.alibaba.cloud.ai.graph.NodeAction;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 三省六部工作流图构建器
 * 
 * 使用Spring AI Alibaba 官方 Graph 框架
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TangDynastyWorkflowBuilder {
    
    private final DashScopeChatModel chatModel;
    
    /**
     * 构建三省六部协作工作流
     */
    public CompiledGraph buildWorkflow() throws Exception {
        StateGraph graph = new StateGraph();
        
        // 添加节点
        graph.addNode("chengxiang", chengxiangNode())      // 丞相分拣
             .addNode("zhongshu", zhongshuNode())          // 中书省规划
             .addNode("menxia", menxiaNode())              // 门下省审议
             .addNode("shangshu", shangshuNode())          // 尚书省派发
             .addNode("libu", libuNode())                  // 礼部执行
             .addNode("hubu", hubuNode())                  // 户部执行
             .addNode("bingbu", bingbuNode())              // 兵部执行
             .addNode("xingbu", xingbuNode())              // 刑部执行
             .addNode("gongbu", gongbuNode())              // 工部执行
             .addNode("libu_hr", libuHrNode());            // 吏部执行
        
        // 设置入口点
        graph.setEntryPoint("chengxiang");
        
        // 添加边（条件路由）
        graph.addConditionalEdges("chengxiang", chengxiangEdge());
        graph.addConditionalEdges("zhongshu", zhongshuEdge());
        graph.addConditionalEdges("menxia", menxiaEdge());
        graph.addConditionalEdges("shangshu", shangshuEdge());
        
        // 六部完成后汇总到尚书省
        graph.addEdge("libu", "shangshu");
        graph.addEdge("hubu", "shangshu");
        graph.addEdge("bingbu", "shangshu");
        graph.addEdge("xingbu", "shangshu");
        graph.addEdge("gongbu", "shangshu");
        graph.addEdge("libu_hr", "shangshu");
        
        // 编译图
        return graph.compile();
    }
    
    /**
     * 丞相节点 - 消息分拣
     */
    private NodeAction chengxiangNode() {
        return (state) -> {
            log.info("[丞相] 开始分拣消息...");
            
            String content = (String) state.value("content").orElse("");
            
            // 判断是闲聊还是任务
            if (isChat(content)) {
                state.updateState(Map.of(
                    "response", "您好！我是您的 AI 助手。请问有什么可以帮助您的吗？",
                    "nextNode", "__END__"  // 结束流程
                ));
            } else {
                state.updateState(Map.of(
                    "taskType", "EDICT",
                    "nextNode", "zhongshu"
                ));
            }
            
            return state;
        };
    }
    
    /**
     * 中书省节点 - 方案规划
     */
    private NodeAction zhongshuNode() {
        return (state) -> {
            log.info("[中书省] 开始规划方案...");
            
            String requirement = (String) state.value("content").orElse("");
            
            // TODO: 调用 AI 模型进行任务拆解
            state.updateState(Map.of(
                "plan", generatePlan(requirement),
                "todos", generateTodos(requirement),
                "nextNode", "menxia"
            ));
            
            return state;
        };
    }
    
    /**
     * 门下省节点 - 审议
     */
    private NodeAction menxiaNode() {
        return (state) -> {
            log.info("[门下省] 开始审议方案...");
            
            String plan = (String) state.value("plan").orElse("");
            
            // TODO: 调用 AI 模型进行审议
            boolean approved = reviewPlan(plan);
            
            if (approved) {
                state.updateState(Map.of(
                    "reviewResult", "APPROVED",
                    "nextNode", "shangshu"
                ));
            } else {
                state.updateState(Map.of(
                    "reviewResult", "REJECTED",
                    "rejectionReason", "方案需要完善",
                    "nextNode", "zhongshu"  // 返回中书省重新规划
                ));
            }
            
            return state;
        };
    }
    
    /**
     * 尚书省节点 - 派发执行
     */
    private NodeAction shangshuNode() {
        return (state) -> {
            log.info("[尚书省] 开始派发任务...");
            
            // 根据任务类型分发给六部
            String taskType = (String) state.value("taskType").orElse("GENERAL");
            
            state.updateState(Map.of(
                "executionDepartments", determineDepartments(taskType),
                "nextNode", "parallel_execution"  // 特殊标记，表示并行执行
            ));
            
            return state;
        };
    }
    
    // 六部执行节点（简化实现）
    private NodeAction libuNode() { return createExecutionNode("礼部"); }
    private NodeAction hubuNode() { return createExecutionNode("户部"); }
    private NodeAction bingbuNode() { return createExecutionNode("兵部"); }
    private NodeAction xingbuNode() { return createExecutionNode("刑部"); }
    private NodeAction gongbuNode() { return createExecutionNode("工部"); }
    private NodeAction libuHrNode() { return createExecutionNode("吏部"); }
    
    /**
     * 创建执行节点
     */
    private NodeAction createExecutionNode(String department) {
        return (state) -> {
            log.info("[{}] 开始执行任务...", department);
            // TODO: 实际执行逻辑
            state.updateState(Map.of(
                department + "_status", "COMPLETED"
            ));
            return state;
        };
    }
    
    /**
     * 丞相节点的边（条件路由）
     */
    private EdgeAction chengxiangEdge() {
        return (state) -> {
            String nextNode = (String) state.value("nextNode").orElse("__END__");
            return Optional.of(nextNode);
        };
    }
    
    /**
     * 中书省节点的边
     */
    private EdgeAction zhongshuEdge() {
        return (state) -> {
            String nextNode = (String) state.value("nextNode").orElse("menxia");
            return Optional.of(nextNode);
        };
    }
    
    /**
     * 门下省节点的边
     */
    private EdgeAction menxiaEdge() {
        return (state) -> {
            String nextNode = (String) state.value("nextNode").orElse("shangshu");
            return Optional.of(nextNode);
        };
    }
    
    /**
     * 尚书省节点的边
     */
    private EdgeAction shangshuEdge() {
        return (state) -> {
            // 根据部门列表决定下一个节点
            return Optional.of("libu"); // 简化处理，先执行礼部
        };
    }
    
    // ========== 辅助方法 ==========
    
    private boolean isChat(String content) {
        return content != null && content.length() < 20 && !containsActionVerb(content);
    }
    
    private boolean containsActionVerb(String content) {
        String[] verbs = {"创建", "编写", "实现", "完成", "执行", "分析", "设计"};
        for (String verb : verbs) {
            if (content != null && content.contains(verb)) {
                return true;
            }
        }
        return false;
    }
    
    private String generatePlan(String requirement) {
        return "# 任务执行方案\n\n需求：" + requirement;
    }
    
    private java.util.List<Map<String, String>> generateTodos(String requirement) {
        return java.util.List.of(
            Map.of("id", "1", "title", "需求分析", "status", "pending"),
            Map.of("id", "2", "title", "方案设计", "status", "pending"),
            Map.of("id", "3", "title", "任务执行", "status", "pending")
        );
    }
    
    private boolean reviewPlan(String plan) {
        // TODO: 使用 AI 模型审议
        return true; // 简化处理，默认通过
    }
    
    private java.util.List<String> determineDepartments(String taskType) {
        // 根据任务类型确定执行部门
        return switch (taskType) {
            case "DOCUMENT" -> java.util.List.of("libu");
            case "DATA" -> java.util.List.of("hubu");
            case "CODE" -> java.util.List.of("bingbu");
            case "TEST" -> java.util.List.of("xingbu");
            case "INFRA" -> java.util.List.of("gongbu");
            default -> java.util.List.of("libu_hr");
        };
    }
}
