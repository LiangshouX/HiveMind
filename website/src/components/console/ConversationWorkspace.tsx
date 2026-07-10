import {
    AudioOutlined,
    BranchesOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    FireOutlined,
    LoadingOutlined,
    PaperClipOutlined,
    PauseCircleOutlined,
    RadarChartOutlined,
    RobotOutlined,
    SendOutlined,
    ThunderboltOutlined,
    ToolOutlined,
} from "@ant-design/icons";
import {Bubble, Prompts, Sender, Welcome} from "@ant-design/x";
import {Avatar, Button, Card, Space, Spin, Tag, Tooltip, Typography} from "antd";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/zh-cn";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import {useEffect, useRef, useState} from "react";
import type {AuthUser, SessionState} from "../../types";
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
                        background: "linear-gradient(135deg, #b32934, #c7a434)",
                        color: "#08111f",
                        fontSize: 11,
                        fontWeight: 900,
                    }}
                >
                    TD
                </Avatar>
            );
    }
}

interface ConversationWorkspaceProps {
    user: AuthUser;
    busy: boolean;
    input: string;
    activeSession?: SessionState;
    activeSessionId?: string;
    selectedModelId?: string;
    onInputChange: (value: string) => void;
    onSelectModel?: (providerId: string, modelId: string) => void;
    onRefreshSession: () => void;
    onInterrupt: () => void | Promise<void>;
    onSend: () => void | Promise<void>;
    onApprove?: () => void | Promise<void>;
    onReject?: () => void | Promise<void>;
}

export function ConversationWorkspace({
                                          user,
                                          busy,
                                          input,
                                          activeSession,
                                          activeSessionId,
                                          selectedModelId,
                                          onInputChange,
                                          onSelectModel,
                                          onInterrupt,
                                          onSend,
                                          onApprove,
                                          onReject,
                                      }: ConversationWorkspaceProps) {
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const [prevMessageCount, setPrevMessageCount] = useState(0);
    const prevSessionIdRef = useRef<string | undefined>(undefined);
    const [shouldScroll, setShouldScroll] = useState(false);

    const bubbleItems =
        activeSession?.messages.map((message) => ({
            key: message.id,
            role: message.role,
            // 禁用 typing 动画以避免与 SSE 实时更新冲突，提升流式输出流畅度
            typing: false,
            content: (
                <div className="message-stack">
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

                    {message.blocks.map((block) => (
                        <div key={block.id} className={`message-block tone-${blockTone(block.type)}`}>
                            {block.title || block.toolName ? (
                                <div className="message-block-title" style={{color: 'var(--td-text-base)'}}>
                                    <Space size={8}>
                                        {toneIcon(block.type)}
                                        {block.title ? <span>{block.title}</span> : null}
                                        {block.toolName ? <Tag>{block.toolName}</Tag> : null}
                                    </Space>
                                </div>
                            ) : null}

                            {/* 工具类型消息不需要展示 rawInput，因为 content 已经是代码块格式 */}
                            {block.rawInput && block.type !== "tool_use" && block.type !== "tool_result" ? (
                                <pre className="message-raw" style={{
                                    background: 'var(--td-code-bg)',
                                    border: '1px solid var(--td-code-border)',
                                    color: 'var(--td-code-text)'
                                }}>{block.rawInput}</pre>
                            ) : null}

                            <div className="markdown-body" style={{color: 'var(--td-text-base)'}}>
                                <ReactMarkdown remarkPlugins={[remarkGfm]}>
                                    {block.content || " "}
                                </ReactMarkdown>
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
                    ))}
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
                                                            style={{
                                                                background: "linear-gradient(135deg, var(--td-primary), var(--td-highlight))",
                                                                color: "var(--td-text-inverse)",
                                                                fontWeight: 600,
                                                            }}
                                                        >
                                                            唐
                                                        </Avatar>
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
                        background: 'var(--td-bg-elevated)',
                        borderTop: '1px solid var(--td-border-light)',
                        borderBottomLeftRadius: '12px',
                        borderBottomRightRadius: '12px',
                        padding: '24px'
                    }}>
                        <div style={{
                            background: 'var(--td-input-bg)',
                            border: '1px solid var(--td-border-color)',
                            borderRadius: '12px',
                            padding: '12px 16px',
                            boxShadow: 'inset 0 2px 8px rgba(0,0,0,0.02), 0 2px 8px var(--td-pattern-color)',
                            transition: 'border-color 0.3s',
                            position: 'relative'
                        }}>
                            <Sender
                                value={input}
                                onChange={onInputChange}
                                onSubmit={() => void onSend()}
                                onCancel={() => void onInterrupt()}
                                loading={busy}
                                submitType="enter"
                                autoSize={{minRows: 2, maxRows: 8}}
                                placeholder="输入任务..."
                                style={{
                                    resize: 'none',
                                    color: 'var(--td-text-base)',
                                    fontSize: '15px',
                                    padding: '0 0 16px 0',
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
                                                height: '40px',
                                                padding: '0 20px',
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
                                        <Space>
                                            <Tooltip title="附件">
                                                <Button type="text" icon={<PaperClipOutlined/>}
                                                        style={{color: 'var(--td-text-tertiary)'}}/>
                                            </Tooltip>
                                            <Tooltip title="语音">
                                                <Button type="text" icon={<AudioOutlined/>}
                                                        style={{color: 'var(--td-text-tertiary)'}}/>
                                            </Tooltip>
                                            <Tooltip title="快捷指令">
                                                <Button type="text" icon={<ThunderboltOutlined/>}
                                                        style={{color: 'var(--td-highlight)'}}/>
                                            </Tooltip>
                                        </Space>
                                    );
                                }}
                            />
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}
