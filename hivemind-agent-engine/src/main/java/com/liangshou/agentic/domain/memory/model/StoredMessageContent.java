package com.liangshou.agentic.domain.memory.model;

import com.liangshou.agentic.common.util.MessageMapper;
import lombok.*;

/**
 * 存储消息内容 - 表示单条消息中的具体内容块。
 *
 * <p>支持的内容类型包括：</p>
 * <ul>
 *     <li><b>text</b> - 普通文本内容，存储在 {@code text} 字段</li>
 *     <li><b>thinking</b> - Agent 的思考过程（思维链），存储在 {@code text} 字段</li>
 *     <li><b>tool_use</b> - 工具调用，包含 {@code name}、{@code input}（JSON）、{@code inputRaw}（原始字符串）、{@code id}</li>
 *     <li><b>tool_result</b> - 工具执行结果，包含 {@code name}、{@code text}（结果文本）、{@code id}</li>
 * </ul>
 *
 * <p>该对象由 {@link MessageMapper} 在 AgentScope Msg 和存储格式之间转换。</p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredMessageContent {

    private String type;

    private String text;

    private String name;

    private String input;

    private String inputRaw;

    private String id;

    // --- Approval 类型字段 ---

    /**
     * 审批记录 ID（仅 type="approval" 时使用）
     */
    private String approvalId;

    /**
     * 风险等级（仅 type="approval" 时使用）
     */
    private String riskLevel;

    /**
     * 审批原因（仅 type="approval" 时使用）
     */
    private String reason;

    /**
     * 审批状态：APPROVED / REJECTED（仅 type="approval" 时使用）
     */
    private String status;

    /**
     * 审批备注（仅 type="approval" 时使用）
     */
    private String reviewComment;
}
