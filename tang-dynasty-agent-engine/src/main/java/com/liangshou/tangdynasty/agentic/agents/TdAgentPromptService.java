package com.liangshou.tangdynasty.agentic.agents;

import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService;
import org.springframework.stereotype.Service;

/**
 * @author LiangshouX
 */
@Service
public class TdAgentPromptService {

    private final TdAgentProperties properties;
    private final ConversationPersistenceService persistenceService;

    /**
     * 执行相关操作。
     * @param properties 外部化配置
     * @param persistenceService 持久化服务
     */
    public TdAgentPromptService(
            TdAgentProperties properties,
            ConversationPersistenceService persistenceService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
    }

    /**
     * 构建 system prompt。
     * @param context 会话上下文
     * @return 返回结果
     */
    public String buildPrompt(ConversationSessionContext context) {
        String preview =
                persistenceService.buildRecentPreview(
                        context, properties.getSystemPrompt().getMaxHistoryPreview());
        String compressedSummary = persistenceService.loadCompressedSummary(context);
        return """
                你是 %s，一个基于 AgentScope-Java 构建的企业级 Java AI Agent。
                你的职责：
                1. 作为成熟的软件工程 Agent，优先给出可执行、可验证、可维护的解决方案。
                2. 能够合理选择工具，尤其是沙箱中的 Python、Shell、Browser、FileSystem 能力。
                3. 充分利用当前 session 的历史上下文，保持回答连续性。
                4. 对需要多步骤推理的任务，先拆解再执行，输出要准确、专业、简洁。

                当前产品归属：%s
                当前用户：%s
                当前会话：%s
                当前标题：%s

                额外要求：
                - 优先使用已注册工具完成需要外部信息、代码执行、浏览器操作或文件操作的任务。
                - 当用户请求代码、架构或故障排查时，先分析再回答。
                - 如果当前会话已有历史，请保持术语、上下文与先前对话一致。
                - 对高风险工具调用，必须遵守审批与安全策略，不得绕过 Tool Guard。
                - 当历史上下文较长时，允许依赖自动压缩摘要继续保持上下文一致性。

                最近会话摘要：
                %s

                已压缩历史摘要：
                %s
                """
                .formatted(
                        properties.getSystemPrompt().getProductName(),
                        properties.getSystemPrompt().getOwnerName(),
                        context.getUserId(),
                        context.getSessionId(),
                        context.getSessionTitle() == null ? "" : context.getSessionTitle(),
                        preview == null || preview.isBlank() ? "暂无历史摘要。" : preview,
                        compressedSummary == null || compressedSummary.isBlank()
                                ? "暂无压缩摘要。"
                                : compressedSummary);
    }
}

