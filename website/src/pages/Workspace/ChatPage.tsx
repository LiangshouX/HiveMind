import {ConfigProvider, Layout, theme} from "antd";
import {LeftOutlined, RightOutlined} from "@ant-design/icons";
import {Tooltip} from "antd";
import {useNavigate} from "react-router-dom";
import {ConsoleSidebar} from "../../components/console/ConsoleSidebar.tsx";
import {ConversationWorkspace} from "../../components/console/ConversationWorkspace.tsx";
import {useAgentConsole} from "../../hooks/useAgentConsole.ts";
import {useAuth} from "../../providers/AuthProvider.tsx";
import {useTheme} from "../../providers/ThemeProvider.tsx";
import {useResponsiveCollapse} from "../../hooks/useResponsiveCollapse.ts";

const {Content} = Layout;

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
        groupedConversationItems,
        selectedModelId,
        setInput,
        selectModel,
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

    const {
        collapsed: sidebarCollapsed,
        toggle: toggleSidebar,
    } = useResponsiveCollapse({
        autoCollapseWidth: 1024,
        expandedWidth: 280,
        collapsedWidth: 0,
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
                height: 'calc(100vh - 94px)',
                width: '100%',
                background: 'transparent',
                display: 'flex',
                flexDirection: 'row',
                gap: '16px',
                overflow: 'hidden',
                minHeight: 0
            }}>
                {/* 会话侧边栏区域：sidebar + 折叠按钮 */}
                <div style={{
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'stretch',
                    flexShrink: 0,
                    height: '100%',
                    minHeight: 0,
                    position: 'relative',
                }}>
                    {/* 侧边栏内容 */}
                    <div style={{
                        width: sidebarCollapsed ? 0 : 280,
                        minWidth: sidebarCollapsed ? 0 : 280,
                        flexShrink: 0,
                        transition: 'width 0.28s cubic-bezier(0.4,0,0.2,1), min-width 0.28s cubic-bezier(0.4,0,0.2,1)',
                        overflow: 'hidden',
                        height: '100%',
                        minHeight: 0,
                    }}>
                        <ConsoleSidebar
                            activeSessionId={activeSessionId}
                            loadingSessions={loadingSessions}
                            groupedConversationItems={groupedConversationItems}
                            onCreateSession={createNewSession}
                            onRefreshSessions={() => void refreshSessions(activeSessionId)}
                            onSelectSession={selectSession}
                            onDeleteSession={deleteSession}
                        />
                    </div>

                    {/* 折叠/展开按钮 — 固定在侧边栏右侧边缘，垂直居中 */}
                    <div style={{
                        width: '32px',
                        flexShrink: 0,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        position: 'relative',
                        zIndex: 2,
                    }}>
                        <Tooltip title={sidebarCollapsed ? "展开会话列表" : "收起会话列表"} placement="right">
                            <button
                                onClick={toggleSidebar}
                                className="sidebar-collapse-toggle"
                                style={{
                                    width: '24px',
                                    height: '48px',
                                    border: '1px solid var(--td-border-light)',
                                    borderRadius: sidebarCollapsed ? '0 8px 8px 0' : '8px',
                                    background: 'var(--td-bg-elevated)',
                                    color: 'var(--td-text-tertiary)',
                                    cursor: 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    padding: 0,
                                    boxShadow: 'var(--td-shadow-base)',
                                    transition: 'all 0.2s ease',
                                    position: 'absolute',
                                    left: sidebarCollapsed ? 0 : '-2px',
                                    top: '50%',
                                    transform: 'translateY(-50%)',
                                }}
                            >
                                {sidebarCollapsed ? <RightOutlined style={{fontSize: '10px'}}/> : <LeftOutlined style={{fontSize: '10px'}}/>}
                            </button>
                        </Tooltip>
                    </div>
                </div>

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
                    minHeight: 0,
                    flex: 1,
                    minWidth: 0,
                }}>
                    <ConversationWorkspace
                        user={user!}
                        busy={busy}
                        input={input}
                        activeSession={activeSession}
                        activeSessionId={activeSessionId}
                        selectedModelId={selectedModelId}
                        onInputChange={setInput}
                        onSelectModel={selectModel}
                        onRefreshSession={() => void refreshSessions(activeSessionId)}
                        onInterrupt={interruptCurrent}
                        onSend={sendMessage}
                        onApprove={() => handleApprovalAction("approve")}
                        onReject={() => handleApprovalAction("reject")}
                    />
                </Content>
            </Layout>

            <style>{`
                .sidebar-collapse-toggle:hover {
                    background: var(--td-item-hover-bg) !important;
                    color: var(--td-highlight) !important;
                    border-color: var(--td-highlight) !important;
                }
            `}</style>
        </ConfigProvider>
    );
}
