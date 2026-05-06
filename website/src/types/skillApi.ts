// Skill 云端存储相关类型定义

export interface SkillFileManifest {
  version: string;
  objectKey: string;
  updatedAt: string;
}

export interface CloudSkill {
  skillId: string;
  userId: string;
  name: string;
  description: string;
  currentVersion: string;
  status: 'draft' | 'published' | 'deprecated' | 'archived';
  tags: string[];
  dependencies?: Record<string, unknown> | null;
  executionEnv?: Record<string, string> | null;
  fileManifest?: SkillFileManifest | null;
  downloadUrl?: string | null;
  /** 技能来源：BUILTIN(系统内置) 或 CUSTOMIZED(用户自定义) */
  source?: 'BUILTIN' | 'CUSTOMIZED';
  enabled?: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface SkillCreateRequest {
  name: string;
  description?: string;
  skillMarkdown: string;
  resources?: Record<string, string>;
  tags?: string[];
  executionEnv?: Record<string, string>;
  dependencies?: Record<string, unknown>;
  publish?: boolean;
  version?: string;
}

export interface SkillVersionRequest {
  skillMarkdown: string;
  resources?: Record<string, string>;
  version: string;
}

export interface SkillPageQuery {
  userId?: string;
  keyword?: string;
  status?: string;
  tags?: string[];
  pageNum: number;
  pageSize: number;
  orderBy?: string;
  orderDir?: 'asc' | 'desc';
}

export interface SkillPageResponse {
  records: CloudSkill[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
