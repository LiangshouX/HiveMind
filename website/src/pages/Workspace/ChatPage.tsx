import {ConfigProvider, Layout, theme} from "antd";
import {useNavigate} from "react-router-dom";
import {ApprovalDrawerPanel} from "../../components/console/ApprovalDrawerPanel.tsx";
import {ConsoleSidebar} from "../../components/console/ConsoleSidebar.tsx";
import {ConversationWorkspace} from "../../components/console/ConversationWorkspace.tsx";
import {useAgentConsole} from "../../hooks/useAgentConsole.ts";
import {useAuth} from "../../providers/AuthProvider.tsx";
import {useTheme} from "../../providers/ThemeProvider.tsx";

const {Sider, Content} = Layout;

export function ChatPage() {
    const navigate = useNavigate();
    const {user} = useAuth();
    const {isDarkMode} = useTheme();
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
        deleteSession,
        sendMessage,
        interruptCurrent,
        refreshSessions,
        handleApprovalAction,
    } = useAgentConsole(user!, {
        onNavigateToSession: (sessionId, replace) => {
            navigate(`/chat/${sessionId}`, {replace});
        },
        onNavigateToHome: (replace) => {
            navigate(`/chat`, {replace});
        },
    });

    return (
        <ConfigProvider
            theme={{
                algorithm: isDarkMode ? theme.darkAlgorithm : theme.defaultAlgorithm,
                token: {
                    colorPrimary: "#b32934",
                    colorBgBase: isDarkMode ? "#07111f" : "#f8f9fa",
                    colorTextBase: isDarkMode ? "#f3f7ff" : "#2c3e50",
                    borderRadius: 12,
                    fontFamily:
                        '"Noto Sans SC", "PingFang SC", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
                },
            }}
        >
            {contextHolder}
            <Layout style={{ 
                height: 'calc(100vh - 120px)', 
                width: '100%', 
                background: 'transparent', 
                display: 'flex', 
                flexDirection: 'row', 
                gap: '24px', 
                overflow: 'hidden', 
                minHeight: 0 
            }}>
                {/* 会话侧边栏 */}
                <Sider
                    width={300}
                    style={{
                        background: 'transparent',
                        overflow: 'hidden',
                        display: 'flex',
                        flexDirection: 'column',
                        height: '100%',
                        minHeight: 0
                    }}
                >
                    <ConsoleSidebar
                        activeSessionId={activeSessionId}
                        loadingSessions={loadingSessions}
                        groupedConversationItems={groupedConversationItems}
                        onCreateSession={createNewSession}
                        onRefreshSessions={() => void refreshSessions(activeSessionId)}
                        onSelectSession={selectSession}
                        onDeleteSession={deleteSession}
                    />
                </Sider>

                {/* 聊天内容区 */}
                <Content style={{
                    display: 'flex',
                    flexDirection: 'column',
                    background: 'var(--td-chat-panel-bg)',
                    borderRadius: '12px',
                    border: '1px solid var(--td-border-light)',
                    backdropFilter: 'blur(10px)',
                    boxShadow: 'var(--td-shadow-base)',
                    height: '100%',
                    overflow: 'hidden',
                    minHeight: 0
                }}>
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

                <ApprovalDrawerPanel
                    approvals={activeSession?.pendingApprovals ?? []}
                    approvalComment={approvalComment}
                    onApprovalCommentChange={(event: React.ChangeEvent<HTMLTextAreaElement>) => setApprovalComment(event.target.value)}
                    onApprove={() => handleApprovalAction("approve")}
                    onReject={() => handleApprovalAction("reject")}
                />
            </Layout>
        </ConfigProvider>
    );
}
