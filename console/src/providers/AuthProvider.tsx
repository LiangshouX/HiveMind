import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { authApi } from "../services/authApi";
import {
  clearAuthSession,
  readAuthSession,
  writeAuthSession,
} from "../services/authStorage";
import type {
  AuthSession,
  AuthUser,
  LoginPayload,
  RegisterPayload,
  UpdateProfilePayload,
} from "../types";

interface AuthContextValue {
  session: AuthSession | null;
  user: AuthUser | null;
  ready: boolean;
  authenticated: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<AuthUser>;
  refreshUser: () => Promise<void>;
  updateProfile: (payload: UpdateProfilePayload) => Promise<AuthUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => readAuthSession());
  const [ready, setReady] = useState(false);

  const persistSession = useCallback((next: AuthSession | null) => {
    setSession(next);
    if (next) {
      writeAuthSession(next);
      return;
    }
    clearAuthSession();
  }, []);

  const refreshUser = useCallback(async () => {
    const current = readAuthSession();
    if (!current) {
      persistSession(null);
      return;
    }
    const user = await authApi.getCurrentUser();
    persistSession({ ...current, user });
  }, [persistSession]);

  useEffect(() => {
    const current = readAuthSession();
    if (!current) {
      setReady(true);
      return;
    }
    authApi
      .getCurrentUser()
      .then((user) => persistSession({ ...current, user }))
      .catch(() => persistSession(null))
      .finally(() => setReady(true));
  }, [persistSession]);

  const login = useCallback(
    async (payload: LoginPayload) => {
      const next = await authApi.login(payload);
      persistSession(next);
    },
    [persistSession],
  );

  const register = useCallback((payload: RegisterPayload) => authApi.register(payload), []);

  const updateProfile = useCallback(
    async (payload: UpdateProfilePayload) => {
      const user = await authApi.updateProfile(payload);
      const current = readAuthSession();
      if (current) {
        persistSession({ ...current, user });
      }
      return user;
    },
    [persistSession],
  );

  const logout = useCallback(() => {
    persistSession(null);
  }, [persistSession]);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      user: session?.user ?? null,
      ready,
      authenticated: Boolean(session?.token),
      login,
      register,
      refreshUser,
      updateProfile,
      logout,
    }),
    [login, logout, ready, refreshUser, register, session, updateProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth 必须在 AuthProvider 内部使用");
  }
  return context;
}
