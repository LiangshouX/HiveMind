import {buildApiUrl, createHeaders, parseApiResult, postJson, putJson} from "./http";
import type {AuthSession, AuthUser, LoginPayload, RegisterPayload, UpdateProfilePayload} from "../types";

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
