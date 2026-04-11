import type { AuthSession } from "../types";

const AUTH_STORAGE_KEY = "td-agent-console-auth";

export function readAuthSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const session = JSON.parse(raw) as AuthSession;
    if (!session.token || !session.expiresAt || !session.user) {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
    if (new Date(session.expiresAt).getTime() <= Date.now()) {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function writeAuthSession(session: AuthSession) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function clearAuthSession() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function getAuthToken() {
  return readAuthSession()?.token ?? null;
}
