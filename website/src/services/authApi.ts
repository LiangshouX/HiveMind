import { buildApiUrl, createHeaders, parseApiResult } from "./http";
import type { AuthSession, AuthUser, LoginPayload, RegisterPayload, UpdateProfilePayload } from "../types";
import { postJson } from "./http";

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
    return postJson<AuthUser>("/auth/me", payload);
  },
};
