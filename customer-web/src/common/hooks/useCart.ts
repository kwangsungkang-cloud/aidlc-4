import { useState, useEffect, useCallback } from 'react';
import type { CartItem, MenuItem } from '@/common/types';

const CART_KEY = 'cart';

function loadCart(): CartItem[] {
  try {
    const raw = localStorage.getItem(CART_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveCart(items: CartItem[]) {
  localStorage.setItem(CART_KEY, JSON.stringify(items));
}

export function useCart() {
  const [items, setItems] = useState<CartItem[]>(loadCart);

  useEffect(() => {
    saveCart(items);
  }, [items]);

  const addItem = useCallback((menu: MenuItem) => {
    setItems(prev => {
      const existing = prev.find(i => i.menuId === menu.menuId);
      if (existing) {
        return prev.map(i =>
          i.menuId === menu.menuId ? { ...i, quantity: i.quantity + 1 } : i
        );
      }
      return [...prev, { menuId: menu.menuId, name: menu.name, price: menu.price, quantity: 1, imageUrl: menu.imageUrl }];
    });
  }, []);

  const updateQuantity = useCallback((menuId: number, quantity: number) => {
    if (quantity < 1) {
      setItems(prev => prev.filter(i => i.menuId !== menuId));
    } else {
      setItems(prev => prev.map(i => i.menuId === menuId ? { ...i, quantity } : i));
    }
  }, []);

  const removeItem = useCallback((menuId: number) => {
    setItems(prev => prev.filter(i => i.menuId !== menuId));
  }, []);

  const clearCart = useCallback(() => {
    setItems([]);
  }, []);

  const totalAmount = items.reduce((sum, i) => sum + i.price * i.quantity, 0);
  const totalCount = items.reduce((sum, i) => sum + i.quantity, 0);

  return { items, addItem, updateQuantity, removeItem, clearCart, totalAmount, totalCount };
}
