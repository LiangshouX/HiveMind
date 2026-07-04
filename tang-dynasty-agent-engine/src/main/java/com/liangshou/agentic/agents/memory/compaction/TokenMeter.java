package com.liangshou.agentic.agents.memory.compaction;

import io.agentscope.core.message.Msg;

import java.util.List;

/**
 * Token 计量器 - 计算消息和文本的 token 数量。
 *
 * <p>不同模型使用不同的 tokenizer，该接口抽象了计量逻辑，
 * 允许根据模型类型选择最优的计量策略。</p>
 *
 * @author LiangshouX
 */
public interface TokenMeter {

    /**
     * 计算文本的 token 数量。
     *
     * @param text 文本内容
     * @return token 数量
     */
    int countTokens(String text);

    /**
     * 计算单条消息的 token 数量（包含角色开销）。
     *
     * @param message 消息对象
     * @return token 数量
     */
    int countMessageTokens(Msg message);

    /**
     * 批量计算消息列表的总 token 数量。
     *
     * @param messages 消息列表
     * @return 总 token 数量
     */
    int countTotalTokens(List<Msg> messages);
}
