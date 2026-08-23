-- ============================================================
--  📻 Flippy · LIMPIEZA total de tablas (ejecutar SOLO UNA VEZ)
--
--  Usa este script únicamente si tu proyecto Supabase tenía
--  tablas viejas de Flippy con una estructura distinta
--  (ej. channels.id de tipo uuid → error "invalid input syntax
--  for type uuid").
--
--  ⚠️ BORRA las 5 tablas y todo su contenido.
--  Después de ejecutarlo, corre supabase/schema.sql completo.
-- ============================================================

drop table if exists public.voice_messages cascade;
drop table if exists public.ptt_events cascade;
drop table if exists public.contacts cascade;
drop table if exists public.channels cascade;
drop table if exists public.profiles cascade;
