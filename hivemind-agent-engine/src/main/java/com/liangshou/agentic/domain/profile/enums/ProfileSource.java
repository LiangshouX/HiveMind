package com.liangshou.agentic.domain.profile.enums;

/**
 * Profile 来源枚举 - 标识 Profile 配置的来源。
 *
 * <p>该枚举用于区分：</p>
 * <ul>
 *     <li><b>DEFAULT</b>：从 resources/profiles/ 加载的默认配置</li>
 *     <li><b>USER_CUSTOMIZED</b>：用户自定义的配置（存储在 MongoDB 中）</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>前端展示：标识当前配置是默认值还是用户自定义</li>
 *     <li>后端逻辑：决定是否从 MongoDB 或 resources 加载文件内容</li>
 *     <li>统计查询：分析有多少用户使用了自定义配置</li>
 * </ul>
 *
 * @author LiangshouX
 */
public enum ProfileSource {
    
    /**
     * 默认配置，从 resources/profiles/ 目录加载
     */
    DEFAULT,
    
    /**
     * 用户自定义配置，存储在 MongoDB 中
     */
    USER_CUSTOMIZED
}
