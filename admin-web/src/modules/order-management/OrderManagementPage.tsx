import { useState, useEffect } from 'react';
import { Button } from '@/common/components/ui/button';
import { Badge } from '@/common/components/ui/badge';
import { Card, CardHeader, CardTitle, CardContent } from '@/common/components/ui/card';
import { api } from '@/common/lib/api-client';
import type { DashboardResponse, DashboardTable } from '@/common/types';

export default function OrderManagementPage() {
  const [tables, setTables] = useState<DashboardTable[]>([]);
  const [selectedTable, setSelectedTable] = useState<DashboardTable | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchDashboard = async () => {
    try {
      const data = await api.get<DashboardResponse>('/admin/orders/dashboard');
      setTables(data.tables);
      if (selectedTable) {
        const updated = data.tables.find((t) => t.tableId === selectedTable.tableId);
        if (updated) setSelectedTable(updated);
      }
    } catch { /* ignore */ } finally { setLoading(false); }
  };

  useEffect(() => { fetchDashboard(); }, []);

  const handleStatusChange = async (orderId: number, status: string) => {
    try {
      await api.patch(`/admin/orders/${orderId}/status`, { status });
      fetchDashboard();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const handleDeleteOrder = async (orderId: number) => {
    if (!confirm('이 주문을 삭제하시겠습니까?')) return;
    try {
      await api.delete(`/admin/orders/${orderId}`);
      fetchDashboard();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const statusLabel = (s: string) => ({ PENDING: '대기중', PREPARING: '준비중', COMPLETED: '완료' }[s] || s);
  const statusVariant = (s: string) => ({ PENDING: 'warning', PREPARING: 'default', COMPLETED: 'success' }[s] || 'secondary') as any;

  if (loading) return <div className="p-8 text-center text-muted-foreground">로딩 중...</div>;

  return (
    <div className="p-6">
      <h2 className="text-xl font-semibold mb-4">주문 관리</h2>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="space-y-2">
          <h3 className="font-medium text-sm text-muted-foreground mb-2">테이블 선택</h3>
          {tables.filter((t) => t.sessionStatus === 'ACTIVE' && t.orderCount > 0).map((table) => (
            <Card
              key={table.tableId}
              className={`cursor-pointer ${selectedTable?.tableId === table.tableId ? 'border-primary' : ''}`}
              onClick={() => setSelectedTable(table)}
            >
              <CardContent className="p-4 flex justify-between items-center">
                <span className="font-medium">테이블 {table.tableNumber}</span>
                <span className="text-sm">{table.orderCount}건 · {table.totalOrderAmount.toLocaleString()}원</span>
              </CardContent>
            </Card>
          ))}
          {tables.filter((t) => t.sessionStatus === 'ACTIVE' && t.orderCount > 0).length === 0 && (
            <p className="text-sm text-muted-foreground">활성 주문이 없습니다</p>
          )}
        </div>

        <div className="lg:col-span-2 space-y-4">
          {selectedTable ? (
            <>
              <h3 className="font-medium">테이블 {selectedTable.tableNumber} 주문 목록</h3>
              {selectedTable.recentOrders.map((order) => (
                <Card key={order.orderId}>
                  <CardContent className="p-4">
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <p className="font-medium">{order.orderNumber}</p>
                        <p className="text-sm text-muted-foreground">{new Date(order.createdAt).toLocaleString('ko-KR')}</p>
                      </div>
                      <Badge variant={statusVariant(order.status)}>{statusLabel(order.status)}</Badge>
                    </div>
                    <p className="text-sm mb-3">{order.itemSummary}</p>
                    <p className="font-semibold mb-3">{order.totalAmount.toLocaleString()}원</p>
                    <div className="flex gap-2">
                      {order.status === 'PENDING' && (
                        <Button size="sm" onClick={() => handleStatusChange(order.orderId, 'PREPARING')}>준비 시작</Button>
                      )}
                      {order.status === 'PREPARING' && (
                        <Button size="sm" variant="secondary" onClick={() => handleStatusChange(order.orderId, 'COMPLETED')}>완료 처리</Button>
                      )}
                      {order.status !== 'COMPLETED' && (
                        <Button size="sm" variant="destructive" onClick={() => handleDeleteOrder(order.orderId)}>삭제</Button>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </>
          ) : (
            <p className="text-muted-foreground">테이블을 선택해주세요</p>
          )}
        </div>
      </div>
    </div>
  );
}
