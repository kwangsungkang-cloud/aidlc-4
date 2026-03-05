import { useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

interface Props {
  orderNumber: string;
  onGoToMenu: () => void;
}

export function OrderSuccessPage({ orderNumber, onGoToMenu }: Props) {
  useEffect(() => {
    const timer = setTimeout(onGoToMenu, 5000);
    return () => clearTimeout(timer);
  }, [onGoToMenu]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <Card className="w-full max-w-md text-center">
        <CardContent className="pt-8 pb-8 space-y-4">
          <div className="text-6xl">✅</div>
          <h1 className="text-2xl font-bold">주문 완료</h1>
          <p className="text-muted-foreground">주문이 접수되었습니다</p>
          <div className="bg-gray-50 rounded-xl p-4">
            <p className="text-sm text-muted-foreground">주문 번호</p>
            <p className="text-xl font-bold mt-1">{orderNumber}</p>
          </div>
          <p className="text-sm text-muted-foreground">5초 후 메뉴 화면으로 이동합니다</p>
          <Button onClick={onGoToMenu} variant="outline" className="w-full h-12">
            메뉴로 돌아가기
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
