package com.liangshou.tangdynasty.agentic.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.domain.document.StoredMessage;
import com.liangshou.tangdynasty.agentic.domain.document.StoredMessageContent;
import io.agentscope.core.message.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 消息映射器 - 在 AgentScope Msg 对象和存储格式之间进行转换。
 *
 * <p>该组件提供双向转换功能：</p>
 * <ul>
 *     <li><b>toStoredMessage</b>：将 AgentScope 的 {@link io.agentscope.core.message.Msg} 转换为 {@link StoredMessage}</li>
 *     <li><b>toMsg</b>：将 {@link StoredMessage} 转换回 AgentScope 的 {@link io.agentscope.core.message.Msg}</li>
 * </ul>
 *
 * <p>支持的内容类型转换：</p>
 * <ul>
 *     <li>{@link io.agentscope.core.message.TextBlock} ↔ text 类型</li>
 *     <li>{@link io.agentscope.core.message.ThinkingBlock} ↔ thinking 类型</li>
 *     <li>{@link io.agentscope.core.message.ToolUseBlock} ↔ tool_use 类型</li>
 *     <li>{@link io.agentscope.core.message.ToolResultBlock} ↔ tool_result 类型</li>
 * </ul>
 *
 * <p>序列化策略：</p>
 * <ul>
 *     <li>复杂对象（如 metadata、tool input）使用 Jackson ObjectMapper 序列化为 JSON 字符串</li>
 *     <li>反序列化失败时抛出 IllegalStateException，确保数据完整性</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Component
public class MessageMapper {

    private final ObjectMapper objectMapper;

    /**
     * 构造消息映射器实例。
     *
     * @param objectMapper Jackson JSON 序列化/反序列化工具，用于处理复杂对象的 JSON 转换
     */
    public MessageMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 AgentScope 的 Msg 对象转换为可存储的 StoredMessage 格式。
     *
     * <p>该方法执行以下转换操作：</p>
     * <ol>
     *     <li>提取消息的基本属性（ID、名称、角色、时间戳）</li>
     *     <li>遍历消息内容块列表，将每个 {@link ContentBlock} 转换为 {@link StoredMessageContent}</li>
     *     <li>将 metadata 元数据序列化为 JSON 字符串</li>
     * </ol>
     *
     * <p><strong>支持的内容类型转换：</strong></p>
     * <ul>
     *     <li>{@link TextBlock} → type="text"，存储文本内容</li>
     *     <li>{@link ThinkingBlock} → type="thinking"，存储思考过程</li>
     *     <li>{@link ToolUseBlock} → type="tool_use"，存储工具名称、输入参数（JSON）、原始内容和工具ID</li>
     *     <li>{@link ToolResultBlock} → type="tool_result"，存储工具名称、输出文本、元数据（JSON）和工具ID</li>
     *     <li>其他未知类型 → 使用类名作为 type，将整个对象序列化为 inputRaw</li>
     * </ul>
     *
     * <p><strong>角色处理：</strong></p>
     * <p>如果消息角色为 null，则存储为 null；否则转换为字符串（如 "USER"、"ASSISTANT"、"SYSTEM"）。</p>
     *
     * <p><strong>典型使用场景：</strong></p>
     * <ul>
     *     <li>Agent 生成新消息后，持久化到 MongoDB 之前进行转换</li>
     *     <li>备份或导出对话历史时序列化消息数据</li>
     * </ul>
     *
     * @param message AgentScope 的消息对象，包含完整的对话内容
     * @return 转换后的存储格式消息，可直接保存到数据库
     * @throws IllegalStateException 如果 metadata 或工具输入参数序列化失败
     */
    public StoredMessage toStoredMessage(Msg message) {
        return StoredMessage.builder()
                .msgId(message.getId())
                .name(message.getName())
                .role(message.getRole() == null ? null : message.getRole().name())
                .content(message.getContent().stream().map(this::toStoredContent).toList())
                .metadata(toJson(message.getMetadata()))
                .timestamp(message.getTimestamp())
                .build();
    }

    /**
     * 将存储格式的 StoredMessage 转换回 AgentScope 的 Msg 对象。
     *
     * <p>该方法执行以下反向转换操作：</p>
     * <ol>
     *     <li>恢复消息的基本属性（ID、名称、角色、时间戳）</li>
     *     <li>遍历存储的内容块列表，将每个 {@link StoredMessageContent} 转换回对应的 {@link ContentBlock}</li>
     *     <li>将 JSON 字符串形式的 metadata 反序列化为 Map 对象</li>
     * </ol>
     *
     * <p><strong>支持的内容类型还原：</strong></p>
     * <ul>
     *     <li>type="text" → {@link TextBlock}，恢复文本内容</li>
     *     <li>type="thinking" → {@link ThinkingBlock}，恢复思考过程</li>
     *     <li>type="tool_use" → {@link ToolUseBlock}，恢复工具名称、输入参数（从 JSON 反序列化）、原始内容和工具ID</li>
     *     <li>type="tool_result" → {@link ToolResultBlock}，恢复工具名称、输出文本（包装为 TextBlock）、元数据（从 JSON 反序列化）和工具ID</li>
     *     <li>未知类型 → 降级为 {@link TextBlock}，使用 text 字段作为内容</li>
     * </ul>
     *
     * <p><strong>角色解析：</strong></p>
     * <p>如果存储的角色为 null 或空白，则默认解析为 {@link MsgRole#USER}；否则通过 {@link MsgRole#valueOf} 还原枚举值。</p>
     *
     * <p><strong>空值处理：</strong></p>
     * <ul>
     *     <li>如果内容列表为 null 或空，返回空列表</li>
     *     <li>如果文本内容为 null，转换为空字符串</li>
     *     <li>如果 metadata JSON 为 null 或空白，返回空 Map</li>
     * </ul>
     *
     * <p><strong>典型使用场景：</strong></p>
     * <ul>
     *     <li>从 MongoDB 加载历史消息后，恢复为 AgentScope 可用的 Msg 对象</li>
     *     <li>会话恢复时重建对话上下文</li>
     * </ul>
     *
     * @param storedMessage 存储格式的消息对象，从数据库读取
     * @return 转换后的 AgentScope Msg 对象，可直接用于 Agent 处理
     * @throws IllegalStateException 如果 metadata 或工具参数反序列化失败
     * @throws IllegalArgumentException 如果角色字符串无法匹配有效的 MsgRole 枚举值
     */
    public Msg toMsg(StoredMessage storedMessage) {
        return Msg.builder()
                .id(storedMessage.getMsgId())
                .name(storedMessage.getName())
                .role(resolveRole(storedMessage.getRole()))
                .content(toContentBlocks(storedMessage.getContent()))
                .metadata(toMap(storedMessage.getMetadata()))
                .timestamp(storedMessage.getTimestamp())
                .build();
    }

    /**
     * 将单个 AgentScope 内容块转换为存储格式。
     *
     * <p>该方法根据内容块的类型执行不同的转换逻辑：</p>
     * <ul>
     *     <li><b>TextBlock</b>：提取文本内容，设置 type="text"</li>
     *     <li><b>ThinkingBlock</b>：提取思考内容，设置 type="thinking"</li>
     *     <li><b>ToolUseBlock</b>：提取工具ID、名称、输入参数（序列化为JSON）、原始内容和工具ID，设置 type="tool_use"</li>
     *     <li><b>ToolResultBlock</b>：提取工具ID、名称、输出文本（通过 {@link #extractToolResultText} 提取）、元数据（序列化为JSON），设置 type="tool_result"</li>
     *     <li><b>其他类型</b>：使用类名作为 type，将整个对象序列化为 inputRaw，作为兜底策略</li>
     * </ul>
     *
     * <p><strong>ToolResultBlock 特殊处理：</strong></p>
     * <p>对于工具结果块，优先提取所有 TextBlock 的输出文本并拼接；如果没有文本块，则将整个输出对象序列化为 JSON。</p>
     *
     * @param block AgentScope 的内容块对象
     * @return 转换后的存储格式内容块
     * @throws IllegalStateException 如果工具输入参数或元数据序列化失败
     */
    private StoredMessageContent toStoredContent(ContentBlock block) {
        if (block instanceof TextBlock textBlock) {
            return StoredMessageContent.builder().type("text").text(textBlock.getText()).build();
        }
        if (block instanceof ThinkingBlock thinkingBlock) {
            return StoredMessageContent.builder()
                    .type("thinking")
                    .text(thinkingBlock.getThinking())
                    .build();
        }
        if (block instanceof ToolUseBlock toolUseBlock) {
            return StoredMessageContent.builder()
                    .type("tool_use")
                    .name(toolUseBlock.getName())
                    .input(toJson(toolUseBlock.getInput()))
                    .inputRaw(toolUseBlock.getContent())
                    .id(toolUseBlock.getId())
                    .build();
        }
        if (block instanceof ToolResultBlock toolResultBlock) {
            return StoredMessageContent.builder()
                    .type("tool_result")
                    .name(toolResultBlock.getName())
                    .text(extractToolResultText(toolResultBlock))
                    .inputRaw(toJson(toolResultBlock.getMetadata()))
                    .id(toolResultBlock.getId())
                    .build();
        }
        return StoredMessageContent.builder()
                .type(block.getClass().getSimpleName())
                .inputRaw(toJson(block))
                .build();
    }

    /**
     * 将存储格式的内容块列表转换回 AgentScope 内容块列表。
     *
     * <p>该方法遍历存储的内容块，根据 type 字段还原为对应的 {@link ContentBlock}：</p>
     * <ul>
     *     <li><b>type="text"</b>：创建 {@link TextBlock}，使用 text 字段作为内容</li>
     *     <li><b>type="thinking"</b>：创建 {@link ThinkingBlock}，使用 text 字段作为思考内容</li>
     *     <li><b>type="tool_use"</b>：创建 {@link ToolUseBlock}，从 JSON 反序列化输入参数，保留原始内容和工具ID</li>
     *     <li><b>type="tool_result"</b>：创建 {@link ToolResultBlock}，将文本包装为 TextBlock 作为输出，从 JSON 反序列化元数据</li>
     *     <li><b>未知类型</b>：降级创建 {@link TextBlock}，使用 text 字段作为内容，确保不会因未知类型导致转换失败</li>
     * </ul>
     *
     * <p><strong>空值处理：</strong></p>
     * <ul>
     *     <li>如果输入列表为 null 或空，返回空列表（不可变）</li>
     *     <li>如果文本字段为 null，通过 {@link #orEmpty} 转换为空字符串</li>
     *     <li>如果 JSON 字段为 null 或空白，通过 {@link #toMap} 返回空 Map</li>
     * </ul>
     *
     * <p><strong>降级策略：</strong></p>
     * <p>对于无法识别的类型，统一降级为 TextBlock，避免抛出异常导致整个消息转换失败。
     * 这种设计确保了向前兼容性，即使未来新增内容类型，旧版本代码也能安全处理。</p>
     *
     * @param contents 存储格式的内容块列表
     * @return 转换后的 AgentScope 内容块列表
     * @throws IllegalStateException 如果工具输入参数或元数据反序列化失败
     */
    private List<ContentBlock> toContentBlocks(List<StoredMessageContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return List.of();
        }
        List<ContentBlock> blocks = new ArrayList<>();
        for (StoredMessageContent content : contents) {
            String type = content.getType();
            if ("text".equals(type)) {
                blocks.add(TextBlock.builder().text(orEmpty(content.getText())).build());
                continue;
            }
            if ("thinking".equals(type)) {
                blocks.add(ThinkingBlock.builder().thinking(orEmpty(content.getText())).build());
                continue;
            }
            if ("tool_use".equals(type)) {
                blocks.add(ToolUseBlock.builder()
                        .id(content.getId())
                        .name(content.getName())
                        .input(toMap(content.getInput()))
                        .content(content.getInputRaw())
                        .build());
                continue;
            }
            if ("tool_result".equals(type)) {
                blocks.add(ToolResultBlock.builder()
                        .id(content.getId())
                        .name(content.getName())
                        .output(TextBlock.builder().text(orEmpty(content.getText())).build())
                        .metadata(toMap(content.getInputRaw()))
                        .build());
                continue;
            }
            blocks.add(TextBlock.builder().text(orEmpty(content.getText())).build());
        }
        return blocks;
    }

    /**
     * 从 ToolResultBlock 中提取工具执行的输出文本。
     *
     * <p>该方法执行以下提取逻辑：</p>
     * <ol>
     *     <li>遍历工具结果块的所有输出内容块</li>
     *     <li>过滤出所有 {@link TextBlock} 类型的块</li>
     *     <li>提取每个文本块的文本内容</li>
     *     <li>使用系统换行符（{@link System#lineSeparator()}）拼接所有文本</li>
     *     <li>如果没有找到任何文本块，则将整个输出对象序列化为 JSON 字符串作为兜底</li>
     * </ol>
     *
     * <p><strong>设计目的：</strong></p>
     * <p>工具执行结果可能包含多种类型的内容（文本、图片、结构化数据等），但存储格式只需要保存人类可读的文本。
     * 此方法优先提取文本内容，确保用户能够看到清晰的工具执行结果。</p>
     *
     * <p><strong>示例：</strong></p>
     * <pre>{@code
     * // 如果输出包含多个 TextBlock：
     * // [TextBlock("第一步结果"), TextBlock("第二步结果")]
     * // 返回："第一步结果\n第二步结果"
     *
     * // 如果输出没有 TextBlock：
     * // [ImageBlock(...), DataBlock(...)]
     * // 返回：整个输出对象的 JSON 序列化字符串
     * }</pre>
     *
     * @param toolResultBlock 工具结果块，包含执行输出的各种内容
     * @return 提取的文本内容，或多个文本拼接的结果；若无文本则返回 JSON 序列化字符串
     */
    private String extractToolResultText(ToolResultBlock toolResultBlock) {
        return toolResultBlock.getOutput().stream()
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse(toJson(toolResultBlock.getOutput()));
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串。
     *
     * <p>该方法使用 Jackson ObjectMapper 将任意对象转换为 JSON 字符串，用于存储复杂数据结构：</p>
     * <ul>
     *     <li>消息元数据（metadata Map）</li>
     *     <li>工具调用输入参数（ToolUseBlock.input）</li>
     *     <li>工具结果元数据（ToolResultBlock.metadata）</li>
     *     <li>未知类型的内容块（兜底序列化）</li>
     * </ul>
     *
     * <p><strong>空值处理：</strong></p>
     * <p>如果输入值为 null，直接返回 null，不进行序列化。</p>
     *
     * <p><strong>异常处理：</strong></p>
     * <p>如果序列化失败（如对象包含循环引用或不支持的类型），抛出 {@link IllegalStateException}，
     * 确保数据完整性，避免静默失败导致数据丢失。</p>
     *
     * @param value 要序列化的 Java 对象，可以为 null
     * @return JSON 字符串表示；如果输入为 null 则返回 null
     * @throws IllegalStateException 如果 Jackson 序列化失败
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize message content.", ex);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Map 对象。
     *
     * <p>该方法用于从存储格式恢复复杂的键值对数据，典型应用场景：</p>
     * <ul>
     *     <li>恢复消息元数据（metadata）</li>
     *     <li>恢复工具调用输入参数（ToolUseBlock.input）</li>
     *     <li>恢复工具结果元数据（ToolResultBlock.metadata）</li>
     * </ul>
     *
     * <p><strong>空值处理：</strong></p>
     * <p>如果输入 JSON 为 null 或空白字符串，返回空的不可变 Map（{@link Collections#emptyMap()}），
     * 避免返回 null 导致后续代码出现 NullPointerException。</p>
     *
     * <p><strong>泛型说明：</strong></p>
     * <p>由于 Java 类型擦除，该方法使用 {@code Map.class} 进行反序列化，返回类型为 {@code Map<String, Object>}。
     * 实际运行时，Object 可能是 String、Number、Boolean、List 或嵌套 Map，调用方需要进行类型检查。</p>
     *
     * <p><strong>异常处理：</strong></p>
     * <p>如果 JSON 格式无效或反序列化失败，抛出 {@link IllegalStateException}，
     * 确保不会因为损坏的数据导致静默错误。</p>
     *
     * @param json JSON 字符串，应为有效的对象格式（如 {"key": "value"}）
     * @return 反序列化后的 Map 对象；如果输入为 null 或空白则返回空 Map
     * @throws IllegalStateException 如果 JSON 格式无效或反序列化失败
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize message metadata.", ex);
        }
    }

    /**
     * 将角色字符串解析为 AgentScope 的 MsgRole 枚举。
     *
     * <p>该方法执行以下解析逻辑：</p>
     * <ul>
     *     <li>如果角色字符串为 null 或空白，返回默认值 {@link MsgRole#USER}</li>
     *     <li>否则通过 {@link MsgRole#valueOf} 将字符串转换为枚举值（如 "USER"、"ASSISTANT"、"SYSTEM"）</li>
     * </ul>
     *
     * <p><strong>默认值设计：</strong></p>
     * <p>选择 USER 作为默认值是因为在对话系统中，缺失角色信息的消息通常默认为用户输入，
     * 这是一种安全的降级策略，避免因数据不完整导致解析失败。</p>
     *
     * <p><strong>异常处理：</strong></p>
     * <p>如果角色字符串不匹配任何有效的 MsgRole 枚举值（如 "INVALID_ROLE"），
     * {@link MsgRole#valueOf} 会抛出 {@link IllegalArgumentException}，
     * 调用方应捕获此异常并提供适当的错误处理。</p>
     *
     * @param role 角色字符串，应为 MsgRole 枚举的名称（不区分大小写）
     * @return 对应的 MsgRole 枚举值；如果输入为 null 或空白则返回 USER
     * @throws IllegalArgumentException 如果角色字符串无法匹配有效的枚举值
     */
    private MsgRole resolveRole(String role) {
        if (role == null || role.isBlank()) {
            return MsgRole.USER;
        }
        return MsgRole.valueOf(role);
    }

    /**
     * 将可能为 null 的字符串转换为非空字符串。
     *
     * <p>该方法提供简单的空值保护：</p>
     * <ul>
     *     <li>如果输入值为 null，返回空字符串 ""</li>
     *     <li>否则返回原始值</li>
     * </ul>
     *
     * <p><strong>使用场景：</strong></p>
     * <p>在消息内容转换过程中，确保文本字段永远不会为 null，避免后续处理出现 NullPointerException。
     * 例如，TextBlock 和 ThinkingBlock 的文本内容、ToolResultBlock 的输出文本等都使用此方法进行保护。</p>
     *
     * <p><strong>与空白的区别：</strong></p>
     * <p>此方法只检查 null，不检查空白字符串。如果输入是 "   "（空白字符串），会原样返回。
     * 如需同时处理空白，应使用 {@link String#isBlank()} 或其他方法。</p>
     *
     * @param value 可能为 null 的字符串
     * @return 非空字符串；如果输入为 null 则返回空字符串
     */
    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
