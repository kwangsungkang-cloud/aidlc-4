import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { apiClient, ApiError } from '@/common/lib/api-client';
import type { CartItem, CreateOrderRequest, CreateOrderResponse } from '@/common/types';

interface Props {
  items: CartItem[];
  totalAmount: number;
  onBack: () => void;
  onOrderSuccess: (orderNumber: string) => void;
}

export function OrderConfirmPage({ items, totalAmount, onBack, onOrderSuccess }: Props) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleConfirm = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const body: CreateOrderRequest = {
        items: items.map(i => ({ menuId: i.menuId, quantity: i.quantity })),
      };
      const res = await apiClient.post<CreateOrderResponse>('/api/customer/orders', body);
      onOrderSuccess(res.orderNumber);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '주문에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-28">
      <header className="sticky top-0 z-10 bg-white border-b px-4 py-3 flex items-center gap-2">
        <Button variant="ghost" size="sm" onClick={onBack}>← 장바구니</Button>
        <h1 className="text-lg font-bold">주문 확인</h1>
      </header>

      <div className="p-4 space-y-3">
        <Card>
          <CardContent className="p-4 space-y-3">
            <p className="font-medium">주문 내역</p>
            {items.map(item => (
              <div key={item.menuId} className="flex justify-between items-center">
                <div>
                  <span className="font-medium">{item.name}</span>
                  <span className="text-muted-foreground ml-2">x{item.quantity}</span>
                </div>
                <span>{(item.price * item.quantity).toLocaleString()}원</span>
              </div>
            ))}
            <Separator />
            <div className="flex justify-between items-center">
              <span className="font-bold text-lg">총 금액</span>
              <span className="font-bold text-lg text-orange-600">{totalAmount.toLocaleString()}원</span>
            </div>
          </CardContent>
        </Card>

        {error && <p className="text-red-500 text-sm text-center" role="alert">{error}</p>}
      </div>

      <div className="fixed bottom-0 left-0 right-0 bg-white border-t p-4">
        <Button onClick={handleConfirm} className="w-full h-14 text-lg" disabled={submitting}>
          {submitting ? '주문 전송 중...' : '주문 확정'}
        </Button>
      </div>
    </div>
  );
}
