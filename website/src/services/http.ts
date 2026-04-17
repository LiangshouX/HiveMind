import type { ApiResult } from "../types";
import { clearAuthSession, getAuthToken } from "./authStorage";

const API_ROOT = (import.meta.env.VITE_BACKEND_API_ROOT?.trim() || "/api/v1").replace(
  /\/$/,
  "",
);

export function buildApiUrl(path: string) {
  return `${API_ROOT}${path.startsWith("/") ? path : `/${path}`}`;
}

export function createHeaders(init?: HeadersInit) {
  const headers = new Headers(init);
  const token = getAuthToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return headers;
}

export async function parseApiResult<T>(response: Response): Promise<T> {
  const isJson = response.headers.get("content-type")?.includes("application/json");
  const payload = isJson ? ((await response.json()) as ApiResult<T>) : null;

  if (!response.ok) {
    if (response.status === 401) {
      clearAuthSession();
    }
    throw new Error(payload?.message || response.statusText || "请求失败");
  }

  if (!payload) {
    throw new Error("响应格式错误");
  }

  // 检查是否是标准的 ApiResult 格式
  if ('code' in payload && 'data' in payload) {
    if (payload.code !== 200) {
      throw new Error(payload.message || "请求失败");
    }
    return payload.data;
  }

  // 如果不是标准的 ApiResult 格式，直接返回 payload 作为数据
  return payload as T;
}

export async function postJson<T>(path: string, payload: unknown): Promise<T> {
  const response = await fetch(buildApiUrl(path), {
    method: "POST",
    headers: createHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(payload),
  });
  return parseApiResult<T>(response);
}

export async function putJson<T>(path: string, payload: unknown): Promise<T> {
  const response = await fetch(buildApiUrl(path), {
    method: "PUT",
    headers: createHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(payload),
  });
  return parseApiResult<T>(response);
}
