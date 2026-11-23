import * as React from 'react';
import { Thumb } from '@/components/Thumb';
import { useAnimatedMoney } from '@/hooks/useAnimatedMoney';
import type { Totals } from '@/hooks/useCart';
import {
  CLIENT_KEY,
  PAY_LABEL,
  REDUCED,
  TYPE_LABEL,
  WHATSAPP_NUMBER,
  money,
} from '@/data/products';

type OrderType = 'recojo' | 'delivery';
type Payment = 'yape' | 'efectivo';

interface Client {
  name?: string;
  address?: string;
  payment?: string;
  orderType?: string;
}

interface OrderSheetProps {
  open: boolean;
  onClose: () => void;
  onBackToMenu: () => void;
  totals: Totals;
  onInc: (id: string) => void;
  onDec: (id: string) => void;
  onRemove: (id: string) => void;
  onClear: () => void;
  notify: (msg: string) => void;
}

function readClient(): Client {
  try {
    return JSON.parse(localStorage.getItem(CLIENT_KEY) || '{}') as Client;
  } catch {
    return {};
  }
}

const DELIVERY_OPTIONS: Array<{ id: OrderType; icon: string; title: string; sub: string }> = [
  { id: 'recojo', icon: '🏪', title: 'Recojo en tienda', sub: 'Listo en ~10 min' },
  { id: 'delivery', icon: '🛵', title: 'Delivery cercano', sub: 'Costo por coordinar' },
];

const PAY_OPTIONS: Array<{ id: Payment; icon: string; title: string; sub: string }> = [
  { id: 'yape', icon: '📱', title: 'Yape / Plin', sub: 'Transferencia al toque' },
  { id: 'efectivo', icon: '💵', title: 'Efectivo', sub: 'Pago contra entrega' },
];

function CheckIcon() {
  return (
    <svg
      className="h-2.5 w-2.5 text-cream"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="m5 13 4 4L19 7" />
    </svg>
  );
}

export function OrderSheet({
  open,
  onClose,
  onBackToMenu,
  totals,
  onInc,
  onDec,
  onRemove,
  onClear,
  notify,
}: OrderSheetProps) {
  const client = React.useMemo(readClient, []);
  const [name, setName] = React.useState(client.name || '');
  const [address, setAddress] = React.useState(client.address || '');
  const [note, setNote] = React.useState('');
  const [orderType, setOrderType] = React.useState<OrderType>(
    client.orderType === 'delivery' ? 'delivery' : 'recojo',
  );
  const [payment, setPayment] = React.useState<Payment>(
    client.payment === 'efectivo' ? 'efectivo' : 'yape',
  );
  const [errors, setErrors] = React.useState<{ name?: boolean; address?: boolean }>({});
  const [sent, setSent] = React.useState(false);

  const nameRef = React.useRef<HTMLInputElement | null>(null);
  const addressRef = React.useRef<HTMLInputElement | null>(null);
  const titleRef = React.useRef<HTMLHeadingElement | null>(null);
  const sentTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  const total = useAnimatedMoney(totals.total);
  const hasItems = totals.list.length > 0;

  /* Bloqueo de scroll + Escape + foco al abrir */
  React.useEffect(() => {
    if (!open) return undefined;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    const t = setTimeout(() => titleRef.current?.focus({ preventScroll: true }), 320);
    return () => {
      document.body.style.overflow = prev;
      document.removeEventListener('keydown', onKey);
      clearTimeout(t);
    };
  }, [open, onClose]);

  React.useEffect(
    () => () => {
      if (sentTimer.current) clearTimeout(sentTimer.current);
    },
    [],
  );

  function send() {
    const nextErrors = {
      name: name.trim().length < 2,
      address: orderType === 'delivery' && address.trim().length < 4,
    };
    setErrors(nextErrors);

    if (nextErrors.name) {
      nameRef.current?.focus();
      nameRef.current?.scrollIntoView({ behavior: REDUCED ? 'auto' : 'smooth', block: 'center' });
      return;
    }
    if (nextErrors.address) {
      addressRef.current?.focus();
      addressRef.current?.scrollIntoView({ behavior: REDUCED ? 'auto' : 'smooth', block: 'center' });
      return;
    }
    if (!hasItems) {
      notify('Tu pedido está vacío');
      return;
    }

    const stamp = new Intl.DateTimeFormat('es-PE', {
      timeZone: 'America/Lima',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    }).format(new Date());

    const lines = [
      '*NUEVO PEDIDO — ALVIÑA PRIMAVERA* ☕🥪',
      '────────────────────',
      '👤 *Nombre:* ' + name.trim(),
      '🛍️ *Tipo:* ' + TYPE_LABEL[orderType],
      orderType === 'delivery' ? '📍 *Dirección:* ' + address.trim() : null,
      '💳 *Pago:* ' + PAY_LABEL[payment],
      note.trim() ? '📝 *Nota:* ' + note.trim() : null,
      '────────────────────',
      '*DETALLE DEL PEDIDO*',
      ...totals.list.map((p) => '• ' + p.qty + ' × ' + p.name + ' — ' + money(p.line)),
      '────────────────────',
      '*TOTAL A PAGAR: ' + money(totals.total) + '*',
      '_' +
        totals.qty +
        ' ' +
        (totals.qty === 1 ? 'producto' : 'productos') +
        ' · ' +
        stamp +
        '_',
      '',
      '¡Gracias! Pedido enviado desde la carta digital 💛',
    ].filter(Boolean) as string[];

    const url = 'https://wa.me/' + WHATSAPP_NUMBER + '?text=' + encodeURIComponent(lines.join('\n'));

    try {
      localStorage.setItem(
        CLIENT_KEY,
        JSON.stringify({ name: name.trim(), address: address.trim(), payment, orderType }),
      );
    } catch {
      /* modo privado */
    }

    window.open(url, '_blank', 'noopener');
    notify('Abriendo WhatsApp con tu pedido');
    setSent(true);
    if (sentTimer.current) clearTimeout(sentTimer.current);
    sentTimer.current = setTimeout(() => setSent(false), 2600);
  }

  const field = (hasError?: boolean) =>
    'field w-full rounded-xl border border-espresso/10 bg-paper px-4 text-[15px] text-espresso ' +
    (hasError ? 'is-error' : '');

  return (
    <div
      id="overlay"
      className={
        'fixed inset-0 z-50 flex items-end justify-center bg-ink/50 backdrop-blur-[3px] md:items-center' +
        (open ? ' is-open' : '')
      }
      role="dialog"
      aria-modal="true"
      aria-labelledby="sheetTitle"
      aria-hidden={!open}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        id="sheet"
        className="flex w-full flex-col overflow-hidden rounded-t-[26px] bg-cream shadow-sheet md:max-w-lg md:rounded-[26px]"
      >
        <div className="shrink-0 border-b border-espresso/10 bg-paper px-5 pb-3.5 pt-3">
          <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-espresso/15 md:hidden" />
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="text-[9px] font-extrabold uppercase tracking-[0.26em] text-gold-deep">
                Alviña Primavera
              </p>
              <h2
                id="sheetTitle"
                ref={titleRef}
                tabIndex={-1}
                className="mt-1 font-display text-[21px] font-semibold leading-none text-espresso outline-none"
              >
                Tu pedido
              </h2>
            </div>
            <div className="flex items-center gap-1.5">
              {hasItems && (
                <button
                  id="clearCart"
                  type="button"
                  onClick={onClear}
                  className="rounded-full px-2.5 py-1.5 text-[10.5px] font-bold uppercase tracking-wide text-coffee/50 transition hover:bg-cream-deep hover:text-coffee active:scale-95"
                >
                  Vaciar
                </button>
              )}
              <button
                id="closeSheet"
                type="button"
                onClick={onClose}
                aria-label="Cerrar"
                className="grid h-9 w-9 place-items-center rounded-full border border-espresso/10 bg-cream text-coffee transition hover:bg-cream-deep active:scale-95"
              >
                <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
                  <path d="M6 6l12 12M18 6 6 18" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto overscroll-contain px-5 py-4">
          {hasItems ? (
            <div id="cartList">
              {totals.list.map((p) => (
                <div key={p.id} className="flex items-center gap-3 border-b border-dashed border-espresso/10 py-3 last:border-0">
                  <Thumb product={p} emojiClassName="text-[18px]" className="h-12 w-12 shrink-0 rounded-[10px]" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[12.5px] font-extrabold leading-tight text-espresso">{p.name}</p>
                    <p className="mt-0.5 text-[10.5px] text-coffee/45">{money(p.price)} c/u</p>
                  </div>
                  <div className="flex items-center gap-1 rounded-full border border-espresso/10 bg-cream/70 p-[2px]">
                    <button
                      type="button"
                      data-cart="dec"
                      data-id={p.id}
                      onClick={() => onDec(p.id)}
                      aria-label={'Quitar uno de ' + p.name}
                      className="step-btn grid h-7 w-7 place-items-center rounded-full text-[15px] font-bold leading-none text-coffee/70 active:bg-cream-deep"
                    >
                      −
                    </button>
                    <span className="w-4 text-center text-[12px] font-extrabold tabular-nums text-espresso">{p.qty}</span>
                    <button
                      type="button"
                      data-cart="inc"
                      data-id={p.id}
                      onClick={() => onInc(p.id)}
                      aria-label={'Agregar uno de ' + p.name}
                      className="step-btn grid h-7 w-7 place-items-center rounded-full bg-espresso text-[15px] font-bold leading-none text-cream active:bg-coffee"
                    >
                      +
                    </button>
                  </div>
                  <div className="w-[58px] shrink-0 text-right">
                    <p className="text-[12.5px] font-extrabold tabular-nums text-espresso">{money(p.line)}</p>
                    <button
                      type="button"
                      data-cart="del"
                      data-id={p.id}
                      onClick={() => onRemove(p.id)}
                      className="text-[10px] font-semibold text-coffee/40 transition hover:text-red-600"
                    >
                      Quitar
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div id="cartEmpty" className="py-12 text-center">
              <div className="mx-auto mb-4 grid h-16 w-16 place-items-center rounded-full border border-espresso/10 bg-paper text-2xl shadow-card">
                🧺
              </div>
              <h3 className="font-display text-lg font-semibold text-espresso">Tu pedido está vacío</h3>
              <p className="mt-1 text-[12.5px] text-coffee/60">Agrega algo rico de la carta para empezar.</p>
              <button
                id="backToMenu"
                type="button"
                onClick={onBackToMenu}
                className="mt-5 rounded-full bg-espresso px-5 py-3 text-[12.5px] font-extrabold tracking-wide text-cream transition active:scale-95"
              >
                Volver a la carta
              </button>
            </div>
          )}

          <div id="orderForm" className={'mt-5 space-y-5' + (hasItems ? '' : ' hidden')}>
            <div>
              <label htmlFor="fName" className="mb-1.5 block text-[10px] font-extrabold uppercase tracking-[0.2em] text-coffee/60">
                Tu nombre <span className="text-red-600">*</span>
              </label>
              <input
                id="fName"
                ref={nameRef}
                type="text"
                autoComplete="name"
                placeholder="Ej. María Fernanda"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  setErrors((s) => ({ ...s, name: false }));
                }}
                className={field(errors.name) + ' py-3.5'}
              />
              {errors.name && (
                <p className="err mt-1.5 text-[11px] font-semibold text-red-600">Escribe tu nombre para el pedido.</p>
              )}
            </div>

            <div>
              <p className="mb-1.5 block text-[10px] font-extrabold uppercase tracking-[0.2em] text-coffee/60">
                ¿Cómo lo quieres?
              </p>
              <div className="grid grid-cols-2 gap-2.5" role="group" aria-label="Tipo de entrega">
                {DELIVERY_OPTIONS.map((o) => (
                  <button
                    key={o.id}
                    type="button"
                    onClick={() => {
                      setOrderType(o.id);
                      setErrors((s) => ({ ...s, address: false }));
                    }}
                    className="opt relative rounded-xl border border-espresso/10 bg-paper p-3 text-left"
                    aria-pressed={orderType === o.id}
                    data-type={o.id}
                  >
                    <span className="opt-check absolute right-2 top-2 grid h-4 w-4 place-items-center rounded-full bg-espresso">
                      <CheckIcon />
                    </span>
                    <span className="block text-base">{o.icon}</span>
                    <span className="mt-1 block text-[12.5px] font-extrabold leading-tight text-espresso">{o.title}</span>
                    <span className="mt-0.5 block text-[10px] text-coffee/50">{o.sub}</span>
                  </button>
                ))}
              </div>
            </div>

            <p
              id="pickupNote"
              className={
                'flex items-start gap-2 rounded-xl border border-espresso/10 bg-cream-deep/60 px-3.5 py-2.5 text-[11px] leading-snug text-coffee/60' +
                (orderType === 'delivery' ? ' hidden' : '')
              }
            >
              <span aria-hidden="true">🏪</span>
              <span>Te esperamos en el local. Si envías a alguien, avísanos por WhatsApp.</span>
            </p>

            <div id="addressWrap" className={orderType === 'delivery' ? '' : 'hidden'}>
              <label htmlFor="fAddress" className="mb-1.5 block text-[10px] font-extrabold uppercase tracking-[0.2em] text-coffee/60">
                Dirección de entrega <span className="text-red-600">*</span>
              </label>
              <input
                id="fAddress"
                ref={addressRef}
                type="text"
                autoComplete="street-address"
                placeholder="Calle, número y referencia"
                value={address}
                onChange={(e) => {
                  setAddress(e.target.value);
                  setErrors((s) => ({ ...s, address: false }));
                }}
                className={field(errors.address) + ' py-3.5'}
              />
              {errors.address && (
                <p className="err mt-1.5 text-[11px] font-semibold text-red-600">Indica tu dirección para el delivery.</p>
              )}
              <p className="mt-1.5 text-[10.5px] text-coffee/45">
                Zona cercana al local. El costo de envío se confirma por WhatsApp.
              </p>
            </div>

            <div>
              <p className="mb-1.5 block text-[10px] font-extrabold uppercase tracking-[0.2em] text-coffee/60">
                Método de pago <span className="text-red-600">*</span>
              </p>
              <div className="grid grid-cols-2 gap-2.5" role="group" aria-label="Método de pago">
                {PAY_OPTIONS.map((o) => (
                  <button
                    key={o.id}
                    type="button"
                    onClick={() => setPayment(o.id)}
                    className="opt relative rounded-xl border border-espresso/10 bg-paper p-3 text-left"
                    aria-pressed={payment === o.id}
                    data-pay={o.id}
                  >
                    <span className="opt-check absolute right-2 top-2 grid h-4 w-4 place-items-center rounded-full bg-espresso">
                      <CheckIcon />
                    </span>
                    <span className="block text-base">{o.icon}</span>
                    <span className="mt-1 block text-[12.5px] font-extrabold leading-tight text-espresso">{o.title}</span>
                    <span className="mt-0.5 block text-[10px] text-coffee/50">{o.sub}</span>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label htmlFor="fNote" className="mb-1.5 block text-[10px] font-extrabold uppercase tracking-[0.2em] text-coffee/60">
                Nota <span className="font-semibold normal-case tracking-normal text-coffee/35">(opcional)</span>
              </label>
              <textarea
                id="fNote"
                rows={2}
                maxLength={180}
                placeholder="Sin cebolla, poco azúcar, tocar el timbre…"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className="field w-full resize-none rounded-xl border border-espresso/10 bg-paper px-4 py-3 text-[14px] text-espresso"
              />
            </div>

            <div className="overflow-hidden rounded-xl border border-gold/35 bg-paper">
              <div className="space-y-2 px-4 py-3.5">
                <div className="flex justify-between text-[12px] text-coffee/60">
                  <span>Productos</span>
                  <span id="sumQty" className="font-bold text-espresso">
                    {totals.qty}
                  </span>
                </div>
                <div className="flex justify-between text-[12px] text-coffee/60">
                  <span>Subtotal</span>
                  <span id="sumSubtotal" className="font-bold text-espresso">
                    {money(totals.total)}
                  </span>
                </div>
                <div className="flex justify-between text-[12px] text-coffee/60">
                  <span>Delivery</span>
                  <span id="sumDelivery" className="font-semibold text-coffee/40">
                    {orderType === 'recojo' ? 'Recojo en tienda' : 'Por coordinar'}
                  </span>
                </div>
              </div>
              <div className="flex items-baseline justify-between border-t border-dashed border-espresso/15 bg-cream-deep/40 px-4 py-3">
                <span className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-coffee/70">
                  Total a pagar
                </span>
                <span id="sumTotal" className="font-display text-[25px] font-semibold leading-none text-espresso">
                  {money(total)}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="shrink-0 border-t border-espresso/10 bg-paper px-5 pt-3 pb-safe-sheet">
          <button
            id="sendOrder"
            type="button"
            onClick={send}
            disabled={!hasItems}
            className={
              'inline-flex w-full items-center justify-center gap-2.5 rounded-2xl px-5 py-4 text-[14px] font-extrabold tracking-wide text-white transition active:scale-[.99] disabled:cursor-not-allowed disabled:opacity-35 disabled:shadow-none ' +
              (sent
                ? 'bg-wa-700'
                : 'bg-wa-500 shadow-[0_16px_30px_-16px_rgba(31,168,85,.9)] hover:bg-wa-600')
            }
          >
            {sent ? (
              <span>✅ ¡Pedido listo en WhatsApp!</span>
            ) : (
              <>
                <svg className="h-[18px] w-[18px]" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                  <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.75.46 3.46 1.32 4.96L2 22l5.25-1.38a9.9 9.9 0 0 0 4.79 1.22h.01c5.46 0 9.91-4.45 9.91-9.91 0-2.65-1.03-5.14-2.9-7.01A9.82 9.82 0 0 0 12.04 2Zm5.8 14.03c-.24.68-1.4 1.3-1.94 1.35-.54.05-1.03.24-3.47-.72-2.94-1.16-4.8-4.2-4.94-4.4-.14-.2-1.17-1.56-1.17-2.98 0-1.42.74-2.12 1.01-2.41.27-.29.58-.36.78-.36.2 0 .39 0 .56.01.18.01.42-.07.66.5.24.58.82 2 .89 2.14.07.15.12.32.02.51-.1.2-.63.7-.83.9-.2.2-.29.31-.15.55.14.24.62.99 1.33 1.62.91.8 1.67 1.06 1.91 1.18.24.12.38.1.52-.06.14-.16.6-.7.76-.94.16-.24.32-.2.54-.12.22.08 1.4.66 1.64.78.24.12.4.18.46.28.06.1.06.58-.18 1.26Z" />
                </svg>
                Enviar Pedido por WhatsApp
              </>
            )}
          </button>
          <p className="mt-1.5 text-center text-[10px] tracking-wide text-coffee/40">
            Se abrirá WhatsApp con tu pedido listo para enviar.
          </p>
        </div>
      </div>
    </div>
  );
}
