import { message as antMessage } from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { agentConsoleApi } from "../services/agentConsoleApi";
import type {
  ChatRequestPayload,
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
import { FIXED_USER_ID, FIXED_USER_NAME } from "../types";

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

function firstBlockText(blocks: UiMessageBlock[]) {
  return blocks.find((block) => block.content.trim())?.content.trim() ?? "";
}

function createSessionId() {
  return `session-${crypto.randomUUID()}`;
}

function createSessionPreview(session: SessionState) {
  const lastMessage = [...session.messages]
    .reverse()
    .find((item) => item.role !== "system");

  if (!lastMessage) {
    return "准备开始新的智能体协作";
  }

  const text = firstBlockText(lastMessage.blocks);
  return text || "空白消息";
}

function deriveTitleFromText(text: string) {
  const normalized = text.trim().replace(/\s+/g, " ");
  if (!normalized) {
    return "未命名会话";
  }
  return normalized.length > 24 ? `${normalized.slice(0, 24)}...` : normalized;
}

function createUserMessage(content: string): UiMessage {
  return {
    id: createId("user"),
    role: "user",
    name: FIXED_USER_NAME,
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

function mapStoredMessage(stored: StoredMessage): UiMessage {
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
        role === "assistant" ? "text" : "text",
        role === "user" ? "你的输入" : "回复",
        item.text ?? item.inputRaw ?? item.input ?? "",
      );
    }) ?? [];

  return {
    id: stored.msgId || createId("history"),
    role,
    name:
      role === "user"
        ? stored.name || FIXED_USER_NAME
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
    title: view.title?.trim() || `Session-${view.sessionId}`,
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
): SessionState {
  const messages = history.messages.map(mapStoredMessage);
  const next: SessionState = {
    ...fallback,
    title: history.session?.title?.trim() || fallback.title,
    createdAt: history.session?.createdAt || fallback.createdAt,
    updatedAt:
      history.session?.updatedAt ||
      history.session?.lastMessageAt ||
      fallback.updatedAt,
    messageCount: history.session?.messageCount ?? messages.length,
    unreadCount: history.session?.unreadCount ?? 0,
    preview:
      messages.length > 0
        ? createSessionPreview({ ...fallback, messages })
        : fallback.preview,
    messages,
  };
  return next;
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
    case "APPROVAL_REQUIRED": {
      const approvals =
        (event.metadata?.pendingApprovals as ToolApproval[] | undefined) ?? [];
      return createBlock("approval", "等待审批", event.content, { approvals });
    }
    default:
      return null;
  }
}

function mergeStreamBlock(
  blocks: UiMessageBlock[],
  incoming: UiMessageBlock,
): UiMessageBlock[] {
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

export function useAgentConsole() {
  const navigate = useNavigate();
  const { sessionId: routeSessionId } = useParams<{ sessionId: string }>();

  const [messageApi, contextHolder] = antMessage.useMessage();
  const [sessions, setSessions] = useState<SessionState[]>([]);
  const [activeSessionId, setActiveSessionId] = useState<string | undefined>(routeSessionId);
  const [input, setInput] = useState("");
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [busy, setBusy] = useState(false);
  const [approvalComment, setApprovalComment] = useState("");
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

  // Sync route -> state (e.g., browser back/forward or initial load)
  useEffect(() => {
    if (routeSessionId !== activeSessionId) {
      if (routeSessionId) {
        setActiveSessionId(routeSessionId);
        persistUiState(routeSessionId);
      } else {
        setActiveSessionId(undefined);
      }
    }
  }, [routeSessionId, activeSessionId, persistUiState]);

  // Sync state -> route (e.g., user clicked a session or created a new one)
  useEffect(() => {
    if (activeSessionId && activeSessionId !== routeSessionId) {
      navigate(`/${activeSessionId}`, { replace: !routeSessionId });
    }
  }, [activeSessionId, routeSessionId, navigate]);

  const updateSession = useCallback(
    (sessionId: string, updater: (current: SessionState) => SessionState) => {
      setSessions((current) =>
        sortSessions(
          current.map((item) =>
            item.sessionId === sessionId ? updater(item) : item,
          ),
        ),
      );
    },
    [],
  );

  const upsertSession = useCallback((session: SessionState) => {
    setSessions((current) => {
      const exists = current.some((item) => item.sessionId === session.sessionId);
      const next = exists
        ? current.map((item) =>
            item.sessionId === session.sessionId ? session : item,
          )
        : [session, ...current];
      return sortSessions(next);
    });
  }, []);

  const ensureSession = useCallback(
    (sessionId: string, title?: string) => {
      const existing = sessionsRef.current.find(
        (item) => item.sessionId === sessionId,
      );
      if (existing) {
        return existing;
      }

      const created = nowIso();
      const next: SessionState = {
        sessionId,
        title: title || `Session-${sessionId}`,
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
    (
      sessionId: string,
      messageId: string,
      updater: (message: UiMessage) => UiMessage,
    ) => {
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
        const approvals = await agentConsoleApi.listPendingApprovals(
          FIXED_USER_ID,
          sessionId,
        );
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
        const history = await agentConsoleApi.getSessionHistory(
          FIXED_USER_ID,
          sessionId,
        );
        const base =
          sessionsRef.current.find((item) => item.sessionId === sessionId) ||
          ensureSession(sessionId);
        const normalized = normalizeHistory(history, base);
        upsertSession({
          ...normalized,
          loadingHistory: false,
        });
        await syncPendingApprovals(sessionId);
      } catch (error) {
        messageApi.error(
          error instanceof Error ? error.message : "读取会话历史失败",
        );
        updateSession(sessionId, (session) => ({
          ...session,
          loadingHistory: false,
        }));
      }
    },
    [ensureSession, messageApi, syncPendingApprovals, updateSession, upsertSession],
  );

  const refreshSessions = useCallback(
    async (preferredSessionId?: string) => {
      setLoadingSessions(true);
      try {
        const list = await agentConsoleApi.listSessions(FIXED_USER_ID);
        const normalized = list.map(normalizeConversationView);
        setSessions((current) => {
          const merged = normalized.map((item) => {
            const existing = current.find(
              (candidate) => candidate.sessionId === item.sessionId,
            );
            return existing ? { ...item, ...existing, ...item } : item;
          });
          const tempOnly = current.filter(
            (item) =>
              item.temp &&
              !merged.some((candidate) => candidate.sessionId === item.sessionId),
          );
          return sortSessions([...merged, ...tempOnly]);
        });

        const targetId =
          preferredSessionId ||
          activeSessionId ||
          JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}").activeSessionId ||
          normalized[0]?.sessionId;

        if (targetId) {
          setActiveSessionId(targetId);
          persistUiState(targetId);
        }
      } catch (error) {
        messageApi.error(
          error instanceof Error ? error.message : "读取会话列表失败",
        );
      } finally {
        setLoadingSessions(false);
      }
    },
    [activeSessionId, messageApi, persistUiState],
  );

  const handleStreamEvent = useCallback(
    (sessionId: string, assistantMessageId: string, event: TdAgentStreamEvent) => {
      if (event.type === "DONE") {
        patchMessage(sessionId, assistantMessageId, (message) => ({
          ...message,
          streaming: false,
        }));

        const paused = Boolean(event.metadata?.paused);
        if (!paused) {
          void refreshSessions(sessionId);
        }
        return;
      }

      const block = toEventBlock(event);
      if (!block) {
        return;
      }

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
          block.type === "approval"
            ? "等待审批继续执行"
            : block.content.trim() || session.preview,
        pendingApprovals:
          block.type === "approval" ? block.approvals || [] : session.pendingApprovals,
      }));
    },
    [patchMessage, refreshSessions, updateSession],
  );

  const runStream = useCallback(
    async (
      sessionId: string,
      title: string,
      requestFactory: (
        assistantMessageId: string,
        signal: AbortSignal,
      ) => Promise<void>,
    ) => {
      setBusy(true);
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
            createBlock(
              "error",
              "异常",
              error instanceof Error ? error.message : "流式请求失败",
            ),
          ),
        }));
        messageApi.error(
          error instanceof Error ? error.message : "流式请求失败，请稍后重试",
        );
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
      }
    },
    [
      createStreamingAssistant,
      messageApi,
      patchMessage,
      syncPendingApprovals,
      updateSession,
    ],
  );

  const sendMessage = useCallback(async () => {
    const text = input.trim();
    if (!text || busy) {
      return;
    }

    const sessionId = activeSessionId || createSessionId();
    const title = activeSession?.title || deriveTitleFromText(text);
    const session = ensureSession(sessionId, title);
    const userMessage = createUserMessage(text);

    setActiveSessionId(sessionId);
    persistUiState(sessionId);
    appendMessage(sessionId, userMessage);
    setInput("");

    const payload: ChatRequestPayload = {
      userId: FIXED_USER_ID,
      sessionId,
      userName: FIXED_USER_NAME,
      title: session.temp ? title : session.title,
      message: text,
    };

    await runStream(sessionId, title, (assistantMessageId, signal) =>
      agentConsoleApi.streamChat(
        payload,
        (event) => handleStreamEvent(sessionId, assistantMessageId, event),
        signal,
      ),
    );
  }, [
    activeSession,
    activeSessionId,
    appendMessage,
    busy,
    ensureSession,
    handleStreamEvent,
    input,
    persistUiState,
    runStream,
  ]);

  const createNewSession = useCallback(() => {
    const sessionId = createSessionId();
    const createdAt = nowIso();
    const session: SessionState = {
      sessionId,
      title: "新会话",
      createdAt,
      updatedAt: createdAt,
      messageCount: 0,
      unreadCount: 0,
      preview: "等待你的首条输入",
      messages: [],
      pendingApprovals: [],
      temp: true,
    };
    upsertSession(session);
    setActiveSessionId(sessionId);
    persistUiState(sessionId);
    setInput("");
  }, [persistUiState, upsertSession]);

  const selectSession = useCallback(
    async (sessionId: string) => {
      setActiveSessionId(sessionId);
      persistUiState(sessionId);

      const current = sessionsRef.current.find(
        (item) => item.sessionId === sessionId,
      );
      if (!current || current.messages.length === 0) {
        await loadSessionHistory(sessionId);
      } else {
        void syncPendingApprovals(sessionId);
      }
    },
    [loadSessionHistory, persistUiState, syncPendingApprovals],
  );

  const interruptCurrent = useCallback(async () => {
    if (!activeSessionId || !busy) {
      return;
    }
    streamAbortRef.current?.abort();
    streamAbortRef.current = null;
    setBusy(false);

    try {
      await agentConsoleApi.interruptChat(FIXED_USER_ID, activeSessionId);
      messageApi.success("已发送中断信号");
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "发送中断失败");
    }
  }, [activeSessionId, busy, messageApi]);

  const handleApprovalAction = useCallback(
    async (action: "approve" | "reject") => {
      if (!activeSession || busy || activeSession.pendingApprovals.length === 0) {
        return;
      }

      const payload: ToolApprovalActionPayload = {
        userId: FIXED_USER_ID,
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

      await runStream(
        activeSession.sessionId,
        activeSession.title,
        (assistantMessageId, signal) =>
        (action === "approve"
          ? agentConsoleApi.approveAndResume
          : agentConsoleApi.rejectAndResume)(
          payload,
          (event) => handleStreamEvent(activeSession.sessionId, assistantMessageId, event),
          signal,
        ),
      );
    },
    [activeSession, approvalComment, busy, handleStreamEvent, runStream, updateSession],
  );

  const groupedConversationItems = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return sessions.map((session) => {
      const updated = new Date(session.updatedAt);
      const group = updated.getTime() >= today.getTime() ? "今天" : "更早";
      return {
        key: session.sessionId,
        label: session.title,
        group,
        timestamp: updated.getTime(),
      };
    });
  }, [sessions]);

  useEffect(() => {
    if (hydratedRef.current) {
      return;
    }
    hydratedRef.current = true;

    const saved = parseJsonSafely<PersistedState>(
      localStorage.getItem(STORAGE_KEY) || undefined,
    );

    void refreshSessions(saved?.activeSessionId);
  }, [refreshSessions]);

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
    sessions,
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
