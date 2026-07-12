/**
 * Memory API 调用封装
 *
 * 所有接口通过 JWT token 自动获取用户身份，无需手动传递 userId。
 */

import { getJson, putJson } from './http';

/** 记忆文件信息 */
export interface MemoryFile {
  name: string;
  path: string;
  type: 'file' | 'directory';
}

/** 记忆编辑请求 */
export interface MemoryEditRequest {
  path: string;
  oldText: string;
  newText: string;
}

export const memoryApi = {
  /**
   * 获取记忆文件列表
   * @param path 目录路径，空字符串表示根目录
   */
  listFiles: (path: string = ''): Promise<string[]> =>
    getJson<string[]>(`/memory/files?path=${encodeURIComponent(path)}`).catch(() => []),

  /**
   * 读取记忆文件内容
   * @param path 文件路径
   */
  readFile: (path: string): Promise<string> =>
    getJson<string>(`/memory/files/read?path=${encodeURIComponent(path)}`).catch(() => ''),

  /**
   * 编辑记忆文件
   */
  editFile: (data: MemoryEditRequest): Promise<boolean> =>
    putJson<boolean>('/memory/files/edit', data),

  /**
   * 搜索记忆
   * @param query 搜索关键词
   * @param limit 返回结果数量
   */
  search: (query: string, limit: number = 5): Promise<string> =>
    getJson<string>(`/memory/search?query=${encodeURIComponent(query)}&limit=${limit}`).catch(() => ''),

  /**
   * 获取 ReMe 服务状态
   */
  getStatus: (): Promise<{ useMcp: boolean; enabled: boolean }> =>
    getJson<{ useMcp: boolean; enabled: boolean }>('/memory/status').catch(() => ({ useMcp: false, enabled: false })),
};
