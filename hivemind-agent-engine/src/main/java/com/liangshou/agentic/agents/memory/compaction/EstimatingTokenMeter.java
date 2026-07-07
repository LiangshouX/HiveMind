package com.liangshou.agentic.agents.memory.compaction;

import io.agentscope.core.message.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于规则的 Token 估算器。
 *
 * <p>估算策略：</p>
 * <ul>
 *     <li>中文字符：约 1.5 tokens/字符</li>
 *     <li>英文/数字/标点：约 0.25 tokens/字符（4 字符 ≈ 1 token）</li>
 *     <li>每条消息的固定开销：约 4 tokens（role, separators）</li>
 *     <li>每个 ContentBlock 的结构开销：约 2 tokens</li>
 * </ul>
 *
 * <p>该估算器的误差范围约 ±15%，对于压缩触发判断已足够精确。
 * 后续可替换为基于 tiktoken-jni 或远程 tokenizer 的精确实现。</p>
 *
 * @author LiangshouX
 */
@Component
public class EstimatingTokenMeter implements TokenMeter {

    private static final double CJK_TOKEN_RATIO = 1.5;
    private static final double ASCII_TOKEN_RATIO = 0.25;
    private static final int MESSAGE_OVERHEAD = 4;
    private static final int CONTENT_BLOCK_OVERHEAD = 2;

    @Override
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        double tokens = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCJK(c)) {
                tokens += CJK_TOKEN_RATIO;
            } else {
                tokens += ASCII_TOKEN_RATIO;
            }
        }
        return Math.max(1, (int) Math.ceil(tokens));
    }

    @Override
    public int countMessageTokens(Msg message) {
        if (message == null) {
            return 0;
        }
        int tokens = MESSAGE_OVERHEAD;
        for (ContentBlock block : message.getContent()) {
            tokens += CONTENT_BLOCK_OVERHEAD;
            tokens += countBlockTokens(block);
        }
        return tokens;
    }

    @Override
    public int countTotalTokens(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return messages.stream().mapToInt(this::countMessageTokens).sum();
    }

    private int countBlockTokens(ContentBlock block) {
        if (block instanceof TextBlock textBlock) {
            return countTokens(textBlock.getText());
        }
        if (block instanceof ThinkingBlock thinkingBlock) {
            return countTokens(thinkingBlock.getThinking());
        }
        if (block instanceof ToolUseBlock toolUseBlock) {
            int nameTokens = countTokens(toolUseBlock.getName());
            int inputTokens = countTokens(toolUseBlock.getContent());
            return nameTokens + inputTokens;
        }
        if (block instanceof ToolResultBlock toolResultBlock) {
            int nameTokens = countTokens(toolResultBlock.getName());
            String outputText = toolResultBlock.getOutput().stream()
                    .filter(TextBlock.class::isInstance)
                    .map(TextBlock.class::cast)
                    .map(TextBlock::getText)
                    .reduce((a, b) -> a + b)
                    .orElse("");
            int outputTokens = countTokens(outputText);
            return nameTokens + outputTokens;
        }
        return 0;
    }

    private boolean isCJK(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
