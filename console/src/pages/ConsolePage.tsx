import {Avatar, Button, ConfigProvider, Layout, Space, theme, Typography} from "antd";
import {LogoutOutlined, MoonOutlined, SunOutlined, UserOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import {ApprovalDrawerPanel} from "../components/console/ApprovalDrawerPanel";
import {ConsoleSidebar} from "../components/console/ConsoleSidebar";
import {ConversationWorkspace} from "../components/console/ConversationWorkspace";
import {useAgentConsole} from "../hooks/useAgentConsole";
import {useAuth} from "../providers/AuthProvider";
import {useTheme} from "../providers/ThemeProvider";

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export function ConsolePage() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();
  const {
    contextHolder,
    loadingSessions,
    busy,
    activeSession,
    activeSessionId,
    input,
    approvalComment,
    groupedConversationItems,
    setInput,
    setApprovalComment,
    createNewSession,
    selectSession,
    sendMessage,
    interruptCurrent,
    refreshSessions,
    handleApprovalAction,
  } = useAgentConsole(user!);

  return (
    <ConfigProvider
      theme={{
        algorithm: isDarkMode ? theme.darkAlgorithm : theme.defaultAlgorithm,
        token: {
          colorPrimary: "#4a89dc",
          colorBgBase: isDarkMode ? "#07111f" : "#f8f9fa",
          colorTextBase: isDarkMode ? "#f3f7ff" : "#2c3e50",
          borderRadius: 12,
          fontFamily:
            '"Segoe UI Variable Text", "PingFang SC", "Microsoft YaHei", sans-serif',
        },
      }}
    >
      {contextHolder}
      <Layout className="app-shell" style={{ minHeight: '100vh' }}>
        <Header className="app-header" style={{ background: isDarkMode ? '#0a182e' : '#ffffff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: `1px solid ${isDarkMode ? '#1e3a5f' : '#e9ecef'}` }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div className="brand-mark" style={{ width: 36, height: 36, borderRadius: 8, background: 'linear-gradient(135deg, #4a89dc, #37bc9b)', color: isDarkMode ? '#09111f' : '#ffffff', fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '16px' }}>
              TD
            </div>
            <Text className="brand-title" style={{ fontSize: '18px', fontWeight: 600, color: isDarkMode ? '#f3f7ff' : '#2c3e50' }}>
              Tang Dynasty Agent
            </Text>
          </div>
          <Space size={12}>
            <Button 
              type="text" 
              icon={isDarkMode ? <SunOutlined /> : <MoonOutlined />} 
              onClick={toggleTheme}
              style={{ color: isDarkMode ? '#f3f7ff' : '#2c3e50' }}
            >
              {isDarkMode ? '切换亮色模式' : '切换暗色模式'}
            </Button>
            <Button 
              type="text" 
              icon={<UserOutlined />}
              onClick={() => navigate("/profile")}
              style={{ color: isDarkMode ? '#f3f7ff' : '#2c3e50' }}
            >
              个人资料
            </Button>
            <Button
              type="text" 
              icon={<LogoutOutlined />} 
              onClick={() => {
                logout();
                navigate("/login", { replace: true });
              }}
              style={{ color: isDarkMode ? '#f3f7ff' : '#2c3e50' }}
            >
              退出登录
            </Button>
            <Avatar
              size={36}
              style={{
                background: 'linear-gradient(135deg, rgba(74,137,220,0.95), rgba(55,188,155,0.95))',
                color: isDarkMode ? '#09111f' : '#ffffff',
                fontWeight: 800,
              }}
            >
              {user?.nickname?.slice(0, 1).toUpperCase()}
            </Avatar>
          </Space>
        </Header>
        <Layout>
          <Sider width={280} className="app-sider" style={{ background: isDarkMode ? '#0a182e' : '#ffffff', borderRight: `1px solid ${isDarkMode ? '#1e3a5f' : '#e9ecef'}` }}>
            <ConsoleSidebar
              activeSessionId={activeSessionId}
              loadingSessions={loadingSessions}
              groupedConversationItems={groupedConversationItems}
              onCreateSession={createNewSession}
              onRefreshSessions={() => void refreshSessions(activeSessionId)}
              onSelectSession={selectSession}
            />
          </Sider>

          <Content className="app-main" style={{ background: isDarkMode ? '#07111f' : '#f8f9fa', padding: '24px' }}>
            <ConversationWorkspace
              user={user!}
              busy={busy}
              input={input}
              activeSession={activeSession}
              activeSessionId={activeSessionId}
              onInputChange={setInput}
              onRefreshSession={() => void refreshSessions(activeSessionId)}
              onInterrupt={interruptCurrent}
              onSend={sendMessage}
            />
          </Content>
        </Layout>

        <ApprovalDrawerPanel
          approvals={activeSession?.pendingApprovals ?? []}
          approvalComment={approvalComment}
          onApprovalCommentChange={(event) => setApprovalComment(event.target.value)}
          onApprove={() => handleApprovalAction("approve")}
          onReject={() => handleApprovalAction("reject")}
        />
      </Layout>
    </ConfigProvider>
  );
}
