import { BAKERY_ITEMS } from './extra';

export type CategoryId = 'todos' | 'sanguches' | 'jugos' | 'cafes' | 'pan';

export interface Product {
  id: string;
  name: string;
  price: number;
  cat: Exclude<CategoryId, 'todos'>;
  emoji: string;
  tag: string;
  desc: string;
  img: string;
  /** Color de acento de la marca para el carrusel de portada. */
  accent: string;
  /** Puntaje mostrado en la portada (4.6 – 5.0). */
  rating: number;
  /** Recorte con fondo transparente, solo para la portada y su tira. */
  cutout?: string;
}

export const WHATSAPP_NUMBER = '51935466580';
export const CART_KEY = 'alvina_cart_v3';
export const CLIENT_KEY = 'alvina_client_v3';

export const PRODUCTS: Product[] = [
  {
    id: 'pan-pollo',
    name: 'Pan con Pollo Especial',
    price: 5.0,
    cat: 'sanguches',
    emoji: '🥪',
    tag: 'Más pedido',
    desc: 'Pan francés crocante, pollo deshilachado, mayonesa casera y lechuga fresca.',
    img: '/assets/products/pan-pollo.jpg',
    accent: '#691312',
    rating: 4.9,
    cutout: '/assets/products/cutout/pan-pollo.webp',
  },
  {
    id: 'mixto',
    name: 'Sándwich Mixto Caliente',
    price: 4.5,
    cat: 'sanguches',
    emoji: '🧀',
    tag: '',
    desc: 'Jamón y queso derretido a la plancha, pan doradito. El clásico de la mañana.',
    img: '/assets/products/mixto.jpg',
    accent: '#C9A227',
    rating: 4.7,
  },
  {
    id: 'jugo-papaya',
    name: 'Jugo de Papaya Natural',
    price: 4.0,
    cat: 'jugos',
    emoji: '🥤',
    tag: '',
    desc: 'Papaya fresca, sin azúcar añadida. Dulce, ligero y bien frío.',
    img: '/assets/products/jugo-papaya.jpg',
    accent: '#E08A1E',
    rating: 4.8,
  },
  {
    id: 'jugo-surtido',
    name: 'Jugo Surtido Especial',
    price: 5.0,
    cat: 'jugos',
    emoji: '🍹',
    tag: 'De la casa',
    desc: 'Piña, papaya, naranja y un toque de miel. Nuestra mezcla de la casa.',
    img: '/assets/products/jugo-surtido.jpg',
    accent: '#C05621',
    rating: 4.9,
    cutout: '/assets/products/cutout/jugo-surtido.webp',
  },
  {
    id: 'cafe-americano',
    name: 'Café Americano Pasado',
    price: 3.0,
    cat: 'cafes',
    emoji: '☕',
    tag: '',
    desc: 'Café de grano peruano pasado en olla. Aroma intenso y cuerpo suave.',
    img: '/assets/products/cafe-americano.jpg',
    accent: '#5A3A24',
    rating: 4.8,
    cutout: '/assets/products/cutout/cafe-americano.webp',
  },
  {
    id: 'avena',
    name: 'Avena Caliente',
    price: 2.5,
    cat: 'cafes',
    emoji: '🥣',
    tag: '',
    desc: 'Avena cocida con leche, canela y clavo de olor. Reconfortante y cremosa.',
    img: '/assets/products/avena.jpg',
    accent: '#B98A4B',
    rating: 4.6,
  },
  {
    id: 'alfajor',
    name: 'Alfajor Artesanal',
    price: 2.0,
    cat: 'pan',
    emoji: '🍪',
    tag: '',
    desc: 'Manjar blanco entre dos galletas suaves, bañado en azúcar impalpable.',
    img: '/assets/products/alfajor.jpg',
    accent: '#A8743C',
    rating: 4.7,
    cutout: '/assets/products/cutout/alfajor.webp',
  },
  {
    id: 'pionono',
    name: 'Pionono de Manjar',
    price: 2.5,
    cat: 'pan',
    emoji: '🍰',
    tag: '',
    desc: 'Bizcocho esponjoso enrollado con manjar de la casa y un toque de azúcar.',
    img: '/assets/products/pionono.jpg',
    accent: '#C08A5A',
    rating: 4.8,
  },
  {
    id: 'gaseosa',
    name: 'Gaseosa Personal',
    price: 2.5,
    cat: 'cafes',
    emoji: '🥤',
    tag: '',
    desc: 'Inca Kola, Coca-Cola o Fanta bien helada, en envase personal.',
    img: '/assets/products/gaseosa.jpg',
    accent: '#A33A2C',
    rating: 4.6,
  },
  {
    id: 'batimix',
    name: 'Batimix',
    price: 3.5,
    cat: 'jugos',
    emoji: '🥛',
    tag: 'Favorito',
    desc: 'Batido cremoso de frutas frescas con leche. Pregunta por la fruta del día.',
    img: '/assets/products/batimix.jpg',
    accent: '#B96A4B',
    rating: 4.8,
  },
];

/**
 * Los 4 imperdibles que van en la portada (sánguche, jugo, café y postre).
 * El resto de la carta sigue completa más abajo.
 */
export const FEATURED_IDS = ['pan-pollo', 'jugo-surtido', 'cafe-americano', 'alfajor'];

export const CATEGORIES: Array<{ id: CategoryId; label: string; emoji: string }> = [
  { id: 'todos', label: 'Todos', emoji: '🍽️' },
  { id: 'sanguches', label: 'Sánguches', emoji: '🥪' },
  { id: 'jugos', label: 'Jugos Naturales', emoji: '🍹' },
  { id: 'cafes', label: 'Cafés y Bebidas', emoji: '☕' },
  { id: 'pan', label: 'Pan y Postres', emoji: '🥐' },
];

export const PAY_LABEL: Record<string, string> = { yape: 'Yape / Plin', efectivo: 'Efectivo' };
export const TYPE_LABEL: Record<string, string> = {
  recojo: 'Recojo en tienda',
  delivery: 'Delivery cercano',
};

export const money = (n: number) => 'S/ ' + n.toFixed(2);

export const norm = (s: string) =>
  s
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');

export const byId = (id: string) =>
  PRODUCTS.find((p) => p.id === id) ?? BAKERY_ITEMS.find((p) => p.id === id);

export const REDUCED =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
    : false;
