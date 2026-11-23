# Alviña Primavera — Carta digital y pedidos por WhatsApp

Carta digital mobile-first de **Alviña Primavera**, cafetería y sanguchería en Trujillo, Perú.
Los pedidos se arman en el navegador y se envían por WhatsApp con el detalle y el total en Soles.

- **Portada**: carrusel-orbe de los platos de la casa (`dish-orbit-carousel`) con GSAP + ScrollTrigger.
- **Carta**: buscador, pestañas por categoría, 10 productos con foto propia y steppers +/−.
- **Pedido**: barra flotante con ítems y total, hoja de pedido (recojo / delivery, Yape-Plin / efectivo) y mensaje de WhatsApp prellenado.
- **Respaldo sin build**: `carta-rapida.html` (versión vanilla, mismo diseño y mismas fotos).

## Stack

| Pieza | Versión |
| --- | --- |
| Vite | 5 |
| React + TypeScript | 18.3 + 5.6 |
| Tailwind CSS | 3.4 (config propia con la paleta de la marca) |
| shadcn/ui | estructura + tokens (`components.json`, `@/components/ui`) |
| GSAP | 3.12 (`ScrollTrigger`) para la portada |

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
    ui/dish-orbit-carousel.tsx portada (GSAP + ScrollTrigger, accesible)
    Header.tsx                 logo, subtítulo, horario, estado abierto/cerrado, buscador
    ProductCard.tsx            tarjeta con stepper
    OrderSheet.tsx             hoja de pedido + mensaje de WhatsApp
    FloatBar.tsx               barra flotante (ítems + total)
    Thumb.tsx, Sections.tsx, Toast.tsx
public/assets/                 logo (PNG sin fondo), favicon y 10 fotos de producto
```

## Comandos

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # tsc --noEmit + vite build → dist/
npm run preview    # sirve dist/
```

## Portada: `dish-orbit-carousel`

```tsx
import { DishOrbitCarousel } from '@/components/ui/dish-orbit-carousel';

<DishOrbitCarousel
  dishes={dishes}                 // { id, name, subtitle, price, image, accent, rating, tag }
  eyebrow="Portada de la casa"
  title={<>Los imperdibles<br /><em>de Alviña</em></>}
  onOrder={(dish) => inc(dish.id)} // agrega al carrito
  onExplore={scrollToCarta}
/>
```

- Toma el scroll con `ScrollTrigger` (pin) para recorrer la órbita; también responde a clic, swipe y teclado.
- Con `prefers-reduced-motion` no se hace pin: se navega con las flechas, los puntos o los satélites.
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
