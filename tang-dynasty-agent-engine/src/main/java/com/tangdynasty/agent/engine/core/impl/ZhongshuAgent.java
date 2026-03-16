package com.tangdynasty.agent.engine.core.impl;

import com.tangdynasty.agent.engine.core.AbstractAgent;
import com.tangdynasty.agent.engine.core.ProgressReport;
import com.tangdynasty.agent.engine.core.Task;
import com.tangdynasty.agent.engine.core.TaskResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 中书省 Agent - 规划官、方案起草总负责
 * 
 * 职责：
 * 1. 接旨后分析需求
 * 2. 拆解为子任务（todos）
 * 3. 调用门下省审议 OR 尚书省咨询
 * 4. 权限：只能调用门下 + 尚书
 */
@Slf4j
public class ZhongshuAgent extends AbstractAgent {
    
    public ZhongshuAgent() {
        super("Zhongshu", "中书省");
    }
    
    @Override
    public boolean canHandle(Task task) {
        return task.getState() == Task.TaskState.CHENG_XIANG || 
               task.getState() == Task.TaskState.ZHONG_SHU ||
               task.getState() == Task.TaskState.MEN_XIA; // 封驳后重新规划
    }
    
    @Override
    public void receiveTask(Task task) {
        super.receiveTask(task);
        log.info("[中书省] 收到任务，开始规划方案...");
    }
    
    @Override
    protected TaskResult doExecute(Map<String, Object> context) throws Exception {
        // 分析需求
        String requirement = (String) context.get("requirement");
        log.info("[中书省] 分析需求：{}", requirement);
        
        // 拆解为子任务
        List<Map<String, String>> todos = analyzeAndDecompose(requirement);
        log.info("[中书省] 拆解为 {} 个子任务", todos.size());
        
        // 生成方案
        String plan = generatePlan(requirement, todos);
        
        // 提交门下省审议
        return TaskResult.builder()
                .success(true)
                .content(plan)
                .extraData(Map.of(
                    "nextOrg", "MEN_XIA",
                    "action", "REVIEW",
                    "todos", todos
                ))
                .build();
    }
    
    /**
     * 分析并拆解任务
     */
    private List<Map<String, String>> analyzeAndDecompose(String requirement) {
        // TODO: 使用 AI 模型进行任务拆解
        // 这里返回示例数据
        return List.of(
            Map.of("id", "1", "title", "需求分析", "status", "completed"),
            Map.of("id", "2", "title", "方案设计", "status", "in-progress"),
            Map.of("id", "3", "title", "等待审议", "status", "not-started")
        );
    }
    
    /**
     * 生成执行方案
     */
    private String generatePlan(String requirement, List<Map<String, String>> todos) {
        // TODO: 使用 AI 模型生成详细方案
        StringBuilder plan = new StringBuilder();
        plan.append("# 任务执行方案\n\n");
        plan.append("## 需求概述\n");
        plan.append(requirement).append("\n\n");
        plan.append("## 执行步骤\n");
        
        for (Map<String, String> todo : todos) {
            plan.append("- ").append(todo.get("title")).append("\n");
        }
        
        return plan.toString();
    }
    
    @Override
    public ProgressReport reportProgress() {
        return ProgressReport.builder()
                .timestamp(LocalDateTime.now())
                .agentName(name)
                .state("ZHONG_SHU")
                .progressText("正在规划方案...")
                .todos(List.of(
                    new ProgressReport.TodoItem("1", "需求分析", "completed"),
                    new ProgressReport.TodoItem("2", "方案设计", "in-progress"),
                    new ProgressReport.TodoItem("3", "等待审议", "not-started")
                ))
                .build();
    }
}
