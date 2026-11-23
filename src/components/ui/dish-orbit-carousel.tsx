'use client';

import * as React from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { cn } from '@/lib/utils';

/* ─────────────────────────────────────────────────────────────
   dish-orbit-carousel — shadcn/ui
   Portada con los platos de la casa orbitando alrededor del
   plato activo. El scroll (GSAP ScrollTrigger, con pin) recorre
   la órbita; también funciona con clic, swipe y teclado.
   ───────────────────────────────────────────────────────────── */

export interface Dish {
  id: string;
  name: string;
  subtitle?: string;
  price: number;
  image: string;
  /** Color de acento del plato (borde/halo). */
  accent?: string;
  rating?: number;
  tag?: string;
}

export interface DishOrbitCarouselProps {
  dishes: Dish[];
  eyebrow?: string;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  className?: string;
  /** Etiqueta del botón principal. */
  ctaLabel?: string;
  /** Segunda acción (p. ej. bajar a la carta). */
  exploreLabel?: string;
  formatPrice?: (value: number) => string;
  onOrder?: (dish: Dish, index: number) => void;
  onExplore?: () => void;
  onChange?: (dish: Dish, index: number) => void;
}

const prefersReducedMotion = () =>
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

export function DishOrbitCarousel({
  dishes,
  eyebrow = 'Portada de la casa',
  title,
  subtitle,
  className,
  ctaLabel = 'Agregar al pedido',
  exploreLabel = 'Ver la carta completa',
  formatPrice = (v) => 'S/ ' + v.toFixed(2),
  onOrder,
  onExplore,
  onChange,
}: DishOrbitCarouselProps) {
  const total = dishes.length;
  const [index, setIndex] = React.useState(0);
  const [added, setAdded] = React.useState(false);

  const indexRef = React.useRef(0);
  const sectionRef = React.useRef<HTMLElement | null>(null);
  const plateRef = React.useRef<HTMLDivElement | null>(null);
  const infoRef = React.useRef<HTMLDivElement | null>(null);
  const dragRef = React.useRef<number | null>(null);
  const addedTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  const step = total > 0 ? 360 / total : 360;
  const active = dishes[index];

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

  /* ── ScrollTrigger: pin + recorrido de la órbita ── */
  React.useEffect(() => {
    const section = sectionRef.current;
    if (!section || total < 2 || prefersReducedMotion()) return undefined;

    gsap.registerPlugin(ScrollTrigger);
    ScrollTrigger.config({ ignoreMobileResize: true });

    const trigger = ScrollTrigger.create({
      trigger: section,
      start: 'top top',
      end: () => '+=' + Math.round(window.innerHeight * 1.15),
      pin: true,
      pinSpacing: true,
      anticipatePin: 1,
      invalidateOnRefresh: true,
      onUpdate: (self) => {
        const next = Math.min(total - 1, Math.max(0, Math.round(self.progress * (total - 1))));
        if (next !== indexRef.current) {
          indexRef.current = next;
          setIndex(next);
        }
      },
    });

    const refresh = () => ScrollTrigger.refresh();
    window.addEventListener('load', refresh);
    if (typeof document.fonts?.ready?.then === 'function') {
      document.fonts.ready.then(refresh).catch(() => undefined);
    }

    return () => {
      window.removeEventListener('load', refresh);
      trigger.kill();
    };
  }, [total]);

  /* ── Aviso al padre cuando cambia el plato activo ── */
  React.useEffect(() => {
    if (active) onChange?.(active, index);
    // onChange se mantiene fuera de las deps para no re-disparar el efecto.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index]);

  /* ── Transición del plato y de la ficha (GSAP) ── */
  React.useEffect(() => {
    const plate = plateRef.current;
    const info = infoRef.current;
    if (!plate || !info || prefersReducedMotion()) return;

    const tl = gsap.timeline({ defaults: { ease: 'power2.out' } });
    tl.fromTo(
      plate,
      { scale: 0.88, rotate: -7 },
      { scale: 1, rotate: 0, duration: 0.7, ease: 'back.out(1.5)' },
    ).fromTo(
      Array.from(info.children),
      { y: 12, opacity: 0 },
      { y: 0, opacity: 1, duration: 0.42, stagger: 0.045 },
      0.06,
    );

    return () => {
      tl.kill();
    };
  }, [index]);

  React.useEffect(() => () => {
    if (addedTimer.current) clearTimeout(addedTimer.current);
  }, []);

  const handleOrder = () => {
    if (!active) return;
    onOrder?.(active, index);
    setAdded(true);
    if (addedTimer.current) clearTimeout(addedTimer.current);
    addedTimer.current = setTimeout(() => setAdded(false), 1600);
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
      ref={sectionRef}
      id="portada"
      className={cn('orbit-section', className)}
      aria-label="Portada de la casa"
    >
      <div className="orbit-stage">
        <header className="orbit-head">
          {eyebrow ? <p className="orbit-eyebrow">{eyebrow}</p> : null}
          {title ? <h2 className="orbit-title">{title}</h2> : null}
          {subtitle ? <p className="orbit-sub">{subtitle}</p> : null}
        </header>

        <div
          className="orbit"
          role="group"
          aria-roledescription="carrusel"
          aria-label="Platos de la casa"
          tabIndex={0}
          onKeyDown={onKeyDown}
          onPointerDown={(e) => {
            dragRef.current = e.clientX;
          }}
          onPointerUp={(e) => {
            if (dragRef.current === null) return;
            const dx = e.clientX - dragRef.current;
            dragRef.current = null;
            if (Math.abs(dx) > 34) go(indexRef.current + (dx < 0 ? 1 : -1));
          }}
          onPointerCancel={() => {
            dragRef.current = null;
          }}
        >
          {dishes.map((dish, i) => {
            const angle = 180 + (i - index) * step;
            const isActive = i === index;
            return (
              <button
                key={dish.id}
                type="button"
                className={cn('orbit-sat', isActive && 'is-out')}
                style={
                  {
                    '--a': angle + 'deg',
                    borderColor: dish.accent
                      ? `color-mix(in oklab, ${dish.accent} 45%, #fffdf9)`
                      : undefined,
                  } as React.CSSProperties
                }
                aria-label={'Ver ' + dish.name}
                aria-current={isActive ? 'true' : undefined}
                tabIndex={isActive ? -1 : 0}
                onClick={() => go(i)}
              >
                <img
                  src={dish.image}
                  alt=""
                  loading={i < 4 ? 'eager' : 'lazy'}
                  decoding="async"
                  draggable={false}
                />
              </button>
            );
          })}

          <div ref={plateRef} className="orbit-plate">
            <img
              className="orbit-plate-img"
              src={active.image}
              alt={active.name}
              draggable={false}
              fetchPriority="high"
            />
            {active.tag ? <span className="orbit-plate-tag">{active.tag}</span> : null}
          </div>
        </div>

        <div className="orbit-info" ref={infoRef} aria-live="polite">
          <h3 className="orbit-info-name">{active.name}</h3>
          {active.subtitle ? <p className="orbit-info-sub">{active.subtitle}</p> : null}

          <p className="orbit-meta">
            {typeof active.rating === 'number' ? (
              <span>
                <span className="orbit-star" aria-hidden="true">
                  ★
                </span>
                {active.rating.toFixed(1)}
                <span className="sr-only"> de 5</span>
              </span>
            ) : null}
            <span aria-hidden="true">·</span>
            <span>Hecho al momento</span>
            <span aria-hidden="true">·</span>
            <span>Recojo en 10 min</span>
          </p>

          <div className="orbit-actions">
            <p className="orbit-price">
              {formatPrice(active.price)}
              <small>incl. IGV</small>
            </p>
            <button
              type="button"
              className={cn('orbit-cta', added && 'is-added')}
              onClick={handleOrder}
            >
              {added ? '✓ Agregado' : ctaLabel}
              {!added ? (
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.6"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M12 5v14M5 12h14" />
                </svg>
              ) : null}
            </button>
            {exploreLabel && onExplore ? (
              <button type="button" className="orbit-explore" onClick={onExplore}>
                {exploreLabel}
              </button>
            ) : null}
          </div>

          <div className="orbit-nav">
            <button
              type="button"
              className="orbit-arrow"
              onClick={() => go(indexRef.current - 1)}
              aria-label="Plato anterior"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
                <path d="M15 6l-6 6 6 6" />
              </svg>
            </button>

            <div className="orbit-dots">
              {dishes.map((dish, i) => (
                <button
                  key={dish.id}
                  type="button"
                  className="orbit-dot"
                  aria-current={i === index ? 'true' : undefined}
                  aria-label={'Ir a ' + dish.name}
                  onClick={() => go(i)}
                />
              ))}
            </div>

            <button
              type="button"
              className="orbit-arrow"
              onClick={() => go(indexRef.current + 1)}
              aria-label="Plato siguiente"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 6l6 6-6 6" />
              </svg>
            </button>
          </div>

          <p className="orbit-count">
            {String(index + 1).padStart(2, '0')} / {String(total).padStart(2, '0')}
          </p>
          <p className="orbit-hint">Desliza sobre la órbita</p>
        </div>
      </div>
    </section>
  );
}

export default DishOrbitCarousel;
