// Skill 云端存储 API 服务
import { getJson, postJson, putJson } from './http';
import type {
  CloudSkill,
  SkillCreateRequest,
  SkillVersionRequest,
  SkillPageQuery,
  SkillPageResponse,
} from '../types/skillApi';

const SKILL_CLOUD_BASE = '/tdagent/skills/cloud';

/**
 * 分页查询云端 Skills
 */
export async function fetchCloudSkills(query: SkillPageQuery): Promise<SkillPageResponse> {
  const params = new URLSearchParams();
  
  if (query.userId) params.append('userId', query.userId);
  if (query.keyword) params.append('keyword', query.keyword);
  if (query.status) params.append('status', query.status);
  if (query.tags && query.tags.length > 0) {
    query.tags.forEach(tag => params.append('tags', tag));
  }
  params.append('pageNum', String(query.pageNum));
  params.append('pageSize', String(query.pageSize));
  if (query.orderBy) params.append('orderBy', query.orderBy);
  if (query.orderDir) params.append('orderDir', query.orderDir);

  return getJson<SkillPageResponse>(`${SKILL_CLOUD_BASE}/page?${params.toString()}`);
}

/**
 * 获取单个 Skill 详情
 */
export async function fetchSkillDetail(userId: string, skillId: string): Promise<CloudSkill> {
  return getJson<CloudSkill>(`${SKILL_CLOUD_BASE}/users/${userId}/${skillId}`);
}

/**
 * 创建云端 Skill
 */
export async function createCloudSkill(
  userId: string,
  request: SkillCreateRequest
): Promise<CloudSkill> {
  return postJson<CloudSkill>(`${SKILL_CLOUD_BASE}/users/${userId}`, request);
}

/**
 * 更新 Skill 并创建新版本
 */
export async function updateCloudSkill(
  userId: string,
  skillId: string,
  request: SkillVersionRequest
): Promise<CloudSkill> {
  return putJson<CloudSkill>(
    `${SKILL_CLOUD_BASE}/users/${userId}/${skillId}/versions`,
    request
  );
}

/**
 * 发布 Skill
 */
export async function publishCloudSkill(
  userId: string,
  skillId: string
): Promise<CloudSkill> {
  return postJson<CloudSkill>(
    `${SKILL_CLOUD_BASE}/users/${userId}/${skillId}/publish`,
    {}
  );
}

/**
 * 归档 Skill
 */
export async function archiveCloudSkill(
  userId: string,
  skillId: string
): Promise<{ success: boolean }> {
  return postJson<{ success: boolean }>(
    `${SKILL_CLOUD_BASE}/users/${userId}/${skillId}/archive`,
    {}
  );
}

/**
 * 删除 Skill
 */
export async function deleteCloudSkill(
  userId: string,
  skillId: string
): Promise<{ success: boolean }> {
  return fetch(`${SKILL_CLOUD_BASE}/users/${userId}/${skillId}`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
    },
  }).then(res => res.json());
}

/**
 * 获取 Skill 下载 URL
 */
export async function getSkillDownloadUrl(
  userId: string,
  skillId: string
): Promise<{ downloadUrl: string }> {
  return getJson<{ downloadUrl: string }>(
    `${SKILL_CLOUD_BASE}/users/${userId}/${skillId}/download`
  );
}

/**
 * 获取合并后的技能列表（系统内置 + 云端技能）
 */
export async function fetchAllSkills(userId: string): Promise<CloudSkill[]> {
  return getJson<CloudSkill[]>(`${SKILL_CLOUD_BASE}/all?userId=${encodeURIComponent(userId)}`);
}

/**
 * 启用 Skill
 */
export async function enableSkill(
  userId: string,
  skillName: string
): Promise<CloudSkill> {
  return postJson<CloudSkill>(
    `/tdagent/skills/users/${userId}/${skillName}/enable`,
    {}
  );
}

/**
 * 禁用 Skill
 */
export async function disableSkill(
  userId: string,
  skillName: string
): Promise<CloudSkill> {
  return postJson<CloudSkill>(
    `/tdagent/skills/users/${userId}/${skillName}/disable`,
    {}
  );
}

