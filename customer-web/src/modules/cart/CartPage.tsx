import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import type { CartItem } from '@/common/types';

interface Props {
  items: CartItem[];
  totalAmount: number;
  onUpdateQuantity: (menuId: number, quantity: number) => void;
  onRemoveItem: (menuId: number) => void;
  onClearCart: () => void;
  onGoBack: () => void;
  onOrder: () => void;
}

export function CartPage({ items, totalAmount, onUpdateQuantity, onRemoveItem, onClearCart, onGoBack, onOrder }: Props) {
  return (
    <div className="min-h-screen bg-gray-50 pb-28">
      <header className="sticky top-0 z-10 bg-white border-b px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={onGoBack}>← 메뉴</Button>
          <h1 className="text-lg font-bold">장바구니</h1>
        </div>
        {items.length > 0 && (
          <Button variant="ghost" size="sm" className="text-red-500" onClick={onClearCart}>비우기</Button>
        )}
      </header>

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-[60vh] text-muted-foreground">
          <span className="text-5xl mb-4">🛒</span>
          <p className="text-lg">장바구니가 비어있습니다</p>
          <Button variant="outline" className="mt-4" onClick={onGoBack}>메뉴 보기</Button>
        </div>
      ) : (
        <div className="p-4 space-y-3">
          {items.map(item => (
            <Card key={item.menuId}>
              <CardContent className="p-4 flex items-center gap-3">
                <div className="w-16 h-16 rounded-lg bg-gray-100 flex-shrink-0 flex items-center justify-center overflow-hidden">
                  {item.imageUrl ? (
                    <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
                  ) : (
                    <span className="text-2xl">🍽️</span>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium truncate">{item.name}</p>
                  <p className="text-sm text-muted-foreground">{item.price.toLocaleString()}원</p>
                  <p className="font-bold">{(item.price * item.quantity).toLocaleString()}원</p>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-9 h-9"
                    onClick={() => onUpdateQuantity(item.menuId, item.quantity - 1)}
                    aria-label={`${item.name} 수량 감소`}
                  >
                    −
                  </Button>
                  <span className="w-6 text-center font-medium">{item.quantity}</span>
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-9 h-9"
                    onClick={() => onUpdateQuantity(item.menuId, item.quantity + 1)}
                    aria-label={`${item.name} 수량 증가`}
                  >
                    +
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-red-400 ml-1"
                    onClick={() => onRemoveItem(item.menuId)}
                    aria-label={`${item.name} 삭제`}
                  >
                    ✕
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {items.length > 0 && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t p-4 space-y-3">
          <div className="flex justify-between items-center">
            <span className="text-muted-foreground">총 금액</span>
            <span className="text-xl font-bold">{totalAmount.toLocaleString()}원</span>
          </div>
          <Separator />
          <Button onClick={onOrder} className="w-full h-14 text-lg">
            주문하기 ({items.length}개 메뉴)
          </Button>
        </div>
      )}
    </div>
  );
}
