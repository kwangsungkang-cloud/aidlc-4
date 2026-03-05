import { useState, useCallback } from 'react';

interface AuthState {
  token: string | null;
  role: 'admin' | 'super-admin' | null;
  storeName: string | null;
  username: string | null;
}

function loadAuth(): AuthState {
  const token = localStorage.getItem('admin_token');
  const role = localStorage.getItem('admin_role') as AuthState['role'];
  const storeName = localStorage.getItem('admin_store_name');
  const username = localStorage.getItem('admin_username');
  return { token, role, storeName, username };
}

export function useAuth() {
  const [auth, setAuth] = useState<AuthState>(loadAuth);

  const loginAsAdmin = useCallback((token: string, storeName: string, username: string) => {
    localStorage.setItem('admin_token', token);
    localStorage.setItem('admin_role', 'admin');
    localStorage.setItem('admin_store_name', storeName);
    localStorage.setItem('admin_username', username);
    setAuth({ token, role: 'admin', storeName, username });
  }, []);

  const loginAsSuperAdmin = useCallback((token: string, username: string) => {
    localStorage.setItem('admin_token', token);
    localStorage.setItem('admin_role', 'super-admin');
    localStorage.setItem('admin_store_name', '');
    localStorage.setItem('admin_username', username);
    setAuth({ token, role: 'super-admin', storeName: '', username });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_role');
    localStorage.removeItem('admin_store_name');
    localStorage.removeItem('admin_username');
    setAuth({ token: null, role: null, storeName: null, username: null });
  }, []);

  return { ...auth, isLoggedIn: !!auth.token, loginAsAdmin, loginAsSuperAdmin, logout };
}
