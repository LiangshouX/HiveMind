import {
    AudioOutlined,
    BranchesOutlined,
    CompressOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    DashboardOutlined,
    DownOutlined,
    FireOutlined,
    LoadingOutlined,
    PaperClipOutlined,
    PauseCircleOutlined,
    RadarChartOutlined,
    RightOutlined,
    RobotOutlined,
    SendOutlined,
    ThunderboltOutlined,
    ToolOutlined,
} from "@ant-design/icons";
import {Bubble, Prompts, Sender, Welcome} from "@ant-design/x";
import type {SenderProps} from "@ant-design/x/es/sender/interface";
import {SenderContext} from "@ant-design/x/es/sender/context";
import {Avatar, Button, Card, Input, Popover, Space, Spin, Tag, Tooltip, Typography} from "antd";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/zh-cn";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import React, {useContext, useEffect, useRef, useState, useCallback} from "react";
import type {AuthUser, SessionContextUsage, SessionState} from "../../types";
import {ChatSendButton} from "./ChatSendButton";
import {ModelSelector} from "../model/ModelSelector";

dayjs.extend(relativeTime);
dayjs.locale("zh-cn");

const {Text} = Typography;

function formatTime(value?: string) {
    if (!value) {
        return "刚刚";
    }
    return dayjs(value).format("MM-DD HH:mm");
}

function formatRelative(value?: string) {
    if (!value) {
        return "刚刚";
    }
    return dayjs(value).fromNow();
}

function blockTone(type: string) {
    switch (type) {
        case "reasoning":
            return "reasoning";
        case "tool_use":
        case "tool_result":
            return "tool";
        case "approval":
            return "approval";
        case "error":
            return "error";
        case "result":
            return "result";
        default:
            return "text";
    }
}

/** 中间过程块：推理 / 工具调用 / 工具结果，可折叠、可主动展开 */
const INTERMEDIATE_BLOCK_TYPES = new Set(["reasoning", "tool_use", "tool_result"]);

function isIntermediateBlock(type: string) {
    return INTERMEDIATE_BLOCK_TYPES.has(type);
}

/** 折叠态摘要：去掉 Markdown 代码围栏后取前若干字符 */
function summarizeBlock(content: string, limit = 60) {
    const plain = content
        .replace(/```[a-zA-Z0-9]*\n?/g, "")
        .replace(/```/g, "")
        .replace(/`/g, "")
        .replace(/\s+/g, " ")
        .trim();
    if (!plain) {
        return "";
    }
    return plain.length > limit ? `${plain.slice(0, limit)}…` : plain;
}

function toneIcon(type: string) {
    switch (type) {
        case "reasoning":
            return <BranchesOutlined/>;
        case "tool_use":
        case "tool_result":
            return <ToolOutlined/>;
        case "approval":
            return <PauseCircleOutlined/>;
        case "error":
            return <RadarChartOutlined/>;
        case "result":
            return <CheckCircleOutlined/>;
        default:
            return (
                <Avatar
                    size={22}
                    style={{
                        background: "var(--td-brand-gradient)",
                        color: "var(--td-text-inverse)",
                        fontSize: 11,
                        fontWeight: 900,
                    }}
                >
                    TD
                </Avatar>
            );
    }
}

/**
 * 提交回调通道。
 *
 * <p>为什么需要它：@ant-design/x 的 Sender 内部用 `useState(!innerValue)` 初始化
 * `submitDisabled` 且此后**从不重新同步**，导致 `triggerSend()` 永远提前 return，
 * 于是"回车发送"完全失效（点按钮能发是因为按钮走的是我们自己的 onClick）。
 * 因此这里自建一条通道，由业务侧直接触发发送。</p>
 */
const SenderSubmitContext = React.createContext<(() => void) | undefined>(undefined);

/** Ref interface expected by the Sender component */
interface SenderTextAreaRef {
    nativeElement?: HTMLElement | null;
    focus: (options?: any) => void;
    blur: () => void;
    insert: (...args: any[]) => void;
    clear: () => void;
    getValue: () => { value: string; slotConfig: any[]; skill?: any };
}

const EnterSendTextArea = React.forwardRef<SenderTextAreaRef>((props, ref) => {
    const ctx = useContext(SenderContext as React.Context<SenderProps & { triggerSend?: () => void }>);
    const submit = useContext(SenderSubmitContext);
    const {
        value,
        onChange,
        onKeyDown: ctxOnKeyDown,
        disabled,
        readOnly,
        submitType = 'enter',
        prefixCls,
        styles = {},
        classNames = {},
        autoSize,
        placeholder,
        onFocus,
        onBlur,
    } = ctx;

    const inputRef = useRef<any>(null);
    const isCompositionRef = useRef(false);

    // Expose the same ref structure as Ant Design's TextArea so Sender can access it
    React.useImperativeHandle(ref, () => ({
        nativeElement: inputRef.current?.resizableTextArea?.textArea,
        focus: (options?: any) => inputRef.current?.focus(options),
        blur: () => inputRef.current?.blur(),
        insert: (...args: any[]) => inputRef.current?.insert?.(...args),
        clear: () => inputRef.current?.clear(),
        // 直接回读上下文里的 value，避免 antd v6 的 TextArea 没有 getValue 导致取空
        getValue: () => ({value: value ?? '', slotConfig: [], skill: undefined}),
    }));

    const handleCompositionStart = () => {
        isCompositionRef.current = true;
    };

    const handleCompositionEnd = () => {
        isCompositionRef.current = false;
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        // 只透传业务侧的 onKeyDown，不调用 Sender 内部那个会吞掉 Enter 的实现
        ctxOnKeyDown?.(e);
        const {key, shiftKey, ctrlKey, altKey, metaKey} = e;

        if (isCompositionRef.current || key !== 'Enter') {
            return;
        }

        const isModifierPressed = ctrlKey || altKey || metaKey;
        const shouldSubmit =
            (submitType === 'enter' && !shiftKey && !isModifierPressed) ||
            (submitType === 'shiftEnter' && shiftKey && !isModifierPressed);

        if (!shouldSubmit) {
            // Shift / Ctrl / Alt + Enter = 换行，交给浏览器默认行为
            return;
        }

        e.preventDefault();
        const text = (value ?? '').trim();
        if (!text || disabled || readOnly) {
            return;
        }
        submit?.();
    };

    return (
        <Input.TextArea
            {...props}
            ref={inputRef}
            value={value}
            onChange={(e) => onChange?.(e.target.value, e)}
            onKeyDown={handleKeyDown}
            onCompositionStart={handleCompositionStart}
            onCompositionEnd={handleCompositionEnd}
            disabled={disabled}
            readOnly={readOnly}
            // 兜底：即使上层没透传 autoSize，也要保证输入框可随内容自动增高
            autoSize={autoSize ?? {minRows: 1, maxRows: 6}}
            placeholder={placeholder}
            onFocus={onFocus}
            onBlur={onBlur}
            variant="borderless"
            className={`${prefixCls}-input ${classNames?.input || ''}`}
            style={styles?.input}
        />
    );
});
EnterSendTextArea.displayName = 'EnterSendTextArea';

interface ConversationWorkspaceProps {
    user: AuthUser;
    busy: boolean;
    input: string;
    activeSession?: SessionState;
    activeSessionId?: string;
    selectedModelId?: string;
    contextUsage?: SessionContextUsage;
    compacting?: boolean;
    onInputChange: (value: string) => void;
    onSelectModel?: (providerId: string, modelId: string) => void;
    onRefreshSession: () => void;
    onInterrupt: () => void | Promise<void>;
    onSend: () => void | Promise<void>;
    onCompactContext?: () => void | Promise<void>;
    onApprove?: () => void | Promise<void>;
    onReject?: () => void | Promise<void>;
}

/** 把 token 数格式化为 K / M，便于阅读 */
function formatTokens(value?: number) {
    if (value === undefined || value === null || Number.isNaN(value)) {
        return "-";
    }
    if (value >= 1_000_000) {
        return `${(value / 1_000_000).toFixed(2)}M`;
    }
    if (value >= 1000) {
        return `${(value / 1000).toFixed(1)}K`;
    }
    return String(value);
}

/** 把毫秒格式化成人类可读的时长 */
function formatDuration(ms?: number) {
    if (!ms || ms <= 0) {
        return "-";
    }
    if (ms < 1000) {
        return `${Math.round(ms)} 毫秒`;
    }
    return `${(ms / 1000).toFixed(1)} 秒`;
}

/** 千分位数字 */
function formatCount(value?: number) {
    if (value === undefined || value === null || Number.isNaN(value)) {
        return "0";
    }
    return Math.round(value).toLocaleString("en-US");
}

/**
 * 上下文窗口占用面板。
 *
 * <p>常驻部分是一个紧凑的百分比徽标（悬浮展开面板），
 * 面板内展示：占用比例与总量、按构成拆分的分段进度条
 * （系统提示词 / 工具定义 / 对话消息）、会话统计
 * （模型用时 / 首 token 延迟 / 输出速度）、Token 用量
 * （缓存命中 / 未缓存输入 / 缓存读取 / 输出），并提供手动压缩入口。</p>
 */
function ContextUsagePanel({
                             usage,
                             compacting,
                             onCompact,
                         }: {
    usage?: SessionContextUsage;
    compacting?: boolean;
    onCompact?: () => void | Promise<void>;
}) {
    if (!usage) {
        return null;
    }

    const percent = Math.round(usage.usageRatio * 100);
    const warning = usage.nearThreshold;
    const thresholdPercent = Math.round(usage.thresholdRatio * 100);
    const window = usage.contextWindow || 1;

    const systemTokens = usage.systemPromptTokens ?? 0;
    const toolTokens = usage.toolDefinitionTokens ?? 0;
    const messageTokens = usage.conversationTokens
        ?? Math.max(0, usage.usedTokens - systemTokens - toolTokens);

    // 各部分占上下文窗口的比例（用于分段进度条）
    const widthOf = (value: number) =>
        `${Math.min(100, Math.max(0, (value / window) * 100))}%`;

    const cacheHitPercent = Math.round((usage.cacheHitRatio ?? 0) * 100);
    const tps = usage.tokensPerSecond ?? 0;

    const panel = (
        <div className="ctx-panel">
            <div className="ctx-panel-header">
                <span className="ctx-panel-title">上下文已用 {percent}%</span>
                <span className="ctx-panel-total">
                    ~{formatTokens(usage.usedTokens)} / {formatTokens(usage.contextWindow)}
                </span>
            </div>

            <div className="ctx-panel-track">
                <span className="ctx-panel-seg is-system" style={{width: widthOf(systemTokens)}}/>
                <span className="ctx-panel-seg is-tools" style={{width: widthOf(toolTokens)}}/>
                <span className="ctx-panel-seg is-messages" style={{width: widthOf(messageTokens)}}/>
                <span
                    className="ctx-panel-threshold"
                    style={{left: `${Math.min(100, thresholdPercent)}%`}}
                    title={`压缩阈值 ${thresholdPercent}%`}
                />
            </div>

            <div className="ctx-panel-legend">
                <div className="ctx-panel-legend-row">
                    <span className="ctx-panel-legend-label">
                        <i className="ctx-dot is-system"/>系统提示词
                    </span>
                    <span className="ctx-panel-legend-value">~{formatTokens(systemTokens)}</span>
                </div>
                <div className="ctx-panel-legend-row">
                    <span className="ctx-panel-legend-label">
                        <i className="ctx-dot is-tools"/>工具定义
                    </span>
                    <span className="ctx-panel-legend-value">~{formatTokens(toolTokens)}</span>
                </div>
                <div className="ctx-panel-legend-row">
                    <span className="ctx-panel-legend-label">
                        <i className="ctx-dot is-messages"/>对话消息
                    </span>
                    <span className="ctx-panel-legend-value">~{formatTokens(messageTokens)}</span>
                </div>
            </div>

            <div className="ctx-panel-divider"/>

            <div className="ctx-panel-section">会话统计</div>
            <div className="ctx-panel-grid">
                <div className="ctx-panel-cell">
                    <div className="ctx-panel-cell-label">模型用时</div>
                    <div className="ctx-panel-cell-value">{formatDuration(usage.modelDurationMs)}</div>
                </div>
                <div className="ctx-panel-cell">
                    <div className="ctx-panel-cell-label">首 token 平均 (TTFT)</div>
                    <div className="ctx-panel-cell-value">{formatDuration(usage.ttftMs)}</div>
                </div>
                <div className="ctx-panel-cell">
                    <div className="ctx-panel-cell-label">输出速度 (TPS)</div>
                    <div className="ctx-panel-cell-value">
                        {tps > 0 ? `${tps.toFixed(1)} tok/s` : "-"}
                    </div>
                </div>
            </div>

            <div className="ctx-panel-divider"/>

            <div className="ctx-panel-section">Token 用量</div>
            <div className="ctx-panel-grid">
                <div className="ctx-panel-cell">
                    <div className="ctx-panel-cell-label">缓存命中</div>
                    <div className="ctx-panel-cell-value">{cacheHitPercent}%</div>
                </div>
                <div className="ctx-panel-cell">
                    <div className="ctx-panel-cell-label">未缓存输入</div>
                    <div className="ctx-panel-cell-value">
                        {formatCount(usage.uncachedInputTokens)} tok
                    </div>
                </div>
                <div className="ctx-panel-cell">
                    <div className="ctx-panel-cell-label">缓存读取</div>
                    <div className="ctx-panel-cell-value">
                        {formatCount(usage.cachedInputTokens)} tok
                    </div>
                </div>
                <div className="ctx-panel-cell">
                    <div className="ctx-panel-cell-label">输出</div>
                    <div className="ctx-panel-cell-value">
                        {formatCount(usage.outputTokens)} tok
                    </div>
                </div>
            </div>

            <div className="ctx-panel-divider"/>

            <div className="ctx-panel-footer">
                <Button
                    size="small"
                    icon={<CompressOutlined/>}
                    loading={compacting}
                    disabled={compacting}
                    onClick={() => void onCompact?.()}
                >
                    压缩上下文
                </Button>
                <span className="ctx-panel-foot-note">
                    阈值 {thresholdPercent}% · 已压缩 {usage.compactionCount} 次
                    {usage.compressedSummaryChars > 0
                        ? `（摘要 ${usage.compressedSummaryChars} 字）`
                        : ""}
                </span>
            </div>
        </div>
    );

    return (
        <div className="context-usage-bar">
            <Popover
                content={panel}
                trigger="hover"
                placement="topRight"
                overlayClassName="ctx-panel-popover"
            >
                <button
                    type="button"
                    className={`ctx-usage-badge${warning ? " is-warning" : ""}`}
                    aria-label="查看上下文窗口占用详情"
                >
                    <DashboardOutlined/>
                    <span>{percent}%</span>
                </button>
            </Popover>
            {warning ? (
                <Tag color="warning" style={{marginInlineEnd: 0}}>
                    临近阈值
                </Tag>
            ) : null}
            <Tooltip title="手动压缩上下文（将历史中间层折叠为摘要）">
                <Button
                    size="small"
                    type="text"
                    icon={<CompressOutlined/>}
                    loading={compacting}
                    disabled={compacting}
                    onClick={() => void onCompact?.()}
                    style={{
                        color: "var(--td-text-tertiary)",
                        height: 22,
                        padding: "0 6px",
                        fontSize: 12,
                    }}
                />
            </Tooltip>
        </div>
    );
}

export function ConversationWorkspace({
                                          user,
                                          busy,
                                          input,
                                          activeSession,
                                          activeSessionId,
                                          selectedModelId,
                                          contextUsage,
                                          compacting,
                                          onInputChange,
                                          onSelectModel,
                                          onInterrupt,
                                          onSend,
                                          onCompactContext,
                                          onApprove,
                                          onReject,
                                      }: ConversationWorkspaceProps) {
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const [prevMessageCount, setPrevMessageCount] = useState(0);
    const prevSessionIdRef = useRef<string | undefined>(undefined);
    const [shouldScroll, setShouldScroll] = useState(false);
    // 中间过程块的展开状态覆盖：
    // undefined = 跟随默认（最终结果渲染完成后自动折叠）
    // true     = 用户手动展开
    // false    = 用户手动折叠
    const [blockExpandOverrides, setBlockExpandOverrides] = useState<Record<string, boolean>>({});

    const toggleBlockCollapse = useCallback((blockId: string, currentlyCollapsed: boolean) => {
        setBlockExpandOverrides((prev) => ({...prev, [blockId]: !currentlyCollapsed}));
    }, []);

    // 输入框回车发送的统一出口（供 EnterSendTextArea 通过 Context 调用）
    const handleSubmit = useCallback(() => {
        void onSend();
    }, [onSend]);

    const bubbleItems =
        activeSession?.messages.map((message) => ({
            key: message.id,
            role: message.role,
            // 禁用 typing 动画以避免与 SSE 实时更新冲突，提升流式输出流畅度
            typing: false,
            content: (
                <div className={`message-stack is-${message.role}`}>
                    <div className="message-meta">
                        <Space size={8}>
                            <Text className="message-author"
                                  style={{color: 'var(--td-text-base)'}}>{message.name}</Text>
                            <Text className="message-time"
                                  style={{color: 'var(--td-text-secondary)'}}>{formatTime(message.createdAt)}</Text>
                        </Space>
                        {message.streaming ? (
                            <Tag color="processing" icon={<LoadingOutlined/>}>
                                思考中……
                            </Tag>
                        ) : message.failed ? (
                            <Tag color="error">已中断</Tag>
                        ) : null}
                    </div>

                    {message.blocks.map((block) => {
                        const intermediate = isIntermediateBlock(block.type);
                        // 默认态：最终结果渲染完成后，中间过程块自动折叠；
                        // 用户手动展开/折叠过的块以其显式选择为准。
                        const hasFinalResult = message.blocks.some((item) => item.type === "result");
                        const defaultCollapsed = intermediate && hasFinalResult;
                        const override = blockExpandOverrides[block.id];
                        const isCollapsed = intermediate ? (override ?? defaultCollapsed) : false;
                        const preview = intermediate && isCollapsed ? summarizeBlock(block.content) : "";
                        // 正文与最终结果不套卡片，直接呈现文字；中间过程块保留色调条
                        const isPlain = block.type === "result" || block.type === "text";

                        return (
                        <div
                            key={block.id}
                            className={`message-block tone-${blockTone(block.type)}${isCollapsed ? " is-collapsed" : ""}${isPlain ? " is-plain" : ""}`}
                        >
                            {block.title || block.toolName ? (
                                <div
                                    className="message-block-title"
                                    style={{
                                        color: 'var(--td-text-base)',
                                        ...(intermediate
                                            ? {cursor: 'pointer', userSelect: 'none'}
                                            : {}),
                                    }}
                                    onClick={intermediate
                                        ? () => toggleBlockCollapse(block.id, isCollapsed)
                                        : undefined}
                                    role={intermediate ? "button" : undefined}
                                    tabIndex={intermediate ? 0 : undefined}
                                    onKeyDown={intermediate
                                        ? (e) => {
                                            if (e.key === "Enter" || e.key === " ") {
                                                e.preventDefault();
                                                toggleBlockCollapse(block.id, isCollapsed);
                                            }
                                        }
                                        : undefined}
                                >
                                    <Space size={8} wrap={false} style={{flexWrap: 'nowrap', minWidth: 0}}>
                                        {intermediate ? (
                                            <span style={{ fontSize: 10, color: 'var(--td-text-tertiary)' }}>
                                                {isCollapsed ? <RightOutlined/> : <DownOutlined/>}
                                            </span>
                                        ) : toneIcon(block.type)}
                                        {block.title ? (
                                            <span className="message-block-label">{block.title}</span>
                                        ) : null}
                                        {block.toolName ? <Tag style={{flexShrink: 0}}>{block.toolName}</Tag> : null}
                                        {preview ? (
                                            <span className="message-block-preview">{preview}</span>
                                        ) : null}
                                    </Space>
                                </div>
                            ) : null}

                            <div style={{
                                display: 'grid',
                                gridTemplateRows: isCollapsed ? '0fr' : '1fr',
                                transition: 'grid-template-rows 200ms ease-out',
                            }}>
                                <div style={{overflow: 'hidden', minHeight: 0}}>
                                    {/* 工具类型消息不需要展示 rawInput，因为 content 已经是代码块格式 */}
                                    {block.rawInput && block.type !== "tool_use" && block.type !== "tool_result" ? (
                                        <pre className="message-raw" style={{
                                            background: 'var(--td-code-bg)',
                                            border: '1px solid var(--td-code-border)',
                                            color: 'var(--td-code-text)'
                                        }}>{block.rawInput}</pre>
                                    ) : null}

                                    <div
                                        className={`markdown-body${intermediate ? " markdown-scroll" : ""}`}
                                        style={{color: 'var(--td-text-base)'}}
                                    >
                                        <ReactMarkdown remarkPlugins={[remarkGfm]}>
                                            {block.content || " "}
                                        </ReactMarkdown>
                                    </div>
                                </div>
                            </div>

                            {block.approvals?.length ? (
                                <div className="approval-inline">
                                    {block.approvals.map((approval) => (
                                        <Card key={approval.id} size="small" className="approval-inline-card" style={{
                                            background: 'var(--td-bg-elevated)',
                                            border: '1px solid var(--td-border-color)'
                                        }}>
                                            <Space direction="vertical" size={4} style={{width: "100%"}}>
                                                <Space wrap>
                                                    <Tag color="gold">{approval.riskLevel || "HIGH"}</Tag>
                                                    <Tag>{approval.toolName || "未知工具"}</Tag>
                                                </Space>
                                                <Text
                                                    style={{color: 'var(--td-text-base)'}}>{approval.reason || "当前工具调用需要人工审批后继续执行。"}</Text>
                                                {approval.toolInputJson ? (
                                                    <pre className="message-raw" style={{
                                                        background: 'var(--td-code-bg)',
                                                        border: '1px solid var(--td-code-border)',
                                                        color: 'var(--td-code-text)'
                                                    }}>{approval.toolInputJson}</pre>
                                                ) : null}
                                            </Space>
                                        </Card>
                                    ))}
                                    {activeSession?.pendingApprovals?.length ? (
                                        <Space style={{marginTop: 8}}>
                                            <Button onClick={() => void onReject?.()}>驳回</Button>
                                            <Button type="primary" onClick={() => void onApprove?.()}>通过</Button>
                                        </Space>
                                    ) : null}
                                </div>
                            ) : null}
                        </div>
                        );
                    })}
                </div>
            ),
        })) ?? [];

    // 监听消息变化，自动滚动到底部 - 这是必要的UI交互逻辑
    useEffect(() => {
        if (!activeSession || !activeSession.messages.length) {
            return;
        }

        const currentCount = activeSession.messages.length;

        // 如果消息数量增加，说明有新消息，滚动到底部
        if (currentCount > prevMessageCount) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setShouldScroll(true);
        }

        setPrevMessageCount(currentCount);
    // 只依赖 messages.length，避免 activeSession 引用变化（如 preview/updatedAt 更新）触发不必要的 effect
    }, [activeSession?.messages.length, prevMessageCount]);

    // 监听会话切换，打开新会话时自动滚动到底部
    useEffect(() => {
        const currentSessionId = activeSessionId;
        const prevSessionId = prevSessionIdRef.current;

        // 如果会话 ID 发生变化，说明切换了会话
        if (currentSessionId && currentSessionId !== prevSessionId) {
            prevSessionIdRef.current = currentSessionId;
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setShouldScroll(true);
        }
    }, [activeSessionId]);

    // 执行滚动
    useEffect(() => {
        if (shouldScroll && messagesEndRef.current) {
            // 使用 requestAnimationFrame 确保 DOM 渲染完成后滚动
            requestAnimationFrame(() => {
                // 找到最近的滚动父容器并滚动到底部
                const element = messagesEndRef.current;
                if (element) {
                    const scrollParent = element.closest('.chat-scroll-area');
                    if (scrollParent) {
                        scrollParent.scrollTo({
                            top: scrollParent.scrollHeight,
                            behavior: 'smooth',
                        });
                    } else {
                        // 备用方案：使用 scrollIntoView
                        element.scrollIntoView({behavior: 'smooth', block: 'end'});
                    }
                }
            });
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setShouldScroll(false);
        }
    }, [shouldScroll, bubbleItems.length]);

    return (
        <>
            <div className="main-content">
                <div className="conversation-surface">
                    <div className="conversation-surface-glow"/>
                    <div className="chat-scroll-area">
                        <div style={{padding: '16px 16px 0'}}>
                            <ModelSelector
                                selectedModelId={selectedModelId}
                                onSelect={onSelectModel}
                            />
                        </div>
                        {!activeSession ? (
                            <div className="empty-state-shell">
                                <Welcome
                                    variant="borderless"
                                    icon={<RobotOutlined/>}
                                    title="AI Agent 交互操作台"
                                    description="侧栏管理会话，主区流式查看回答、推理、工具结果与审批状态。未发送首条消息前，不会在后端落库空会话。"
                                />
                                <Prompts
                                    title="快速开始"
                                    wrap
                                    items={[
                                        {
                                            key: "arch",
                                            icon: <RadarChartOutlined/>,
                                            label: "分析当前仓库",
                                            description: "让 Agent 总结目录、模块边界与关键流程",
                                        },
                                        {
                                            key: "debug",
                                            icon: <FireOutlined/>,
                                            label: "定位流式输出问题",
                                            description: "检查 `/chat/stream` 的事件顺序、异常和 DONE 收尾",
                                        },
                                        {
                                            key: "tools",
                                            icon: <ToolOutlined/>,
                                            label: "检查高风险工具",
                                            description: "列出待审批工具调用并解释风险等级",
                                        },
                                    ]}
                                    onItemClick={({data}) =>
                                        onInputChange(String(data.label || data.description || ""))
                                    }
                                />
                            </div>
                        ) : (
                            <>
                                <div className="session-hero" style={{
                                    background: 'var(--td-bg-elevated)',
                                    border: '1px solid var(--td-border-light)',
                                    padding: '16px',
                                    borderRadius: '12px',
                                    marginBottom: '16px'
                                }}>
                                    <div>
                                        <Space size={8} wrap>
                                            <Tag color="blue">{activeSession.sessionId}</Tag>
                                            <Tag icon={<ClockCircleOutlined/>}>
                                                更新于 {formatRelative(activeSession.updatedAt)}
                                            </Tag>
                                            <Tag icon={<SendOutlined/>}>{activeSession.messageCount} 条消息</Tag>
                                        </Space>
                                    </div>
                                </div>

                                {activeSession.loadingHistory ? (
                                    <div className="loading-shell">
                                        <Spin size="large"/>
                                    </div>
                                ) : activeSession.messages.length === 0 ? (
                                    <div className="empty-state-shell">
                                        <Welcome
                                            variant="filled"
                                            icon={<RobotOutlined/>}
                                            title="新对话已就绪"
                                            description="输入你的第一条需求后，标题将自动提取首个问题的前 10 个字，并与后端会话保持一致。"
                                        />
                                    </div>
                                ) : (
                                    <>
                                        <Bubble.List
                                            items={bubbleItems}
                                            role={{
                                                user: {
                                                    placement: "end",
                                                    variant: "filled",
                                                    shape: "corner",
                                                    avatar: (
                                                        <Avatar
                                                            size={32}
                                                            style={{
                                                                background:
                                                                    "linear-gradient(135deg, var(--td-primary), var(--td-highlight))",
                                                                color: "var(--td-text-inverse)",
                                                                fontWeight: 600,
                                                            }}
                                                        >
                                                            {(user.nickname || user.userId).slice(0, 1).toUpperCase()}
                                                        </Avatar>
                                                    ),
                                                },
                                                assistant: {
                                                    placement: "start",
                                                    variant: "shadow",
                                                    shape: "round",
                                                    avatar: (
                                                        <Avatar
                                                            size={32}
                                                            src="/logo.svg"
                                                            style={{
                                                                background: "linear-gradient(135deg, var(--td-primary), var(--td-highlight))",
                                                            }}
                                                        />
                                                    ),
                                                },
                                                system: {
                                                    placement: "start",
                                                    variant: "outlined",
                                                    avatar: (
                                                        <Avatar
                                                            size={32}
                                                            style={{
                                                                background: "var(--td-bg-container)",
                                                                color: "var(--td-text-secondary)",
                                                                border: `1px solid var(--td-border-color)`,
                                                            }}
                                                            icon={<RobotOutlined />}
                                                        />
                                                    ),
                                                },
                                            }}
                                        />
                                        {/* 滚动锚点 */}
                                        <div ref={messagesEndRef} style={{height: "1px"}}/>
                                    </>
                                )}
                            </>
                        )}
                    </div>

                    <div className="sender-shell" style={{
                        background: 'var(--td-input-bg)',
                        borderTop: '1px solid var(--td-border-light)',
                        borderBottomLeftRadius: '12px',
                        borderBottomRightRadius: '12px',
                        padding: '0 0 4px',
                        boxShadow: 'inset 0 2px 6px rgba(0,0,0,0.02)',
                        transition: 'border-color 0.3s',
                    }}>
                        <SenderSubmitContext.Provider value={handleSubmit}>
                        <Sender
                            value={input}
                            onChange={onInputChange}
                            onSubmit={() => void onSend()}
                            onCancel={() => void onInterrupt()}
                            loading={busy}
                            submitType="enter"
                            components={{input: EnterSendTextArea as any}}
                            autoSize={{minRows: 1, maxRows: 6}}
                            placeholder="输入任务...（Enter 发送 / Shift+Enter 换行）"
                            style={{
                                resize: 'none',
                                color: 'var(--td-text-base)',
                                fontSize: '15px',
                                padding: '8px 12px 4px',
                                boxShadow: 'none',
                                background: 'transparent'
                            }}
                            suffix={busy ? (
                                <Tooltip title="中断输出">
                                    <Button
                                        type="primary"
                                        danger
                                        icon={<PauseCircleOutlined/>}
                                        onClick={() => void onInterrupt()}
                                        style={{
                                            background: 'var(--td-primary)',
                                            borderColor: 'var(--td-primary)',
                                            borderRadius: '8px',
                                            height: '32px',
                                            padding: '0 16px',
                                            fontWeight: 600,
                                            letterSpacing: '1px'
                                        }}
                                    >
                                        停
                                    </Button>
                                </Tooltip>
                            ) : (
                                <ChatSendButton
                                    disabled={!input.trim()}
                                    loading={busy}
                                    onClick={() => void onSend()}
                                />
                            )}
                            footer={() => {
                                return (
                                    <Space size={2}>
                                        <Tooltip title="附件">
                                            <Button type="text" icon={<PaperClipOutlined/>} size="small"
                                                    style={{color: 'var(--td-text-tertiary)', width: 26, height: 26}}/>
                                        </Tooltip>
                                        <Tooltip title="语音">
                                            <Button type="text" icon={<AudioOutlined/>} size="small"
                                                    style={{color: 'var(--td-text-tertiary)', width: 26, height: 26}}/>
                                        </Tooltip>
                                        <Tooltip title="快捷指令">
                                            <Button type="text" icon={<ThunderboltOutlined/>} size="small"
                                                    style={{color: 'var(--td-highlight)', width: 26, height: 26}}/>
                                        </Tooltip>
                                    </Space>
                                );
                            }}
                        />
                        </SenderSubmitContext.Provider>
                        <ContextUsagePanel
                            usage={contextUsage}
                            compacting={compacting}
                            onCompact={onCompactContext}
                        />
                    </div>
                </div>
            </div>
        </>
    );
}
