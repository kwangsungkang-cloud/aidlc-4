import { useState, useEffect } from 'react';
import { Button } from '@/common/components/ui/button';
import { Input } from '@/common/components/ui/input';
import { Card, CardHeader, CardTitle, CardContent } from '@/common/components/ui/card';
import { api } from '@/common/lib/api-client';
import type { MenuCategory, MenuItem } from '@/common/types';

export default function MenuManagementPage() {
  const [categories, setCategories] = useState<MenuCategory[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [editingMenu, setEditingMenu] = useState<MenuItem | null>(null);
  const [formName, setFormName] = useState('');
  const [formPrice, setFormPrice] = useState('');
  const [formDesc, setFormDesc] = useState('');
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formImage, setFormImage] = useState<File | null>(null);
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
    setFormImage(null); setEditingMenu(null); setIsCreating(false);
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    const fd = new FormData();
    fd.append('categoryId', formCategoryId || String(selectedCategory));
    fd.append('name', formName);
    fd.append('price', formPrice);
    if (formDesc) fd.append('description', formDesc);
    if (formImage) fd.append('image', formImage);
    try {
      await api.postForm('/admin/menus', fd);
      resetForm();
      fetchMenus();
    } catch (err: any) { alert(err.message); }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingMenu) return;
    const fd = new FormData();
    fd.append('categoryId', formCategoryId || String(selectedCategory));
    fd.append('name', formName);
    fd.append('price', formPrice);
    if (formDesc) fd.append('description', formDesc);
    if (formImage) fd.append('image', formImage);
    try {
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
              <div className="grid grid-cols-2 gap-3">
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
              <div>
                <label className="text-sm font-medium">이미지</label>
                <Input type="file" accept="image/jpeg,image/png,image/webp" onChange={(e) => setFormImage(e.target.files?.[0] || null)} />
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
              {menu.imageUrl && (
                <img src={menu.imageUrl} alt={menu.name} className="w-full h-32 object-cover rounded mb-2" />
              )}
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
