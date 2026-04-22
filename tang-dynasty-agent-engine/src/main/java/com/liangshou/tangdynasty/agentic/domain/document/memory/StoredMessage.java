package com.liangshou.tangdynasty.agentic.domain.document.memory;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储消息 - 表示持久化到 MongoDB 的单条对话消息。
 *
 * <p>该对象包含以下字段：</p>
 * <ul>
 *     <li>{@code msgId} - 消息唯一标识</li>
 *     <li>{@code name} - 消息发送者名称</li>
 *     <li>{@code role} - 消息角色（USER/ASSISTANT/SYSTEM）</li>
 *     <li>{@code content} - 消息内容列表，支持文本、思考、工具调用等多种类型</li>
 *     <li>{@code metadata} - 元数据 JSON 字符串，包含额外信息</li>
 *     <li>{@code timestamp} - 消息时间戳</li>
 * </ul>
 *
 * <p>具体的内容结构由 {@link StoredMessageContent} 表示，支持多模态内容。</p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredMessage {

    private String msgId;

    private String name;

    private String role;

    @Builder.Default
    private List<StoredMessageContent> content = new ArrayList<>();

    private String metadata;

    private String timestamp;
}
