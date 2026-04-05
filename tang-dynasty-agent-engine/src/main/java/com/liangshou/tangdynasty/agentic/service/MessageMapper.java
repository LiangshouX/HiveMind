package com.liangshou.tangdynasty.agentic.service;

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
 * @author LiangshouX
 */
@Component
public class MessageMapper {

    private final ObjectMapper objectMapper;

    /**
     * 执行相关操作。
     *
     * @param objectMapper ObjectMapper
     */
    public MessageMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 toStoredMessage 操作。
     *
     * @param message 消息
     * @return 返回结果
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
     * 执行 toMsg 操作。
     *
     * @param storedMessage 存储消息
     * @return 返回结果
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

    private String extractToolResultText(ToolResultBlock toolResultBlock) {
        return toolResultBlock.getOutput().stream()
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse(toJson(toolResultBlock.getOutput()));
    }

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

    private MsgRole resolveRole(String role) {
        if (role == null || role.isBlank()) {
            return MsgRole.USER;
        }
        return MsgRole.valueOf(role);
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
