import type { Product } from './products';

/* ─────────────────────────────────────────────────────────────
   Panadería y bocaditos — catálogo por unidad
   Tomado de las cartas de la casa: los panes se cobran por unidad
   (S/ 0.25 – S/ 3.00) y los bocaditos también (S/ 0.90 – S/ 3.00).
   Estos ítems NO salen en la grilla de la carta: viven en su propia
   sección, pero comparten carrito, totales y mensaje de WhatsApp.
   ───────────────────────────────────────────────────────────── */

export type BakeryGroup =
  | 'clasicos'
  | 'integrales'
  | 'dulces'
  | 'especiales'
  | 'bocaditos-dulces'
  | 'bocaditos-salados';

export interface BakeryItem extends Product {
  group: BakeryGroup;
  /** Unidad de venta mostrada al lado del precio. */
  unit: string;
}

const item = (
  id: string,
  name: string,
  price: number,
  group: BakeryGroup,
  desc: string,
  emoji: string,
): BakeryItem => ({
  id,
  name,
  price,
  group,
  desc,
  emoji,
  cat: 'pan',
  tag: '',
  img: '',
  accent: '#C9A227',
  rating: 4.9,
  unit: 'c/u',
});

/* ── Panes clásicos ── */
const CLASICOS: BakeryItem[] = [
  item('pan-piso', 'Pan de piso', 0.25, 'clasicos', 'Bollo de miga tierna, recién salido del horno.', '🥖'),
  item('pan-arabe', 'Pan árabe', 0.25, 'clasicos', 'Delgado y elástico, perfecto para enrollar.', '🫓'),
  item('pan-frances', 'Francés', 0.33, 'clasicos', 'Corteza crocante y miga aireada de toda la vida.', '🥖'),
  item('pan-ciabatta', 'Ciabatta', 0.33, 'clasicos', 'Miga abierta y corteza firme, ideal para sánguches.', '🥖'),
  item('pan-mica', 'Mica', 0.25, 'clasicos', 'Pan de mesa, miga compacta y sabor suave.', '🍞'),
  item('pan-italiano', 'Italiano', 0.33, 'clasicos', 'Forma en espiral, corteza dorada y miga sedosa.', '🥐'),
  item('pan-coliza', 'Coliza', 0.33, 'clasicos', 'El clásico de barrio: miga esponjosa y base crujiente.', '🥯'),
  item('pan-espiga', 'Espiga', 0.33, 'clasicos', 'Trenzado a mano, con corteza fina y dorada.', '🥖'),
  item('pan-petipan', 'Petipan', 0.33, 'clasicos', 'Bocado pequeño para la mesa o el café.', '🥯'),
  item('pan-caramanduca', 'Caramanduca', 0.33, 'clasicos', 'Pan rural de corteza profunda y miga firme.', '🍞'),
  item('pan-marraqueta', 'Marraqueta', 3.0, 'clasicos', 'Hogaza grande de corteza crujiente, para compartir.', '🥖'),
];

/* ── Integrales ── */
const INTEGRALES: BakeryItem[] = [
  item('pan-chapla-integral', 'Chapla integral', 0.33, 'integrales', 'Con harina integral y un toque de salvado.', '🫓'),
  item('pan-integral', 'Pan integral', 0.25, 'integrales', 'Migado con fibra, suave y de larga duración.', '🍞'),
  item('pan-linaza', 'Pan de linaza', 0.33, 'integrales', 'Semillas de linaza en cada bocado.', '🍞'),
  item('pan-semita-integral', 'Semita integral', 1.0, 'integrales', 'Hogaza integral de miga densa y aroma tostado.', '🥖'),
];

/* ── Panes dulces ── */
const DULCES: BakeryItem[] = [
  item('pan-tortita-anis', 'Tortita de anís', 0.33, 'dulces', 'Aroma de anís y azúcar glass por encima.', '🍪'),
  item('pan-tortita-manteca', 'Tortita de manteca', 0.25, 'dulces', 'Manteca, huevo y ese dorado de casa.', '🍪'),
  item('pan-cachito-manteca', 'Cachito de manteca', 0.25, 'dulces', 'Enrollado dulce con corazón de manteca.', '🥐'),
  item('pan-yema', 'Yema', 0.33, 'dulces', 'Miga amarilla y tierna, dulce en su punto.', '🥐'),
  item('pan-camote', 'Pan de camote', 0.33, 'dulces', 'Camote peruano en la masa: color y dulzor natural.', '🍠'),
  item('pan-zanahoria', 'Pan de zanahoria', 0.33, 'dulces', 'Tierno y húmedo, con hebras de zanahoria.', '🥕'),
  item('pan-pinita', 'Pan piñita', 0.25, 'dulces', 'Bollito de miga fina, perfecto para el desayuno.', '🥯'),
];

/* ── Especiales ── */
const ESPECIALES: BakeryItem[] = [
  item('pan-hamburguesa', 'Hamburguesa chica', 0.33, 'especiales', 'Blando y brillante, listo para armar tu hamburguesa.', '🍔'),
  item('pan-hotdog', 'Hot dog', 0.25, 'especiales', 'Alargado y suave, se dobla sin romperse.', '🌭'),
  item('pan-maiz', 'Pan de maíz', 0.25, 'especiales', 'Maíz en la masa, miga húmeda y aroma dulce.', '🌽'),
  item('pan-semita-clasica', 'Semita clásica', 1.0, 'especiales', 'Hogaza de casa, corteza dorada y miga apretada.', '🍞'),
  item('pan-semita-encimada', 'Semita encimada', 0.5, 'especiales', 'Con costra de azúcar encima, dulce y tostada.', '🍞'),
];

/* ── Bocaditos dulces ── */
const BDULCES: BakeryItem[] = [
  item('bk-alfajor', 'Alfajor', 0.9, 'bocaditos-dulces', 'Manjar blanco entre dos galletas y azúcar glass.', '🍪'),
  item('bk-botija', 'Botija', 0.9, 'bocaditos-dulces', 'Soleado, esponjoso y con azúcar encima.', '🍩'),
  item('bk-defidoras', 'Defidoras', 0.9, 'bocaditos-dulces', 'Bocadito de masa quebrada con azúcar.', '🍪'),
  item('bk-oreja', 'Oreja', 0.9, 'bocaditos-dulces', 'Hojaldre en forma de oreja, azúcar caramelizada.', '🥐'),
  item('bk-chocochip', 'Galleta chocochip', 0.9, 'bocaditos-dulces', 'Chispas de chocolate en masa de mantequilla.', '🍪'),
  item('bk-donas', 'Donas', 1.2, 'bocaditos-dulces', 'Baño glaseado y miga liviana.', '🍩'),
  item('bk-trufa', 'Trufa', 0.9, 'bocaditos-dulces', 'Bocado de chocolate intenso.', '🍫'),
  item('bk-tarta-limon', 'Tarta de limón', 1.4, 'bocaditos-dulces', 'Crema de limón y merengue.', '🍰'),
  item('bk-tarta-manzana', 'Tarta de manzana', 1.4, 'bocaditos-dulces', 'Manzana en compota y canela.', '🥧'),
  item('bk-tarta-fresa', 'Tarta de fresa', 1.4, 'bocaditos-dulces', 'Fresas frescas sobre crema pastelera.', '🍰'),
  item('bk-pie-limon', 'Pie de limón', 1.4, 'bocaditos-dulces', 'Base crocante y relleno de limón.', '🥧'),
  item('bk-pie-manzana', 'Pie de manzana', 1.4, 'bocaditos-dulces', 'Manzana caramelizada y hojaldre.', '🥧'),
  item('bk-profiteroles', 'Profiteroles', 1.0, 'bocaditos-dulces', 'Masa choux rellena de crema.', '🧁'),
  item('bk-mousse-chocolate', 'Mousse de chocolate', 1.6, 'bocaditos-dulces', 'Chocolate amargo batido, textura aireada.', '🍫'),
  item('bk-mousse-fresa', 'Mousse de fresa', 1.6, 'bocaditos-dulces', 'Fresa natural y crema.', '🍓'),
  item('bk-mousse-lucuma', 'Mousse de lúcuma', 1.6, 'bocaditos-dulces', 'Lúcuma peruana, dulce y aterciopelada.', '🧁'),
  item('bk-pastel-manzana', 'Pastel de manzana', 1.0, 'bocaditos-dulces', 'Masa quebrada con manzana.', '🥧'),
  item('bk-cupcake', 'Cupcake con chantilly', 3.0, 'bocaditos-dulces', 'Vainilla y chantilly, tamaño grande.', '🧁'),
  item('bk-brownie', 'Brownie', 1.0, 'bocaditos-dulces', 'Chocolate denso con centro húmedo.', '🍫'),
  item('bk-pionono', 'Pionono', 0.9, 'bocaditos-dulces', 'Bizcocho enrollado con manjar blanco.', '🍰'),
];

/* ── Bocaditos salados ── */
const BSALADOS: BakeryItem[] = [
  item('bk-enrollado-hotdog', 'Enrollado de hot dog', 1.0, 'bocaditos-salados', 'Masa de pan con hot dog y queso.', '🌭'),
  item('bk-empanada-pollo', 'Empanada de pollo', 1.0, 'bocaditos-salados', 'Pollo con cebolla salteada, masa crocante.', '🥟'),
  item('bk-empanada-carne', 'Empanada de carne', 1.2, 'bocaditos-salados', 'Carne de res en cubos con aderezo.', '🥟'),
  item('bk-empanada-lomo', 'Empanada de lomo', 1.3, 'bocaditos-salados', 'Lomo saltado dentro de la empanada.', '🥟'),
  item('bk-minipizza', 'Minipizza', 1.4, 'bocaditos-salados', 'Americana o pepperoni, masa fina.', '🍕'),
  item('bk-minitriple', 'Minitriple', 2.0, 'bocaditos-salados', 'Queso, jamón y pollo en pan de mesa.', '🥪'),
  item('bk-pan-pollo', 'Pan francés con pollo', 3.0, 'bocaditos-salados', 'Pan francés con pollo deshilachado.', '🥖'),
  item('bk-pan-yema-pollo', 'Pan yema con pollo', 2.5, 'bocaditos-salados', 'Pan yema relleno de pollo y mayonesa.', '🥐'),
];

export const BAKERY_ITEMS: BakeryItem[] = [
  ...CLASICOS,
  ...INTEGRALES,
  ...DULCES,
  ...ESPECIALES,
  ...BDULCES,
  ...BSALADOS,
];

export const BAKERY_GROUPS: Array<{ id: BakeryGroup; label: string; short: string; note: string }> = [
  { id: 'clasicos', label: 'Panes clásicos', short: 'Clásicos', note: 'Del horno de la mañana' },
  { id: 'integrales', label: 'Integrales', short: 'Integrales', note: 'Con fibra y semillas' },
  { id: 'dulces', label: 'Panes dulces', short: 'Panes dulces', note: 'Para el café de las 5' },
  { id: 'especiales', label: 'Especiales', short: 'Especiales', note: 'Para hamburguesas y sánguches' },
  { id: 'bocaditos-dulces', label: 'Bocaditos dulces', short: 'Bocaditos dulces', note: 'Dulces de vitrina' },
  { id: 'bocaditos-salados', label: 'Bocaditos salados', short: 'Bocaditos salados', note: 'Salados recién horneados' },
];

export const BAKERY_BY_GROUP = (group: BakeryGroup) =>
  BAKERY_ITEMS.filter((p) => p.group === group);
