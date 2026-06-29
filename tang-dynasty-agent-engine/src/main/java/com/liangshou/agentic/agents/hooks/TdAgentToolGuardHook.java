package com.liangshou.agentic.agents.hooks;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.guard.ToolGuardDecision;
import com.liangshou.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolUseBlock;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Agent 工具防护 Hook - 在推理后对工具调用进行安全检查和审批控制。
 *
 * <p><b>AgentScope Hook 机制说明：</b></p>
 * <p>根据 <a href="https://java.agentscope.io/zh/task/hook.html">AgentScope-Java Hook 官方文档</a>，
 * Hook 是 AgentScope 提供的生命周期拦截机制，允许开发者在 Agent 执行的关键节点插入自定义逻辑。
 * Hook 通过 {@link io.agentscope.core.hook.Hook#onEvent(HookEvent)} 方法监听特定事件，
 * 并可以修改事件中的数据或控制 Agent 的执行流程。</p>
 *
 * <p><b>本 Hook 的作用时机：</b></p>
 * <p>本 Hook 监听 {@link PostReasoningEvent} 事件，该事件在 Agent 完成推理（LLM 返回结果）之后触发。
 * 在这个时机点，Agent 已经生成了包含工具调用指令的响应，但尚未实际执行这些工具。
 * 这是进行安全检查、风险控制和人工审批的理想时机。</p>
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 *     <li><b>工具调用风险评估</b>：通过 {@link ToolGuardEngine#evaluate} 对每个工具调用进行安全评估，
 *         检查是否包含危险命令、敏感路径访问等高风险操作</li>
 *     <li><b>自动拒绝危险操作</b>：如果工具调用被判定为不允许执行（如包含 {@code rm -rf} 等危险命令），
 *         直接替换推理结果为拒绝消息，阻止工具执行</li>
 *     <li><b>人工审批流程</b>：对于需要审批的高风险工具（如文件写入、Shell 命令执行等），
 *         创建待审批记录并暂停 Agent 执行，等待用户确认</li>
 *     <li><b>审批状态管理</b>：通过 {@link ToolApprovalService} 管理审批记录的创建、查询和状态更新</li>
 * </ul>
 *
 * <p><b>工作流程：</b></p>
 * <ol>
 *     <li>Agent 完成推理，生成包含 {@link ToolUseBlock} 的响应消息</li>
 *     <li>触发 {@link PostReasoningEvent} 事件，本 Hook 的 {@link #onEvent} 方法被调用</li>
 *     <li>提取响应中的所有工具调用指令（{@link ToolUseBlock} 列表）</li>
 *     <li>对每个工具调用执行安全评估：
 *         <ul>
 *             <li>检查是否命中危险命令模式（如 {@code rm -rf}、{@code format}、{@code shutdown} 等）</li>
 *             <li>在严格模式下检查是否访问敏感路径（如 {@code .env}、{@code .git}、{@code id_rsa} 等）</li>
 *             <li>识别是否需要人工审批的工具（如 {@code run_shell_command}、{@code fs_write_file} 等）</li>
 *         </ul>
 *     </li>
 *     <li>如果存在被拒绝的工具调用：
 *         <ul>
 *             <li>构造拒绝消息，说明被拒绝的原因</li>
 *             <li>通过 {@link PostReasoningEvent#setReasoningMessage} 替换原始推理结果</li>
 *             <li>Agent 将拒绝消息返回给用户，不执行任何工具</li>
 *         </ul>
 *     </li>
 *     <li>如果存在需要审批的工具调用：
 *         <ul>
 *             <li>通过 {@link ToolApprovalService#createPendingApprovals} 创建待审批记录</li>
 *             <li>调用 {@link PostReasoningEvent#stopAgent} 暂停 Agent 执行</li>
 *             <li>前端收到 {@code APPROVAL_REQUIRED} 事件，展示审批界面给用户</li>
 *             <li>用户批准后，Agent 从暂停状态恢复，继续执行工具调用</li>
 *         </ul>
 *     </li>
 *     <li>如果所有工具调用都通过安全检查，Agent 正常执行工具调用</li>
 * </ol>
 *
 * <p><b>优先级设置：</b></p>
 * <p>本 Hook 的优先级为 20（通过 {@link #priority()} 方法返回）。在 AgentScope 中，
 * 优先级数值越小，Hook 越先执行。这个优先级确保了记忆压缩 Hook（优先级 10）在本 Hook 之前执行，
 * 保证工具防护基于优化后的上下文进行评估。</p>
 *
 * <p><b>配置驱动：</b></p>
 * <p>工具防护行为由 {@link com.liangshou.agentic.common.config.TdAgentProperties.ToolGuard} 配置控制：
 * <ul>
 *     <li>{@code enabled} - 是否启用工具防护（默认 true）</li>
 *     <li>{@code strictMode} - 是否启用严格模式，严格模式下会检查敏感路径访问（默认 false）</li>
 *     <li>{@code pendingExpireMinutes} - 待审批记录的有效期（分钟），超时后自动失效（默认 30）</li>
 * </ul>
 * </p>
 *
 * <p><b>使用场景：</b></p>
 * <ul>
 *     <li>安全防护：防止 Agent 执行危险命令（如删除系统文件、格式化磁盘等）</li>
 *     <li>权限控制：对敏感操作（如文件写入、网络请求）要求人工审批</li>
 *     <li>审计追踪：记录所有工具调用的审批历史，便于事后审计</li>
 *     <li>风险控制：在多租户环境中，防止 Agent 越权访问其他用户的数据</li>
 * </ul>
 *
 * @author LiangshouX
 * @see io.agentscope.core.hook.Hook
 * @see io.agentscope.core.hook.PostReasoningEvent
 * @see ToolGuardEngine
 * @see ToolApprovalService
 */
public class TdAgentToolGuardHook implements Hook {

    private final ConversationSessionContext context;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;

    /**
     * 构造工具防护 Hook。
     *
     * @param context             会话上下文，包含用户 ID、会话 ID 等身份信息
     * @param toolGuardEngine     工具防护引擎，负责评估工具调用的安全风险
     * @param toolApprovalService 工具审批服务，管理审批记录的创建和状态更新
     */
    public TdAgentToolGuardHook(
            ConversationSessionContext context,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService) {
        this.context = context;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
    }

    /**
     * 处理 Hook 事件，在推理后对工具调用进行安全检查和审批控制。
     *
     * <p>该方法实现了 {@link Hook#onEvent(HookEvent)} 接口，是 Hook 的核心逻辑入口。</p>
     *
     * <p><b>处理流程：</b></p>
     * <ol>
     *     <li>检查事件类型是否为 {@link PostReasoningEvent}，如果不是则直接透传事件</li>
     *     <li>从推理结果中提取所有 {@link ToolUseBlock}（工具调用指令）</li>
     *     <li>对每个工具调用执行安全评估，生成 {@link ToolGuardDecision} 决策列表</li>
     *     <li>检查是否存在被拒绝的工具调用：
     *         <ul>
     *             <li>如果存在，构造拒绝消息并替换原始推理结果</li>
     *             <li>通过 {@link PostReasoningEvent#setReasoningMessage} 设置新的推理消息</li>
     *             <li>返回修改后的事件，阻止工具执行</li>
     *         </ul>
     *     </li>
     *     <li>检查是否存在需要审批的工具调用：
     *         <ul>
     *             <li>如果存在，调用 {@link ToolApprovalService#createPendingApprovals} 创建待审批记录</li>
     *             <li>通过 {@link PostReasoningEvent#stopAgent} 暂停 Agent 执行</li>
     *             <li>等待用户在前端界面进行审批操作</li>
     *         </ul>
     *     </li>
     *     <li>如果所有工具调用都通过安全检查，返回原事件，Agent 继续正常执行工具调用</li>
     * </ol>
     *
     * <p><b>响应式编程模型：</b></p>
     * <p>该方法返回 {@link Mono<T>}，符合 AgentScope 的响应式编程范式。
     * 由于工具防护是同步操作，直接使用 {@link Mono#just(Object)} 包装返回的事件即可。</p>
     *
     * <p><b>关键 API 使用：</b></p>
     * <ul>
     *     <li>{@link PostReasoningEvent#getReasoningMessage()} - 获取 Agent 的推理结果</li>
     *     <li>{@link PostReasoningEvent#setReasoningMessage(Msg)} - 替换推理结果（用于拒绝消息）</li>
     *     <li>{@link PostReasoningEvent#stopAgent()} - 暂停 Agent 执行（用于等待审批）</li>
     *     <li>{@link io.agentscope.core.message.Msg#hasContentBlocks(Class)} - 检查是否包含特定类型的内容块</li>
     *     <li>{@link io.agentscope.core.message.Msg#getContentBlocks(Class)} - 提取指定类型的内容块列表</li>
     * </ul>
     *
     * @param event Hook 事件，可能是任意类型的 {@link HookEvent}
     * @param <T>   事件类型，由 AgentScope 框架推断
     * @return 修改后的事件，包裹在 {@link Mono} 中
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (!(event instanceof PostReasoningEvent postReasoningEvent)) {
            return Mono.just(event);
        }
        Msg reasoningMessage = postReasoningEvent.getReasoningMessage();
        if (reasoningMessage == null || !reasoningMessage.hasContentBlocks(ToolUseBlock.class)) {
            return Mono.just(event);
        }
        List<ToolUseBlock> toolUses = reasoningMessage.getContentBlocks(ToolUseBlock.class);
        List<Map.Entry<ToolUseBlock, ToolGuardDecision>> decisions =
                toolUses.stream()
                        .map(toolUse -> Map.entry(toolUse, toolGuardEngine.evaluate(toolUse.getName(), toolUse.getInput())))
                        .toList();
        List<Map.Entry<ToolUseBlock, ToolGuardDecision>> denied =
                decisions.stream().filter(entry -> !entry.getValue().isAllowed()).toList();
        if (!denied.isEmpty()) {
            String denialText =
                    denied.stream()
                            .map(
                                    entry ->
                                            "工具 "
                                                    + entry.getKey().getName()
                                                    + " 已被拒绝："
                                                    + entry.getValue().getReason())
                            .reduce((left, right) -> left + System.lineSeparator() + right)
                            .orElse("工具调用已被安全策略拒绝。");
            postReasoningEvent.setReasoningMessage(
                    Msg.builder().role(MsgRole.ASSISTANT).textContent(denialText).build());
            return Mono.just(event);
        }
        List<Map.Entry<ToolUseBlock, ToolGuardDecision>> approvals =
                decisions.stream().filter(entry -> entry.getValue().isRequiresApproval()).toList();
        if (!approvals.isEmpty()) {
            toolApprovalService.createPendingApprovals(
                    context,
                    approvals.stream().map(Map.Entry::getKey).toList(),
                    toolUse ->
                            approvals.stream()
                                    .filter(entry -> entry.getKey().getId().equals(toolUse.getId()))
                                    .map(Map.Entry::getValue)
                                    .findFirst()
                                    .orElse(toolGuardEngine.evaluate(toolUse.getName(), toolUse.getInput())));
            postReasoningEvent.stopAgent();
        }
        return Mono.just(event);
    }

    /**
     * 获取 Hook 的优先级。
     *
     * <p>在 AgentScope 中，Hook 按照优先级从低到高的顺序执行（数值越小，优先级越高）。</p>
     *
     * <p>本 Hook 返回优先级 20，确保在记忆压缩 Hook（优先级 10）之后执行，
     * 这样可以在优化后的上下文基础上进行工具调用评估。</p>
     *
     * @return Hook 优先级，数值为 20
     */
    @Override
    public int priority() {
        return 20;
    }
}
