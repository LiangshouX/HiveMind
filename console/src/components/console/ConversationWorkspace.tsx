import {
  ApiOutlined,
  BranchesOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FireOutlined,
  LoadingOutlined,
  PauseCircleOutlined,
  RadarChartOutlined,
  RobotOutlined,
  SendOutlined,
  StopOutlined,
  ToolOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Bubble, Prompts, Sender, Welcome } from "@ant-design/x";
import { Avatar, Button, Card, Flex, Space, Spin, Tag, Typography } from "antd";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/zh-cn";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import type { AuthUser, SessionState } from "../../types";

dayjs.extend(relativeTime);
dayjs.locale("zh-cn");

const { Text, Title, Paragraph } = Typography;

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
      return <BranchesOutlined />;
    case "tool_use":
    case "tool_result":
      return <ToolOutlined />;
    case "approval":
      return <PauseCircleOutlined />;
    case "error":
      return <RadarChartOutlined />;
    case "result":
      return <CheckCircleOutlined />;
    default:
      return (
        <Avatar
          size={22}
          style={{
            background: "linear-gradient(135deg, #5e89ff, #75ffd2)",
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
  onInputChange: (value: string) => void;
  onRefreshSession: () => void;
  onInterrupt: () => void | Promise<void>;
  onSend: () => void | Promise<void>;
  onOpenProfile: () => void;
}

export function ConversationWorkspace({
  user,
  busy,
  input,
  activeSession,
  activeSessionId,
  onInputChange,
  onRefreshSession,
  onInterrupt,
  onSend,
  onOpenProfile,
}: ConversationWorkspaceProps) {
  const bubbleItems =
    activeSession?.messages.map((message) => ({
      key: message.id,
      role: message.role,
      placement: message.role === "user" ? ("end" as const) : ("start" as const),
      typing: message.role === "assistant" && message.streaming ? { step: 3, interval: 14 } : false,
      content: (
        <div className="message-stack">
          <div className="message-meta">
            <Space size={8}>
              <Text className="message-author">{message.name}</Text>
              <Text className="message-time">{formatTime(message.createdAt)}</Text>
            </Space>
            {message.streaming ? (
              <Tag color="processing" icon={<LoadingOutlined />}>
                流式输出中
              </Tag>
            ) : message.failed ? (
              <Tag color="error">已中断</Tag>
            ) : null}
          </div>

          {message.blocks.map((block) => (
            <div key={block.id} className={`message-block tone-${blockTone(block.type)}`}>
              <div className="message-block-title">
                <Space size={8}>
                  {toneIcon(block.type)}
                  <span>{block.title}</span>
                  {block.toolName ? <Tag>{block.toolName}</Tag> : null}
                </Space>
              </div>

              {block.rawInput ? <pre className="message-raw">{block.rawInput}</pre> : null}

              <div className="markdown-body">
                <ReactMarkdown remarkPlugins={[remarkGfm]}>
                  {block.content || " "}
                </ReactMarkdown>
              </div>

              {block.approvals?.length ? (
                <div className="approval-inline">
                  {block.approvals.map((approval) => (
                    <Card key={approval.id} size="small" className="approval-inline-card">
                      <Space direction="vertical" size={4} style={{ width: "100%" }}>
                        <Space wrap>
                          <Tag color="gold">{approval.riskLevel || "HIGH"}</Tag>
                          <Tag>{approval.toolName || "未知工具"}</Tag>
                        </Space>
                        <Text>{approval.reason || "当前工具调用需要人工审批后继续执行。"}</Text>
                        {approval.toolInputJson ? (
                          <pre className="message-raw">{approval.toolInputJson}</pre>
                        ) : null}
                      </Space>
                    </Card>
                  ))}
                </div>
              ) : null}
            </div>
          ))}
        </div>
      ),
    })) ?? [];

  return (
    <>
      <div className="main-header">
        <div>
          <Space size={10} align="center">
            <Title level={3} className="header-title">
              {activeSession?.title || "Agent 对话工作台"}
            </Title>
            {busy ? (
              <Tag color="processing" icon={<LoadingOutlined />}>
                智能体运行中
              </Tag>
            ) : activeSession?.pendingApprovals.length ? (
              <Tag color="warning" icon={<ClockCircleOutlined />}>
                等待审批
              </Tag>
            ) : (
              <Tag color="success" icon={<CheckCircleOutlined />}>
                已就绪
              </Tag>
            )}
          </Space>
          <Text className="header-description">
            使用登录用户驱动会话、审批与历史读取，Session 以 session_id 为唯一标识。
          </Text>
        </div>

        <Space size={12}>
          <Button icon={<UserOutlined />} onClick={onOpenProfile}>
            {user.nickname}
          </Button>
          <Button icon={<ApiOutlined />} onClick={onRefreshSession} disabled={!activeSessionId}>
            同步后端
          </Button>
          <Button danger icon={<StopOutlined />} onClick={() => void onInterrupt()} disabled={!busy}>
            中断执行
          </Button>
        </Space>
      </div>

      <div className="main-content">
        <div className="conversation-surface">
          <div className="conversation-surface-glow" />
          <div className="chat-scroll-area">
            {!activeSession ? (
              <div className="empty-state-shell">
                <Welcome
                  variant="borderless"
                  icon={<RobotOutlined />}
                  title="精致的 Agent 交互操作台"
                  description="侧栏管理会话，主区流式查看回答、推理、工具结果与审批状态。未发送首条消息前，不会在后端落库空会话。"
                />
                <Prompts
                  title="快速开始"
                  wrap
                  items={[
                    {
                      key: "arch",
                      icon: <RadarChartOutlined />,
                      label: "分析当前仓库",
                      description: "让 Agent 总结目录、模块边界与关键流程",
                    },
                    {
                      key: "debug",
                      icon: <FireOutlined />,
                      label: "定位流式输出问题",
                      description: "检查 `/chat/stream` 的事件顺序、异常和 DONE 收尾",
                    },
                    {
                      key: "tools",
                      icon: <ToolOutlined />,
                      label: "检查高风险工具",
                      description: "列出待审批工具调用并解释风险等级",
                    },
                  ]}
                  onItemClick={({ data }) =>
                    onInputChange(String(data.label || data.description || ""))
                  }
                />
              </div>
            ) : (
              <>
                <div className="session-hero">
                  <div>
                    <Space size={8} wrap>
                      <Tag color="blue">{activeSession.sessionId}</Tag>
                      <Tag icon={<ClockCircleOutlined />}>
                        更新于 {formatRelative(activeSession.updatedAt)}
                      </Tag>
                      <Tag icon={<SendOutlined />}>{activeSession.messageCount} 条消息</Tag>
                    </Space>
                    <Paragraph className="session-hero-text">{activeSession.preview}</Paragraph>
                  </div>
                </div>

                {activeSession.loadingHistory ? (
                  <div className="loading-shell">
                    <Spin size="large" />
                  </div>
                ) : activeSession.messages.length === 0 ? (
                  <div className="empty-state-shell">
                    <Welcome
                      variant="filled"
                      icon={<RobotOutlined />}
                      title="新对话已就绪"
                      description="输入你的第一条需求后，标题将自动提取首个问题的前 10 个字，并与后端会话保持一致。"
                    />
                  </div>
                ) : (
                  <Bubble.List
                    items={bubbleItems}
                    autoScroll
                    roles={{
                      user: {
                        placement: "end",
                        variant: "filled",
                        shape: "corner",
                        avatar: {
                          style: {
                            background:
                              "linear-gradient(135deg, rgba(110,168,255,0.96), rgba(120,255,211,0.96))",
                            color: "#08101d",
                            fontWeight: 800,
                          },
                          children: user.nickname.slice(0, 1).toUpperCase(),
                        },
                      },
                      assistant: {
                        placement: "start",
                        variant: "shadow",
                        shape: "round",
                        avatar: {
                          style: {
                            background: "linear-gradient(135deg, #5e89ff, #75ffd2)",
                            color: "#08111f",
                            fontWeight: 800,
                          },
                          children: "TD",
                        },
                      },
                      system: {
                        placement: "start",
                        variant: "outlined",
                        avatar: {
                          style: {
                            background: "rgba(255,255,255,0.08)",
                            color: "#ffffff",
                          },
                          icon: <ApiOutlined />,
                        },
                      },
                    }}
                  />
                )}
              </>
            )}
          </div>

          <div className="sender-shell">
            <Sender
              value={input}
              onChange={onInputChange}
              onSubmit={() => void onSend()}
              loading={busy}
              submitType="enter"
              autoSize={{ minRows: 2, maxRows: 8 }}
              placeholder="告诉 TDAgent 你要它做什么，支持长指令与多段上下文..."
              prefix={
                <Space size={8}>
                  <Tag color="cyan">JWT</Tag>
                  <Tag color="geekblue">SSE</Tag>
                </Space>
              }
              actions={(origin) => (
                <Space size={8}>
                  {busy ? (
                    <Button danger icon={<StopOutlined />} onClick={() => void onInterrupt()}>
                      停止
                    </Button>
                  ) : null}
                  {origin}
                </Space>
              )}
              footer={() => (
                <Flex justify="space-between" align="center" className="sender-footer">
                  <Text className="sender-hint">
                    Enter 发送，Shift + Enter 换行。Token 24 小时内保持登录，会话切换仅以 session_id 驱动。
                  </Text>
                  <Space size={6}>
                    <Tag bordered={false} color="processing">
                      流式增量
                    </Tag>
                    <Tag bordered={false}>审批恢复</Tag>
                  </Space>
                </Flex>
              )}
            />
          </div>
        </div>
      </div>
    </>
  );
}
