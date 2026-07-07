/**
 * 上下文压缩模块 - 基于 token 计量的智能上下文压缩能力。
 *
 * <p>该模块提供以下核心组件：</p>
 * <ul>
 *     <li>{@link com.liangshou.agentic.agents.memory.compaction.TokenMeter} - Token 计量接口</li>
 *     <li>{@link com.liangshou.agentic.agents.memory.compaction.EstimatingTokenMeter} - 基于规则的估算实现</li>
 *     <li>{@link com.liangshou.agentic.agents.memory.compaction.ContextWindowManager} - 上下文窗口管理器</li>
 *     <li>{@link com.liangshou.agentic.agents.memory.compaction.ContextCompressor} - 核心压缩器</li>
 *     <li>{@link com.liangshou.agentic.agents.memory.compaction.CompactionResult} - 压缩结果模型</li>
 * </ul>
 *
 * <p>压缩策略：当上下文 token 数达到模型窗口的 85% 时自动触发分层压缩——
 * System Prompt 永不压缩、前 10% 消息保留、最近 N 条保留、中间消息压缩为摘要。</p>
 *
 * @author LiangshouX
 */
package com.liangshou.agentic.agents.memory.compaction;
