import type {UiMessage} from "../types";

/**
 * 前端 Token 估算器。
 *
 * <p>口径必须与后端 `EstimatingTokenMeter` 保持一致，否则会出现
 * "界面显示 30% 但压缩器认为已满"的割裂。</p>
 *
 * <ul>
 *     <li>中文字符：约 1.5 tokens/字符</li>
 *     <li>英文/数字/标点：约 0.25 tokens/字符</li>
 *     <li>每条消息固定开销：4 tokens</li>
 *     <li>每个内容块结构开销：2 tokens</li>
 * </ul>
 */
const CJK_TOKEN_RATIO = 1.5;
const ASCII_TOKEN_RATIO = 0.25;
const MESSAGE_OVERHEAD = 4;
const CONTENT_BLOCK_OVERHEAD = 2;

function isCjk(char: string) {
    const code = char.charCodeAt(0);
    // CJK 统一表意文字（含扩展 A/B）、中日韩符号与标点、全角/半角形式
    return (
        (code >= 0x4e00 && code <= 0x9fff) ||
        (code >= 0x3400 && code <= 0x4dbf) ||
        (code >= 0xf900 && code <= 0xfaff) ||
        (code >= 0x3000 && code <= 0x303f) ||
        (code >= 0xff00 && code <= 0xffef)
    );
}

export function estimateTextTokens(text?: string) {
    if (!text) {
        return 0;
    }
    let tokens = 0;
    for (const char of text) {
        tokens += isCjk(char) ? CJK_TOKEN_RATIO : ASCII_TOKEN_RATIO;
    }
    return Math.max(1, Math.ceil(tokens));
}

function estimateBlockTokens(block: UiMessage["blocks"][number]) {
    switch (block.type) {
        case "tool_use":
        case "tool_result":
            return estimateTextTokens(block.toolName) + estimateTextTokens(block.content);
        default:
            return estimateTextTokens(block.content);
    }
}

/** 估算一组消息占用的 token 总数。 */
export function estimateMessagesTokens(messages?: UiMessage[]) {
    if (!messages || messages.length === 0) {
        return 0;
    }
    let total = 0;
    for (const message of messages) {
        total += MESSAGE_OVERHEAD;
        for (const block of message.blocks) {
            total += CONTENT_BLOCK_OVERHEAD;
            total += estimateBlockTokens(block);
        }
    }
    return total;
}

/** 统计会话中的推理块数量。 */
export function countReasoningBlocks(messages?: UiMessage[]) {
    if (!messages) {
        return 0;
    }
    return messages.reduce(
        (sum, message) =>
            sum + message.blocks.filter((block) => block.type === "reasoning").length,
        0,
    );
}

/** 统计会话中的工具调用次数（tool_use 块计数）。 */
export function countToolCalls(messages?: UiMessage[]) {
    if (!messages) {
        return 0;
    }
    return messages.reduce(
        (sum, message) =>
            sum + message.blocks.filter((block) => block.type === "tool_use").length,
        0,
    );
}
