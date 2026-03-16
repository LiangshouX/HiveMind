package com.tangdynasty.agent.engine.core.impl;

import com.tangdynasty.agent.engine.core.AbstractAgent;
import com.tangdynasty.agent.engine.core.ProgressReport;
import com.tangdynasty.agent.engine.core.Task;
import com.tangdynasty.agent.engine.core.TaskResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 丞相 Agent - 分拣官、消息接入总负责
 * 
 * 职责：
 * 1. 识别：这是旨意还是闲聊？
 * 2. 执行：直接回复闲聊 || 建立任务→转中书
 * 3. 权限：只能调用中书省
 */
@Slf4j
public class ChengXiangAgent extends AbstractAgent {
    
    public ChengXiangAgent() {
        super("ChengXiang", "丞相");
    }
    
    @Override
    public boolean canHandle(Task task) {
        // 丞相可以处理所有新任务
        return task.getState() == Task.TaskState.PENDING || 
               task.getState() == Task.TaskState.CHENG_XIANG;
    }
    
    @Override
    public void receiveTask(Task task) {
        super.receiveTask(task);
        log.info("[丞相] 收到新任务，开始分拣...");
    }
    
    @Override
    protected TaskResult doExecute(Map<String, Object> context) throws Exception {
        // 分析任务类型
        String content = (String) context.get("content");
        
        if (isChat(content)) {
            log.info("[丞相] 判断为闲聊，直接回复");
            return TaskResult.builder()
                    .success(true)
                    .content(generateChatResponse(content))
                    .build();
        } else {
            log.info("[丞相] 判断为旨意，需要建立任务并转交中书省");
            return TaskResult.builder()
                    .success(true)
                    .content("任务已创建，将转交中书省规划")
                    .extraData(Map.of(
                        "nextOrg", "ZHONG_SHU",
                        "action", "CREATE_TASK"
                    ))
                    .build();
        }
    }
    
    /**
     * 判断是否为闲聊
     */
    private boolean isChat(String content) {
        // TODO: 使用 AI 模型判断
        // 简单规则：如果包含命令式动词或较长文本，可能是任务
        if (content == null || content.isEmpty()) {
            return true;
        }
        
        // 简短的问候、问题等视为闲聊
        return content.length() < 20 && !containsActionVerb(content);
    }
    
    /**
     * 检查是否包含动作动词
     */
    private boolean containsActionVerb(String content) {
        // 简化实现，实际应该使用 AI 语义理解
        String[] actionVerbs = {"创建", "编写", "实现", "完成", "执行", "分析", "设计"};
        for (String verb : actionVerbs) {
            if (content.contains(verb)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成闲聊回复
     */
    private String generateChatResponse(String content) {
        // TODO: 调用 AI 模型生成回复
        return "您好！我是您的 AI 助手。请问有什么可以帮助您的吗？";
    }
    
    @Override
    public ProgressReport reportProgress() {
        return ProgressReport.builder()
                .timestamp(LocalDateTime.now())
                .agentName(name)
                .state("CHENG_XIANG")
                .progressText("正在分拣消息...")
                .build();
    }
}
