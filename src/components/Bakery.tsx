import * as React from 'react';
import { BAKERY_BY_GROUP, BAKERY_GROUPS, type BakeryGroup } from '@/data/extra';
import { money } from '@/data/products';

interface BakeryProps {
  qtyOf: (id: string) => number;
  onAdd: (id: string, n: number) => void;
  onDec: (id: string) => void;
}

/* ─────────────────────────────────────────────────────────────
   Panadería — carta por unidad
   Lista tipo menú impreso: nombre · puntos guía · precio c/u.
   Con atajos +6 / +12 porque en panadería se compra por docena.
   ───────────────────────────────────────────────────────────── */

export function Bakery({ qtyOf, onAdd, onDec }: BakeryProps) {
  const [group, setGroup] = React.useState<BakeryGroup>('clasicos');
  const items = React.useMemo(() => BAKERY_BY_GROUP(group), [group]);
  const meta = BAKERY_GROUPS.find((g) => g.id === group) ?? BAKERY_GROUPS[0];

  const moveTab = (dir: number) => {
    const i = BAKERY_GROUPS.findIndex((g) => g.id === group);
    const next = BAKERY_GROUPS[(i + dir + BAKERY_GROUPS.length) % BAKERY_GROUPS.length];
    setGroup(next.id);
  };

  return (
    <section id="panaderia" className="bakery" aria-labelledby="bakeryTitle">
      <header className="bakery-head" data-reveal="heading">
        <div className="bakery-head-text">
          <p className="bakery-eyebrow">Horneado del día</p>
          <h2 id="bakeryTitle" className="bakery-title">
            Panadería <em>y bocaditos</em>
          </h2>
          <p className="bakery-lead">
            Todo se vende <strong>por unidad</strong>. Arma tu media docena o la docena completa con los
            atajos <span className="bk-kbd">+6</span> y <span className="bk-kbd">+12</span>, y lo sumamos a tu
            pedido de WhatsApp.
          </p>
        </div>
        <figure className="bakery-figure">
          <img
            src="/assets/bakery/banda.webp"
            alt="Panes artesanales de Alviña recién horneados"
            loading="lazy"
            decoding="async"
            width={720}
            height={540}
          />
          <figcaption>Salen del horno a las 7:30 y a las 3:30</figcaption>
        </figure>
      </header>

      <div className="bakery-tabs-wrap">
        <div
          id="bakeryTabs"
          className="bakery-tabs no-scrollbar"
          role="tablist"
          aria-label="Grupos de panadería"
          onKeyDown={(e) => {
            if (e.key === 'ArrowRight') {
              e.preventDefault();
              moveTab(1);
            }
            if (e.key === 'ArrowLeft') {
              e.preventDefault();
              moveTab(-1);
            }
          }}
        >
          {BAKERY_GROUPS.map((g) => (
            <button
              key={g.id}
              type="button"
              role="tab"
              data-bk-tab={g.id}
              aria-selected={group === g.id}
              onClick={() => setGroup(g.id)}
              className="bk-tab"
            >
              {g.short}
            </button>
          ))}
        </div>
        <p className="bakery-note-inline">
          <span aria-hidden="true">✦</span> {meta.note}
        </p>
      </div>

      <div key={group} className="bakery-list" role="tabpanel" aria-label={meta.label}>
        {items.map((p, i) => {
          const qty = qtyOf(p.id);
          return (
            <article
              key={p.id}
              className={'bk-item' + (qty > 0 ? ' is-in' : '')}
              data-bk={p.id}
              style={{ '--i': i } as React.CSSProperties}
            >
              <div className="bk-thumb">
                <img
                  src={'/assets/bakery/thumbs/' + p.id + '.webp'}
                  alt={'Foto de ' + p.name}
                  width={54}
                  height={54}
                  loading="lazy"
                  decoding="async"
                />
              </div>

              <div className="bk-body">
                <div className="bk-item-top">
                  <h3 className="bk-name">{p.name}</h3>
                  <span className="bk-dots" aria-hidden="true" />
                  <p className="bk-price">
                    {money(p.price)}
                    <span className="bk-unit">{p.unit}</span>
                  </p>
                </div>

                <div className="bk-item-bot">
                  <p className="bk-desc">{p.desc}</p>

                <div className="bk-actions">
                  <button
                    type="button"
                    className="bk-chip"
                    data-act="add6"
                    onClick={() => onAdd(p.id, 6)}
                    aria-label={'Agregar media docena de ' + p.name}
                  >
                    +6
                  </button>
                  <button
                    type="button"
                    className="bk-chip"
                    data-act="add12"
                    onClick={() => onAdd(p.id, 12)}
                    aria-label={'Agregar una docena de ' + p.name}
                  >
                    +12
                  </button>

                  <div className="bk-step" role="group" aria-label={'Cantidad de ' + p.name}>
                    <button
                      type="button"
                      className="bk-step-btn"
                      data-act="dec"
                      disabled={qty === 0}
                      onClick={() => onDec(p.id)}
                      aria-label={'Quitar uno de ' + p.name}
                    >
                      −
                    </button>
                    <span className="bk-qty" data-qty>
                      {qty}
                    </span>
                    <button
                      type="button"
                      className="bk-step-btn is-add"
                      data-act="inc"
                      onClick={() => onAdd(p.id, 1)}
                      aria-label={'Agregar uno de ' + p.name}
                    >
                      +
                    </button>
                    </div>
                  </div>
                </div>
              </div>
            </article>
          );
        })}
      </div>

      <aside className="bakery-callout" data-reveal="section">
        <div>
          <p className="bakery-callout-title">¿Lo necesitas en otro tamaño?</p>
          <p className="bakery-callout-text">
            Elaboramos nuestros panes en tamaño grande, bocadito o medidas personalizadas: hamburguesa, hot
            dog, francés, ciabatta, hamburguesa en aro y más. Indícanos el tamaño, peso o diámetro que
            necesitas y lo hacemos a tu medida.
          </p>
        </div>
        <p className="bakery-callout-claim">“El sabor que crece contigo…”</p>
      </aside>
    </section>
  );
}
