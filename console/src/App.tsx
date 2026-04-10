import {
  ApiOutlined,
  BranchesOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FireOutlined,
  LoadingOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  RadarChartOutlined,
  RobotOutlined,
  SendOutlined,
  StopOutlined,
  ToolOutlined,
} from "@ant-design/icons";
import { Bubble, Conversations, Prompts, Sender, Welcome } from "@ant-design/x";
import {
  Avatar,
  Badge,
  Button,
  Card,
  ConfigProvider,
  Drawer,
  Empty,
  Flex,
  Input,
  Layout,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  theme,
} from "antd";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/zh-cn";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import "./index.css";
import { useAgentConsole } from "./hooks/useAgentConsole";
import { FIXED_USER_ID, FIXED_USER_NAME } from "./types";

dayjs.extend(relativeTime);
dayjs.locale("zh-cn");

const { Sider, Header, Content } = Layout;
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

function App() {
  const {
    contextHolder,
    loadingSessions,
    busy,
    activeSession,
    activeSessionId,
    input,
    approvalComment,
    groupedConversationItems,
    apiBase,
    setInput,
    setApprovalComment,
    createNewSession,
    selectSession,
    sendMessage,
    interruptCurrent,
    refreshSessions,
    handleApprovalAction,
  } = useAgentConsole();

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
            <div
              key={block.id}
              className={`message-block tone-${blockTone(block.type)}`}
            >
              <div className="message-block-title">
                <Space size={8}>
                  {toneIcon(block.type)}
                  <span>{block.title}</span>
                  {block.toolName ? <Tag>{block.toolName}</Tag> : null}
                </Space>
              </div>

              {block.rawInput ? (
                <pre className="message-raw">{block.rawInput}</pre>
              ) : null}

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
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: "#6ea8ff",
          colorBgBase: "#07111f",
          colorTextBase: "#f3f7ff",
          borderRadius: 18,
          fontFamily:
            '"Segoe UI Variable Text", "PingFang SC", "Microsoft YaHei", sans-serif',
        },
      }}
    >
      {contextHolder}
      <Layout className="app-shell">
        <Sider width={320} className="app-sider">
          <div className="brand-card">
            <div className="brand-mark">TD</div>
            <div>
              <Title level={4} className="brand-title">
                Tang Dynasty Agent
              </Title>
              <Text className="brand-subtitle">
                智能体实验操作台
              </Text>
            </div>
          </div>

          <Button
            type="primary"
            icon={<PlusOutlined />}
            className="new-chat-button"
            onClick={createNewSession}
            block
          >
            新建会话
          </Button>

          <div className="sidebar-metrics">
            <Card size="small" className="metric-card">
              <Text className="metric-label">用户</Text>
              <Title level={5}>{FIXED_USER_NAME}</Title>
              <Text className="metric-foot">{FIXED_USER_ID}</Text>
            </Card>
            <Card size="small" className="metric-card">
              <Text className="metric-label">接口基址</Text>
              <Title level={5}>TDAgent</Title>
              <Text className="metric-foot">{apiBase}</Text>
            </Card>
          </div>

          <div className="session-panel">
            <Flex align="center" justify="space-between" className="session-panel-head">
              <Space size={8}>
                <Badge status={loadingSessions ? "processing" : "success"} />
                <Text className="session-head-title">会话列表</Text>
              </Space>
              <Tooltip title="刷新会话">
                <Button
                  type="text"
                  icon={<ApiOutlined />}
                  onClick={() => void refreshSessions(activeSessionId)}
                />
              </Tooltip>
            </Flex>

            <Conversations
              items={groupedConversationItems}
              activeKey={activeSessionId}
              onActiveChange={(value) => void selectSession(value)}
              groupable
              className="conversation-list"
            />
          </div>

          <div className="sidebar-footer">
            <Space size={10}>
              <Avatar
                size={40}
                style={{
                  background:
                    "linear-gradient(135deg, rgba(110,168,255,0.95), rgba(120,255,211,0.95))",
                  color: "#09111f",
                  fontWeight: 800,
                }}
              >
                XN
              </Avatar>
              <div>
                <Text className="brand-subtitle">前端生成会话 ID</Text>
                <Paragraph className="sidebar-footer-text">
                  与 `/chat/stream`、审批恢复、历史拉取无缝协同。
                </Paragraph>
              </div>
            </Space>
          </div>
        </Sider>

        <Layout className="app-main">
          <Header className="main-header">
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
                深色高密度智能体控制台，支持流式输出、推理面板、工具回执和人工审批继续执行。
              </Text>
            </div>

            <Space size={12}>
              <Button
                icon={<ApiOutlined />}
                onClick={() => activeSessionId && void refreshSessions(activeSessionId)}
              >
                同步后端
              </Button>
              <Button
                danger
                icon={<StopOutlined />}
                onClick={() => void interruptCurrent()}
                disabled={!busy}
              >
                中断执行
              </Button>
            </Space>
          </Header>

          <Content className="main-content">
            <div className="conversation-surface">
              <div className="conversation-surface-glow" />
              <div className="chat-scroll-area">
                {!activeSession ? (
                  <div className="empty-state-shell">
                    <Welcome
                      variant="borderless"
                      icon={<RobotOutlined />}
                      title="精致的 Agent 交互操作台"
                      description="侧栏管理会话，主区流式查看回答、推理、工具结果与审批状态。首次发送消息时前端自动生成 sessionId。"
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
                        setInput(String(data.label || data.description || ""))
                      }
                    />
                  </div>
                ) : (
                  <>
                    <div className="session-hero">
                      <div>
                        <Space size={8} wrap>
                          <Tag color="blue" icon={<PlayCircleOutlined />}>
                            {activeSession.sessionId}
                          </Tag>
                          <Tag icon={<ClockCircleOutlined />}>
                            更新于 {formatRelative(activeSession.updatedAt)}
                          </Tag>
                          <Tag icon={<SendOutlined />}>
                            {activeSession.messageCount} 条消息
                          </Tag>
                        </Space>
                        <Paragraph className="session-hero-text">
                          {activeSession.preview}
                        </Paragraph>
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
                          title="新会话已就绪"
                          description="输入你的第一条需求，前端会自动生成 sessionId 并与后端流式接口建立连接。"
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
                              children: "XN",
                            },
                          },
                          assistant: {
                            placement: "start",
                            variant: "shadow",
                            shape: "round",
                            avatar: {
                              style: {
                                background:
                                  "linear-gradient(135deg, #5e89ff, #75ffd2)",
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
                  onChange={setInput}
                  onSubmit={() => void sendMessage()}
                  loading={busy}
                  submitType="enter"
                  autoSize={{ minRows: 2, maxRows: 8 }}
                  placeholder="告诉 TDAgent 你要它做什么，支持长指令与多段上下文..."
                  prefix={
                    <Space size={8}>
                      <Tag color="cyan">SSE</Tag>
                      <Tag color="geekblue">Agent</Tag>
                    </Space>
                  }
                  actions={(origin) => (
                    <Space size={8}>
                      {busy ? (
                        <Button danger icon={<StopOutlined />} onClick={() => void interruptCurrent()}>
                          停止
                        </Button>
                      ) : null}
                      {origin}
                    </Space>
                  )}
                  footer={() => (
                    <Flex justify="space-between" align="center" className="sender-footer">
                      <Text className="sender-hint">
                        Enter 发送，Shift + Enter 换行。首轮自动附带 `user001 / XNLLUZ` 与前端生成的 `sessionId`。
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
          </Content>
        </Layout>

        <Drawer
          title="待审批工具调用"
          open={Boolean(activeSession?.pendingApprovals.length)}
          onClose={() => setApprovalComment("")}
          width={420}
          className="approval-drawer"
          extra={
            <Space>
              <Button onClick={() => void handleApprovalAction("reject")}>
                驳回并继续
              </Button>
              <Button
                type="primary"
                onClick={() => void handleApprovalAction("approve")}
              >
                通过并继续
              </Button>
            </Space>
          }
        >
          {!activeSession?.pendingApprovals.length ? (
            <Empty description="当前没有待审批项" />
          ) : (
            <Space direction="vertical" size={12} style={{ width: "100%" }}>
              {activeSession.pendingApprovals.map((approval) => (
                <Card key={approval.id} className="approval-card">
                  <Space direction="vertical" size={8} style={{ width: "100%" }}>
                    <Space wrap>
                      <Tag color="warning">{approval.riskLevel || "HIGH"}</Tag>
                      <Tag icon={<ToolOutlined />}>
                        {approval.toolName || "未知工具"}
                      </Tag>
                    </Space>
                    <Text>{approval.reason || "该工具调用被判定为高风险，需要人工确认。"}</Text>
                    {approval.toolInputJson ? (
                      <pre className="message-raw">{approval.toolInputJson}</pre>
                    ) : null}
                  </Space>
                </Card>
              ))}

              <Input.TextArea
                value={approvalComment}
                onChange={(event) => setApprovalComment(event.target.value)}
                autoSize={{ minRows: 3, maxRows: 6 }}
                placeholder="可填写审批备注，将随 approve / reject 请求一并提交..."
              />
            </Space>
          )}
        </Drawer>
      </Layout>
    </ConfigProvider>
  );
}

export default App;
