import * as React from 'react';
import { useAnimatedMoney } from '@/hooks/useAnimatedMoney';
import { money, REDUCED } from '@/data/products';

interface FloatBarProps {
  qty: number;
  total: number;
  onOpen: () => void;
}

export function FloatBar({ qty, total, onOpen }: FloatBarProps) {
  const display = useAnimatedMoney(total);
  const barRef = React.useRef<HTMLDivElement | null>(null);
  const prevQty = React.useRef(qty);

  React.useEffect(() => {
    if (qty > prevQty.current && barRef.current && !REDUCED && typeof barRef.current.animate === 'function') {
      barRef.current.animate(
        [{ transform: 'scale(1)' }, { transform: 'scale(1.03)' }, { transform: 'scale(1)' }],
        { duration: 380, easing: 'cubic-bezier(.34,1.4,.64,1)' },
      );
    }
    prevQty.current = qty;
  }, [qty]);

  return (
    <div
      id="floatBar"
      className="float-bar fixed inset-x-0 bottom-0 z-40 px-3 pb-[calc(0.7rem+env(safe-area-inset-bottom))] pt-1"
    >
      <div
        ref={barRef}
        className="grain mx-auto flex max-w-2xl items-center gap-3 overflow-hidden rounded-[18px] border border-white/10 bg-gradient-to-r from-[#2E1B0F] to-[#1A0F08] p-2.5 pl-4 shadow-bar"
      >
        <div className="relative z-[2] min-w-0">
          <p className="text-[9.5px] font-extrabold uppercase leading-none tracking-[0.2em] text-cream/45">
            <span id="barCount">{qty}</span>{' '}
            <span id="barCountLabel">{qty === 1 ? 'ítem' : 'ítems'}</span>
          </p>
          <p id="barTotal" className="mt-1 font-display text-[21px] font-medium leading-none text-gold-soft">
            {money(display)}
          </p>
        </div>

        <button
          id="viewOrder"
          type="button"
          onClick={onOpen}
          disabled={qty === 0}
          className="relative z-[2] ml-auto inline-flex shrink-0 items-center gap-2 rounded-[13px] bg-cream px-4 py-3.5 text-[12.5px] font-extrabold tracking-wide text-espresso shadow-soft transition active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-40"
        >
          Ver Pedido
          <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M5 12h13M13 6l6 6-6 6" />
          </svg>
        </button>
      </div>
    </div>
  );
}
