import { message as antMessage } from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useParams } from "react-router-dom";
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

export function useAgentConsole(
  user: AuthUser,
  options?: {
    onNavigateToSession?: (sessionId: string, replace?: boolean) => void;
    onNavigateToHome?: (replace?: boolean) => void;
  }
) {
  const { sessionId: routeSessionId } = useParams<{ sessionId: string }>();

  const onNavigateToSession = options?.onNavigateToSession;
  const onNavigateToHome = options?.onNavigateToHome;

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
  // 流式事件批处理：收集待处理的事件，通过 requestAnimationFrame 批量更新
  const pendingEventsRef = useRef<Array<{ sessionId: string; assistantMessageId: string; event: TdAgentStreamEvent }>>([]);
  const rafIdRef = useRef<number | null>(null);

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
        onNavigateToSession?.(sessionId, replace);
      } else {
        onNavigateToHome?.(replace);
      }
    },
    [persistUiState, onNavigateToSession, onNavigateToHome],
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
            onNavigateToSession?.(targetId, true);
          }
        }
      } catch (error) {
        messageApi.error(error instanceof Error ? error.message : "读取会话列表失败");
      } finally {
        setLoadingSessions(false);
      }
    },
    [activeSessionId, messageApi, persistUiState, routeSessionId, onNavigateToSession],
  );

  // 批量处理待处理的流式事件
  const flushPendingEvents = useCallback(() => {
    rafIdRef.current = null;
    const events = pendingEventsRef.current;
    pendingEventsRef.current = [];

    if (events.length === 0) {
      return;
    }

    console.log("[flushPendingEvents] 开始处理，事件数量:", events.length);

    // 按 sessionId + assistantMessageId 分组，合并同一消息的事件
    const grouped = new Map<string, typeof events>();
    for (const evt of events) {
      const key = `${evt.sessionId}:${evt.assistantMessageId}`;
      if (!grouped.has(key)) {
        grouped.set(key, []);
      }
      grouped.get(key)!.push(evt);
    }

    // 对每个消息，处理所有事件（而不是只处理最后一个）
    // 因为 APPROVAL_REQUIRED 需要更新 pendingApprovals，即使后面还有 DONE 事件
    for (const [, group] of grouped) {
      const { sessionId, assistantMessageId } = group[0];
      let hasApprovalRequired = false;
      let pendingApprovalsFromEvent: ToolApproval[] = [];

      // 第一遍：收集所有 APPROVAL_REQUIRED 事件的数据
      for (const item of group) {
        const event = item.event;
        if (event.type === "APPROVAL_REQUIRED") {
          hasApprovalRequired = true;
          pendingApprovalsFromEvent = (event.metadata?.pendingApprovals as ToolApproval[] | undefined) ?? [];
          console.log("[flushPendingEvents] 检测到 APPROVAL_REQUIRED - count:", pendingApprovalsFromEvent.length);
          break;
        }
      }

      // 第二遍：处理所有非 DONE 事件
      for (const item of group) {
        const event = item.event;
        if (event.type === "DONE") {
          continue;
        }

        const block = toEventBlock(event);
        if (!block) {
          continue;
        }

        console.log("[flushPendingEvents] 处理事件 - type:", event.type, "block.type:", block.type);

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
          // 关键修复：如果有 APPROVAL_REQUIRED 事件，优先使用其 metadata 中的数据
          // 这样可以避免 syncPendingApprovals 异步延迟导致的数据不一致
          pendingApprovals: hasApprovalRequired && block.type === "approval"
            ? pendingApprovalsFromEvent
            : (block.type === "approval" ? block.approvals || [] : session.pendingApprovals),
        }));
      }

      // 最后处理 DONE 事件（如果存在）
      const doneItem = group.find(e => e.event.type === "DONE");
      if (doneItem) {
        const event = doneItem.event;
        console.log("[flushPendingEvents] 处理 DONE 事件");
        patchMessage(sessionId, assistantMessageId, (message) => ({
          ...message,
          streaming: false,
        }));
        const paused = Boolean(event.metadata?.paused);
        if (!paused) {
          // 使用防抖延迟刷新会话列表，避免 DONE 事件后立即刷新导致卡顿
          setTimeout(() => void refreshSessions(sessionId), 300);
        } else {
          // 如果后端标记了 paused 但没有 APPROVAL_REQUIRED 事件，主动同步审批列表
          // 但如果有 APPROVAL_REQUIRED 事件，已经更新了 pendingApprovals，不需要同步
          if (!hasApprovalRequired) {
            console.log("[flushPendingEvents] DONE 事件标记了 paused 但没有 APPROVAL_REQUIRED，同步审批列表");
            void syncPendingApprovals(sessionId);
          } else {
            console.log("[flushPendingEvents] DONE 事件标记了 paused 且有 APPROVAL_REQUIRED，已更新 pendingApprovals");
          }
        }
      }
    }
  }, [patchMessage, refreshSessions, syncPendingApprovals, updateSession]);

  const handleStreamEvent = useCallback(
    (sessionId: string, assistantMessageId: string, event: TdAgentStreamEvent) => {
      // 将事件加入待处理队列
      pendingEventsRef.current.push({ sessionId, assistantMessageId, event });

      // 如果已经有待处理的帧，不再重复请求
      if (rafIdRef.current !== null) {
        return;
      }

      // 使用 requestAnimationFrame 批量处理
      rafIdRef.current = requestAnimationFrame(() => {
        flushPendingEvents();
      });
    },
    [flushPendingEvents],
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
      } catch (error) {
        // 如果是用户主动中断，不显示错误信息
        if (controller.signal.aborted) {
          console.log("[流式请求] 用户主动中断");
          return;
        }

        // 检查是否是最大迭代次数错误且 ReMe 不可用
        const errorMessage = error instanceof Error ? error.message : "流式请求失败，请稍后重试";
        const isMaxItersError = errorMessage.toLowerCase().includes("最大执行次数") ||
                                errorMessage.toLowerCase().includes("max iterations") ||
                                errorMessage.toLowerCase().includes("max_iters");

        // 检查 ReMe 不可用的提示
        const isRemeUnavailable = errorMessage.toLowerCase().includes("reme 不可用") ||
                                  errorMessage.toLowerCase().includes("reme unavailable") ||
                                  errorMessage.toLowerCase().includes("reme 服务不可用");

        // 如果是最大迭代次数错误且 ReMe 不可用，显示友好提示
        if (isMaxItersError || isRemeUnavailable) {
          const friendlyMessage = "已达到最大执行次数，ReMe 长期记忆服务不可用，请开启新对话。";
          patchMessage(sessionId, assistantMessageId, (message) => ({
            ...message,
            streaming: false,
            failed: true,
            blocks: mergeStreamBlock(
              message.blocks,
              createBlock("error", "服务降级提示", friendlyMessage),
            ),
          }));
          messageApi.warning(friendlyMessage);
        } else {
          patchMessage(sessionId, assistantMessageId, (message) => ({
            ...message,
            streaming: false,
            failed: true,
            blocks: mergeStreamBlock(
              message.blocks,
              createBlock("error", "异常", errorMessage),
            ),
          }));
          messageApi.error(errorMessage);
        }
      } finally {
        // 确保在所有情况下都正确清理状态
        updateSession(sessionId, (session) => ({
          ...session,
          title,
          temp: false,
        }));
        if (streamAbortRef.current === controller) {
          streamAbortRef.current = null;
        }
        // 关键修复：先清理 busy 状态，让审批按钮可以点击
        setBusy(false);
        setRunningSessionId(undefined);
        // 清理可能残留的 requestAnimationFrame
        if (rafIdRef.current !== null) {
          cancelAnimationFrame(rafIdRef.current);
          rafIdRef.current = null;
        }
        // 处理所有待处理的事件，确保状态一致
        flushPendingEvents();
        // 异步同步审批列表，不阻塞 UI 更新
        // flushPendingEvents 已经处理了 APPROVAL_REQUIRED 事件，这里是双重保障
        void syncPendingApprovals(sessionId);
      }
    },
    [createStreamingAssistant, messageApi, patchMessage, syncPendingApprovals, updateSession, flushPendingEvents],
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

    const targetSessionId = runningSessionId;

    // 第一步：前端 AbortController 中断，立即停止接收流
    streamAbortRef.current?.abort();
    streamAbortRef.current = null;

    // 第二步：清理当前流式消息的状态（标记为不再 streaming）
    // 找到当前正在流式输出的 assistant 消息并标记为结束
    setSessions((current) =>
      current.map((session) => {
        if (session.sessionId !== targetSessionId) {
          return session;
        }
        return {
          ...session,
          messages: session.messages.map((msg) => {
            // 将所有 streaming 状态的 assistant 消息标记为结束
            if (msg.role === "assistant" && msg.streaming) {
              return { ...msg, streaming: false, failed: true };
            }
            return msg;
          }),
        };
      })
    );

    // 第三步：更新 UI 状态
    setBusy(false);
    setRunningSessionId(undefined);

    // 第四步：通知后端中断执行
    try {
      await agentConsoleApi.interruptChat(targetSessionId);
      messageApi.success("已中断会话");
    } catch (error) {
      // 中断失败不报错，因为前端已经停止
      console.warn("中断后端会话失败（前端已停止）", error);
    }
  }, [busy, messageApi, runningSessionId]);

  const handleApprovalAction = useCallback(
    async (action: "approve" | "reject") => {
      console.log("[审批操作] 开始执行 - action:", action, "sessionId:", activeSessionId);

      const currentSession = sessionsRef.current.find((s) => s.sessionId === activeSessionId);

      console.log("[审批操作] 会话检查", {
        found: !!currentSession,
        sessionId: activeSessionId,
        busy,
        runningSessionId,
        pendingCount: currentSession?.pendingApprovals.length ?? 0,
        pendingIds: currentSession?.pendingApprovals.map(a => a.id),
      });

      if (!currentSession) {
        messageApi.error("当前会话不存在，请刷新后重试");
        return;
      }

      if (currentSession.pendingApprovals.length === 0) {
        messageApi.warning("当前没有待审批的项，请刷新后重试");
        return;
      }

      // 检查是否真的有流式输出在进行中（通过 runningSessionId 判断）
      if (runningSessionId && runningSessionId !== currentSession.sessionId) {
        console.log("[审批操作] 有其他会话正在运行，拒绝审批操作");
        messageApi.warning("有其他会话正在运行，请稍后再试");
        return;
      }

      const payload: ToolApprovalActionPayload = {
        sessionId: currentSession.sessionId,
        approvalIds: currentSession.pendingApprovals.map((item) => item.id),
        title: currentSession.title,
        comment: approvalComment.trim() || undefined,
      };

      console.log("[审批操作] 准备发送请求 - payload:", payload);

      // 立即清空 pendingApprovals，提供更好的用户体验
      updateSession(currentSession.sessionId, (session) => ({
        ...session,
        pendingApprovals: [],
      }));
      setApprovalComment("");

      try {
        await runStream(currentSession.sessionId, currentSession.title, (assistantMessageId, signal) =>
          (action === "approve" ? agentConsoleApi.approveAndResume : agentConsoleApi.rejectAndResume)(
            payload,
            (event) => handleStreamEvent(currentSession.sessionId, assistantMessageId, event),
            signal,
          ),
        );
        console.log("[审批操作] 审批恢复成功");
      } catch (error) {
        console.error("[审批操作] 审批恢复失败", error);
        messageApi.error(error instanceof Error ? error.message : "审批恢复失败");
      }
    },
    [activeSessionId, approvalComment, busy, handleStreamEvent, messageApi, runStream, runningSessionId, updateSession],
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
