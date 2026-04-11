import { message as antMessage } from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { agentConsoleApi } from "../services/agentConsoleApi";
import type {
  AuthUser,
  ChatRequestPayload,
  ConversationGroupItem,
  ConversationView,
  SessionHistoryResponse,
  SessionState,
  StoredMessage,
  TdAgentStreamEvent,
  ToolApproval,
  ToolApprovalActionPayload,
  UiMessage,
  UiMessageBlock,
  UiMessageBlockType,
} from "../types";

const STORAGE_KEY = "td-agent-console-ui";

interface PersistedState {
  activeSessionId?: string;
}

function createId(prefix: string) {
  return `${prefix}-${crypto.randomUUID()}`;
}

function nowIso() {
  return new Date().toISOString();
}

function createSessionId() {
  return `session-${crypto.randomUUID()}`;
}

function parseJsonSafely<T>(raw?: string): T | null {
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function normalizeText(value?: string) {
  return value?.trim().replace(/\s+/g, " ") ?? "";
}

function deriveTitleFromText(text: string) {
  const normalized = normalizeText(text);
  if (!normalized) {
    return "新对话";
  }
  return normalized.length > 10 ? normalized.slice(0, 10) : normalized;
}

function getRole(role?: string): UiMessage["role"] {
  if (!role) {
    return "assistant";
  }
  const normalized = role.toUpperCase();
  if (normalized === "USER") {
    return "user";
  }
  if (normalized === "SYSTEM") {
    return "system";
  }
  return "assistant";
}

function createBlock(
  type: UiMessageBlockType,
  title: string,
  content: string,
  extras?: Partial<UiMessageBlock>,
): UiMessageBlock {
  return {
    id: createId("block"),
    type,
    title,
    content,
    ...extras,
  };
}

function firstBlockText(blocks: UiMessageBlock[]) {
  return blocks.find((block) => block.content.trim())?.content.trim() ?? "";
}

function createSessionPreview(session: SessionState) {
  const lastMessage = [...session.messages]
    .reverse()
    .find((item) => item.role !== "system");
  if (!lastMessage) {
    return "等待你的首条输入";
  }
  return firstBlockText(lastMessage.blocks) || "空白消息";
}

function createUserMessage(content: string, user: AuthUser): UiMessage {
  return {
    id: createId("user"),
    role: "user",
    name: user.nickname || user.userId,
    createdAt: nowIso(),
    blocks: [
      {
        id: createId("block"),
        type: "text",
        title: "你的输入",
        content,
      },
    ],
  };
}

function createAssistantMessage(): UiMessage {
  return {
    id: createId("assistant"),
    role: "assistant",
    name: "TDAgent",
    createdAt: nowIso(),
    blocks: [],
    streaming: true,
  };
}

function mapStoredMessage(stored: StoredMessage, user: AuthUser): UiMessage {
  const role = getRole(stored.role);
  const blocks =
    stored.content?.map((item) => {
      if (item.type === "thinking") {
        return createBlock("reasoning", "推理", item.text ?? "");
      }
      if (item.type === "tool_use") {
        return createBlock("tool_use", "工具调用", item.inputRaw || item.input || "", {
          toolName: item.name,
          rawInput: item.input,
        });
      }
      if (item.type === "tool_result") {
        return createBlock("tool_result", "工具结果", item.text ?? "", {
          toolName: item.name,
          rawInput: item.inputRaw,
        });
      }
      return createBlock(
        "text",
        role === "user" ? "你的输入" : "回复",
        item.text ?? item.inputRaw ?? item.input ?? "",
      );
    }) ?? [];

  return {
    id: stored.msgId || createId("history"),
    role,
    name:
      role === "user"
        ? stored.name || user.nickname || user.userId
        : stored.name || (role === "system" ? "System" : "TDAgent"),
    createdAt: stored.timestamp || nowIso(),
    blocks,
  };
}

function sortSessions(list: SessionState[]) {
  return [...list].sort(
    (left, right) =>
      new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime(),
  );
}

function normalizeConversationView(view: ConversationView): SessionState {
  return {
    sessionId: view.sessionId,
    title: view.title?.trim() || "新对话",
    createdAt: view.createdAt || nowIso(),
    updatedAt: view.updatedAt || view.lastMessageAt || view.createdAt || nowIso(),
    messageCount: view.messageCount ?? 0,
    unreadCount: view.unreadCount ?? 0,
    preview: "加载历史中...",
    messages: [],
    pendingApprovals: [],
  };
}

function normalizeHistory(
  history: SessionHistoryResponse,
  fallback: SessionState,
  user: AuthUser,
): SessionState {
  const messages = history.messages.map((item) => mapStoredMessage(item, user));
  return {
    ...fallback,
    title: history.session?.title?.trim() || fallback.title,
    createdAt: history.session?.createdAt || fallback.createdAt,
    updatedAt:
      history.session?.updatedAt || history.session?.lastMessageAt || fallback.updatedAt,
    messageCount: history.session?.messageCount ?? messages.length,
    unreadCount: history.session?.unreadCount ?? 0,
    preview: messages.length ? createSessionPreview({ ...fallback, messages }) : fallback.preview,
    messages,
  };
}

function toEventBlock(event: TdAgentStreamEvent): UiMessageBlock | null {
  switch (event.type) {
    case "MESSAGE":
      return createBlock("text", "流式回复", event.content);
    case "REASONING":
      return createBlock("reasoning", "推理", event.content);
    case "TOOL_RESULT":
      return createBlock("tool_result", "工具结果", event.content);
    case "RESULT":
      return createBlock("result", "最终结果", event.content);
    case "ERROR":
      return createBlock("error", "异常", event.content);
    case "APPROVAL_REQUIRED":
      return createBlock("approval", "等待审批", event.content, {
        approvals: (event.metadata?.pendingApprovals as ToolApproval[] | undefined) ?? [],
      });
    default:
      return null;
  }
}

function mergeStreamBlock(blocks: UiMessageBlock[], incoming: UiMessageBlock): UiMessageBlock[] {
  const previous = blocks.at(-1);
  if (
    previous &&
    previous.type === incoming.type &&
    previous.toolName === incoming.toolName &&
    incoming.type !== "approval"
  ) {
    return [
      ...blocks.slice(0, -1),
      {
        ...previous,
        content: `${previous.content}${incoming.content}`,
      },
    ];
  }
  return [...blocks, incoming];
}

export function useAgentConsole(user: AuthUser) {
  const navigate = useNavigate();
  const { sessionId: routeSessionId } = useParams<{ sessionId: string }>();

  const [messageApi, contextHolder] = antMessage.useMessage();
  const [sessions, setSessions] = useState<SessionState[]>([]);
  const [activeSessionId, setActiveSessionId] = useState<string | undefined>(routeSessionId);
  const [input, setInput] = useState("");
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [busy, setBusy] = useState(false);
  const [approvalComment, setApprovalComment] = useState("");
  const [runningSessionId, setRunningSessionId] = useState<string>();
  const streamAbortRef = useRef<AbortController | null>(null);
  const hydratedRef = useRef(false);
  const sessionsRef = useRef<SessionState[]>([]);

  useEffect(() => {
    sessionsRef.current = sessions;
  }, [sessions]);

  const activeSession = useMemo(
    () => sessions.find((session) => session.sessionId === activeSessionId),
    [activeSessionId, sessions],
  );

  const persistUiState = useCallback((sessionId?: string) => {
    const payload: PersistedState = { activeSessionId: sessionId };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
  }, []);

  const activateSession = useCallback(
    (sessionId?: string, replace = false) => {
      setActiveSessionId(sessionId);
      persistUiState(sessionId);
      if (sessionId) {
        navigate(`/${sessionId}`, { replace });
      } else if (window.location.pathname !== "/") {
        navigate("/", { replace });
      }
    },
    [navigate, persistUiState],
  );

  useEffect(() => {
    if (!routeSessionId) {
      return;
    }
    if (routeSessionId !== activeSessionId) {
      setActiveSessionId(routeSessionId);
      persistUiState(routeSessionId);
    }
  }, [activeSessionId, persistUiState, routeSessionId]);

  const updateSession = useCallback(
    (sessionId: string, updater: (current: SessionState) => SessionState) => {
      setSessions((current) =>
        sortSessions(current.map((item) => (item.sessionId === sessionId ? updater(item) : item))),
      );
    },
    [],
  );

  const upsertSession = useCallback((session: SessionState) => {
    setSessions((current) => {
      const exists = current.some((item) => item.sessionId === session.sessionId);
      const next = exists
        ? current.map((item) => (item.sessionId === session.sessionId ? session : item))
        : [session, ...current];
      return sortSessions(next);
    });
  }, []);

  const ensureSession = useCallback(
    (sessionId: string, title = "新对话") => {
      const existing = sessionsRef.current.find((item) => item.sessionId === sessionId);
      if (existing) {
        return existing;
      }
      const created = nowIso();
      const next: SessionState = {
        sessionId,
        title,
        createdAt: created,
        updatedAt: created,
        messageCount: 0,
        unreadCount: 0,
        preview: "等待你的首条输入",
        messages: [],
        pendingApprovals: [],
        temp: true,
      };
      upsertSession(next);
      return next;
    },
    [upsertSession],
  );

  const appendMessage = useCallback(
    (sessionId: string, message: UiMessage) => {
      updateSession(sessionId, (session) => {
        const messages = [...session.messages, message];
        return {
          ...session,
          messages,
          messageCount: messages.length,
          preview: createSessionPreview({ ...session, messages }),
          updatedAt: message.createdAt,
          temp: false,
        };
      });
    },
    [updateSession],
  );

  const createStreamingAssistant = useCallback(
    (sessionId: string) => {
      const assistant = createAssistantMessage();
      appendMessage(sessionId, assistant);
      return assistant.id;
    },
    [appendMessage],
  );

  const patchMessage = useCallback(
    (sessionId: string, messageId: string, updater: (message: UiMessage) => UiMessage) => {
      updateSession(sessionId, (session) => ({
        ...session,
        messages: session.messages.map((message) =>
          message.id === messageId ? updater(message) : message,
        ),
      }));
    },
    [updateSession],
  );

  const syncPendingApprovals = useCallback(
    async (sessionId: string) => {
      try {
        const approvals = await agentConsoleApi.listPendingApprovals(sessionId);
        updateSession(sessionId, (session) => ({
          ...session,
          pendingApprovals: approvals,
        }));
      } catch (error) {
        console.warn("读取审批列表失败", error);
      }
    },
    [updateSession],
  );

  const loadSessionHistory = useCallback(
    async (sessionId: string) => {
      updateSession(sessionId, (session) => ({
        ...session,
        loadingHistory: true,
      }));
      try {
        const history = await agentConsoleApi.getSessionHistory(sessionId);
        const base = sessionsRef.current.find((item) => item.sessionId === sessionId) || ensureSession(sessionId);
        const normalized = normalizeHistory(history, base, user);
        upsertSession({
          ...normalized,
          loadingHistory: false,
          temp: false,
        });
        await syncPendingApprovals(sessionId);
      } catch (error) {
        messageApi.error(error instanceof Error ? error.message : "读取会话历史失败");
        updateSession(sessionId, (session) => ({
          ...session,
          loadingHistory: false,
        }));
      }
    },
    [ensureSession, messageApi, syncPendingApprovals, updateSession, upsertSession, user],
  );

  const refreshSessions = useCallback(
    async (preferredSessionId?: string) => {
      setLoadingSessions(true);
      try {
        const list = await agentConsoleApi.listSessions();
        const normalized = list.map(normalizeConversationView);
        setSessions((current) => {
          const merged = normalized.map((item) => {
            const existing = current.find((candidate) => candidate.sessionId === item.sessionId);
            if (!existing) {
              return item;
            }
            return {
              ...item,
              messages: existing.messages,
              pendingApprovals: existing.pendingApprovals,
              loadingHistory: existing.loadingHistory,
              temp: false,
              preview: existing.messages.length ? existing.preview : item.preview,
            };
          });
          const tempOnly = current.filter(
            (item) =>
              item.temp &&
              !merged.some((candidate) => candidate.sessionId === item.sessionId) &&
              item.messages.length === 0,
          );
          return sortSessions([...merged, ...tempOnly]);
        });

        const saved = parseJsonSafely<PersistedState>(localStorage.getItem(STORAGE_KEY) || undefined);
        const targetId =
          routeSessionId || preferredSessionId || activeSessionId || saved?.activeSessionId || normalized[0]?.sessionId;

        if (targetId) {
          setActiveSessionId(targetId);
          persistUiState(targetId);
          if (!routeSessionId) {
            navigate(`/${targetId}`, { replace: true });
          }
        }
      } catch (error) {
        messageApi.error(error instanceof Error ? error.message : "读取会话列表失败");
      } finally {
        setLoadingSessions(false);
      }
    },
    [activeSessionId, messageApi, navigate, persistUiState, routeSessionId],
  );

  const handleStreamEvent = useCallback(
    (sessionId: string, assistantMessageId: string, event: TdAgentStreamEvent) => {
      // 添加调试日志，验证实时接收
      console.log('[SSE Event]', event.type, '内容长度:', event.content?.length, '时间:', new Date().toISOString());
      
      if (event.type === "DONE") {
        console.log('[SSE Done] 流式输出完成');
        patchMessage(sessionId, assistantMessageId, (message) => ({
          ...message,
          streaming: false,
        }));
        const paused = Boolean(event.metadata?.paused);
        if (!paused) {
          void refreshSessions(sessionId);
        } else {
          void syncPendingApprovals(sessionId);
        }
        return;
      }

      const block = toEventBlock(event);
      if (!block) {
        return;
      }

      console.log('[SSE Block] 类型:', block.type, '内容:', block.content.substring(0, 50));

      patchMessage(sessionId, assistantMessageId, (message) => ({
        ...message,
        blocks: mergeStreamBlock(message.blocks, block),
        streaming: event.type !== "ERROR",
        failed: event.type === "ERROR",
      }));

      updateSession(sessionId, (session) => ({
        ...session,
        updatedAt: nowIso(),
        preview:
          block.type === "approval" ? "等待审批继续执行" : block.content.trim() || session.preview,
        pendingApprovals: block.type === "approval" ? block.approvals || [] : session.pendingApprovals,
      }));
    },
    [patchMessage, refreshSessions, syncPendingApprovals, updateSession],
  );

  const runStream = useCallback(
    async (
      sessionId: string,
      title: string,
      requestFactory: (assistantMessageId: string, signal: AbortSignal) => Promise<void>,
    ) => {
      setBusy(true);
      setRunningSessionId(sessionId);
      streamAbortRef.current?.abort();
      const controller = new AbortController();
      streamAbortRef.current = controller;
      const assistantMessageId = createStreamingAssistant(sessionId);

      try {
        await requestFactory(assistantMessageId, controller.signal);
        await syncPendingApprovals(sessionId);
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }
        patchMessage(sessionId, assistantMessageId, (message) => ({
          ...message,
          streaming: false,
          failed: true,
          blocks: mergeStreamBlock(
            message.blocks,
            createBlock("error", "异常", error instanceof Error ? error.message : "流式请求失败"),
          ),
        }));
        messageApi.error(error instanceof Error ? error.message : "流式请求失败，请稍后重试");
      } finally {
        updateSession(sessionId, (session) => ({
          ...session,
          title,
          temp: false,
        }));
        if (streamAbortRef.current === controller) {
          streamAbortRef.current = null;
        }
        setBusy(false);
        setRunningSessionId(undefined);
      }
    },
    [createStreamingAssistant, messageApi, patchMessage, syncPendingApprovals, updateSession],
  );

  const sendMessage = useCallback(async () => {
    const text = input.trim();
    if (!text || busy) {
      return;
    }
    const sessionId = activeSessionId || createSessionId();
    const nextTitle =
      activeSession?.temp || !activeSession?.messages.length
        ? deriveTitleFromText(text)
        : activeSession?.title || deriveTitleFromText(text);
    const session = ensureSession(sessionId, activeSession?.temp ? "新对话" : nextTitle);
    const userMessage = createUserMessage(text, user);

    activateSession(sessionId, !routeSessionId);
    appendMessage(sessionId, userMessage);
    setInput("");

    const payload: ChatRequestPayload = {
      sessionId,
      userName: user.nickname || user.userId,
      title: session.temp ? nextTitle : session.title,
      message: text,
    };

    await runStream(sessionId, payload.title || session.title, (assistantMessageId, signal) =>
      agentConsoleApi.streamChat(
        payload,
        (event) => handleStreamEvent(sessionId, assistantMessageId, event),
        signal,
      ),
    );
  }, [
    activateSession,
    activeSession,
    activeSessionId,
    appendMessage,
    busy,
    ensureSession,
    handleStreamEvent,
    input,
    routeSessionId,
    runStream,
    user,
  ]);

  const createNewSession = useCallback(() => {
    const sessionId = createSessionId();
    const createdAt = nowIso();
    upsertSession({
      sessionId,
      title: "新对话",
      createdAt,
      updatedAt: createdAt,
      messageCount: 0,
      unreadCount: 0,
      preview: "等待你的首条输入",
      messages: [],
      pendingApprovals: [],
      temp: true,
    });
    activateSession(sessionId);
    setInput("");
  }, [activateSession, upsertSession]);

  const selectSession = useCallback(
    async (sessionId: string) => {
      activateSession(sessionId);
      const current = sessionsRef.current.find((item) => item.sessionId === sessionId);
      if (!current || current.messages.length === 0) {
        await loadSessionHistory(sessionId);
      } else {
        void syncPendingApprovals(sessionId);
      }
    },
    [activateSession, loadSessionHistory, syncPendingApprovals],
  );

  const interruptCurrent = useCallback(async () => {
    if (!runningSessionId || !busy) {
      return;
    }
    streamAbortRef.current?.abort();
    streamAbortRef.current = null;
    setBusy(false);
    setRunningSessionId(undefined);
    try {
      await agentConsoleApi.interruptChat(runningSessionId);
      messageApi.success("已发送中断信号");
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "发送中断失败");
    }
  }, [busy, messageApi, runningSessionId]);

  const handleApprovalAction = useCallback(
    async (action: "approve" | "reject") => {
      if (!activeSession || busy || activeSession.pendingApprovals.length === 0) {
        return;
      }
      const payload: ToolApprovalActionPayload = {
        sessionId: activeSession.sessionId,
        approvalIds: activeSession.pendingApprovals.map((item) => item.id),
        title: activeSession.title,
        comment: approvalComment.trim() || undefined,
      };
      updateSession(activeSession.sessionId, (session) => ({
        ...session,
        pendingApprovals: [],
      }));
      setApprovalComment("");
      await runStream(activeSession.sessionId, activeSession.title, (assistantMessageId, signal) =>
        (action === "approve" ? agentConsoleApi.approveAndResume : agentConsoleApi.rejectAndResume)(
          payload,
          (event) => handleStreamEvent(activeSession.sessionId, assistantMessageId, event),
          signal,
        ),
      );
    },
    [activeSession, approvalComment, busy, handleStreamEvent, runStream, updateSession],
  );

  const groupedConversationItems = useMemo<ConversationGroupItem[]>(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return sessions.map((session) => {
      const updated = new Date(session.updatedAt);
      return {
        key: session.sessionId,
        label: session.title,
        group: updated.getTime() >= today.getTime() ? "今天" : "更早",
        timestamp: updated.getTime(),
      };
    });
  }, [sessions]);

  useEffect(() => {
    if (hydratedRef.current) {
      return;
    }
    hydratedRef.current = true;
    const saved = parseJsonSafely<PersistedState>(localStorage.getItem(STORAGE_KEY) || undefined);
    void refreshSessions(routeSessionId || saved?.activeSessionId);
  }, [refreshSessions, routeSessionId]);

  useEffect(() => {
    if (!activeSessionId) {
      return;
    }
    const current = sessions.find((item) => item.sessionId === activeSessionId);
    if (current && current.messages.length === 0 && !current.temp) {
      void loadSessionHistory(activeSessionId);
    }
  }, [activeSessionId, loadSessionHistory, sessions]);

  return {
    contextHolder,
    loadingSessions,
    busy,
    activeSession,
    activeSessionId,
    input,
    approvalComment,
    groupedConversationItems,
    apiBase: agentConsoleApi.apiBase,
    setInput,
    setApprovalComment,
    createNewSession,
    selectSession,
    sendMessage,
    interruptCurrent,
    refreshSessions,
    handleApprovalAction,
  };
}
