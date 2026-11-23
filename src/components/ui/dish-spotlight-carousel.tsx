'use client';

import * as React from 'react';
import gsap from 'gsap';
import { cn } from '@/lib/utils';

/* ─────────────────────────────────────────────────────────────
   dish-spotlight-carousel — shadcn/ui
   Portada editorial: plato grande + titular a dos tonos sobre una
   franja diagonal, y tira de 4 platos con flechas.
   Todo con la paleta y las tipografías de Alviña.
   ───────────────────────────────────────────────────────────── */

export interface Dish {
  id: string;
  name: string;
  subtitle?: string;
  price: number;
  image: string;
  /** Color de acento del plato. */
  accent?: string;
  rating?: number;
  tag?: string;
}

export interface DishSpotlightCarouselProps {
  dishes: Dish[];
  eyebrow?: string;
  ctaLabel?: string;
  exploreLabel?: string;
  brandLine?: string;
  className?: string;
  formatPrice?: (value: number) => string;
  onOrder?: (dish: Dish, index: number) => void;
  onExplore?: () => void;
  onChange?: (dish: Dish, index: number) => void;
}

const prefersReducedMotion = () =>
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/** "Pan con Pollo Especial" → ["Pan con Pollo", "Especial"] */
function splitName(name: string): [string, string] {
  const words = name.trim().split(/\s+/);
  if (words.length < 2) return [name, ''];
  return [words.slice(0, -1).join(' '), words[words.length - 1]];
}

export function DishSpotlightCarousel({
  dishes,
  eyebrow = 'El más pedido de la casa',
  ctaLabel = 'Agregar al pedido',
  exploreLabel = 'Ver la carta completa',
  brandLine = 'Alviña Primavera · Trujillo, Perú',
  className,
  formatPrice = (v) => 'S/ ' + v.toFixed(2),
  onOrder,
  onExplore,
  onChange,
}: DishSpotlightCarouselProps) {
  const total = dishes.length;
  const [index, setIndex] = React.useState(0);
  const [added, setAdded] = React.useState(false);

  const indexRef = React.useRef(0);
  const plateRef = React.useRef<HTMLDivElement | null>(null);
  const copyRef = React.useRef<HTMLDivElement | null>(null);
  const dragRef = React.useRef<number | null>(null);
  const addedTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  const active = dishes[index];
  const [firstLine, secondLine] = React.useMemo(
    () => (active ? splitName(active.name) : ['', '']),
    [active],
  );

  const go = React.useCallback(
    (next: number) => {
      if (!total) return;
      const i = ((next % total) + total) % total;
      if (i === indexRef.current) return;
      indexRef.current = i;
      setIndex(i);
    },
    [total],
  );

  /* Transición suave entre platos */
  React.useEffect(() => {
    const plate = plateRef.current;
    const copy = copyRef.current;
    if (!plate || !copy || prefersReducedMotion()) return undefined;

    const tl = gsap.timeline();
    tl.fromTo(
      plate,
      { scale: 0.965, opacity: 0.35 },
      { scale: 1, opacity: 1, duration: 0.72, ease: 'power2.out' },
    ).fromTo(
      Array.from(copy.children),
      { y: 14, opacity: 0 },
      { y: 0, opacity: 1, duration: 0.5, stagger: 0.045, ease: 'power2.out' },
      0.04,
    );

    return () => {
      tl.kill();
    };
  }, [index]);

  React.useEffect(() => {
    setAdded(false);
    if (active) onChange?.(active, index);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index]);

  React.useEffect(
    () => () => {
      if (addedTimer.current) clearTimeout(addedTimer.current);
    },
    [],
  );

  const handleOrder = () => {
    if (!active) return;
    onOrder?.(active, index);
    setAdded(true);
    if (addedTimer.current) clearTimeout(addedTimer.current);
    addedTimer.current = setTimeout(() => setAdded(false), 1700);
  };

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
      e.preventDefault();
      go(indexRef.current + 1);
    }
    if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
      e.preventDefault();
      go(indexRef.current - 1);
    }
    if (e.key === 'Home') {
      e.preventDefault();
      go(0);
    }
    if (e.key === 'End') {
      e.preventDefault();
      go(total - 1);
    }
  };

  if (!active) return null;

  return (
    <section
      id="portada"
      className={cn('spotlight', className)}
      aria-label="El favorito de la casa"
    >
      <img
        className="spotlight-watermark"
        src="/assets/logo-alvina-cream.png"
        alt=""
        aria-hidden="true"
      />
      <div
        className="spotlight-inner"
        role="group"
        aria-roledescription="carrusel"
        aria-label="Platos destacados"
        tabIndex={0}
        onKeyDown={onKeyDown}
        onPointerDown={(e) => {
          dragRef.current = e.clientX;
        }}
        onPointerUp={(e) => {
          if (dragRef.current === null) return;
          const dx = e.clientX - dragRef.current;
          dragRef.current = null;
          if (Math.abs(dx) > 40) go(indexRef.current + (dx < 0 ? 1 : -1));
        }}
        onPointerCancel={() => {
          dragRef.current = null;
        }}
      >
        <div className="spotlight-plate-col">
          <div className="spotlight-index" aria-hidden="true">
            <span className="spotlight-index-now">{String(index + 1).padStart(2, '0')}</span>
            <span className="spotlight-index-track">
              <span
                className="spotlight-index-fill"
                style={{ height: `${((index + 1) / total) * 100}%` }}
              />
            </span>
            <span className="spotlight-index-total">{String(total).padStart(2, '0')}</span>
          </div>

          <div className="spotlight-plate" ref={plateRef}>
            <img
              className="spotlight-plate-img"
              src={active.image}
              alt={active.name}
              draggable={false}
              fetchPriority="high"
            />
          </div>
        </div>

        <div className="spotlight-copy" ref={copyRef}>
          <p className="spotlight-eyebrow">
            {eyebrow}
            {active.tag ? <span className="spotlight-badge">{active.tag}</span> : null}
          </p>

          <h2 className="spotlight-title">
            <span className="spotlight-title-1">{firstLine}</span>
            {secondLine ? <span className="spotlight-title-2">{secondLine}</span> : null}
            <span className="spotlight-band" aria-hidden="true" />
          </h2>

          <p className="spotlight-desc">{active.subtitle}</p>

          <p className="spotlight-meta">
            {typeof active.rating === 'number' ? (
              <span>
                <span className="spotlight-star" aria-hidden="true">
                  ★
                </span>{' '}
                {active.rating.toFixed(1)}
                <span className="sr-only"> de 5</span>
              </span>
            ) : null}
            <span className="sep" aria-hidden="true" />
            <span>{formatPrice(active.price)}</span>
            <span className="sep" aria-hidden="true" />
            <span>Recojo en 10 min</span>
          </p>

          <div className="spotlight-actions">
            <button
              type="button"
              className={cn('spotlight-cta', added && 'is-added')}
              onClick={handleOrder}
            >
              {added ? '✓ Agregado' : ctaLabel}
            </button>
            {onExplore ? (
              <button type="button" className="spotlight-link" onClick={onExplore}>
                {exploreLabel}
                <svg
                  width="13"
                  height="13"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.6"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M5 12h13M13 6l6 6-6 6" />
                </svg>
              </button>
            ) : null}
          </div>

          <p className="spotlight-brand">{brandLine}</p>
        </div>
      </div>

      <div className="spotlight-strip">
        <button
          type="button"
          className="strip-arrow"
          onClick={() => go(indexRef.current - 1)}
          aria-label="Plato anterior"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </button>

        <div className="strip-track">
          {dishes.map((dish, i) => (
            <button
              key={dish.id}
              type="button"
              className="strip-card"
              data-dish={dish.id}
              aria-current={i === index ? 'true' : undefined}
              onClick={() => go(i)}
            >
              <span className="strip-thumb">
                <img src={dish.image} alt="" loading={i < 4 ? 'eager' : 'lazy'} decoding="async" />
              </span>
              <span className="strip-text">
                <span className="strip-name">{dish.name}</span>
                <span className="strip-price">{formatPrice(dish.price)}</span>
              </span>
            </button>
          ))}
        </div>

        <button
          type="button"
          className="strip-arrow"
          onClick={() => go(indexRef.current + 1)}
          aria-label="Plato siguiente"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </button>
      </div>
    </section>
  );
}

export default DishSpotlightCarousel;
