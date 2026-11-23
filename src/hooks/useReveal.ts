import { useEffect } from 'react';
import { REDUCED } from '@/data/products';

/** Aparición suave de las secciones con `.reveal`. */
export function useReveal() {
  useEffect(() => {
    const els = Array.from(document.querySelectorAll<HTMLElement>('.reveal'));
    if (!('IntersectionObserver' in window) || REDUCED) {
      els.forEach((el) => el.classList.add('is-in'));
      return undefined;
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((en) => {
          if (en.isIntersecting) {
            en.target.classList.add('is-in');
            io.unobserve(en.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    );
    els.forEach((el) => io.observe(el));
    return () => io.disconnect();
  }, []);
}
