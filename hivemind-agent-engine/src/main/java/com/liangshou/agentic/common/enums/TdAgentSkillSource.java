package com.liangshou.agentic.common.enums;

/**
 * Agent Skill 来源类型枚举，用于区分 Skill 的来源渠道。
 * <p>
 * 包含两种类型：
 * <ul>
 *   <li>BUILTIN - 内置 Skill，由系统预定义并提供，通常从 classpath 或文件系统加载</li>
 *   <li>CUSTOMIZED - 自定义 Skill，由用户创建和管理，存储在 MongoDB 中</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
public enum TdAgentSkillSource {
    BUILTIN,
    CUSTOMIZED
}
