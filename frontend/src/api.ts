import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from './types';

const api = axios.create({ baseURL: '/api' });
// separate client without interceptors, used only to refresh the token
const bare = axios.create({ baseURL: '/api' });

export const getAccessToken = () => localStorage.getItem('accessToken');

export function saveSession(auth: AuthResponse) {
  localStorage.setItem('accessToken', auth.accessToken);
  localStorage.setItem('refreshToken', auth.refreshToken);
  localStorage.setItem('user', JSON.stringify(auth.user));
}

export function clearSession() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

let refreshing: Promise<string | null> | null = null;

/** Gets a new access token using the refresh token. Many calls at once share one refresh request. */
export function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return Promise.resolve(null);
  if (!refreshing) {
    refreshing = bare
      .post<AuthResponse>('/auth/refresh', { refreshToken })
      .then((r) => {
        saveSession(r.data);
        return r.data.accessToken;
      })
      .catch(() => null)
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

function secondsLeft(token: string): number {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload.exp - Date.now() / 1000;
  } catch {
    return 0;
  }
}

/** Used before opening the WebSocket so it never connects with an expired token. */
export async function ensureFreshToken(): Promise<string | null> {
  const token = getAccessToken();
  if (token && secondsLeft(token) > 30) return token;
  return refreshAccessToken();
}

api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    const isAuthCall = original?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && original && !original._retry && !isAuthCall) {
      original._retry = true;
      const token = await refreshAccessToken();
      if (token) {
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      }
      clearSession();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export function errMsg(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const message = (e.response?.data as { message?: string } | undefined)?.message;
    return message ?? 'Could not reach the server. Check that the backend is running.';
  }
  return 'Something went wrong. Please try again.';
}

export default api;
