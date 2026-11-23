import * as React from 'react';
import { cn } from '@/lib/utils';
import type { Product } from '@/data/products';

type ThumbProduct = Pick<Product, 'img' | 'emoji' | 'name' | 'tag'>;

interface ThumbProps {
  product: ThumbProduct;
  className?: string;
  /** Texto alternativo; si se omite, la imagen es decorativa. */
  alt?: string;
  showTag?: boolean;
  rounded?: boolean;
  emojiClassName?: string;
  eager?: boolean;
}

export function Thumb({
  product,
  className,
  alt = '',
  showTag = false,
  rounded = false,
  emojiClassName = 'text-[34px]',
  eager = false,
}: ThumbProps) {
  const [state, setState] = React.useState<'loading' | 'loaded' | 'error'>('loading');

  return (
    <div
      className={cn(
        'thumb relative grid place-items-center overflow-hidden',
        state === 'error' && 'is-fallback',
        className,
      )}
    >
      <span className={cn('thumb-emoji absolute', emojiClassName)} aria-hidden="true">
        {product.emoji}
      </span>
      {state !== 'error' && (
        <img
          src={product.img}
          alt={alt}
          loading={eager ? 'eager' : 'lazy'}
          decoding="async"
          width={360}
          height={360}
          className={cn('relative h-full w-full object-cover', state === 'loaded' && 'is-loaded')}
          onLoad={() => setState('loaded')}
          onError={() => setState('error')}
        />
      )}
      {rounded && (
        <span className="pointer-events-none absolute inset-0 rounded-[13px] ring-1 ring-inset ring-white/25 sm:rounded-none" />
      )}
      {showTag && product.tag && (
        <span className="absolute left-2 top-2 rounded-full bg-gold-soft/95 px-2 py-[3px] text-[8.5px] font-extrabold uppercase tracking-[0.12em] text-ink shadow-sm backdrop-blur">
          {product.tag}
        </span>
      )}
    </div>
  );
}
