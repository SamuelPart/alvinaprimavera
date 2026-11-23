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
  const rootRef = React.useRef<HTMLElement | null>(null);
  const colRef = React.useRef<HTMLDivElement | null>(null);
  const floatRef = React.useRef<HTMLDivElement | null>(null);
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

  /* ── Coreografía de entrada del banner ──
     Se arma pausada (immediateRender deja el estado inicial listo) y se
     reproduce cuando la portada entra en pantalla. */
  React.useLayoutEffect(() => {
    const root = rootRef.current;
    if (!root || prefersReducedMotion()) return undefined;

    const q = gsap.utils.selector(root);
    const copyTargets = q(
      '.spotlight-eyebrow, .spotlight-title-1, .spotlight-title-2, .spotlight-desc, .spotlight-meta, .spotlight-actions > *, .spotlight-brand, .strip-card, .strip-arrow',
    );

    const tl = gsap.timeline({
      paused: true,
      defaults: { ease: 'power3.out' },
      onComplete: () => gsap.set(copyTargets, { clearProps: 'all' }),
    });

    tl.from(q('.spotlight-band'), {
      scaleX: 0,
      opacity: 0,
      transformOrigin: 'left center',
      duration: 1,
      ease: 'power4.out',
    })
      .from(q('.spotlight-plate-float'), { scale: 0.82, opacity: 0, rotate: -5, duration: 1.2 }, 0.08)
      .from(q('.spotlight-eyebrow'), { y: 14, opacity: 0, duration: 0.6 }, 0.05)
      .from(q('.spotlight-title-1'), { y: 28, opacity: 0, duration: 0.9 }, 0.16)
      .from(q('.spotlight-title-2'), { y: 28, opacity: 0, duration: 0.9 }, 0.28)
      .from(q('.spotlight-desc'), { y: 16, opacity: 0, duration: 0.6 }, 0.46)
      .from(q('.spotlight-meta'), { y: 12, opacity: 0, duration: 0.6 }, 0.54)
      .from(q('.spotlight-actions > *'), { y: 14, opacity: 0, duration: 0.55, stagger: 0.08 }, 0.62)
      .from(q('.spotlight-brand'), { y: 10, opacity: 0, duration: 0.5 }, 0.78)
      .from(q('.strip-card'), { y: 18, opacity: 0, duration: 0.55, stagger: 0.07 }, 0.5)
      .from(q('.strip-arrow'), { opacity: 0, duration: 0.45 }, 0.8);

    tl.progress(0);

    let io: IntersectionObserver | null = null;
    const start = () => {
      tl.play(0);
    };

    if (!('IntersectionObserver' in window)) {
      start();
    } else {
      io = new IntersectionObserver(
        (entries) => {
          if (entries.some((en) => en.isIntersecting)) {
            io?.disconnect();
            io = null;
            start();
          }
        },
        { threshold: 0.22 },
      );
      io.observe(root);
    }

    return () => {
      io?.disconnect();
      tl.kill();
    };
  }, []);

  /* ── Flotación sutil del plato + inclinación con el cursor ── */
  React.useEffect(() => {
    const el = floatRef.current;
    const root = rootRef.current;
    if (!el || !root || prefersReducedMotion()) return undefined;

    const float = gsap.to(el, {
      y: -10,
      duration: 3.6,
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut',
    });

    const finePointer =
      typeof window.matchMedia === 'function' && window.matchMedia('(pointer: fine)').matches;

    if (!finePointer) {
      return () => {
        float.kill();
      };
    }

    gsap.set(el, { transformPerspective: 900 });
    const rotY = gsap.quickTo(el, 'rotationY', { duration: 0.7, ease: 'power3.out' });
    const rotX = gsap.quickTo(el, 'rotationX', { duration: 0.7, ease: 'power3.out' });

    const onMove = (e: PointerEvent) => {
      const r = root.getBoundingClientRect();
      const x = (e.clientX - r.left) / Math.max(1, r.width) - 0.5;
      const y = (e.clientY - r.top) / Math.max(1, r.height) - 0.5;
      rotY(x * 7);
      rotX(-y * 7);
    };
    const onLeave = () => {
      rotY(0);
      rotX(0);
    };

    root.addEventListener('pointermove', onMove);
    root.addEventListener('pointerleave', onLeave);

    return () => {
      root.removeEventListener('pointermove', onMove);
      root.removeEventListener('pointerleave', onLeave);
      float.kill();
    };
  }, []);

  /* ── Parallax al hacer scroll (sin secuestrar el scroll) ── */
  React.useEffect(() => {
    const root = rootRef.current;
    const col = colRef.current;
    if (!root || !col || prefersReducedMotion()) return undefined;

    const toCol = gsap.quickTo(col, 'y', { duration: 0.45, ease: 'power2.out' });
    let raf = 0;

    const update = () => {
      raf = 0;
      const r = root.getBoundingClientRect();
      const p = Math.min(1, Math.max(0, -r.top / Math.max(1, r.height)));
      toCol(p * -44);
    };
    const onScroll = () => {
      if (!raf) raf = window.requestAnimationFrame(update);
    };

    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onScroll);
    update();

    return () => {
      window.removeEventListener('scroll', onScroll);
      window.removeEventListener('resize', onScroll);
      if (raf) window.cancelAnimationFrame(raf);
    };
  }, []);

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
      ref={rootRef}
      className={cn('spotlight', className)}
      aria-label="El favorito de la casa"
    >
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
        <div className="spotlight-plate-col" ref={colRef}>
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

          <div className="spotlight-plate-float" ref={floatRef}>
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
