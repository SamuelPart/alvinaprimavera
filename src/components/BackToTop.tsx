import * as React from 'react';
import { REDUCED } from '@/data/products';

/**
 * Botón discreto en la esquina inferior derecha para volver al inicio.
 * Aparece cuando el usuario ya bajó lo suficiente y respeta reduced-motion.
 */
export function BackToTop() {
  const [visible, setVisible] = React.useState(false);
  const frame = React.useRef(0);

  React.useEffect(() => {
    const onScroll = () => {
      if (frame.current) return;
      frame.current = window.requestAnimationFrame(() => {
        frame.current = 0;
        setVisible(window.scrollY > 620);
      });
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => {
      window.removeEventListener('scroll', onScroll);
      if (frame.current) window.cancelAnimationFrame(frame.current);
    };
  }, []);

  const toTop = () => {
    window.scrollTo({ top: 0, behavior: REDUCED ? 'auto' : 'smooth' });
  };

  return (
    <button
      id="toTop"
      type="button"
      className={'to-top' + (visible ? ' is-visible' : '')}
      aria-label="Volver arriba"
      title="Volver arriba"
      tabIndex={visible ? 0 : -1}
      aria-hidden={!visible}
      onClick={toTop}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M12 19V6M6 12l6-6 6 6" />
      </svg>
    </button>
  );
}
