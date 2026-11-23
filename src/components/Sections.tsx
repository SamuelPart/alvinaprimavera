const BENEFITS = [
  {
    icon: '🏪',
    title: 'Recojo en tienda',
    text: 'Pide desde tu celular y recoge sin esperar en el mostrador.',
  },
  {
    icon: '🛵',
    title: 'Delivery cercano',
    text: 'Coordinamos el reparto y el costo por WhatsApp según tu zona.',
  },
  {
    icon: '💛',
    title: 'Hecho al momento',
    text: 'Sánguches, jugos y café recién preparados cada día.',
  },
];

export function Benefits() {
  return (
    <section className="reveal mt-9">
      <div className="mb-3 flex items-center gap-3">
        <h2 className="font-display text-[17px] font-semibold text-espresso">Por qué pedir aquí</h2>
        <span className="hairline flex-1" />
      </div>
      <div className="grid gap-2.5 sm:grid-cols-3">
        {BENEFITS.map((it) => (
          <div key={it.title} className="rounded-2xl border border-espresso/10 bg-paper p-4 shadow-card">
            <div className="grid h-9 w-9 place-items-center rounded-full bg-cream-deep text-[15px]">{it.icon}</div>
            <h3 className="mt-3 text-[13px] font-extrabold text-espresso">{it.title}</h3>
            <p className="mt-1 text-[11.5px] leading-relaxed text-coffee/60">{it.text}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

export function Contact() {
  return (
    <section className="reveal grain relative mt-3 overflow-hidden rounded-[22px] border border-gold/20 bg-gradient-to-br from-[#3E2A18] via-espresso to-ink p-5 text-cream shadow-lift">
      <div className="relative z-[2]">
        <p className="text-[9.5px] font-extrabold uppercase tracking-[0.26em] text-gold/80">Atención directa</p>
        <h2 className="mt-2 font-display text-[22px] font-medium leading-tight text-cream">¿Dudas con tu pedido?</h2>
        <p className="mt-1.5 text-[12.5px] leading-relaxed text-cream/65">
          Escríbenos al <span className="font-bold text-gold-soft">+51 935 466 580</span> y te atendemos al toque.
        </p>
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <a
            href="https://wa.me/51935466580?text=Hola%20Alvi%C3%B1a%20Primavera%2C%20quisiera%20hacer%20una%20consulta."
            target="_blank"
            rel="noopener"
            className="inline-flex items-center gap-2 rounded-full bg-cream px-4 py-2.5 text-[12.5px] font-extrabold text-espresso shadow-soft transition active:scale-95 hover:bg-white"
          >
            <svg className="h-4 w-4 text-wa-500" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.75.46 3.46 1.32 4.96L2 22l5.25-1.38a9.9 9.9 0 0 0 4.79 1.22h.01c5.46 0 9.91-4.45 9.91-9.91 0-2.65-1.03-5.14-2.9-7.01A9.82 9.82 0 0 0 12.04 2Zm5.8 14.03c-.24.68-1.4 1.3-1.94 1.35-.54.05-1.03.24-3.47-.72-2.94-1.16-4.8-4.2-4.94-4.4-.14-.2-1.17-1.56-1.17-2.98 0-1.42.74-2.12 1.01-2.41.27-.29.58-.36.78-.36.2 0 .39 0 .56.01.18.01.42-.07.66.5.24.58.82 2 .89 2.14.07.15.12.32.02.51-.1.2-.63.7-.83.9-.2.2-.29.31-.15.55.14.24.62.99 1.33 1.62.91.8 1.67 1.06 1.91 1.18.24.12.38.1.52-.06.14-.16.6-.7.76-.94.16-.24.32-.2.54-.12.22.08 1.4.66 1.64.78.24.12.4.18.46.28.06.1.06.58-.18 1.26Z" />
            </svg>
            Consultar por WhatsApp
          </a>
          <span className="inline-flex items-center rounded-full border border-white/15 px-4 py-2.5 text-[11px] font-semibold text-cream/60">
            Yape · Plin · Efectivo
          </span>
        </div>
      </div>
    </section>
  );
}

export function Footer() {
  return (
    <footer className="reveal mt-9 text-center">
      <div className="mx-auto mb-4 h-px w-16 bg-espresso/15" />
      <p className="font-display text-[14px] font-semibold text-espresso">Alviña Primavera</p>
      <p className="mt-1 text-[10.5px] uppercase tracking-[0.18em] text-coffee/45">Trujillo · Perú</p>
      <p className="mt-2.5 text-[10.5px] text-coffee/45">Precios en Soles (S/) · Incluyen IGV</p>
      <p className="mt-1 text-[10.5px] text-coffee/45">
        Carta digital hecha con React, Vite y cariño 💛
      </p>
    </footer>
  );
}
