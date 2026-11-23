# Alviña Primavera — Carta digital y pedidos por WhatsApp

Carta digital mobile-first de **Alviña Primavera**, cafetería y sanguchería en Trujillo, Perú.
Los pedidos se arman en el navegador y se envían por WhatsApp con el detalle y el total en Soles.

- **Portada**: hero editorial con los **4 imperdibles** de la casa (`dish-spotlight-carousel`): plato enmarcado, titular a dos tonos sobre franja burdeos y tira de miniaturas.
- **Carta**: buscador, pestañas por categoría, 10 productos con foto propia y steppers +/−.
- **Portada**: los 4 platos destacados usan recortes con fondo transparente (`public/assets/products/cutout/*.webp`) y flotan sobre la portada; la carta sigue con sus fotos originales.
- **Banner animado**: coreografía de entrada del plato y los textos (GSAP), flotación continua, inclinación 3D con el cursor y parallax al hacer scroll.
- **Movimiento**: scroll reveal con IntersectionObserver (`[data-reveal]` → fade-up escalonado) y botón flotante para volver arriba.
- **Pedido**: barra flotante con ítems y total, hoja de pedido (recojo / delivery, Yape-Plin / efectivo) y mensaje de WhatsApp prellenado.
- **Respaldo sin build**: `carta-rapida.html` (versión vanilla, mismo diseño y mismas fotos).

## Stack

| Pieza | Versión |
| --- | --- |
| Vite | 5 |
| React + TypeScript | 18.3 + 5.6 |
| Tailwind CSS | 3.4 (config propia con la paleta de la marca) |
| shadcn/ui | estructura + tokens (`components.json`, `@/components/ui`) |
| GSAP | 3.12 para las transiciones de la portada |

El proyecto usa la convención de shadcn/ui (`components.json`, alias `@/*`, `cn()` en `@/lib/utils`,
tokens CSS en `src/index.css`) con Tailwind 3.4 para conservar exactamente los estilos ya aprobados.
Si más adelante quieres migrar a Tailwind v4, basta con `npx shadcn@latest init` y mover los
tokens de `:root` al bloque `@theme`.

## Estructura

```
index.html                     entrada Vite (metadatos, fuentes, splash)
carta-rapida.html              versión vanilla (multipágina, sin React)
src/
  main.tsx                     monta <App />
  App.tsx                      composición: cabecera, portada, carta, barra, modal
  index.css                    Tailwind + tokens shadcn + sistema visual + CSS de la portada
  data/products.ts             catálogo, categorías, formato de Soles, WhatsApp
  hooks/                       carrito (localStorage), horario, dinero animado, reveal
  components/
    ui/dish-spotlight-carousel.tsx  portada editorial (GSAP, accesible)
    Header.tsx                 logo, subtítulo, horario, estado abierto/cerrado, buscador
    ProductCard.tsx            tarjeta con stepper
    OrderSheet.tsx             hoja de pedido + mensaje de WhatsApp
    FloatBar.tsx               barra flotante (ítems + total)
    Thumb.tsx, Sections.tsx, Toast.tsx, BackToTop.tsx
public/assets/                 logo (PNG sin fondo), favicon y 10 fotos de producto
```

## Comandos

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # tsc --noEmit + vite build → dist/
npm run preview    # sirve dist/
```

## Portada: `dish-spotlight-carousel`

```tsx
import { DishSpotlightCarousel } from '@/components/ui/dish-spotlight-carousel';

<DishSpotlightCarousel
  dishes={dishes}                  // los 4 de FEATURED_IDS
  eyebrow="El más pedido de la casa"
  onOrder={(dish) => inc(dish.id)}  // agrega al carrito
  onExplore={scrollToCarta}
/>
```

- Banda espresso a sangre con grano, marca de agua del logo, plato enmarcado y titular Fraunces en
  dos tonos (mayúsculas crema + itálica dorada) sobre una franja diagonal burdeos.
- Tira inferior con los 4 platos (nombre + precio) y flechas; también responde a teclado (`←` `→`
  `Home` `End`) y swipe. `prefers-reduced-motion` desactiva las transiciones.
- En ≥900 px el plato va a la izquierda y el texto a la derecha; en móvil, plato primero y luego el texto.
- Consume los tokens `--color-background`, `--color-foreground`, `--color-primary` y
  `--color-muted-foreground` definidos en `src/index.css`.

## Fotos

`public/assets/products/*.jpg` — 900 × 900, JPEG progresivo, ~100 KB cada una, estética de marca
(fondo de café, tabla de madera, lino y banderita burdeos con el logo). Se sirven como
`/assets/products/<id>.jpg` y se cargan con `loading="lazy"` salvo las primeras de la portada.

## Deploy

Vercel detecta Vite automáticamente: **build** `npm run build`, **output** `dist`.
También funciona en GitHub Pages o cualquier hosting estático.

## Datos del negocio

- WhatsApp: **+51 935 466 580** (`https://wa.me/51935466580`)
- Horario: 7:30 AM – 12:00 PM y 3:30 PM – 7:00 PM (America/Lima)
- Precios en Soles (S/), incluyen IGV.
