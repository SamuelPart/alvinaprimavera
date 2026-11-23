# Alviña Primavera · Carta Digital

Carta digital y catálogo de pedidos por WhatsApp para **Alviña Primavera**, cafetería y sanguchería en Trujillo, Perú.

- **Web:** una sola página, mobile-first, sin backend.
- **Pedidos:** se arman en un carrito y se envían a `wa.me/51935466580` con el detalle y el total.
- **Precios:** en Soles (S/).

## Archivos

| Archivo | Descripción |
| --- | --- |
| `index.html` | App principal, hecha con **React 18** (UMD + Babel standalone, sin build step) y Tailwind CSS por CDN. |
| `carta-rapida.html` | Misma carta en **JavaScript puro**, sin React. Más liviana (~57 KB) y sin dependencias JS. |
| `assets/logo-alvina.png` | Logo oficial de Alviña sin fondo, en tono espresso (para fondos claros). |
| `assets/logo-alvina-cream.png` | Misma marca en tono crema (para el panel oscuro del hero). |
| `assets/favicon.png` | Ícono de la pestaña del navegador. |
| `assets/products/*.jpg` | Fotos propias de los 10 productos (900×900, ~100 KB c/u) con la banderita de la marca. |

Ambas comparten el mismo diseño, catálogo, carrito y mensaje de WhatsApp. Solo cambia la tecnología.

## Cómo usar

Abre `index.html` en cualquier navegador, o sirve la carpeta:

```bash
python3 -m http.server 8000
```

## Deploy

Es un sitio 100 % estático. En Vercel (o Netlify, Cloudflare Pages, GitHub Pages) basta con publicar la raíz del repo: `index.html` se sirve automáticamente. No requiere build, variables de entorno ni configuración.

## Personalización

- **Productos, precios y categorías:** array `PRODUCTS` y `CATEGORIES` dentro de `index.html`.
- **Número de WhatsApp:** constante `WHATSAPP_NUMBER` (`51935466580`).
- **Horario y estado abierto/cerrado:** panel del hero y función `computeStatus()` (hora de Lima).
- **Colores y tipografía:** bloque `tailwind.config` al inicio del archivo.
- **Imágenes:** fotos propias en `assets/products/` (900×900, JPEG q78). Si alguna falla, se muestra un emoji sobre un degradado cálido.
- **Logo:** el original venía sobre fondo rojo; se convirtió a PNG con transparencia (se conserva el alfa del texto original). Para cambiarlo, reemplaza los archivos de `assets/` manteniendo el mismo nombre.

## Datos del negocio

- Horario: 7:30 AM – 12:00 PM y 3:30 PM – 7:00 PM
- WhatsApp: +51 935 466 580
- Pagos: Yape / Plin y efectivo
- Entrega: recojo en tienda o delivery cercano
