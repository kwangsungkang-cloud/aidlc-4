import { useState, useEffect } from 'react';
import { Button } from '@/common/components/ui/button';
import { Input } from '@/common/components/ui/input';
import { Card, CardHeader, CardTitle, CardContent } from '@/common/components/ui/card';
import { api } from '@/common/lib/api-client';
import type { MenuCategory, MenuItem } from '@/common/types';

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

export default function MenuManagementPage() {
  const [categories, setCategories] = useState<MenuCategory[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [editingMenu, setEditingMenu] = useState<MenuItem | null>(null);
  const [formName, setFormName] = useState('');
  const [formPrice, setFormPrice] = useState('');
  const [formDesc, setFormDesc] = useState('');
  const [formCategoryId, setFormCategoryId] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchMenus = async () => {
    try {
      const data = await api.get<{ categories: MenuCategory[] }>('/customer/menus');
      setCategories(data.categories);
      if (!selectedCategory && data.categories.length > 0) {
        setSelectedCategory(data.categories[0].categoryId);
      }
    } catch { /* ignore */ } finally { setLoading(false); }
  };

  useEffect(() => { fetchMenus(); }, []);

  const resetForm = () => {
    setFormName(''); setFormPrice(''); setFormDesc(''); setFormCategoryId('');
    setEditingMenu(null); setIsCreating(false);
  };

  const buildMenuFormData = () => {
    const catId = formCategoryId ? Number(formCategoryId) : selectedCategory;
    if (!catId) throw new Error('카테고리를 선택해주세요');
    const fd = new FormData();
    const menuJson = JSON.stringify({
      categoryId: catId,
      name: formName,
      price: Number(formPrice),
      description: formDesc || null,
    });
    fd.append('menu', new Blob([menuJson], { type: 'application/json' }));
    return fd;
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const fd = buildMenuFormData();
      await api.postForm('/admin/menus', fd);
      resetForm();
      fetchMenus();
    } catch (err: any) { alert(err.message); }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingMenu) return;
    try {
      const fd = buildMenuFormData();
      await api.putForm(`/admin/menus/${editingMenu.menuId}`, fd);
      resetForm();
      fetchMenus();
    } catch (err: any) { alert(err.message); }
  };

  const handleDelete = async (menuId: number) => {
    if (!confirm('이 메뉴를 삭제하시겠습니까?')) return;
    try {
      await api.delete(`/admin/menus/${menuId}`);
      fetchMenus();
    } catch (err: any) { alert(err.message); }
  };

  const startEdit = (menu: MenuItem) => {
    setEditingMenu(menu);
    setFormName(menu.name);
    setFormPrice(String(menu.price));
    setFormDesc(menu.description || '');
    setFormCategoryId(String(selectedCategory || ''));
    setIsCreating(false);
  };

  const currentMenus = categories.find((c) => c.categoryId === selectedCategory)?.menus || [];

  if (loading) return <div className="p-8 text-center text-muted-foreground">로딩 중...</div>;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">메뉴 관리</h2>
        <Button onClick={() => { resetForm(); setIsCreating(true); }}>메뉴 추가</Button>
      </div>

      <div className="flex gap-2 mb-4 flex-wrap">
        {categories.map((cat) => (
          <Button
            key={cat.categoryId}
            variant={selectedCategory === cat.categoryId ? 'default' : 'outline'}
            size="sm"
            onClick={() => setSelectedCategory(cat.categoryId)}
          >
            {cat.name} ({cat.menus.length})
          </Button>
        ))}
      </div>

      {(isCreating || editingMenu) && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-base">{editingMenu ? '메뉴 수정' : '메뉴 추가'}</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={editingMenu ? handleUpdate : handleCreate} className="space-y-3">
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="text-sm font-medium">카테고리</label>
                  <select
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={formCategoryId || selectedCategory || ''}
                    onChange={(e) => setFormCategoryId(e.target.value)}
                  >
                    {categories.map((cat) => (
                      <option key={cat.categoryId} value={cat.categoryId}>{cat.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-sm font-medium">메뉴명</label>
                  <Input value={formName} onChange={(e) => setFormName(e.target.value)} required maxLength={100} />
                </div>
                <div>
                  <label className="text-sm font-medium">가격</label>
                  <Input type="number" min="1" max="10000000" value={formPrice} onChange={(e) => setFormPrice(e.target.value)} required />
                </div>
              </div>
              <div>
                <label className="text-sm font-medium">설명</label>
                <Input value={formDesc} onChange={(e) => setFormDesc(e.target.value)} maxLength={500} />
              </div>
              <div className="flex gap-2">
                <Button type="submit">{editingMenu ? '수정' : '추가'}</Button>
                <Button type="button" variant="outline" onClick={resetForm}>취소</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {currentMenus.map((menu) => (
          <Card key={menu.menuId}>
            <CardContent className="p-4">
              <div className="text-4xl mb-2">{getMenuEmoji(menu.name)}</div>
              <h4 className="font-medium">{menu.name}</h4>
              <p className="text-lg font-bold">{menu.price.toLocaleString()}원</p>
              {menu.description && <p className="text-sm text-muted-foreground mt-1">{menu.description}</p>}
              <div className="flex gap-2 mt-3">
                <Button size="sm" variant="outline" onClick={() => startEdit(menu)}>수정</Button>
                <Button size="sm" variant="destructive" onClick={() => handleDelete(menu.menuId)}>삭제</Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
