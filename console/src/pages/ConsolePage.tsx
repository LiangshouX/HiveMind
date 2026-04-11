import { ConfigProvider, Layout, theme } from "antd";
import { useNavigate } from "react-router-dom";
import { ApprovalDrawerPanel } from "../components/console/ApprovalDrawerPanel";
import { ConsoleSidebar } from "../components/console/ConsoleSidebar";
import { ConversationWorkspace } from "../components/console/ConversationWorkspace";
import { useAgentConsole } from "../hooks/useAgentConsole";
import { useAuth } from "../providers/AuthProvider";

const { Sider, Content } = Layout;

export function ConsolePage() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();
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
  } = useAgentConsole(user!);

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
          <ConsoleSidebar
            user={user!}
            apiBase={apiBase}
            activeSessionId={activeSessionId}
            loadingSessions={loadingSessions}
            groupedConversationItems={groupedConversationItems}
            onCreateSession={createNewSession}
            onRefreshSessions={() => void refreshSessions(activeSessionId)}
            onSelectSession={selectSession}
            onOpenProfile={() => navigate("/profile")}
            onLogout={() => {
              logout();
              navigate("/login", { replace: true });
            }}
          />
        </Sider>

        <Content className="app-main">
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
            onOpenProfile={() => navigate("/profile")}
          />
        </Content>

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
