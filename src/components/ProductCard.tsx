import * as React from 'react';
import { Thumb } from '@/components/Thumb';
import { money, REDUCED, type Product } from '@/data/products';

interface ProductCardProps {
  product: Product;
  qty: number;
  onInc: (id: string) => void;
  onDec: (id: string) => void;
  index: number;
}

export function ProductCard({ product, qty, onInc, onDec, index }: ProductCardProps) {
  const qtyRef = React.useRef<HTMLSpanElement | null>(null);
  const prevQty = React.useRef(qty);

  React.useEffect(() => {
    if (
      qty !== prevQty.current &&
      qtyRef.current &&
      !REDUCED &&
      typeof qtyRef.current.animate === 'function'
    ) {
      qtyRef.current.animate(
        [{ transform: 'scale(1)' }, { transform: 'scale(1.35)' }, { transform: 'scale(1)' }],
        { duration: 340, easing: 'cubic-bezier(.34,1.56,.64,1)' },
      );
    }
    prevQty.current = qty;
  }, [qty]);

  return (
    <article
      className="card fade-up group relative flex gap-3.5 overflow-hidden rounded-[18px] border border-espresso/10 bg-paper p-2.5 shadow-card sm:flex-col sm:gap-0 sm:p-0"
      data-id={product.id}
      style={{ animationDelay: Math.min(index * 30, 220) + 'ms' }}
    >
      <Thumb
        product={product}
        showTag
        rounded
        alt={product.name}
        emojiClassName="text-[17px] sm:text-[34px]"
        className="h-[96px] w-[96px] shrink-0 rounded-[13px] sm:h-44 sm:w-full sm:rounded-none"
      />

      <div className="flex min-w-0 flex-1 flex-col justify-between sm:p-4">
        <div className="min-w-0">
          <h3 className="font-display text-[15px] font-semibold leading-snug tracking-[-0.01em] text-espresso sm:text-[16px]">
            {product.name}
          </h3>
          <p className="mt-1 line-clamp-2 text-[11px] leading-relaxed text-coffee/55 sm:text-[11.5px]">
            {product.desc}
          </p>
        </div>

        <div className="mt-2 flex items-end justify-between gap-2 sm:mt-3.5">
          <p className="font-display text-[19px] font-semibold leading-none text-espresso">
            {money(product.price)}
          </p>

          <div
            className="flex items-center gap-1 rounded-full border border-espresso/10 bg-cream/70 p-[3px]"
            role="group"
            aria-label={'Cantidad de ' + product.name}
          >
            <button
              type="button"
              data-act="dec"
              onClick={() => onDec(product.id)}
              disabled={qty === 0}
              aria-label={'Quitar uno de ' + product.name}
              className="step-btn grid h-8 w-8 place-items-center rounded-full text-[17px] font-bold leading-none text-coffee/70 active:bg-cream-deep disabled:cursor-not-allowed disabled:opacity-25"
            >
              −
            </button>
            <span
              ref={qtyRef}
              className={
                'qty-display w-5 text-center text-[13px] font-extrabold tabular-nums ' +
                (qty ? 'text-espresso' : 'text-espresso/25')
              }
            >
              {qty}
            </span>
            <button
              type="button"
              data-act="inc"
              onClick={() => onInc(product.id)}
              aria-label={'Agregar uno de ' + product.name}
              className="step-btn grid h-8 w-8 place-items-center rounded-full bg-espresso text-[17px] font-bold leading-none text-cream shadow-sm active:bg-coffee"
            >
              +
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}
