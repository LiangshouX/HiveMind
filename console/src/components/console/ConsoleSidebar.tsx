import { ApiOutlined, LogoutOutlined, PlusOutlined, SettingOutlined } from "@ant-design/icons";
import { Conversations } from "@ant-design/x";
import { Avatar, Badge, Button, Card, Flex, Space, Tooltip, Typography } from "antd";
import type { AuthUser, ConversationGroupItem } from "../../types";

const { Text, Title, Paragraph } = Typography;

interface ConsoleSidebarProps {
  user: AuthUser;
  apiBase: string;
  activeSessionId?: string;
  loadingSessions: boolean;
  groupedConversationItems: ConversationGroupItem[];
  onCreateSession: () => void;
  onRefreshSessions: () => void;
  onSelectSession: (sessionId: string) => void | Promise<void>;
  onOpenProfile: () => void;
  onLogout: () => void;
}

export function ConsoleSidebar({
  user,
  apiBase,
  activeSessionId,
  loadingSessions,
  groupedConversationItems,
  onCreateSession,
  onRefreshSessions,
  onSelectSession,
  onOpenProfile,
  onLogout,
}: ConsoleSidebarProps) {
  return (
    <>
      <div className="brand-card">
        <div className="brand-mark">TD</div>
        <div>
          <Title level={4} className="brand-title">
            Tang Dynasty Agent
          </Title>
          <Text className="brand-subtitle">智能体实验操作台</Text>
        </div>
      </div>

      <Button
        type="primary"
        icon={<PlusOutlined />}
        className="new-chat-button"
        onClick={onCreateSession}
        block
      >
        新建对话
      </Button>

      <div className="sidebar-metrics">
        <Card size="small" className="metric-card">
          <Text className="metric-label">当前用户</Text>
          <Title level={5}>{user.nickname}</Title>
          <Text className="metric-foot">{user.userId}</Text>
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
            <Button type="text" icon={<ApiOutlined />} onClick={onRefreshSessions} />
          </Tooltip>
        </Flex>

        <Conversations
          items={groupedConversationItems}
          activeKey={activeSessionId}
          onActiveChange={(value) => value && void onSelectSession(value)}
          groupable
          className="conversation-list"
        />
      </div>

      <div className="sidebar-footer">
        <Space size={12} align="start">
          <Avatar
            size={44}
            style={{
              background:
                "linear-gradient(135deg, rgba(110,168,255,0.95), rgba(120,255,211,0.95))",
              color: "#09111f",
              fontWeight: 800,
            }}
          >
            {user.nickname.slice(0, 1).toUpperCase()}
          </Avatar>
          <div className="sidebar-footer-main">
            <Text className="brand-subtitle">{user.role}</Text>
            <Paragraph className="sidebar-footer-text">
              你的所有对话、审批与个人设置已与登录身份绑定。
            </Paragraph>
            <Space size={8}>
              <Button icon={<SettingOutlined />} onClick={onOpenProfile}>
                个人资料
              </Button>
              <Button icon={<LogoutOutlined />} onClick={onLogout}>
                退出登录
              </Button>
            </Space>
          </div>
        </Space>
      </div>
    </>
  );
}
