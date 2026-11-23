import { useEffect } from 'react';
import { REDUCED } from '@/data/products';

/**
 * Scroll reveal con IntersectionObserver.
 *
 * Los elementos con `[data-reveal]` arrancan invisibles 22 px más abajo y
 * aparecen con fade-up cuando entran al viewport (funciona bajando y subiendo).
 * Se revelan una sola vez para que al volver a pasar no parpadeen.
 *
 * @param deps vuelve a escanear el DOM cuando cambia el contenido (filtros, búsqueda…).
 */
export function useScrollReveal(deps: unknown[] = []) {
  useEffect(() => {
    const els = Array.from(
      document.querySelectorAll<HTMLElement>('[data-reveal]:not(.is-revealed)'),
    );
    if (!els.length) return undefined;

    if (!('IntersectionObserver' in window) || REDUCED) {
      els.forEach((el) => el.classList.add('is-revealed'));
      return undefined;
    }

    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((en) => {
          if (en.isIntersecting) {
            en.target.classList.add('is-revealed');
            io.unobserve(en.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -6% 0px' },
    );
    els.forEach((el) => io.observe(el));
    return () => io.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}
