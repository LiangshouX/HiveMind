/**
 * Profile 相关类型定义
 */

export interface ProfileFile {
  filename: string;
  content: string;
  enabled: boolean;
  source: 'DEFAULT' | 'USER_CUSTOMIZED';
  size: string;
  updatedAt: string;
}

export interface ProfileUpdateRequest {
  filename: string;
  content: string;
  enabled?: boolean;
}

export interface BatchUpdateRequest {
  profiles: ProfileUpdateRequest[];
}

export interface BatchUpdateResponse {
  updatedCount: number;
  updatedAt: string;
}
