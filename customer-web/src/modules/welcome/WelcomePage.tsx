import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

interface Props {
  storeName: string;
  tableNumber: number;
  onStart: () => void;
}

export function WelcomePage({ storeName, tableNumber, onStart }: Props) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-b from-orange-50 to-white p-4">
      <Card className="w-full max-w-md text-center">
        <CardContent className="pt-8 pb-8 space-y-6">
          <div className="text-5xl">🍽️</div>
          <div>
            <h1 className="text-2xl font-bold">{storeName}</h1>
            <p className="text-lg text-muted-foreground mt-1">테이블 {tableNumber}번</p>
          </div>
          <p className="text-muted-foreground">환영합니다!</p>

          <div className="bg-gray-50 rounded-xl p-4 space-y-3 text-left">
            <p className="font-medium text-center">이용 방법</p>
            <div className="flex items-center gap-3">
              <span className="flex-shrink-0 w-8 h-8 bg-orange-100 rounded-full flex items-center justify-center text-lg">📋</span>
              <span>메뉴를 탐색하고 원하는 메뉴를 선택하세요</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="flex-shrink-0 w-8 h-8 bg-orange-100 rounded-full flex items-center justify-center text-lg">🛒</span>
              <span>장바구니에 담고 수량을 조절하세요</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="flex-shrink-0 w-8 h-8 bg-orange-100 rounded-full flex items-center justify-center text-lg">✅</span>
              <span>주문하기 버튼으로 주문을 확정하세요</span>
            </div>
          </div>

          <Button onClick={onStart} className="w-full h-14 text-lg">
            주문 시작하기
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
