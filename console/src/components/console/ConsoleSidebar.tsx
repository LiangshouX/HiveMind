import { ApiOutlined, PlusOutlined } from "@ant-design/icons";
import { Conversations } from "@ant-design/x";
import { Badge, Button, Flex, Space, Tooltip, Typography } from "antd";
import type { ConversationGroupItem } from "../../types";
import { useTheme } from "../../providers/ThemeProvider";

const { Text } = Typography;

interface ConsoleSidebarProps {
  activeSessionId?: string;
  loadingSessions: boolean;
  groupedConversationItems: ConversationGroupItem[];
  onCreateSession: () => void;
  onRefreshSessions: () => void;
  onSelectSession: (sessionId: string) => void | Promise<void>;
}

export function ConsoleSidebar({
  activeSessionId,
  loadingSessions,
  groupedConversationItems,
  onCreateSession,
  onRefreshSessions,
  onSelectSession,
}: ConsoleSidebarProps) {
  const { isDarkMode } = useTheme();

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', padding: '16px' }}>
      <Button
        type="primary"
        icon={<PlusOutlined />}
        className="new-chat-button"
        onClick={onCreateSession}
        block
        style={{ marginBottom: '16px', height: '44px', fontSize: '14px', fontWeight: 600 }}
      >
        新建对话
      </Button>

      <div className="session-panel" style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <Flex align="center" justify="space-between" className="session-panel-head" style={{ marginBottom: '12px', padding: '0 4px' }}>
          <Space size={8}>
            <Badge status={loadingSessions ? "processing" : "success"} />
            <Text className="session-head-title" style={{ color: isDarkMode ? '#f3f7ff' : '#2c3e50', fontWeight: 600 }}>会话列表</Text>
          </Space>
          <Tooltip title="刷新会话">
            <Button type="text" icon={<ApiOutlined />} onClick={onRefreshSessions} style={{ color: isDarkMode ? '#a0b3d6' : '#6c757d' }} />
          </Tooltip>
        </Flex>

        <div style={{ flex: 1, overflow: 'auto' }}>
          <Conversations
            items={groupedConversationItems}
            activeKey={activeSessionId}
            onActiveChange={(value) => value && void onSelectSession(value)}
            groupable
            className="conversation-list"
            style={{
              background: 'transparent',
              '& .ant-conversations-item': {
                backgroundColor: 'transparent',
                borderBottom: `1px solid ${isDarkMode ? '#1e3a5f' : '#e9ecef'}`,
                '&:hover': {
                  backgroundColor: isDarkMode ? 'rgba(110, 168, 255, 0.1)' : 'rgba(74, 137, 220, 0.05)'
                }
              },
              '& .ant-conversations-item-active': {
                backgroundColor: isDarkMode ? 'rgba(110, 168, 255, 0.2)' : 'rgba(74, 137, 220, 0.1)',
                borderLeft: '3px solid #4a89dc'
              },
              '& .ant-conversations-group-title': {
                color: isDarkMode ? '#a0b3d6' : '#6c757d',
                fontSize: '12px',
                fontWeight: 600
              }
            }}
          />
        </div>
      </div>
    </div>
  );
}
