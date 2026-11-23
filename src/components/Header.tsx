import type { OpenStatus } from '@/hooks/useOpenStatus';

interface HeaderProps {
  status: OpenStatus;
  query: string;
  onQueryChange: (value: string) => void;
}

export function Header({ status, query, onQueryChange }: HeaderProps) {
  return (
    <header className="relative overflow-hidden">
      <div className="pointer-events-none absolute -top-24 right-0 h-64 w-64 rounded-full bg-gold/10 blur-3xl" />

      <div className="relative mx-auto max-w-5xl px-4 pt-5">
        <div className="flex items-center gap-3 sm:gap-4">
          <h1 className="sr-only">Alviña Primavera · Carta digital, Trujillo</h1>
          <img
            src="/assets/logo-alvina.png"
            alt="Alviña — Panadería Café"
            width={387}
            height={172}
            fetchPriority="high"
            className="h-11 w-auto shrink-0 sm:h-12"
          />
          <div className="min-w-0 flex-1">
            <p className="text-[10px] font-extrabold uppercase leading-[1.55] tracking-[0.15em] text-coffee/70 sm:text-[11px] sm:tracking-[0.18em]">
              Cafetería · Sanguches · Jugos
            </p>
            <p className="mt-0.5 text-[10px] leading-snug text-coffee/50 sm:text-[11px]">
              7:30 AM – 12:00 PM · 3:30 PM – 7:00 PM
            </p>
          </div>

          <div
            id="openBadge"
            className="inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full border border-espresso/10 bg-paper px-2.5 py-1.5 text-[10px] font-bold text-coffee/70 shadow-sm"
          >
            <span
              id="openDot"
              className={'h-1.5 w-1.5 rounded-full ' + (status.open ? 'animate-pulse bg-emerald-500' : 'bg-coffee/30')}
            />
            <span id="openText">{status.text}</span>
          </div>
        </div>

        <div className="relative mt-4">
          <svg
            className="pointer-events-none absolute left-4 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-coffee/40"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.9"
            strokeLinecap="round"
            aria-hidden="true"
          >
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.6-3.6" />
          </svg>
          <input
            id="search"
            type="search"
            inputMode="search"
            autoComplete="off"
            enterKeyHint="search"
            placeholder="Buscar sánguche, jugo, café…"
            aria-label="Buscar en la carta"
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            className="field w-full appearance-none rounded-2xl border border-espresso/10 bg-paper py-3.5 pl-11 pr-11 text-[15px] text-espresso shadow-card"
          />
          {query && (
            <button
              id="clearSearch"
              type="button"
              aria-label="Limpiar búsqueda"
              onClick={() => onQueryChange('')}
              className="absolute right-2.5 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-full text-coffee/50 transition hover:bg-cream-deep active:scale-95"
            >
              <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
                <path d="M6 6l12 12M18 6 6 18" />
              </svg>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
