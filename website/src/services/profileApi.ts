/**
 * Profile API 调用封装
 */

import { getJson, putJson, postJson, postFormData } from './http';
import type { ProfileFile, ProfileUpdateRequest, BatchUpdateRequest, BatchUpdateResponse } from '../types/profile';

export const profileApi = {
  /**
   * 获取用户的 Profile 列表
   */
  listProfiles: (): Promise<ProfileFile[]> =>
    getJson<ProfileFile[]>('/tdagent/profiles').catch(() => []),

  /**
   * 获取单个 Profile
   */
  getProfile: (filename: string): Promise<ProfileFile | null> =>
    getJson<ProfileFile>(`/tdagent/profiles/${filename}`).catch(() => null),

  /**
   * 更新 Profile
   */
  updateProfile: (filename: string, data: ProfileUpdateRequest): Promise<ProfileFile> =>
    putJson<ProfileFile>(`/tdagent/profiles/${filename}`, data),

  /**
   * 批量更新 Profile
   */
  batchUpdateProfiles: (data: BatchUpdateRequest): Promise<BatchUpdateResponse> =>
    putJson<BatchUpdateResponse>('/tdagent/profiles/batch', data),

  /**
   * 重置 Profile 为默认值
   */
  resetProfile: (filename: string): Promise<ProfileFile> =>
    postJson<ProfileFile>(`/tdagent/profiles/${filename}/reset`, {}),

  /**
   * 下载 Profile 文件
   */
  downloadProfile: async (filename: string): Promise<Blob> => {
    const response = await fetch(`/api/v1/tdagent/profiles/${filename}/download`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('td-agent-console-auth')}`,
      },
    });
    
    if (!response.ok) {
      throw new Error('下载失败');
    }
    
    return response.blob();
  },

  /**
   * 上传 Profile 文件
   */
  uploadProfile: (filename: string, file: File): Promise<ProfileFile> => {
    const formData = new FormData();
    formData.append('file', file);
    return postFormData<ProfileFile>(`/tdagent/profiles/${filename}/upload`, formData);
  },
};
