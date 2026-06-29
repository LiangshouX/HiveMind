package com.liangshou.agentic.common.enums;

/**
 * 流式事件类型枚举 - 定义 Agent 流式响应中的事件类型。
 *
 * <p>支持的事件类型包括：</p>
 * <ul>
 *     <li><b>MESSAGE</b> - 普通文本消息片段</li>
 *     <li><b>REASONING</b> - Agent 的思考过程（思维链）</li>
 *     <li><b>TOOL_RESULT</b> - 工具执行结果</li>
 *     <li><b>RESULT</b> - 最终响应结果</li>
 *     <li><b>APPROVAL_REQUIRED</b> - 需要用户审批工具执行</li>
 *     <li><b>ERROR</b> - 错误信息</li>
 *     <li><b>DONE</b> - 流式响应结束标记</li>
 * </ul>
 *
 * <p>这些事件类型用于前端区分不同类型的流式数据，实现差异化的 UI 展示。</p>
 *
 * @author LiangshouX
 */
public enum TdAgentStreamEventType {

    MESSAGE,

    REASONING,

    TOOL_RESULT,

    RESULT,

    APPROVAL_REQUIRED,

    ERROR,

    DONE
}

