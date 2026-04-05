package com.liangshou.tangdynasty.agentic.domain.document;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * StoredMessage 组件，表示一条历史会话的 Message 结构。
 * <p>
 * 具体的 content 由 {@link StoredMessageContent} 表示
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
