import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Separator } from '@/components/ui/separator';
import type { OrderResponse } from '@/common/types';

interface Props {
  orders: OrderResponse[];
  sessionTotal: number;
  onGoBack: () => void;
}

type Mode = 'by-person' | 'by-menu';

export function SplitBillPage({ orders, sessionTotal, onGoBack }: Props) {
  const [mode, setMode] = useState<Mode>('by-person');
  const [personCount, setPersonCount] = useState(2);
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set());

  // Flatten all order items
  const allItems = orders.flatMap(o =>
    o.items.map(item => ({
      key: `${o.orderId}-${item.menuName}`,
      menuName: item.menuName,
      quantity: item.quantity,
      subtotal: item.subtotal,
    }))
  );

  const toggleItem = (key: string) => {
    setSelectedItems(prev => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const groupATotal = allItems.filter(i => selectedItems.has(i.key)).reduce((s, i) => s + i.subtotal, 0);
  const groupBTotal = sessionTotal - groupATotal;
  const perPerson = personCount > 0 ? Math.ceil(sessionTotal / personCount) : 0;

  return (
    <div className="min-h-screen bg-gray-50 pb-6">
      <header className="sticky top-0 z-10 bg-white border-b px-4 py-3 flex items-center gap-2">
        <Button variant="ghost" size="sm" onClick={onGoBack}>← 주문내역</Button>
        <h1 className="text-lg font-bold">분할 계산</h1>
      </header>

      <div className="p-4 space-y-4">
        <Card className="bg-orange-50 border-orange-200">
          <CardContent className="p-4 text-center">
            <p className="text-sm text-muted-foreground">총 금액</p>
            <p className="text-2xl font-bold text-orange-600">{sessionTotal.toLocaleString()}원</p>
          </CardContent>
        </Card>

        <Tabs value={mode} onValueChange={v => setMode(v as Mode)}>
          <TabsList className="w-full">
            <TabsTrigger value="by-person" className="flex-1">인원별 분할</TabsTrigger>
            <TabsTrigger value="by-menu" className="flex-1">메뉴별 분할</TabsTrigger>
          </TabsList>
        </Tabs>

        {mode === 'by-person' && (
          <Card>
            <CardContent className="p-4 space-y-4">
              <div className="flex items-center justify-center gap-4">
                <Button
                  variant="outline"
                  size="sm"
                  className="w-11 h-11"
                  onClick={() => setPersonCount(Math.max(2, personCount - 1))}
                  aria-label="인원 감소"
                >
                  −
                </Button>
                <span className="text-2xl font-bold w-12 text-center">{personCount}</span>
                <Button
                  variant="outline"
                  size="sm"
                  className="w-11 h-11"
                  onClick={() => setPersonCount(personCount + 1)}
                  aria-label="인원 증가"
                >
                  +
                </Button>
                <span className="text-muted-foreground">명</span>
              </div>
              <Separator />
              <div className="text-center">
                <p className="text-sm text-muted-foreground">1인당 금액</p>
                <p className="text-3xl font-bold">{perPerson.toLocaleString()}원</p>
              </div>
            </CardContent>
          </Card>
        )}

        {mode === 'by-menu' && (
          <div className="space-y-3">
            <p className="text-sm text-muted-foreground">그룹 A에 포함할 메뉴를 선택하세요</p>
            {allItems.map(item => (
              <Card
                key={item.key}
                className={`cursor-pointer transition-colors ${selectedItems.has(item.key) ? 'border-orange-400 bg-orange-50' : ''}`}
                onClick={() => toggleItem(item.key)}
              >
                <CardContent className="p-3 flex justify-between items-center">
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={selectedItems.has(item.key)}
                      onChange={() => toggleItem(item.key)}
                      className="w-5 h-5"
                      aria-label={`${item.menuName} 선택`}
                    />
                    <span>{item.menuName} x{item.quantity}</span>
                  </div>
                  <span className="font-medium">{item.subtotal.toLocaleString()}원</span>
                </CardContent>
              </Card>
            ))}
            <Separator />
            <div className="grid grid-cols-2 gap-3">
              <Card className="bg-orange-50 border-orange-200">
                <CardContent className="p-3 text-center">
                  <p className="text-xs text-muted-foreground">그룹 A</p>
                  <p className="text-lg font-bold">{groupATotal.toLocaleString()}원</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-3 text-center">
                  <p className="text-xs text-muted-foreground">나머지</p>
                  <p className="text-lg font-bold">{groupBTotal.toLocaleString()}원</p>
                </CardContent>
              </Card>
            </div>
          </div>
        )}

        <p className="text-xs text-center text-muted-foreground">
          분할 계산 결과는 참고용이며 서버에 저장되지 않습니다
        </p>
      </div>
    </div>
  );
}
