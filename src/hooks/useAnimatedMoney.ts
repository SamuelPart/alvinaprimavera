import { useEffect, useRef, useState } from 'react';
import { REDUCED } from '@/data/products';

/** Interpola el total para que la barra flotante no salte de golpe. */
export function useAnimatedMoney(value: number): number {
  const [display, setDisplay] = useState(value);
  const current = useRef(value);

  useEffect(() => {
    const from = current.current;
    current.current = value;
    if (REDUCED || from === value) {
      setDisplay(value);
      return undefined;
    }
    let raf = 0;
    const t0 = performance.now();
    const tick = (t: number) => {
      const p = Math.min(1, (t - t0) / 420);
      const e = 1 - Math.pow(1 - p, 3);
      setDisplay(from + (value - from) * e);
      if (p < 1) raf = requestAnimationFrame(tick);
      else setDisplay(value);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [value]);

  return display;
}
