/**
 * 领域层 - 核心业务逻辑和领域模型。
 *
 * <p>该包包含 HiveMind Agent 的核心领域模型，遵循 DDD 聚合根和实体模式。</p>
 *
 * <h2>子包说明</h2>
 * <ul>
 *   <li><b>agent/</b> - Agent 聚合根及其工厂、Hook、Prompt 服务</li>
 *   <li><b>memory/</b> - 对话记忆聚合（Document 模型、服务、仓储接口）</li>
 *   <li><b>session/</b> - 会话聚合根（状态 Document、服务、仓储接口）</li>
 *   <li><b>tool/</b> - 工具聚合（Tool Guard、审批流、内置工具）</li>
 *   <li><b>skill/</b> - 技能聚合（Skill Document、服务、仓储接口）</li>
 *   <li><b>provider/</b> - 模型供应商聚合（ProviderRegistry、模型配置）</li>
 *   <li><b>streaming/</b> - 流式事件聚合（事件类型、会话注册）</li>
 *   <li><b>shared/</b> - 跨聚合共享的领域对象（枚举、常量）</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>领域模型（model/）只依赖领域层，不依赖基础设施</li>
 *   <li>仓储接口（repository/）定义在领域层，实现在基础设施层</li>
 *   <li>工厂类（factory/）负责创建复杂的聚合根</li>
 * </ul>
 */
package com.liangshou.agentic.domain;
