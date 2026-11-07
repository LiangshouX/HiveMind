export type TdAgentStreamEventType =
  | "MESSAGE"
  | "REASONING"
  | "TOOL_USE"
  | "TOOL_RESULT"
  | "RESULT"
  | "APPROVAL_REQUIRED"
  | "ERROR"
  | "DONE";

export interface TdAgentStreamEvent {
  type: TdAgentStreamEventType;
  sessionId: string;
  userId: string;
  messageId?: string | null;
  content: string;
  last: boolean;
  metadata?: Record<string, unknown>;
}

export interface ApiResult<T> {
  code: string;
  message: string;
  data: T;
}

export interface AuthUser {
  id: number;
  userId: string;
  nickname: string;
  role: string;
  createTime?: string;
  updateTime?: string;
}

export interface AuthSession {
  token: string;
  expiresAt: string;
  user: AuthUser;
}

export interface LoginPayload {
  userId: string;
  password: string;
}

export interface RegisterPayload {
  userId: string;
  password: string;
  nickname?: string;
}

export interface UpdateProfilePayload {
  nickname?: string;
  password?: string;
}

export interface ChatRequestPayload {
  sessionId: string;
  title?: string;
  message: string;
  modelId?: string;
  providerId?: string;
}

export interface ToolApprovalActionPayload {
  sessionId: string;
  approvalIds: string[];
  title?: string;
  comment?: string;
}

export interface ConversationView {
  id?: string;
  sessionId: string;
  userId: string;
  title?: string;
  lastMessageAt?: string | null;
  updatedAt?: string | null;
  createdAt?: string | null;
  unreadCount?: number;
  messageCount?: number;
}

export interface StoredMessageContent {
  type: string;
  text?: string;
  name?: string;
  input?: string;
  inputRaw?: string;
  id?: string;
  // approval 类型字段
  approvalId?: string;
  riskLevel?: string;
  reason?: string;
  status?: string;      // APPROVED / REJECTED
  reviewComment?: string;
}

export interface StoredMessage {
  msgId?: string;
  name?: string;
  role?: string;
  content?: StoredMessageContent[];
  metadata?: string;
  timestamp?: string;
}

export interface SessionHistoryResponse {
  session?: ConversationView | null;
  messages: StoredMessage[];
}

/** 会话上下文窗口占用情况 */
export interface SessionContextUsage {
  usedTokens: number;
  contextWindow: number;
  usageRatio: number;
  thresholdRatio: number;
  thresholdTokens: number;
  outputReserveTokens: number;
  /** 系统提示词固定部分占用的 tokens（每次调用都会占用上下文窗口） */
  systemPromptTokens?: number;
  /** 工具定义（Tool Schema）占用的 tokens（每次调用都会占用上下文窗口） */
  toolDefinitionTokens?: number;
  /** 对话消息（历史 + 压缩摘要）占用的 tokens */
  conversationTokens?: number;
  /** 本轮模型总耗时（毫秒） */
  modelDurationMs?: number;
  /** 首 token 延迟（毫秒，Time To First Token） */
  ttftMs?: number;
  /** 输出速度（tokens / 秒） */
  tokensPerSecond?: number;
  /** 缓存命中率（0~1） */
  cacheHitRatio?: number;
  /** 未命中缓存、真实计算的输入 tokens */
  uncachedInputTokens?: number;
  /** 缓存读取（命中）的 tokens */
  cachedInputTokens?: number;
  /** 输出 tokens */
  outputTokens?: number;
  /** 模型调用次数 */
  modelCalls?: number;
  /** 当前会话使用的模型展示名 */
  modelDisplayName?: string;
  messageCount: number;
  reasoningCount: number;
  toolCallCount: number;
  compactionCount: number;
  compressedSummaryChars: number;
  providerId?: string;
  modelId?: string;
  nearThreshold: boolean;
}

/** 手动压缩上下文的结果 */
export interface CompactContextResult {
  compacted: boolean;
  tokensBefore: number;
  tokensAfter: number;
  messagesBefore: number;
  messagesAfter: number;
  middleCompressed: number;
  strategy: string;
  durationMs: number;
  compressionRatio: number;
  summary: string;
}

export interface ToolApproval {
  id: string;
  sessionId: string;
  userId: string;
  toolCallId?: string;
  toolName?: string;
  toolInputJson?: string;
  riskLevel?: string;
  reason?: string;
  status?: string;
  reviewComment?: string;
  createdAt?: string;
  updatedAt?: string;
  expiresAt?: string;
}

export type UiMessageRole = "user" | "assistant" | "system";

export type UiMessageBlockType =
  | "text"
  | "reasoning"
  | "tool_use"
  | "tool_result"
  | "result"
  | "approval"
  | "error";

export interface UiMessageBlock {
  id: string;
  type: UiMessageBlockType;
  title: string;
  content: string;
  toolName?: string;
  rawInput?: string;
  approvals?: ToolApproval[];
  /** true 表示该块内容应覆盖上一块同类内容，而不是追加（用于"完整推理"替换"增量推理"） */
  replace?: boolean;
}

export interface UiMessage {
  id: string;
  role: UiMessageRole;
  name: string;
  createdAt: string;
  blocks: UiMessageBlock[];
  streaming?: boolean;
  failed?: boolean;
}

export interface SessionState {
  sessionId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  unreadCount: number;
  preview: string;
  messages: UiMessage[];
  pendingApprovals: ToolApproval[];
  loadingHistory?: boolean;
  temp?: boolean;
}

export interface ConversationGroupItem {
  key: string;
  label: string;
  group: string;
  timestamp: number;
}
