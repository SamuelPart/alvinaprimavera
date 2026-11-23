import * as React from 'react';
import { WHATSAPP_NUMBER } from '@/data/products';

/* ─────────────────────────────────────────────────────────────
   Tortas — vitrina por encargo
   La foto es la real de la casa, retocada: se limpia el fondo y se
   deja la torta tal cual. Como todavía no hay lista de precios, el
   pedido se cierra por WhatsApp en vez de inventar montos.
   ───────────────────────────────────────────────────────────── */

const MENSAJE = [
  'Hola Alviña 👋',
  'Quiero encargar una *torta de chocolate*.',
  '¿Qué tamaños y precios tienen?',
].join('\n');

export function Cakes() {
  const href = `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(MENSAJE)}`;

  return (
    <section id="tortas" className="cakes" aria-labelledby="cakesTitle" data-reveal="section">
      <figure className="cakes-media">
        <img
          src="/assets/tortas/torta-chocolate.webp"
          alt="Torta de chocolate de Alviña con ganache y virutas, sobre base de vidrio"
          width={900}
          height={1125}
          loading="lazy"
          decoding="async"
        />
        <figcaption>
          <span className="cakes-chip">Torta de chocolate</span>
        </figcaption>
      </figure>

      <div className="cakes-body">
        <p className="cakes-eyebrow">Por encargo</p>
        <h2 id="cakesTitle" className="cakes-title">
          Tortas <em>hechas por pedido</em>
        </h2>
        <p className="cakes-text">
          Bizcocho húmedo de chocolate, ganache brillante, virutas y rosetas hechas a mano. Se hornean
          por pedido: coordinamos el tamaño y la hora de recojo.
        </p>

        <ul className="cakes-points">
          <li>Horneada el mismo día del recojo</li>
          <li>Recojo en tienda o delivery cercano</li>
          <li>Cumpleaños, oficina y fechas especiales</li>
        </ul>

        <a className="cakes-cta" href={href} target="_blank" rel="noopener noreferrer">
          <svg viewBox="0 0 24 24" aria-hidden="true" width="18" height="18" fill="currentColor">
            <path d="M17.5 14.4c-.3-.2-1.8-.9-2.1-1s-.5-.1-.7.2-.8 1-.9 1.2-.3.2-.6 0-1.2-.5-2.3-1.4c-.9-.8-1.4-1.7-1.6-2s0-.4.1-.6l.5-.6c.2-.2.2-.3.3-.5s0-.4 0-.5-.7-1.6-.9-2.2c-.2-.5-.4-.5-.6-.5h-.5c-.2 0-.5.1-.7.3-.3.3-1 1-1 2.3 0 1.4 1 2.7 1.1 2.9s2 3.1 4.9 4.3c.7.3 1.2.5 1.7.6.7.2 1.3.2 1.8.1.5-.1 1.8-.7 2-1.5.3-.7.3-1.4.2-1.5-.1-.2-.3-.2-.6-.4z" />
            <path d="M12 2a10 10 0 0 0-8.6 15.1L2 22l5-1.3A10 10 0 1 0 12 2zm0 18.2c-1.5 0-3-.4-4.3-1.2l-.3-.2-3 .8.8-2.9-.2-.3A8.2 8.2 0 1 1 12 20.2z" />
          </svg>
          Consultar por WhatsApp
        </a>
      </div>
    </section>
  );
}
