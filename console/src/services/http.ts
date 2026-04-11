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
  if (payload.code !== 200) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}
