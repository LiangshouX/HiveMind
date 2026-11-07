/* eslint-disable @typescript-eslint/no-explicit-any */
import axios from 'axios';
import { clearAuthSession, getAuthToken } from './services/authStorage';

// ── 工具配置类型定义（必须在 api 对象之前） ──

export interface ToolExample {
  title: string;
  input: string;
}

export interface ToolConfig {
  toolName: string;
  description: string;
  category: 'builtin' | 'sandbox' | 'browser' | 'custom';
  runEnvironment: 'system' | 'sandbox';
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  enabled: boolean;
  approvalRequired: boolean;
  denyPatterns: string[];
  examples: ToolExample[];
  customized: boolean;
  updatedAt: string | null;
}

export interface ToolConfigUpdateRequest {
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  enabled?: boolean;
  approvalRequired?: boolean;
  denyPatterns?: string[];
}

export interface ToolSyncResponse {
  synced: number;
  message: string;
}

// ── 沙箱管理相关类型 ──

export interface SandboxInfo {
  containerId: string;
  containerName: string;
  sandboxType: string;
  status: string;
  ports: string[];
  baseUrl: string;
  browserUrl: string;
  mountDir: string;
  version: string;
  refCount: number;
  providedTools: string[];
}

export interface SandboxHealth {
  sandboxEnabled: boolean;
  dockerConnected: boolean;
  errorMessage: string | null;
  totalSandboxes: number;
  runningCount: number;
  stoppedCount: number;
}

// ── 任务模板相关类型 ──

export interface TaskTemplateParam {
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'select' | 'input';
  required?: boolean;
  defaultValue?: string;
  options?: string[];
}

export interface TaskTemplate {
  templateId: string;
  name: string;
  description: string;
  category: string;
  icon: string;
  command: string;
  params: TaskTemplateParam[];
  depts: string[];
  est: string;
  cost: string;
  type: 'SYSTEM' | 'USER';
  userId: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface TaskCreateRequest {
  name: string;
  description?: string;
  category: string;
  icon?: string;
  command: string;
  params: TaskTemplateParam[];
  depts?: string[];
  est?: string;
  cost?: string;
}

export interface TaskExecuteResponse {
  ok: boolean;
  sessionId: string;
  title: string;
  message: string;
}

// ── 定时任务相关类型 ──

export interface PageResult<T> {
  total: number;
  records: T[];
  current: number;
  size: number;
}

export interface ScheduledJob {
  id: number;
  jobId: string | null;
  jobName: string;
  isActivated: number;
  cronConfig: string | null;
  timeZone: string | null;
  jobDescription: string | null;
  jobDrivedTaskId: string | null;
  createTime: string | null;
  updateTime: string | null;
}

export interface ScheduledJobPayload {
  jobId?: string;
  jobName: string;
  isActivated: number;
  cronConfig: string;
  timeZone: string;
  jobDescription?: string;
}

// 统一 baseURL：默认走同源相对路径（由 Nginx / Vite proxy 反代到后端），
// 仅在跨域部署时通过 VITE_API_URL 指定绝对地址。
const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');

// 创建 axios 实例
const request = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
});

// 请求拦截器：token 注入
request.interceptors.request.use(
  (config) => {
    const token = getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：统一解包 Result<T> 包装并处理错误码
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    // 标准 Result<T> 包装：{ code, data, message }
    if (res && typeof res === 'object' && 'code' in res && 'data' in res) {
      if (res.code !== 'SUCCESS') {
        return Promise.reject(new Error(res.message || '请求失败'));
      }
      return res.data;
    }
    // 少数接口直接返回裸数据
    return res;
  },
  (error) => {
    if (error.response) {
      const { status } = error.response;
      if (status === 401) {
        // 未授权，清除认证信息并跳转登录
        clearAuthSession();
        window.location.href = '/login';
      } else if (status === 403) {
        console.error('无权限访问');
      } else if (status >= 500) {
        console.error('服务器异常');
      }
    } else {
      console.error('网络超时或断开连接');
    }
    return Promise.reject(error);
  }
);

// ── API 接口定义，保留原有方法签名 ──

export const api = {
  // 核心数据 — 后端无对应 Controller，直接返回兜底数据避免 NoResourceFoundException
  liveStatus: (): Promise<LiveStatus> => Promise.resolve({ tasks: [], syncStatus: { ok: false } }) as any,
  agentConfig: (): Promise<AgentConfig> => Promise.resolve({ agents: [], knownModels: [] }) as any,
  modelChangeLog: (): Promise<ChangeLogEntry[]> => Promise.resolve([]) as any,
  officialsStats: (): Promise<OfficialsData> => Promise.resolve({ officials: [], totals: { tasks_done: 0, cost_cny: 0 }, top_official: '' }) as any,
  morningBrief: (): Promise<MorningBrief> => Promise.resolve({ categories: {} }) as any,
  morningConfig: (): Promise<SubConfig> => Promise.resolve({ categories: [], keywords: [], custom_feeds: [], feishu_webhook: '' }) as any,
  agentsStatus: (): Promise<AgentsStatusData> => Promise.resolve({ ok: false, gateway: { alive: false, probe: false, status: '' }, agents: [], checkedAt: '' }) as any,

  // Token Usage
  tokenUsageSummary: (startDate: string, endDate: string): Promise<TokenUsageSummary> =>
    request.get('/api/agent/token-usage/summary', { params: { startDate, endDate } }),
  tokenUsageByModel: (startDate: string, endDate: string): Promise<TokenUsageByModel[]> =>
    request.get('/api/agent/token-usage/by-model', { params: { startDate, endDate } }),
  tokenUsageByProvider: (startDate: string, endDate: string): Promise<TokenUsageByProvider[]> =>
    request.get('/api/agent/token-usage/by-provider', { params: { startDate, endDate } }),
  tokenUsageByDate: (startDate: string, endDate: string): Promise<TokenUsageByDate[]> =>
    request.get('/api/agent/token-usage/by-date', { params: { startDate, endDate } }),

  // 工具配置
  listToolConfigs: (): Promise<ToolConfig[]> =>
    request.get('/api/agent/tool-config'),
  updateToolConfig: (toolName: string, data: ToolConfigUpdateRequest): Promise<ToolConfig> =>
    request.put(`/api/agent/tool-config/${encodeURIComponent(toolName)}`, data),
  syncSystemTools: (): Promise<ToolSyncResponse> =>
    request.post('/api/agent/tool-config/sync'),

  // 沙箱管理
  listSandboxes: (): Promise<SandboxInfo[]> =>
    request.get('/api/agent/sandboxes'),
  getSandboxHealth: (): Promise<SandboxHealth> =>
    request.get('/api/agent/sandboxes/health'),
  getSandboxDetail: (containerId: string): Promise<SandboxInfo> =>
    request.get(`/api/agent/sandboxes/${encodeURIComponent(containerId)}`),
  stopSandbox: (containerId: string): Promise<{ success: boolean }> =>
    request.post(`/api/agent/sandboxes/${encodeURIComponent(containerId)}/stop`),
  removeSandbox: (containerId: string): Promise<{ success: boolean }> =>
    request.post(`/api/agent/sandboxes/${encodeURIComponent(containerId)}/remove`),
  cleanupSandboxes: (): Promise<{ removed: number }> =>
    request.post('/api/agent/sandboxes/cleanup'),

  // 定时任务
  listScheduledJobs: (current = 1, size = 20): Promise<PageResult<ScheduledJob>> =>
    request.get('/api/scheduled-jobs', { params: { current, size } }),
  createScheduledJob: (data: ScheduledJobPayload): Promise<boolean> =>
    request.post('/api/scheduled-jobs', data),
  updateScheduledJob: (id: number, data: ScheduledJobPayload): Promise<boolean> =>
    request.put(`/api/scheduled-jobs/${id}`, data),
  deleteScheduledJob: (id: number): Promise<boolean> =>
    request.delete(`/api/scheduled-jobs/${id}`),

  // 任务模板
  listTaskTemplates: (): Promise<TaskTemplate[]> =>
    request.get('/api/agent/task-template'),
  getTaskTemplate: (templateId: string): Promise<TaskTemplate> =>
    request.get(`/api/agent/task-template/${encodeURIComponent(templateId)}`),
  createTaskTemplate: (data: TaskCreateRequest): Promise<TaskTemplate> =>
    request.post('/api/agent/task-template', data),
  updateTaskTemplate: (templateId: string, data: Partial<TaskCreateRequest>): Promise<TaskTemplate> =>
    request.put(`/api/agent/task-template/${encodeURIComponent(templateId)}`, data),
  deleteTaskTemplate: (templateId: string): Promise<void> =>
    request.delete(`/api/agent/task-template/${encodeURIComponent(templateId)}`),
  executeTaskTemplate: (templateId: string, params: Record<string, string>): Promise<TaskExecuteResponse> =>
    request.post(`/api/agent/task-template/${encodeURIComponent(templateId)}/execute`, { params }),
};

// Types can be imported or re-declared here (using any for now to speed up implementation, but better to keep types from original api.ts)
// I will keep the original types below.

export interface ActionResult {
  ok: boolean;
  message?: string;
  error?: string;
}

export interface FlowEntry {
  at: string;
  from: string;
  to: string;
  remark: string;
}

export interface TodoItem {
  id: string | number;
  title: string;
  status: 'not-started' | 'in-progress' | 'completed';
  detail?: string;
}

export interface Heartbeat {
  status: 'active' | 'warn' | 'stalled' | 'unknown' | 'idle';
  label: string;
}

export interface Task {
  id: string;
  title: string;
  state: string;
  org: string;
  now: string;
  eta: string;
  block: string;
  ac: string;
  output: string;
  heartbeat: Heartbeat;
  flow_log: FlowEntry[];
  todos: TodoItem[];
  review_round: number;
  archived: boolean;
  archivedAt?: string;
  updatedAt?: string;
  sourceMeta?: Record<string, unknown>;
  activity?: any[];
  _prev_state?: string;
}

export interface LiveStatus {
  tasks: Task[];
  syncStatus: any;
}

export interface AgentConfig {
  agents: any[];
  knownModels?: any[];
}

export interface ChangeLogEntry {
  at: string;
  agentId: string;
  oldModel: string;
  newModel: string;
  rolledBack?: boolean;
}

export interface OfficialsData {
  officials: any[];
  totals: { tasks_done: number; cost_cny: number };
  top_official: string;
}

export interface AgentsStatusData {
  ok: boolean;
  gateway: any;
  agents: any[];
  checkedAt: string;
}

export interface MorningBrief {
  date?: string;
  generated_at?: string;
  categories: Record<string, any[]>;
}

export interface SubConfig {
  categories: any[];
  keywords: string[];
  custom_feeds: any[];
  feishu_webhook: string;
}

export interface RemoteSkillItem {
  agentId: string;
  skillName: string;
  sourceUrl: string;
  description: string;
  status?: string;
  lastUpdated?: string;
  addedAt?: string;
}

// Token Usage 相关类型
export interface TokenUsageSummary {
  totalInputTokens: number;
  totalOutputTokens: number;
  totalTokens: number;
  totalCalls: number;
  estimatedCost: number;
  startDate: string;
  endDate: string;
}

export interface TokenUsageByModel {
  modelName: string;
  modelProvider: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  callCount: number;
  estimatedCost: number;
}

export interface TokenUsageByProvider {
  modelProvider: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  callCount: number;
  modelCount: number;
}

export interface TokenUsageByDate {
  usageDate: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  callCount: number;
}

/* eslint-disable @typescript-eslint/no-explicit-any */
