import type {
  ChatRequestPayload,
  ConversationView,
  SessionHistoryResponse,
  TdAgentStreamEvent,
  ToolApproval,
  ToolApprovalActionPayload,
} from "../types";
import { buildApiUrl, createHeaders, parseApiResult } from "./http";

const API_BASE = buildApiUrl(
  (import.meta.env.VITE_AGENT_API_BASE?.trim() || "/tdagent").replace(/\/$/, ""),
);

function buildUrl(path: string) {
  return `${API_BASE}${path.startsWith("/") ? path : `/${path}`}`;
}

function consumeSseBuffer(
  source: string,
  onEvent: (event: TdAgentStreamEvent) => void,
): string {
  let buffer = source;

  while (true) {
    const match = buffer.match(/\r?\n\r?\n/);
    if (!match || match.index === undefined) {
      break;
    }
    const boundaryIndex = match.index;
    const boundaryLength = match[0].length;

    const chunk = buffer.slice(0, boundaryIndex).trim();
    buffer = buffer.slice(boundaryIndex + boundaryLength);

    if (!chunk) {
      continue;
    }

    const lines = chunk.split(/\r?\n/);
    const dataLines = lines
      .filter((line) => line.startsWith("data:"))
      .map((line) => line.slice(5).trimStart());

    if (!dataLines.length) {
      continue;
    }

    const raw = dataLines.join("\n");
    try {
      onEvent(JSON.parse(raw) as TdAgentStreamEvent);
    } catch (error) {
      throw new Error(
        `SSE 数据解析失败: ${error instanceof Error ? error.message : String(error)}`,
      );
    }
  }

  return buffer;
}

async function postSse(
  path: string,
  payload: unknown,
  onEvent: (event: TdAgentStreamEvent) => void,
  signal?: AbortSignal,
) {
  const response = await fetch(buildUrl(path), {
    method: "POST",
    headers: createHeaders({
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    }),
    body: JSON.stringify(payload),
    signal,
  });

  if (!response.ok || !response.body) {
    throw new Error((await response.text()) || `流式请求失败: ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    buffer = consumeSseBuffer(buffer, onEvent);
  }

  buffer += decoder.decode();
  if (buffer.trim()) {
    consumeSseBuffer(`${buffer}\n\n`, onEvent);
  }
}

export const agentConsoleApi = {
  apiBase: API_BASE,

  async listSessions() {
    const response = await fetch(buildUrl("/sessions/me"), {
      headers: createHeaders(),
    });
    return parseApiResult<ConversationView[]>(response);
  },

  async getSessionHistory(sessionId: string) {
    const response = await fetch(buildUrl(`/sessions/me/${sessionId}`), {
      headers: createHeaders(),
    });
    return parseApiResult<SessionHistoryResponse>(response);
  },

  async listPendingApprovals(sessionId: string) {
    const response = await fetch(buildUrl(`/approvals/me/${sessionId}`), {
      headers: createHeaders(),
    });
    return parseApiResult<ToolApproval[]>(response);
  },

  async interruptChat(sessionId: string) {
    const response = await fetch(buildUrl("/chat/interrupt"), {
      method: "POST",
      headers: createHeaders({
        "Content-Type": "application/json",
      }),
      body: JSON.stringify({ sessionId }),
    });
    return response.json();
  },

  streamChat(
    payload: ChatRequestPayload,
    onEvent: (event: TdAgentStreamEvent) => void,
    signal?: AbortSignal,
  ) {
    return postSse("/chat/stream", payload, onEvent, signal);
  },

  approveAndResume(
    payload: ToolApprovalActionPayload,
    onEvent: (event: TdAgentStreamEvent) => void,
    signal?: AbortSignal,
  ) {
    return postSse("/approvals/approve", payload, onEvent, signal);
  },

  rejectAndResume(
    payload: ToolApprovalActionPayload,
    onEvent: (event: TdAgentStreamEvent) => void,
    signal?: AbortSignal,
  ) {
    return postSse("/approvals/reject", payload, onEvent, signal);
  },
};
