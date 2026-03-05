import React, { useState } from 'react';
import { Button } from '@/common/components/ui/button';
import { Input } from '@/common/components/ui/input';
import { Card, CardHeader, CardTitle, CardContent } from '@/common/components/ui/card';
import { api, ApiError } from '@/common/lib/api-client';
import type { AdminLoginResponse, SuperAdminLoginResponse } from '@/common/types';

interface Props {
  onAdminLogin: (token: string, storeName: string, username: string) => void;
  onSuperAdminLogin: (token: string, username: string) => void;
}

export default function LoginPage({ onAdminLogin, onSuperAdminLogin }: Props) {
  const [mode, setMode] = useState<'admin' | 'super-admin'>('admin');
  const [storeCode, setStoreCode] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (mode === 'admin') {
        const res = await api.post<AdminLoginResponse>('/admin/auth/login', { storeCode, username, password });
        onAdminLogin(res.token, res.storeName, res.username);
      } else {
        const res = await api.post<SuperAdminLoginResponse>('/super-admin/auth/login', { username, password });
        onSuperAdminLogin(res.token, res.username);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-center">테이블오더 관리자</CardTitle>
          <div className="flex gap-2 pt-2">
            <Button
              variant={mode === 'admin' ? 'default' : 'outline'}
              className="flex-1"
              onClick={() => setMode('admin')}
              type="button"
            >
              매장 관리자
            </Button>
            <Button
              variant={mode === 'super-admin' ? 'default' : 'outline'}
              className="flex-1"
              onClick={() => setMode('super-admin')}
              type="button"
            >
              슈퍼 관리자
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === 'admin' && (
              <div>
                <label htmlFor="storeCode" className="text-sm font-medium">매장 코드</label>
                <Input id="storeCode" value={storeCode} onChange={(e) => setStoreCode(e.target.value)} required />
              </div>
            )}
            <div>
              <label htmlFor="username" className="text-sm font-medium">사용자명</label>
              <Input id="username" value={username} onChange={(e) => setUsername(e.target.value)} required />
            </div>
            <div>
              <label htmlFor="password" className="text-sm font-medium">비밀번호</label>
              <Input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? '로그인 중...' : '로그인'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
