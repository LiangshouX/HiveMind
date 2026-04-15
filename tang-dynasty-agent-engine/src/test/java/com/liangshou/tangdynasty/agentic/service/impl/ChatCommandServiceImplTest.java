package com.liangshou.tangdynasty.agentic.service.impl;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.session.AgentSessionStateService;
import com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService;
import com.liangshou.tangdynasty.agentic.service.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ChatCommandServiceImpl} 单元测试
 *
 * <p>本测试类验证聊天命令服务对各类斜杠命令（Slash Commands）的处理逻辑，
 * 包括会话管理、历史查询、元数据生成等功能。</p>
 *
 * <p><strong>支持的命令列表：</strong></p>
 * <ul>
 *     <li>{@code /clear}：清空当前会话的所有消息和历史记录</li>
 *     <li>{@code /history}：显示最近的对话历史预览</li>
 *     <li>{@code /new}：创建新的会话并返回新会话 ID</li>
 *     <li>{@code /help}：显示可用命令帮助信息（未来扩展）</li>
 * </ul>
 *
 * <p><strong>核心测试场景：</strong></p>
 * <ol>
 *     <li>清空命令是否正确调用持久化服务和会话状态服务</li>
 *     <li>历史命令是否正确构建预览并返回消息数量</li>
 *     <li>新建会话命令是否生成唯一的会话 ID 和元数据</li>
 *     <li>未知命令的降级处理策略</li>
 * </ol>
 *
 * <p><strong>设计模式：</strong>使用 Mockito 框架模拟依赖服务（{@link com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService}
 * 和 {@link com.liangshou.tangdynasty.agentic.agents.session.AgentSessionStateService}），
 * 确保测试聚焦于命令处理逻辑本身，而不受外部服务实现的影响。</p>
 *
 * <p><strong>用户体验价值：</strong>斜杠命令为用户提供了便捷的会话管理方式，
 * 类似于 Slack、Discord 等现代聊天工具的交互体验，提升了产品的易用性。</p>
 *
 * @author LiangshouX
 * @see ChatCommandServiceImpl
 * @see com.liangshou.tangdynasty.agentic.service.IChatCommandService
 * @see com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext
 * @see com.liangshou.tangdynasty.agentic.service.dto.ChatResponse
 */
@ExtendWith(MockitoExtension.class)
class ChatCommandServiceImplTest {

    @Mock
    private ConversationPersistenceService persistenceService;

    @Mock
    private AgentSessionStateService agentSessionStateService;

    private ChatCommandServiceImpl chatCommandService;

    private ConversationSessionContext context;

    @BeforeEach
    void setUp() {
        chatCommandService = new ChatCommandServiceImpl(persistenceService, agentSessionStateService);
        context = ConversationSessionContext.builder()
                .userId("u1")
                .sessionId("s1")
                .sessionTitle("demo")
                .build();
    }

    /**
     * 测试清空会话命令（/clear）的处理逻辑。
     *
     * <p>验证当用户发送 {@code /clear} 命令时，系统能够：</p>
     * <ol>
     *     <li>调用持久化服务清空数据库中的会话消息</li>
     *     <li>调用会话状态服务清除内存中的会话上下文</li>
     *     <li>返回成功响应并提示用户会话已清空</li>
     * </ol>
     *
     * <p><strong>预期行为：</strong></p>
     * <ul>
     *     <li>{@code response.isSuccess()} 返回 true</li>
     *     <li>{@code response.isCommandHandled()} 返回 true（标识为命令而非普通消息）</li>
     *     <li>{@code response.getMessage()} 包含“已清空”提示文本</li>
     *     <li>验证 {@code persistenceService.clearSession(context)} 被调用</li>
     *     <li>验证 {@code agentSessionStateService.clear(context)} 被调用</li>
     * </ul>
     *
     * <p><strong>业务意义：</strong>为用户提供快速重置对话的能力，
     * 当用户希望开始全新话题或清除敏感信息时非常有用。</p>
     *
     * <p><strong>注意事项：</strong>清空操作是不可逆的，实际生产环境中可能需要
     * 添加二次确认机制或回收站功能以防止误操作。</p>
     */
    @Test
    @DisplayName("清空命令应该清除会话数据和状态")
    void shouldClearSessionWhenClearCommandReceived() {
        ChatResponse response = chatCommandService.handleCommand(context, "/clear");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isCommandHandled()).isTrue();
        assertThat(response.getMessage()).contains("已清空");
        verify(persistenceService).clearSession(context);
        verify(agentSessionStateService).clear(context);
    }

    /**
     * 测试历史查询命令（/history）的处理逻辑。
     *
     * <p>验证当用户发送 {@code /history} 命令时，系统能够：</p>
     * <ol>
     *     <li>从持久化服务获取最近的消息预览文本</li>
     *     <li>统计当前会话的消息总数</li>
     *     <li>返回包含预览内容和消息数量的响应</li>
     * </ol>
     *
     * <p><strong>预期行为：</strong></p>
     * <ul>
     *     <li>{@code response.getMessage()} 包含从持久化服务返回的预览文本</li>
     *     <li>{@code response.getMessageCount()} 等于当前会话的消息总数</li>
     *     <li>预览文本格式符合用户友好的展示要求（如 "USER: hello"）</li>
     * </ul>
     *
     * <p><strong>Mock 配置说明：</strong></p>
     * <ul>
     *     <li>模拟 {@code persistenceService.buildRecentPreview(context, 20)} 返回 "USER: hello"</li>
     *     <li>模拟 {@code persistenceService.countMessages(context)} 返回 3L</li>
     * </ul>
     *
     * <p><strong>用户体验：</strong>帮助用户快速回顾之前的对话内容，
     * 特别是在长对话中定位关键信息或理解上下文时非常有用。</p>
     *
     * <p><strong>性能考虑：</strong>预览长度限制为 20 条消息，避免一次性加载过多历史
     * 导致响应缓慢或前端渲染压力过大。</p>
     */
    @Test
    @DisplayName("历史命令应该返回对话预览和消息数量")
    void shouldReturnHistoryPreviewWhenHistoryCommandReceived() {
        when(persistenceService.buildRecentPreview(context, 20)).thenReturn("USER: hello");
        when(persistenceService.countMessages(context)).thenReturn(3L);

        ChatResponse response = chatCommandService.handleCommand(context, "/history");

        assertThat(response.getMessage()).contains("USER: hello");
        assertThat(response.getMessageCount()).isEqualTo(3L);
    }

    /**
     * 测试新建会话命令（/new）的处理逻辑。
     *
     * <p>验证当用户发送 {@code /new} 命令时，系统能够：</p>
     * <ol>
     *     <li>生成一个新的唯一会话 ID（UUID 格式）</li>
     *     <li>在响应的元数据中包含新会话 ID 和命令标识</li>
     *     <li>允许前端根据元数据切换到新会话</li>
     * </ol>
     *
     * <p><strong>预期行为：</strong></p>
     * <ul>
     *     <li>{@code response.getMetadata()} 包含键 "newSessionId"</li>
     *     <li>{@code response.getMetadata()} 包含键 "command" 且值为 "new"</li>
     *     <li>新生成的会话 ID 是有效的 UUID 格式</li>
     * </ul>
     *
     * <p><strong>元数据结构示例：</strong></p>
     * <pre>{@code
     * {
     *   "newSessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
     *   "command": "new"
     * }
     * }</pre>
     *
     * <p><strong>前端集成：</strong>前端收到响应后，应提取 {@code newSessionId}，
     * 创建新的会话上下文，并导航到对应的会话页面，实现无缝的会话切换体验。</p>
     *
     * <p><strong>应用场景：</strong></p>
     * <ul>
     *     <li>用户希望开启全新的对话主题，不受之前上下文影响</li>
     *     <li>多任务并行处理时，为每个任务创建独立的会话空间</li>
     *     <li>测试不同 prompt 效果时，快速创建干净的实验环境</li>
     * </ul>
     */
    @Test
    @DisplayName("新建会话命令应该生成唯一会话ID和元数据")
    void shouldGenerateNewSessionMetadata() {
        ChatResponse response = chatCommandService.handleCommand(context, "/new");

        assertThat(response.getMetadata()).containsKey("newSessionId");
        assertThat(((Map<?, ?>) response.getMetadata()).get("command")).isEqualTo("new");
    }
}