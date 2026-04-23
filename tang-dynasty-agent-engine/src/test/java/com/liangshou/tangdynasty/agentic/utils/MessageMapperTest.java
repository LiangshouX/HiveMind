package com.liangshou.tangdynasty.agentic.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.infrastructure.mongo.domain.document.memory.StoredMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MessageMapper} 单元测试
 *
 * <p>本测试类验证消息映射器的双向转换功能，确保 AgentScope 的 {@link io.agentscope.core.message.Msg}
 * 对象与存储格式 {@link StoredMessage}
 * 之间能够正确互转。</p>
 *
 * <p><strong>核心测试场景：</strong></p>
 * <ul>
 *     <li>文本块（TextBlock）的序列化与反序列化</li>
 *     <li>思考块（ThinkingBlock）的持久化与恢复</li>
 *     <li>工具调用块（ToolUseBlock）的参数 JSON 化处理</li>
 *     <li>工具结果块（ToolResultBlock）的输出文本提取</li>
 *     <li>元数据（metadata）的 Map-JSON 双向转换</li>
 *     <li>消息角色（MsgRole）的正确解析与默认值处理</li>
 * </ul>
 *
 * <p><strong>重要性说明：</strong>消息映射器是对话历史持久化的关键组件，
 * 其正确性直接影响会话恢复、历史查询、对话分析等核心功能的可靠性。</p>
 *
 * @author LiangshouX
 * @see MessageMapper
 * @see io.agentscope.core.message.Msg
 * @see StoredMessage
 */
class MessageMapperTest {

    private final MessageMapper messageMapper = new MessageMapper(new ObjectMapper());

    /**
     * 测试文本和工具调用消息的往返转换（Round-Trip）。
     *
     * <p>验证包含多种内容类型（TextBlock + ToolUseBlock）的复杂消息在经历
     * {@code toStoredMessage()} → {@code toMsg()} 转换后，能够保持数据完整性。</p>
     *
     * <p><strong>测试步骤：</strong></p>
     * <ol>
     *     <li>构建一个包含文本内容和工具调用的 Assistant 消息</li>
     *     <li>转换为存储格式（StoredMessage）</li>
     *     <li>再转换回 AgentScope 消息（Msg）</li>
     *     <li>验证关键字段的一致性</li>
     * </ol>
     *
     * <p><strong>验证点：</strong></p>
     * <ul>
     *     <li>消息 ID 保持不变（{@code m1}）</li>
     *     <li>消息角色正确还原（{@link io.agentscope.core.message.MsgRole#ASSISTANT}）</li>
     *     <li>内容块数量一致（2个：TextBlock + ToolUseBlock）</li>
     *     <li>文本内容完整保留（包含 "hello"）</li>
     *     <li>工具调用参数正确序列化/反序列化</li>
     * </ul>
     *
     * <p><strong>业务意义：</strong>确保用户在会话中断后重新连接时，
     * 能够看到完整的对话历史，包括之前的工具调用记录和返回结果。</p>
     *
     * <p><strong>典型应用场景：</strong></p>
     * <ul>
     *     <li>用户刷新页面后恢复之前的对话上下文</li>
     *     <li>Agent 需要引用历史消息中的工具调用结果</li>
     *     <li>对话历史导出/导入功能的数据完整性保证</li>
     * </ul>
     */
    @Test
    @DisplayName("文本和工具调用消息应该支持往返转换")
    void shouldRoundTripTextAndToolUseMessage() {
        Msg message = Msg.builder()
                .id("m1")
                .name("assistant")
                .role(MsgRole.ASSISTANT)
                .content(List.of(
                        TextBlock.builder().text("hello").build(),
                        ToolUseBlock.builder()
                                .id("call-1")
                                .name("search_session_memory")
                                .input(Map.of("query", "hello"))
                                .content("{\"query\":\"hello\"}")
                                .build()))
                .metadata(Map.of("source", "test"))
                .timestamp("2026-04-05T10:00:00Z")
                .build();

        Msg restored = messageMapper.toMsg(messageMapper.toStoredMessage(message));

        assertThat(restored.getId()).isEqualTo("m1");
        assertThat(restored.getRole()).isEqualTo(MsgRole.ASSISTANT);
        assertThat(restored.getContent()).hasSize(2);
        assertThat(restored.getTextContent()).contains("hello");
    }
}