import { useState, useEffect, useCallback } from 'react';
import { apiClient, ApiError } from '@/common/lib/api-client';
import type { TableLoginRequest, TableLoginResponse } from '@/common/types';

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  storeName: string;
  storeCode: string;
  tableNumber: number;
  sessionId: number | null;
  isNewSession: boolean;
  error: string | null;
}

const STORAGE_KEYS = {
  token: 'token',
  storeCode: 'storeCode',
  tableNumber: 'tableNumber',
  password: 'password',
  storeName: 'storeName',
  sessionId: 'sessionId',
  welcomeSeen: 'welcomeSeen',
} as const;

export function useAuth() {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    isLoading: true,
    storeName: '',
    storeCode: '',
    tableNumber: 0,
    sessionId: null,
    isNewSession: false,
    error: null,
  });

  const saveSession = (res: TableLoginResponse) => {
    localStorage.setItem(STORAGE_KEYS.token, res.token);
    localStorage.setItem(STORAGE_KEYS.storeName, res.storeName);
    localStorage.setItem(STORAGE_KEYS.storeCode, res.storeCode);
    localStorage.setItem(STORAGE_KEYS.tableNumber, String(res.tableNumber));
    localStorage.setItem(STORAGE_KEYS.sessionId, String(res.sessionId));
  };

  const login = useCallback(async (req: TableLoginRequest) => {
    setState(s => ({ ...s, isLoading: true, error: null }));
    try {
      const res = await apiClient.post<TableLoginResponse>('/api/table/auth/login', req);
      saveSession(res);
      localStorage.setItem(STORAGE_KEYS.storeCode, req.storeCode);
      localStorage.setItem(STORAGE_KEYS.tableNumber, String(req.tableNumber));
      localStorage.setItem(STORAGE_KEYS.password, req.password);
      setState({
        isAuthenticated: true,
        isLoading: false,
        storeName: res.storeName,
        storeCode: res.storeCode,
        tableNumber: res.tableNumber,
        sessionId: res.sessionId,
        isNewSession: res.isNewSession,
        error: null,
      });
      return res;
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : '로그인에 실패했습니다';
      setState(s => ({ ...s, isLoading: false, error: msg, isAuthenticated: false }));
      throw e;
    }
  }, []);

  const autoLogin = useCallback(async () => {
    const storeCode = localStorage.getItem(STORAGE_KEYS.storeCode);
    const tableNumber = localStorage.getItem(STORAGE_KEYS.tableNumber);
    const password = localStorage.getItem(STORAGE_KEYS.password);

    if (!storeCode || !tableNumber || !password) {
      setState(s => ({ ...s, isLoading: false }));
      return;
    }

    try {
      await login({ storeCode, tableNumber: Number(tableNumber), password });
    } catch {
      localStorage.removeItem(STORAGE_KEYS.token);
      localStorage.removeItem(STORAGE_KEYS.password);
      setState(s => ({ ...s, isLoading: false, isAuthenticated: false, error: '자동 로그인에 실패했습니다. 다시 설정해주세요.' }));
    }
  }, [login]);

  const logout = useCallback(() => {
    Object.values(STORAGE_KEYS).forEach(k => localStorage.removeItem(k));
    setState({
      isAuthenticated: false, isLoading: false, storeName: '', storeCode: '',
      tableNumber: 0, sessionId: null, isNewSession: false, error: null,
    });
  }, []);

  useEffect(() => {
    autoLogin();
  }, [autoLogin]);

  return { ...state, login, logout };
}
