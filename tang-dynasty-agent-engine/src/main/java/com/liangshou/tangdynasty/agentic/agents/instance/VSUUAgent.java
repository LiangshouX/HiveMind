package com.liangshou.tangdynasty.agentic.agents.instance;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 中书令 (VSUU) Agent
 * <p>
 * 定位：任务规划官，负责针对任务方案的起草；
 * 职责：接旨后分析需求，拆解为子任务（todos），交由 门下省(MFXX) 审议；
 * 权限：只能调用 门下省
 * </p>
 */
@Component
public class VSUUAgent extends BaseTangDynastyAgent {

    @Autowired
    private MFXXAgent mfxxAgent;

    @Override
    public String getName() {
        return "VSUU";
    }

    @Override
    public String getDescription() {
        return "中书令，负责起草任务方案并提交门下省审议";
    }

    @Override
    public ReActAgent buildAgent(String sessionId, String userId) {
        Toolkit toolkit = new Toolkit();

        // 注册 门下省(MFXX) 作为子 Agent 工具
        ReActAgent mfxxReActAgent = mfxxAgent.buildAgent(sessionId, userId);
        toolkit.registration()
                .subAgent(() -> mfxxReActAgent)
                .name("transfer_to_mfxx")
                .description("将起草好的任务方案提交门下省审议")
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
