import { useCallback, useEffect, useMemo, useState } from 'react';
import { CART_KEY, byId, type Product } from '@/data/products';

export type Cart = Record<string, number>;
export type CartLine = Product & { qty: number; line: number };
export interface Totals {
  list: CartLine[];
  qty: number;
  total: number;
}

function readCart(): Cart {
  try {
    const raw = JSON.parse(localStorage.getItem(CART_KEY) || '{}') as Record<string, unknown>;
    const clean: Cart = {};
    Object.keys(raw).forEach((id) => {
      const q = parseInt(String(raw[id]), 10);
      if (byId(id) && q > 0) clean[id] = Math.min(q, 99);
    });
    return clean;
  } catch {
    return {};
  }
}

export function useCart() {
  const [cart, setCart] = useState<Cart>(readCart);

  useEffect(() => {
    try {
      localStorage.setItem(CART_KEY, JSON.stringify(cart));
    } catch {
      /* modo privado */
    }
  }, [cart]);

  const changeQty = useCallback((id: string, delta: number) => {
    setCart((prev) => {
      const current = prev[id] || 0;
      const next = Math.max(0, Math.min(99, current + delta));
      if (next === current) return prev;
      const copy = { ...prev };
      if (next === 0) delete copy[id];
      else copy[id] = next;
      return copy;
    });
  }, []);

  const inc = useCallback((id: string) => changeQty(id, 1), [changeQty]);
  /** Suma N unidades de golpe (media docena / docena en panadería). */
  const add = useCallback((id: string, n: number) => changeQty(id, n), [changeQty]);
  const dec = useCallback((id: string) => changeQty(id, -1), [changeQty]);

  const remove = useCallback((id: string) => {
    setCart((prev) => {
      if (!prev[id]) return prev;
      const copy = { ...prev };
      delete copy[id];
      return copy;
    });
  }, []);

  const clear = useCallback(() => setCart({}), []);

  const totals = useMemo<Totals>(() => {
    const list = Object.keys(cart)
      .filter((id) => cart[id] > 0 && byId(id))
      .map((id) => {
        const p = byId(id) as Product;
        return { ...p, qty: cart[id], line: p.price * cart[id] };
      });
    return {
      list,
      qty: list.reduce((a, i) => a + i.qty, 0),
      total: list.reduce((a, i) => a + i.line, 0),
    };
  }, [cart]);

  return { cart, inc, add, dec, remove, clear, totals };
}
