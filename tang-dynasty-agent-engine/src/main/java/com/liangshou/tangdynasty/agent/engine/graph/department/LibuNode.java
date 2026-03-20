package com.tangdynasty.agent.engine.graph.department;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.GraphState;
import com.tangdynasty.agent.engine.graph.base.AbstractDepartmentNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 礼部 - 文档编制官
 * 
 * 职责：负责文档编写、报告生成、文书处理等
 */
@Slf4j
@Component
public class LibuNode extends AbstractDepartmentNode {
    
    private final DashScopeChatModel chatModel;
    
    public LibuNode(DashScopeChatModel chatModel) {
        super("礼部");
        this.chatModel = chatModel;
    }
    
    @Override
    protected DepartmentResult doExecute(GraphState state) throws Exception {
        try {
            // 获取任务信息
            String taskTitle = (String) state.value("taskTitle").orElse("未命名任务");
            String plan = (String) state.value("plan").orElse("");
            
            // 调用 AI 模型生成文档
            String documentPrompt = """
                你是礼部官员，负责文档编制。请根据以下信息生成一份专业的文档：
                
                任务标题：%s
                执行方案：%s
                
                要求：
                1. 文档结构清晰，包含标题、摘要、正文、结论
                2. 使用正式的公文语言
                3. 重点突出，逻辑严密
                4. 字数控制在 800-1500 字
                """.formatted(taskTitle, plan);
            
            String document = callAIModel(documentPrompt);
            
            return DepartmentResult.success(document);
            
        } catch (Exception e) {
            log.error("[礼部] 文档编制失败", e);
            return DepartmentResult.failure("文档编制失败：" + e.getMessage());
        }
    }
    
    /**
     * 调用 AI 模型
     */
    private String callAIModel(String prompt) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("你是唐朝礼部官员，擅长文书写作。"));
        messages.add(new UserMessage(prompt));
        
        return chatModel.call(new Prompt(messages)).getResult().getOutput().getText();
    }
}
