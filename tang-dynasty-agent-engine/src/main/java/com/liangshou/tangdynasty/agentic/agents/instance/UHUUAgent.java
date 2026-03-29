package com.liangshou.tangdynasty.agentic.agents.instance;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.sandbox.box.Sandbox;
import org.springframework.stereotype.Component;

/**
 * 尚书令 (UHUU) Agent
 * 职责：任务派发官、执行总指挥，负责连接沙箱执行具体工具操作。
 */
@Component
public class UHUUAgent extends BaseTangDynastyAgent {

    @Override
    public String getName() {
        return "UHUU";
    }

    @Override
    public String getDescription() {
        return "尚书令：任务派发官、执行总指挥，负责具体任务执行与沙箱操作";
    }

    @Override
    public ReActAgent buildAgent(String sessionId, String userId) {
        Toolkit toolkit = new Toolkit();

        // 连接沙箱并注册工具
        if (sandboxService != null) {
            Sandbox sandbox = createSandbox(sessionId, userId);
            if (sandbox != null) {
                // 根据需求注册具体的沙箱工具
                toolkit.registerTool(ToolkitInit.BrowserNavigateTool(sandbox));
                // 如果需要其他工具如 Python 执行等，可在此添加
            }
        }

        ReActAgent.Builder builder = ReActAgent.builder()
                .name(getName())
                .sysPrompt(getSystemPrompt())
                .toolkit(toolkit)
                .model(createModel());

        // 短期记忆（MongoDB Session History）
        if (sessionHistoryService != null) {
            builder.memory(new MemoryAdapter(sessionHistoryService, userId, sessionId));
        }

        // 长期记忆（ReMe）
        if (reMeLongTermMemoryAdapter != null) {
            // Note: If memoryService is also needed by ReMe adapter, we can wrap it, 
            // but the ReMeLongTermMemoryAdapter creates ReMeLongTermMemory directly
            builder.longTermMemory(reMeLongTermMemoryAdapter.create(userId))
                   .longTermMemoryMode(LongTermMemoryMode.BOTH);
        } else if (memoryService != null) {
            // 回退到默认的长记忆
            builder.longTermMemory(new LongTermMemoryAdapter(memoryService, userId, sessionId))
                   .longTermMemoryMode(LongTermMemoryMode.BOTH);
        }

        return builder.build();
    }
}
