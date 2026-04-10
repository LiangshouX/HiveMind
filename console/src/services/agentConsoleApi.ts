import type {
  ChatRequestPayload,
  ConversationView,
  SessionHistoryResponse,
  TdAgentStreamEvent,
  ToolApproval,
  ToolApprovalActionPayload,
} from "../types";

const API_BASE = (
  import.meta.env.VITE_API_BASE?.trim() || "/api/v1/tdagent"
).replace(/\/$/, "");

function buildUrl(path: string) {
  return `${API_BASE}${path.startsWith("/") ? path : `/${path}`}`;
}

async function parseJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `请求失败: ${response.status}`);
  }
  return (await response.json()) as T;
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
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    },
    body: JSON.stringify(payload),
    signal,
  });

  if (!response.ok || !response.body) {
    const message = await response.text();
    throw new Error(message || `流式请求失败: ${response.status}`);
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

  async listSessions(userId: string) {
    const response = await fetch(buildUrl(`/sessions/${userId}`));
    return parseJson<ConversationView[]>(response);
  },

  async getSessionHistory(userId: string, sessionId: string) {
    const response = await fetch(buildUrl(`/sessions/${userId}/${sessionId}`));
    return parseJson<SessionHistoryResponse>(response);
  },

  async listPendingApprovals(userId: string, sessionId: string) {
    const response = await fetch(buildUrl(`/approvals/${userId}/${sessionId}`));
    return parseJson<ToolApproval[]>(response);
  },

  async interruptChat(userId: string, sessionId: string) {
    const response = await fetch(buildUrl("/chat/interrupt"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ userId, sessionId }),
    });

    if (!response.ok) {
      throw new Error((await response.text()) || "发送中断失败");
    }

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
