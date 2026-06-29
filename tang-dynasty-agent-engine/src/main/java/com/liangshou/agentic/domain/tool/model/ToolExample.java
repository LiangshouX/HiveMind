package com.liangshou.agentic.domain.tool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具使用示例 - 描述工具的使用场景和输入参数。
 *
 * <p>用于前端展示工具的具体用法，帮助用户理解如何调用工具。</p>
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExample {

    /**
     * 示例标题，简要说明该示例做什么
     */
    private String title;

    /**
     * 工具输入参数的 JSON 字符串
     */
    private String input;
}
