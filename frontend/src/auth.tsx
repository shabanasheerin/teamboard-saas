import { createContext, ReactNode, useContext, useState } from 'react';
import api, { clearSession, saveSession } from './api';
import { disconnectRealtime } from './realtime';
import type { AuthResponse, UserInfo } from './types';

interface AuthContextValue {
  user: UserInfo | null;
  login: (email: string, password: string) => Promise<UserInfo>;
  register: (organizationName: string, name: string, email: string, password: string) => Promise<UserInfo>;
  updateUser: (patch: Partial<UserInfo>) => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}

function readStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem('user');
    return raw ? (JSON.parse(raw) as UserInfo) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(readStoredUser);

  const start = (auth: AuthResponse) => {
    saveSession(auth);
    setUser(auth.user);
    return auth.user;
  };

  const login = async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password });
    return start(data);
  };

  const register = async (organizationName: string, name: string, email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/auth/register', { organizationName, name, email, password });
    return start(data);
  };

  const updateUser = (patch: Partial<UserInfo>) => {
    if (!user) return;
    const next = { ...user, ...patch };
    localStorage.setItem('user', JSON.stringify(next));
    setUser(next);
  };

  const logout = async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      await api.post('/auth/logout', { refreshToken }).catch(() => undefined);
    }
    disconnectRealtime();
    clearSession();
    setUser(null);
  };

  return <AuthContext.Provider value={{ user, login, register, updateUser, logout }}>{children}</AuthContext.Provider>;
}
