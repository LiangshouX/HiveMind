import { MoreOutlined, PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import { Badge, Button, Dropdown, Flex, Space, Tooltip, Typography, Modal } from "antd";
import type { ConversationGroupItem } from "../../types";
import { useMemo } from "react";

const { Text } = Typography;

interface ConsoleSidebarProps {
  activeSessionId?: string;
  loadingSessions: boolean;
  groupedConversationItems: ConversationGroupItem[];
  onCreateSession: () => void;
  onRefreshSessions: () => void;
  onSelectSession: (sessionId: string) => void | Promise<void>;
  onDeleteSession: (sessionId: string) => void | Promise<void>;
}

// 分组顺序
const GROUP_ORDER = ["今天", "七天内", "更早"];

export function ConsoleSidebar({
  activeSessionId,
  loadingSessions,
  groupedConversationItems,
  onCreateSession,
  onRefreshSessions,
  onSelectSession,
  onDeleteSession,
}: ConsoleSidebarProps) {
  // 按 group 分组
  const groupedItems = useMemo(() => {
    const groups = new Map<string, ConversationGroupItem[]>();
    for (const item of groupedConversationItems) {
      const group = item.group || "更早";
      if (!groups.has(group)) {
        groups.set(group, []);
      }
      groups.get(group)!.push(item);
    }
    // 按指定顺序排序
    const sorted = GROUP_ORDER
      .filter((g) => groups.has(g))
      .map((group) => ({
        group,
        items: groups.get(group)!,
      }));
    return sorted;
  }, [groupedConversationItems]);

  return (
    <div style={{
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      background: 'var(--td-bg-elevated)',
      borderRadius: '12px',
      border: '1px solid var(--td-border-light)',
      boxShadow: 'var(--td-shadow-base)',
      overflow: 'hidden'
    }}>
      {/* 新建对话按钮 */}
      <div style={{ padding: '20px', borderBottom: '1px solid var(--td-border-light)' }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={onCreateSession}
          block
          style={{
            height: '44px',
            borderRadius: '8px',
            background: 'var(--td-primary)',
            border: 'none',
            boxShadow: 'var(--td-shadow-primary)',
            fontSize: '16px',
            fontWeight: 'bold',
            letterSpacing: '2px',
            color: 'var(--td-text-inverse)'
          }}
        >
          新建对话
        </Button>
      </div>

      {/* 会话列表面板 */}
      <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {/* 面板头部 */}
        <Flex align="center" justify="space-between" style={{ marginBottom: '12px', padding: '16px 16px 8px' }}>
          <Space size={8}>
            <Badge status={loadingSessions ? "processing" : "success"} />
            <Text style={{
              color: 'var(--td-text-base)',
              fontWeight: 600,
              fontSize: '14px'
            }}>
              近期对话
            </Text>
          </Space>
          <Tooltip title="刷新会话">
            <Button
              type="text"
              icon={<ReloadOutlined />}
              onClick={onRefreshSessions}
              size="small"
              style={{ color: 'var(--td-text-tertiary)' }}
            />
          </Tooltip>
        </Flex>

        {/* 会话列表 - 带分组 */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '0 12px 12px', minHeight: 0 }}>
          {groupedItems.length === 0 ? (
            <div style={{
              padding: '40px 16px',
              textAlign: 'center',
              color: 'var(--td-text-tertiary)',
              fontSize: '13px'
            }}>
              暂无对话记录
            </div>
          ) : (
            groupedItems.map(({ group, items }) => (
              <div key={group} style={{ marginBottom: group !== groupedItems[0].group ? '8px' : '0' }}>
                {/* 分组标题 */}
                <div style={{
                  padding: '8px 8px 4px',
                  fontSize: '12px',
                  fontWeight: 600,
                  color: 'var(--td-text-tertiary)',
                  letterSpacing: '1px',
                  textTransform: 'uppercase'
                }}>
                  {group}
                </div>
                {/* 分组下的会话项 */}
                {items.map((item) => {
                  const isActive = item.key === activeSessionId;
                  return (
                    <div
                      key={item.key}
                      style={{
                        padding: '12px 16px',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        marginBottom: '8px',
                        background: isActive ? 'var(--td-item-selected-bg)' : 'transparent',
                        border: `1px solid ${isActive ? 'var(--td-border-color)' : 'transparent'}`,
                        transition: 'all 0.3s ease',
                        position: 'relative',
                        overflow: 'hidden'
                      }}
                      className="chat-session-item"
                    >
                      {isActive && (
                        <div style={{
                          position: 'absolute',
                          left: 0,
                          top: 0,
                          bottom: 0,
                          width: '3px',
                          background: 'var(--td-highlight)'
                        }} />
                      )}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div
                          style={{ flex: 1, minWidth: 0, marginRight: '8px' }}
                          onClick={() => onSelectSession(item.key)}
                        >
                          <Text
                            strong
                            style={{
                              color: isActive ? 'var(--td-highlight)' : 'var(--td-text-base)',
                              fontSize: '14px',
                              display: 'block',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap'
                            }}
                          >
                            {item.label}
                          </Text>
                        </div>
                        <Dropdown
                          menu={{
                            items: [
                              {
                                key: 'delete',
                                label: '删除此对话',
                                danger: true,
                                icon: <span style={{ color: '#ff4d4f' }}>🗑️</span>,
                              },
                            ],
                            onClick: ({ key }) => {
                              if (key === 'delete') {
                                Modal.confirm({
                                  title: '确认删除',
                                  content: `确定要删除"${item.label}"吗？此操作不可恢复。`,
                                  okText: '确认删除',
                                  okType: 'danger',
                                  cancelText: '取消',
                                  onOk: () => onDeleteSession(item.key),
                                });
                              }
                            },
                          }}
                          trigger={['click']}
                          placement="bottomRight"
                        >
                          <Button
                            type="text"
                            size="small"
                            icon={<MoreOutlined style={{ fontSize: '16px' }} />}
                            onClick={(e) => e.stopPropagation()}
                            style={{
                              color: 'var(--td-text-tertiary)',
                              padding: '4px',
                              height: 'auto',
                            }}
                          />
                        </Dropdown>
                      </div>
                    </div>
                  );
                })}
              </div>
            ))
          )}
        </div>
      </div>

      <style>{`
        .chat-session-item:hover {
          background: var(--td-item-hover-bg) !important;
        }
      `}</style>
    </div>
  );
}
