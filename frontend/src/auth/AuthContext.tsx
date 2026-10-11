import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, setAuthToken, setUnauthorizedHandler } from "../api/client";
import type { Role } from "../api/types";

interface Session {
  token: string;
  username: string;
  role: Role;
  expiresAt: number;
}

interface AuthValue {
  session: Session | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const STORAGE_KEY = "hap.session";
const AuthContext = createContext<AuthValue | null>(null);

function readStored(): Session | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Session;
    return s.expiresAt > Date.now() ? s : null;
  } catch {
    return null;
  }
}

function store(session: Session | null) {
  try {
    if (session) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    /* storage unavailable (private mode) - session lives in memory only */
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => {
    const s = readStored();
    setAuthToken(s?.token ?? null);
    return s;
  });

  const logout = useCallback(() => {
    setAuthToken(null);
    store(null);
    setSession(null);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const res = await api.login(username, password);
    const s: Session = {
      token: res.token,
      username: res.username,
      role: res.role,
      expiresAt: Date.now() + res.expiresInSeconds * 1000,
    };
    setAuthToken(s.token);
    store(s);
    setSession(s);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  // Log out automatically when the token expires.
  useEffect(() => {
    if (!session) return;
    const t = window.setTimeout(logout, Math.max(0, session.expiresAt - Date.now()));
    return () => window.clearTimeout(t);
  }, [session, logout]);

  const value = useMemo(() => ({ session, login, logout }), [session, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
