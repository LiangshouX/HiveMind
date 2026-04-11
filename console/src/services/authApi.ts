import type {
  AuthSession,
  AuthUser,
  LoginPayload,
  RegisterPayload,
  UpdateProfilePayload,
} from "../types";
import { buildApiUrl, createHeaders, parseApiResult } from "./http";

async function postJson<T>(path: string, body: unknown, withAuth = false) {
  const response = await fetch(buildApiUrl(path), {
    method: "POST",
    headers: createHeaders(
      withAuth ? { "Content-Type": "application/json" } : { "Content-Type": "application/json" },
    ),
    body: JSON.stringify(body),
  });
  return parseApiResult<T>(response);
}

async function putJson<T>(path: string, body: unknown) {
  const response = await fetch(buildApiUrl(path), {
    method: "PUT",
    headers: createHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(body),
  });
  return parseApiResult<T>(response);
}

export const authApi = {
  login(payload: LoginPayload) {
    return postJson<AuthSession>("/auth/login", payload);
  },

  register(payload: RegisterPayload) {
    return postJson<AuthUser>("/auth/register", payload);
  },

  async getCurrentUser() {
    const response = await fetch(buildApiUrl("/auth/me"), {
      headers: createHeaders(),
    });
    return parseApiResult<AuthUser>(response);
  },

  updateProfile(payload: UpdateProfilePayload) {
    return putJson<AuthUser>("/auth/me", payload);
  },
};
