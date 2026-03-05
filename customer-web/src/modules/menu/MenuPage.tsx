import { useEffect, useState } from 'react';
import { apiClient } from '@/common/lib/api-client';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import type { Category, MenuResponse, MenuItem, CartItem } from '@/common/types';

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
      <div className="p-4 grid grid-cols-2 gap-3">
        {activeMenus.map(menu => {
          const qty = getCartQuantity(menu.menuId);
          return (
            <Card key={menu.menuId} className="overflow-hidden">
              {menu.imageUrl && (
                <div className="aspect-square bg-gray-100">
                  <img src={menu.imageUrl} alt={menu.name} className="w-full h-full object-cover" />
                </div>
              )}
              {!menu.imageUrl && (
                <div className="aspect-square bg-gray-100 flex items-center justify-center text-4xl">🍽️</div>
              )}
              <CardContent className="p-3 space-y-1">
                <p className="font-medium text-sm line-clamp-1">{menu.name}</p>
                {menu.description && <p className="text-xs text-muted-foreground line-clamp-2">{menu.description}</p>}
                <p className="font-bold text-base">{menu.price.toLocaleString()}원</p>
                <Button
                  onClick={() => onAddToCart(menu)}
                  className="w-full h-11 text-sm mt-1"
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
