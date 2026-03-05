import { useState, useEffect } from 'react';
import { Button } from '@/common/components/ui/button';
import { Input } from '@/common/components/ui/input';
import { Card, CardHeader, CardTitle, CardContent } from '@/common/components/ui/card';
import { Badge } from '@/common/components/ui/badge';
import { api } from '@/common/lib/api-client';
import type { DashboardResponse, DashboardTable, TableHistoryResponse, HistorySession } from '@/common/types';

export default function TableManagementPage() {
  const [tables, setTables] = useState<DashboardTable[]>([]);
  const [newTableNumber, setNewTableNumber] = useState('');
  const [newTablePassword, setNewTablePassword] = useState('');
  const [history, setHistory] = useState<HistorySession[] | null>(null);
  const [historyTableNumber, setHistoryTableNumber] = useState<number>(0);
  const [loading, setLoading] = useState(true);

  const fetchTables = async () => {
    try {
      const data = await api.get<DashboardResponse>('/admin/orders/dashboard');
      setTables(data.tables);
    } catch { /* ignore */ } finally { setLoading(false); }
  };

  useEffect(() => { fetchTables(); }, []);

  const handleCreateTable = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post('/admin/tables', {
        tableNumber: parseInt(newTableNumber),
        password: newTablePassword,
      });
      setNewTableNumber('');
      setNewTablePassword('');
      fetchTables();
    } catch (err: any) { alert(err.message); }
  };

  const handleEndSession = async (tableId: number) => {
    if (!confirm('이 테이블의 이용을 완료하시겠습니까? 모든 미완료 주문이 완료 처리됩니다.')) return;
    try {
      await api.post(`/admin/tables/${tableId}/end-session`);
      fetchTables();
    } catch (err: any) { alert(err.message); }
  };

  const handleViewHistory = async (tableId: number, tableNumber: number) => {
    try {
      const data = await api.get<TableHistoryResponse>(`/admin/tables/${tableId}/history`);
      setHistory(data.sessions);
      setHistoryTableNumber(tableNumber);
    } catch (err: any) { alert(err.message); }
  };

  if (loading) return <div className="p-8 text-center text-muted-foreground">로딩 중...</div>;

  return (
    <div className="p-6">
      <h2 className="text-xl font-semibold mb-4">테이블 관리</h2>

      <Card className="mb-6">
        <CardHeader><CardTitle className="text-base">테이블 등록</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={handleCreateTable} className="flex gap-3 items-end">
            <div>
              <label htmlFor="tableNum" className="text-sm font-medium">테이블 번호</label>
              <Input id="tableNum" type="number" min="1" value={newTableNumber} onChange={(e) => setNewTableNumber(e.target.value)} required />
            </div>
            <div>
              <label htmlFor="tablePw" className="text-sm font-medium">비밀번호</label>
              <Input id="tablePw" type="password" minLength={4} value={newTablePassword} onChange={(e) => setNewTablePassword(e.target.value)} required />
            </div>
            <Button type="submit">등록</Button>
          </form>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
        {tables.map((table) => (
          <Card key={table.tableId}>
            <CardContent className="p-4">
              <div className="flex justify-between items-center mb-3">
                <span className="font-medium text-lg">테이블 {table.tableNumber}</span>
                {table.sessionStatus === 'ACTIVE' ? (
                  <Badge variant="success">이용중</Badge>
                ) : (
                  <Badge variant="secondary">비어있음</Badge>
                )}
              </div>
              <p className="text-sm text-muted-foreground mb-3">
                주문 {table.orderCount}건 · {table.totalOrderAmount.toLocaleString()}원
              </p>
              <div className="flex gap-2">
                {table.sessionStatus === 'ACTIVE' && (
                  <Button size="sm" variant="destructive" onClick={() => handleEndSession(table.tableId)}>이용 완료</Button>
                )}
                <Button size="sm" variant="outline" onClick={() => handleViewHistory(table.tableId, table.tableNumber)}>과거 내역</Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {history !== null && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle className="text-base">테이블 {historyTableNumber} 과거 내역</CardTitle>
              <Button size="sm" variant="ghost" onClick={() => setHistory(null)}>닫기</Button>
            </div>
          </CardHeader>
          <CardContent>
            {history.length === 0 ? (
              <p className="text-sm text-muted-foreground">과거 내역이 없습니다</p>
            ) : (
              history.map((session) => (
                <div key={session.sessionId} className="border-b pb-4 mb-4 last:border-0">
                  <div className="flex justify-between text-sm mb-2">
                    <span>{new Date(session.startedAt).toLocaleString('ko-KR')} ~ {new Date(session.completedAt).toLocaleString('ko-KR')}</span>
                    <span className="font-semibold">{session.totalAmount.toLocaleString()}원</span>
                  </div>
                  {session.orders.map((order) => (
                    <div key={order.orderId} className="ml-4 text-sm py-1">
                      <span className="text-muted-foreground">{order.orderNumber}</span>
                      <span className="ml-2">{order.totalAmount.toLocaleString()}원</span>
                      <span className="ml-2 text-muted-foreground">
                        {order.items.map((i) => `${i.menuName} x${i.quantity}`).join(', ')}
                      </span>
                    </div>
                  ))}
                </div>
              ))
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
