import { useState, useEffect, useCallback } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@/common/components/ui/card';
import { Badge } from '@/common/components/ui/badge';
import { Button } from '@/common/components/ui/button';
import { api } from '@/common/lib/api-client';
import { createSseConnection } from '@/common/lib/sse-client';
import type { DashboardResponse, DashboardTable, SseOrderEvent, SseSessionEvent } from '@/common/types';

interface Props {
  onSelectTable: (tableId: number, tableNumber: number) => void;
}

export default function DashboardPage({ onSelectTable }: Props) {
  const [tables, setTables] = useState<DashboardTable[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchDashboard = useCallback(async () => {
    try {
      const data = await api.get<DashboardResponse>('/admin/orders/dashboard');
      setTables(data.tables);
    } catch {
      // 에러 시 기존 데이터 유지
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboard();

    const es = createSseConnection({
      NEW_ORDER: () => fetchDashboard(),
      ORDER_STATUS_CHANGED: () => fetchDashboard(),
      ORDER_DELETED: () => fetchDashboard(),
      SESSION_COMPLETED: () => fetchDashboard(),
    });

    return () => es?.close();
  }, [fetchDashboard]);

  if (loading) return <div className="p-8 text-center text-muted-foreground">로딩 중...</div>;

  const statusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'warning' as const;
      case 'PREPARING': return 'default' as const;
      case 'COMPLETED': return 'success' as const;
      default: return 'secondary' as const;
    }
  };

  const statusLabel = (status: string) => {
    switch (status) {
      case 'PENDING': return '대기중';
      case 'PREPARING': return '준비중';
      case 'COMPLETED': return '완료';
      default: return status;
    }
  };

  return (
    <div className="p-6">
      <h2 className="text-xl font-semibold mb-4">실시간 주문 대시보드</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {tables.map((table) => (
          <Card
            key={table.tableId}
            className={`cursor-pointer hover:shadow-md transition-shadow ${
              table.sessionStatus === 'ACTIVE' && table.orderCount > 0 ? 'border-primary' : ''
            }`}
            onClick={() => onSelectTable(table.tableId, table.tableNumber)}
          >
            <CardHeader className="pb-2">
              <div className="flex justify-between items-center">
                <CardTitle className="text-lg">테이블 {table.tableNumber}</CardTitle>
                {table.sessionStatus === 'ACTIVE' ? (
                  <Badge variant="success">이용중</Badge>
                ) : (
                  <Badge variant="secondary">비어있음</Badge>
                )}
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold mb-2">
                {table.totalOrderAmount.toLocaleString()}원
              </p>
              <p className="text-sm text-muted-foreground mb-3">주문 {table.orderCount}건</p>
              {table.recentOrders.slice(0, 3).map((order) => (
                <div key={order.orderId} className="flex justify-between items-center text-sm py-1 border-t">
                  <span className="truncate flex-1">{order.itemSummary || order.orderNumber}</span>
                  <Badge variant={statusColor(order.status)} className="ml-2">
                    {statusLabel(order.status)}
                  </Badge>
                </div>
              ))}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
