import { useEffect, useState } from 'react';
import { apiClient } from '@/common/lib/api-client';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import type { Category, MenuResponse, MenuItem, CartItem } from '@/common/types';

const FOOD_EMOJI_MAP: [RegExp, string][] = [
  [/김치/, '🥬'], [/찌개/, '🍲'], [/탕|국/, '🍜'], [/밥|비빔|볶음밥/, '🍚'],
  [/불고기|고기|갈비|삼겹/, '🥩'], [/치킨|닭/, '🍗'], [/피자/, '🍕'], [/버거|햄버거/, '🍔'],
  [/면|라면|국수|파스타|냉면/, '🍝'], [/초밥|스시|회/, '🍣'], [/돈까스|까스|튀김/, '🍤'],
  [/떡볶이|떡/, '🍡'], [/만두|교자/, '🥟'], [/샐러드/, '🥗'], [/스테이크/, '🥩'],
  [/커피/, '☕'], [/라떼/, '☕'], [/아메리카노/, '☕'],
  [/주스|에이드/, '🧃'], [/맥주|비어/, '🍺'], [/소주|사케/, '🍶'], [/와인/, '🍷'],
  [/콜라|사이다|음료|탄산/, '🥤'], [/차|녹차|홍차/, '🍵'], [/스무디/, '🥤'],
  [/아이스크림|빙수/, '🍨'], [/케이크/, '🍰'], [/빵|토스트/, '🍞'], [/쿠키/, '🍪'],
  [/샌드위치/, '🥪'], [/타코/, '🌮'], [/카레/, '🍛'], [/덮밥/, '🍛'],
  [/새우/, '🦐'], [/생선|연어|참치/, '🐟'], [/해물|해산물|조개/, '🦪'],
  [/계란|달걀|오믈렛/, '🥚'], [/두부/, '🧈'], [/감자|프렌치|프라이/, '🍟'],
];

function getMenuEmoji(name: string): string {
  for (const [pattern, emoji] of FOOD_EMOJI_MAP) {
    if (pattern.test(name)) return emoji;
  }
  return '🍽️';
}

interface Props {
  cart: CartItem[];
  onAddToCart: (menu: MenuItem) => void;
  onGoToCart: () => void;
  onGoToOrders: () => void;
  totalCount: number;
  totalAmount: number;
}

export function MenuPage({ cart, onAddToCart, onGoToCart, onGoToOrders, totalCount, totalAmount }: Props) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [activeCategory, setActiveCategory] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.get<MenuResponse>('/api/customer/menus').then(data => {
      setCategories(data.categories);
      if (data.categories.length > 0) {
        setActiveCategory(data.categories[0].categoryId);
      }
    }).finally(() => setLoading(false));
  }, []);

  const activeMenus = categories.find(c => c.categoryId === activeCategory)?.menus ?? [];

  const getCartQuantity = (menuId: number) => cart.find(i => i.menuId === menuId)?.quantity ?? 0;

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center"><p>메뉴를 불러오는 중...</p></div>;
  }

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header */}
      <header className="sticky top-0 z-10 bg-white border-b px-4 py-3 flex items-center justify-between">
        <h1 className="text-lg font-bold">메뉴</h1>
        <Button variant="ghost" size="sm" onClick={onGoToOrders}>주문내역</Button>
      </header>

      {/* Category Tabs */}
      {categories.length > 0 && (
        <div className="sticky top-[53px] z-10 bg-white border-b">
          <Tabs value={String(activeCategory)} onValueChange={v => setActiveCategory(Number(v))}>
            <TabsList className="w-full justify-start overflow-x-auto px-2 h-auto flex-nowrap">
              {categories.map(c => (
                <TabsTrigger key={c.categoryId} value={String(c.categoryId)} className="min-w-fit px-4 py-2.5 text-base">
                  {c.name}
                </TabsTrigger>
              ))}
            </TabsList>
          </Tabs>
        </div>
      )}

      {/* Menu Grid */}
      <div className="p-4 grid grid-cols-4 gap-3">
        {activeMenus.map(menu => {
          const qty = getCartQuantity(menu.menuId);
          return (
            <Card key={menu.menuId} className="overflow-hidden">
              <div className="h-20 bg-gray-50 flex items-center justify-center text-5xl">
                {getMenuEmoji(menu.name)}
              </div>
              <CardContent className="p-2.5 space-y-0.5">
                <p className="font-medium text-sm line-clamp-1">{menu.name}</p>
                {menu.description && <p className="text-xs text-muted-foreground line-clamp-1">{menu.description}</p>}
                <p className="font-bold text-sm">{menu.price.toLocaleString()}원</p>
                <Button
                  onClick={() => onAddToCart(menu)}
                  className="w-full h-8 text-xs mt-1"
                  size="sm"
                >
                  {qty > 0 ? `담기 (${qty})` : '담기'}
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Cart Bottom Bar */}
      {totalCount > 0 && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t p-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Badge variant="secondary" className="text-base px-3 py-1">{totalCount}개</Badge>
            <span className="font-bold text-lg">{totalAmount.toLocaleString()}원</span>
          </div>
          <Button onClick={onGoToCart} className="h-12 px-6 text-base">
            장바구니 보기
          </Button>
        </div>
      )}
    </div>
  );
}
