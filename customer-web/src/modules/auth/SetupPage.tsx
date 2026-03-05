import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { TableLoginRequest } from '@/common/types';

interface Props {
  onLogin: (req: TableLoginRequest) => Promise<unknown>;
  error: string | null;
  isLoading: boolean;
}

export function SetupPage({ onLogin, error, isLoading }: Props) {
  const [storeCode, setStoreCode] = useState('');
  const [tableNumber, setTableNumber] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLogin({ storeCode, tableNumber: Number(tableNumber), password });
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">테이블 오더</CardTitle>
          <p className="text-muted-foreground mt-1">초기 설정</p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="storeCode" className="block text-sm font-medium mb-1">매장 코드</label>
              <input
                id="storeCode"
                type="text"
                value={storeCode}
                onChange={e => setStoreCode(e.target.value)}
                className="w-full border rounded-lg px-3 py-3 text-lg"
                placeholder="매장 코드 입력"
                required
                maxLength={50}
              />
            </div>
            <div>
              <label htmlFor="tableNumber" className="block text-sm font-medium mb-1">테이블 번호</label>
              <input
                id="tableNumber"
                type="number"
                value={tableNumber}
                onChange={e => setTableNumber(e.target.value)}
                className="w-full border rounded-lg px-3 py-3 text-lg"
                placeholder="테이블 번호"
                required
                min={1}
              />
            </div>
            <div>
              <label htmlFor="password" className="block text-sm font-medium mb-1">비밀번호</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                className="w-full border rounded-lg px-3 py-3 text-lg"
                placeholder="비밀번호"
                required
              />
            </div>
            {error && <p className="text-red-500 text-sm" role="alert">{error}</p>}
            <Button type="submit" className="w-full h-12 text-lg" disabled={isLoading}>
              {isLoading ? '연결 중...' : '설정 완료'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
