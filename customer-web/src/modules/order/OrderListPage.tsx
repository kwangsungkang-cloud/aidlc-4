import { useEffect, useState } from 'react';
import { apiClient } from '@/common/lib/api-client';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import type { OrderListResponse, OrderResponse } from '@/common/types';

interface Props {
  onGoBack: () => void;
  onGoToSplitBill: (orders: OrderResponse[], sessionTotal: number) => void;
}

const STATUS_MAP: Record<string, { label: string; variant: 'default' | 'secondary' | 'outline' }> = {
  PENDING: { label: '대기중', variant: 'secondary' },
  PREPARING: { label: '준비중', variant: 'default' },
  COMPLETED: { label: '완료', variant: 'outline' },
};

export function OrderListPage({ onGoBack, onGoToSplitBill }: Props) {
  const [data, setData] = useState<OrderListResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.get<OrderListResponse>('/api/customer/orders')
      .then(setData)
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center"><p>주문 내역을 불러오는 중...</p></div>;
  }

  const orders = data?.orders ?? [];

  return (
    <div className="min-h-screen bg-gray-50 pb-6">
      <header className="sticky top-0 z-10 bg-white border-b px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={onGoBack}>← 메뉴</Button>
          <h1 className="text-lg font-bold">주문 내역</h1>
        </div>
        {orders.length > 0 && (
          <Button variant="outline" size="sm" onClick={() => onGoToSplitBill(orders, data?.sessionTotalAmount ?? 0)}>
            분할 계산
          </Button>
        )}
      </header>

      {orders.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-[60vh] text-muted-foreground">
          <span className="text-5xl mb-4">📋</span>
          <p className="text-lg">주문 내역이 없습니다</p>
        </div>
      ) : (
        <div className="p-4 space-y-3">
          {orders.map(order => {
            const s = STATUS_MAP[order.status] ?? STATUS_MAP.PENDING;
            return (
              <Card key={order.orderId}>
                <CardContent className="p-4 space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="font-medium">{order.orderNumber}</span>
                    <Badge variant={s.variant}>{s.label}</Badge>
                  </div>
                  <p className="text-xs text-muted-foreground">{new Date(order.createdAt).toLocaleString('ko-KR')}</p>
                  <Separator />
                  {order.items.map((item, idx) => (
                    <div key={idx} className="flex justify-between text-sm">
                      <span>{item.menuName} x{item.quantity}</span>
                      <span>{item.subtotal.toLocaleString()}원</span>
                    </div>
                  ))}
                  <Separator />
                  <div className="flex justify-between font-bold">
                    <span>합계</span>
                    <span>{order.totalAmount.toLocaleString()}원</span>
                  </div>
                </CardContent>
              </Card>
            );
          })}

          <Card className="bg-orange-50 border-orange-200">
            <CardContent className="p-4 flex justify-between items-center">
              <span className="font-bold text-lg">세션 총 금액</span>
              <span className="font-bold text-lg text-orange-600">{data?.sessionTotalAmount.toLocaleString()}원</span>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
