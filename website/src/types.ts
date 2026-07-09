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
  code: number;
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
