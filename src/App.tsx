import * as React from 'react';
import { DishOrbitCarousel, type Dish } from '@/components/ui/dish-orbit-carousel';
import { FloatBar } from '@/components/FloatBar';
import { Header } from '@/components/Header';
import { OrderSheet } from '@/components/OrderSheet';
import { ProductCard } from '@/components/ProductCard';
import { Benefits, Contact, Footer } from '@/components/Sections';
import { Toast, type ToastMessage } from '@/components/Toast';
import { CATEGORIES, PRODUCTS, byId, money, norm, type CategoryId } from '@/data/products';
import { useCart } from '@/hooks/useCart';
import { useOpenStatus } from '@/hooks/useOpenStatus';
import { useReveal } from '@/hooks/useReveal';

export default function App() {
  const { cart, inc: addQty, dec: removeOne, remove, clear, totals } = useCart();
  const status = useOpenStatus();
  useReveal();

  const [cat, setCat] = React.useState<CategoryId>('todos');
  const [query, setQuery] = React.useState('');
  const [sheetOpen, setSheetOpen] = React.useState(false);
  const [toast, setToast] = React.useState<ToastMessage | null>(null);

  const toastTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null);
  const lastFocused = React.useRef<HTMLElement | null>(null);

  const notify = React.useCallback((msg: string) => {
    setToast({ msg, id: Date.now() });
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 2200);
  }, []);

  React.useEffect(
    () => () => {
      if (toastTimer.current) clearTimeout(toastTimer.current);
    },
    [],
  );

  const inc = React.useCallback(
    (id: string) => {
      addQty(id);
      const p = byId(id);
      if (p) notify('+1 ' + p.name + ' · ' + money(p.price));
    },
    [addQty, notify],
  );

  const dec = React.useCallback((id: string) => removeOne(id), [removeOne]);

  const handleRemove = React.useCallback(
    (id: string) => {
      remove(id);
      const p = byId(id);
      if (p) notify('Quitaste ' + p.name);
    },
    [remove, notify],
  );

  const handleClear = React.useCallback(() => {
    clear();
    notify('Pedido vaciado');
  }, [clear, notify]);

  const visible = React.useMemo(() => {
    const q = norm(query.trim());
    return PRODUCTS.filter((p) => {
      if (cat !== 'todos' && p.cat !== cat) return false;
      if (!q) return true;
      return norm(p.name).includes(q) || norm(p.desc).includes(q);
    });
  }, [cat, query]);

  const dishes = React.useMemo<Dish[]>(
    () =>
      PRODUCTS.map((p) => ({
        id: p.id,
        name: p.name,
        subtitle: p.desc,
        price: p.price,
        image: p.img,
        accent: p.accent,
        rating: p.rating,
        tag: p.tag || undefined,
      })),
    [],
  );

  const activeCat = CATEGORIES.find((c) => c.id === cat) ?? CATEGORIES[0];
  const resultLabel =
    visible.length === 1
      ? '1 producto encontrado'
      : visible.length + ' productos ' + (query ? 'encontrados' : 'disponibles');

  const scrollToCarta = React.useCallback(() => {
    document.getElementById('carta')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, []);

  const openSheet = () => {
    if (totals.qty === 0) {
      notify('Agrega algo a tu pedido primero');
      return;
    }
    lastFocused.current = document.activeElement as HTMLElement | null;
    setSheetOpen(true);
  };

  const closeSheet = React.useCallback(() => {
    setSheetOpen(false);
    lastFocused.current?.focus({ preventScroll: true });
  }, []);

  return (
    <>
      {/* Cinta superior */}
      <div className="topbar grain">
        <div className="mx-auto flex max-w-5xl items-center justify-center gap-2 overflow-hidden px-4 py-1.5 text-[9px] font-semibold uppercase tracking-[0.18em] sm:text-[10px] sm:tracking-[0.22em]">
          <span className="text-gold/70" aria-hidden="true">
            ✦
          </span>
          <span>Pedidos por WhatsApp · +51 935 466 580</span>
          <span className="text-gold/70" aria-hidden="true">
            ✦
          </span>
        </div>
      </div>

      <Header status={status} query={query} onQueryChange={setQuery} />

      {/* Portada: carrusel de platos */}
      <DishOrbitCarousel
        dishes={dishes}
        eyebrow="Portada de la casa"
        title={
          <>
            Los imperdibles
            <br />
            <em>de Alviña</em>
          </>
        }
        subtitle="Sánguches, jugos y café pasado, hechos al momento. Gira la órbita y arma tu pedido."
        onOrder={(dish) => inc(dish.id)}
        exploreLabel="Ver la carta completa"
        onExplore={scrollToCarta}
      />

      {/* Carta */}
      <nav id="carta" className="sticky top-0 z-30 border-y border-espresso/10 bg-cream/95 backdrop-blur-md">
        <div className="mx-auto max-w-5xl">
          <div id="tabs" role="tablist" aria-label="Categorías de la carta" className="no-scrollbar flex gap-2 overflow-x-auto px-4 py-3">
            {CATEGORIES.map((c) => (
              <button
                key={c.id}
                type="button"
                role="tab"
                data-cat={c.id}
                aria-selected={cat === c.id}
                onClick={() => setCat(c.id)}
                className="tab inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border border-espresso/10 bg-paper px-3.5 py-2 text-[12.5px] font-bold text-coffee/75"
              >
                <span aria-hidden="true" className="text-[12px]">
                  {c.emoji}
                </span>
                {c.label}
              </button>
            ))}
          </div>
        </div>
      </nav>

      <main className="mx-auto max-w-5xl px-4 pb-safe pt-5">
        <div className="reveal mb-3.5 flex items-end justify-between gap-3">
          <div>
            <h2 id="sectionTitle" className="font-display text-[22px] font-semibold leading-none text-espresso">
              {query ? 'Resultados' : activeCat.id === 'todos' ? 'Toda la carta' : activeCat.label}
            </h2>
            <p id="resultInfo" className="mt-1.5 text-[11.5px] font-medium text-coffee/55">
              {resultLabel}
            </p>
          </div>
          <div className="hidden items-center gap-2 rounded-full border border-espresso/10 bg-paper px-3 py-1.5 text-[10.5px] font-semibold text-coffee/60 sm:flex">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" /> Precios en Soles
          </div>
        </div>

        <section
          id="grid"
          aria-live="polite"
          className={'grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3' + (visible.length === 0 ? ' hidden' : '')}
        >
          {visible.map((p, i) => (
            <ProductCard key={p.id} product={p} qty={cart[p.id] || 0} onInc={inc} onDec={dec} index={i} />
          ))}
        </section>

        {visible.length === 0 && (
          <section id="empty" className="py-16 text-center">
            <div className="mx-auto mb-4 grid h-16 w-16 place-items-center rounded-full border border-espresso/10 bg-paper text-2xl shadow-card">
              🔍
            </div>
            <h3 className="font-display text-xl font-semibold text-espresso">No encontramos ese producto</h3>
            <p className="mx-auto mt-1.5 max-w-[26ch] text-[12.5px] leading-relaxed text-coffee/60">
              Prueba con otra palabra o vuelve a ver toda la carta.
            </p>
            <button
              id="resetFilters"
              type="button"
              onClick={() => {
                setCat('todos');
                setQuery('');
              }}
              className="mt-5 inline-flex items-center gap-2 rounded-full bg-espresso px-5 py-3 text-[12.5px] font-extrabold tracking-wide text-cream shadow-card transition active:scale-95"
            >
              Ver toda la carta
            </button>
          </section>
        )}

        <Benefits />
        <Contact />
        <Footer />
      </main>

      <FloatBar qty={totals.qty} total={totals.total} onOpen={openSheet} />

      <OrderSheet
        open={sheetOpen}
        onClose={closeSheet}
        onBackToMenu={() => {
          closeSheet();
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }}
        totals={totals}
        onInc={inc}
        onDec={dec}
        onRemove={handleRemove}
        onClear={handleClear}
        notify={notify}
      />

      <Toast toast={toast} />
    </>
  );
}
