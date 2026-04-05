package com.liangshou.tangdynasty.agentic.domain.document;

import lombok.*;

/**
 * StoredMessageContent 组件，表示一条 Message 中具体的 Content
 *
 *  @author LiangshouX
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
}
