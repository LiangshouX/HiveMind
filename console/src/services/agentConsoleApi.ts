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

  // 使用 indexOf 替代正则表达式，提升大缓冲区的解析性能
  while (true) {
    // 查找双换行分隔符（支持 \n\n, \r\n\r\n, \r\n\n, \n\r\n）
    const doubleNewlinePos = findDoubleNewline(buffer);
    if (doubleNewlinePos === -1) {
      break;
    }

    const chunk = buffer.slice(0, doubleNewlinePos).trim();
    buffer = buffer.slice(doubleNewlinePos + getNewlineLength(buffer, doubleNewlinePos));

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

// 查找双换行符的位置
function findDoubleNewline(buffer: string): number {
  const len = buffer.length;
  for (let i = 0; i < len - 1; i++) {
    const char = buffer[i];
    const nextChar = buffer[i + 1];
    
    // 检查 \n\n, \n\r, \r\n
    if (char === "\n" && (nextChar === "\n" || nextChar === "\r")) {
      return i;
    }
    if (char === "\r" && nextChar === "\n") {
      // 继续检查下一个字符
      if (i + 2 < len) {
        const thirdChar = buffer[i + 2];
        if (thirdChar === "\n" || thirdChar === "\r") {
          return i;
        }
      }
    }
  }
  return -1;
}

// 获取换行符的长度
function getNewlineLength(buffer: string, pos: number): number {
  const char = buffer[pos];
  const nextChar = buffer[pos + 1];
  
  if (char === "\n" && nextChar === "\r") return 2;
  if (char === "\n" && nextChar === "\n") return 2;
  if (char === "\r" && nextChar === "\n") {
    const thirdChar = buffer[pos + 2];
    if (thirdChar === "\n" || thirdChar === "\r") return 3;
    return 2;
  }
  return 2;
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
