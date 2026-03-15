import axios from 'axios';

// 统一 baseURL
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// 创建 axios 实例
const request = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
});

// 请求拦截器：token 注入
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：错误码处理
request.interceptors.response.use(
  (response) => {
    // 假设后端返回 { code: 200, data: ..., message: ... }
    const res = response.data;
    // 有些接口直接返回数据
    return res;
  },
  (error) => {
    if (error.response) {
      const { status } = error.response;
      if (status === 401) {
        // 未授权，清除 token 并跳转登录（需要配合路由处理）
        localStorage.removeItem('token');
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
  // 核心数据
  liveStatus: (): Promise<LiveStatus> => request.get('/api/live-status').catch(() => ({ tasks: [], syncStatus: { ok: false } })) as any,
  agentConfig: (): Promise<AgentConfig> => request.get('/api/agent-config').catch(() => ({ agents: [], knownModels: [] })) as any,
  modelChangeLog: (): Promise<ChangeLogEntry[]> => request.get('/api/model-change-log').catch(() => []) as any,
  officialsStats: (): Promise<OfficialsData> => request.get('/api/officials-stats').catch(() => ({ officials: [], totals: { tasks_done: 0, cost_cny: 0 }, top_official: '' })) as any,
  morningBrief: (): Promise<MorningBrief> => request.get('/api/morning-brief').catch(() => ({ categories: {} })) as any,
  morningConfig: (): Promise<SubConfig> => request.get('/api/morning-config').catch(() => ({ categories: [], keywords: [], custom_feeds: [], feishu_webhook: '' })) as any,
  agentsStatus: (): Promise<AgentsStatusData> => request.get('/api/agents-status').catch(() => ({ ok: false, gateway: { alive: false, probe: false, status: '' }, agents: [], checkedAt: '' })) as any,

  // 任务实时动态
  taskActivity: (id: string): Promise<any> => request.get(`/api/task-activity/${encodeURIComponent(id)}`),
  schedulerState: (id: string): Promise<any> => request.get(`/api/scheduler-state/${encodeURIComponent(id)}`),

  // 技能内容
  skillContent: (agentId: string, skillName: string): Promise<any> => request.get(`/api/skill-content/${encodeURIComponent(agentId)}/${encodeURIComponent(skillName)}`),

  // 操作类
  setModel: (agentId: string, model: string): Promise<any> => request.post('/api/set-model', { agentId, model }),
  agentWake: (agentId: string): Promise<any> => request.post('/api/agent-wake', { agentId }),
  taskAction: (taskId: string, action: string, reason: string): Promise<any> => request.post('/api/task-action', { taskId, action, reason }),
  reviewAction: (taskId: string, action: string, comment: string): Promise<any> => request.post('/api/review-action', { taskId, action, comment }),
  advanceState: (taskId: string, comment: string): Promise<any> => request.post('/api/advance-state', { taskId, comment }),
  archiveTask: (taskId: string, archived: boolean): Promise<any> => request.post('/api/archive-task', { taskId, archived }),
  archiveAllDone: (): Promise<any> => request.post('/api/archive-task', { archiveAllDone: true }),
  schedulerScan: (thresholdSec = 180): Promise<any> => request.post('/api/scheduler-scan', { thresholdSec }),
  schedulerRetry: (taskId: string, reason: string): Promise<any> => request.post('/api/scheduler-retry', { taskId, reason }),
  schedulerEscalate: (taskId: string, reason: string): Promise<any> => request.post('/api/scheduler-escalate', { taskId, reason }),
  schedulerRollback: (taskId: string, reason: string): Promise<any> => request.post('/api/scheduler-rollback', { taskId, reason }),
  refreshMorning: (): Promise<any> => request.post('/api/morning-brief/refresh', {}),
  saveMorningConfig: (config: any): Promise<any> => request.post('/api/morning-config', config),
  addSkill: (agentId: string, skillName: string, description: string, trigger: string): Promise<any> => request.post('/api/add-skill', { agentId, skillName, description, trigger }),

  createTask: (data: any): Promise<any> => request.post('/api/create-task', data),

  // Remote Skills
  remoteSkillsList: (): Promise<any> => request.get('/api/remote-skills').catch(() => ({ ok: false, error: '获取失败' })) as any,
  addRemoteSkill: (agentId: string, skillName: string, sourceUrl: string, description: string): Promise<any> => request.post('/api/add-remote-skill', { agentId, skillName, sourceUrl, description }),
  updateRemoteSkill: (agentId: string, skillName: string): Promise<any> => request.post('/api/update-remote-skill', { agentId, skillName }),
  removeRemoteSkill: (agentId: string, skillName: string): Promise<any> => request.post('/api/remove-remote-skill', { agentId, skillName }),
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
