package com.liangshou.tangdynasty.agentic.agents.instance;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 宰相 (ZDXL) Agent
 * <p>
 * 定位：消息分拣官，负责消息的接收、处理和分配
 * 职责：首先识别皇上的输入是任务旨意还是闲聊，闲聊则直接回复，任务（旨意）则整理任务实体转中书省(VSUU)处理；
 * 权限：只能调用 中书令
 * </p>
 */
@Component
public class ZDXLAgent extends BaseTangDynastyAgent {

    @Autowired
    private VSUUAgent vsuuAgent;

    @Override
    public String getName() {
        return "ZDXL";
    }

    @Override
    public String getDescription() {
        return "宰相，负责消息分拣，闲聊直接回复，任务转交中书省";
    }

    @Override
    public ReActAgent buildAgent(String sessionId, String userId) {
        Toolkit toolkit = new Toolkit();

        // 注册 中书省(VSUU) 作为子 Agent 工具
        ReActAgent vsuuReActAgent = vsuuAgent.buildAgent(sessionId, userId);
        toolkit.registration()
                .subAgent(() -> vsuuReActAgent)
                .name("transfer_to_vsuu")
                .description("将明确的旨意或复杂任务转交中书省处理")
                .apply();

        ReActAgent.Builder builder = ReActAgent.builder()
                .name(getName())
                .sysPrompt(getSystemPrompt())
                .toolkit(toolkit)
                .model(createModel());

        // Memory
        if (sessionHistoryService != null) {
            builder.memory(new MemoryAdapter(sessionHistoryService, userId, sessionId));
        }

        // Long Term Memory
        if (reMeLongTermMemoryAdapter != null) {
            builder.longTermMemory(reMeLongTermMemoryAdapter.create(userId))
                   .longTermMemoryMode(LongTermMemoryMode.BOTH);
        } else if (memoryService != null) {
            builder.longTermMemory(new LongTermMemoryAdapter(memoryService, userId, sessionId))
                   .longTermMemoryMode(LongTermMemoryMode.BOTH);
        }

        return builder.build();
    }
}
