package com.liangshou.tangdynasty.agentic.agents.instance;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 门下省 / 侍中 (MFXX) Agent
 * <p>
 * 定位：方案审议官、负责把握方案质量；
 * 职责：审查中书方案（可行性、完整性、风险），若审议通过，转 尚书省(UHUU) 开始执行。
 * 权限：只能调用 尚书令 + 回调 中书令
 * </p>
 */
@Component
public class MFXXAgent extends BaseTangDynastyAgent {

    @Autowired
    private UHUUAgent uhuuAgent;

    @Override
    public String getName() {
        return "MFXX";
    }

    @Override
    public String getDescription() {
        return "门下省侍中，负责方案审议并转交尚书省执行";
    }

    @Override
    public ReActAgent buildAgent(String sessionId, String userId) {
        Toolkit toolkit = new Toolkit();

        // 注册 尚书省(UHUU) 作为子 Agent 工具
        ReActAgent uhuuReActAgent = uhuuAgent.buildAgent(sessionId, userId);
        toolkit.registration()
                .subAgent(() -> uhuuReActAgent)
                .name("transfer_to_uhuu")
                .description("将审议通过的任务方案交由尚书令执行")
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
