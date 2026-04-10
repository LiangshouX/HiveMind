package com.liangshou.tangdynasty.agentic.agents;

import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService;
import org.springframework.stereotype.Service;

/**
 * Agent System Prompt 构建服务，负责生成动态的系统提示词。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>根据配置和会话上下文构建个性化的 system prompt</li>
 *   <li>注入最近会话摘要，帮助 Agent 理解当前对话背景</li>
 *   <li>注入压缩历史摘要，在长对话中保持上下文一致性</li>
 *   <li>定义 Agent 的行为准则和工具使用规范</li>
 *   <li>包含产品信息、用户信息和会话元数据</li>
 * </ul>
 * </p>
 * <p>
 * 生成的 prompt 指导 Agent 作为软件工程专家，优先提供可执行、可验证、可维护的解决方案，
 * 并遵守工具调用审批和安全策略。
 * </p>
 *
 * @author LiangshouX
 */
@Service
public class TdAgentPromptService {

    private final TdAgentProperties properties;
    private final ConversationPersistenceService persistenceService;

    /**
     * 构造 Agent System Prompt 构建服务实例。
     *
     * @param properties         Agent 外部化配置，包含系统提示词模板、产品名称等信息
     * @param persistenceService 对话持久化服务，用于加载会话历史和摘要信息
     */
    public TdAgentPromptService(
            TdAgentProperties properties,
            ConversationPersistenceService persistenceService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
    }

    /**
     * 根据会话上下文动态构建个性化的 System Prompt。
     *
     * <p>该方法生成一个完整的系统提示词，指导 Agent 的行为和响应风格。生成的 prompt 包含：</p>
     * <ul>
     *     <li><b>角色定义</b>：明确 Agent 的身份（基于 AgentScope-Java 的企业级 Java AI Agent）</li>
     *     <li><b>核心职责</b>：作为软件工程专家，提供可执行、可验证、可维护的解决方案</li>
     *     <li><b>工具使用规范</b>：合理使用沙箱工具（Python、Shell、Browser、FileSystem）</li>
     *     <li><b>上下文保持</b>：利用会话历史保持回答连续性，术语一致性</li>
     *     <li><b>安全策略</b>：遵守工具调用审批和安全策略，不得绕过 Tool Guard</li>
     *     <li><b>元数据注入</b>：产品信息、用户ID、会话ID、会话标题等</li>
     *     <li><b>最近会话摘要</b>：从持久化服务加载的最近对话预览</li>
     *     <li><b>压缩历史摘要</b>：长对话中保留的关键历史信息</li>
     * </ul>
     *
     * <p>该方法通过 {@link ConversationPersistenceService} 获取两种摘要：</p>
     * <ol>
     *     <li>{@code buildRecentPreview}：最近的对话预览，帮助 Agent 理解当前讨论主题</li>
     *     <li>{@code loadCompressedSummary}：经过压缩的历史摘要，在长对话中保持上下文一致性</li>
     * </ol>
     *
     * <p><strong>注意：</strong>如果摘要为空或 null，会自动替换为默认提示文本（"暂无历史摘要。"或"暂无压缩摘要。"）。</p>
     *
     * @param context 会话上下文，包含用户ID、会话ID、会话标题等元数据
     * @return 完整的系统提示词字符串，已填充所有占位符并格式化
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

