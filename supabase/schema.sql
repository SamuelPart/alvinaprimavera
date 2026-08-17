-- ============================================================
--  📻 Flippy Walkie-Talkie · Esquema de base de datos Supabase
--
--  Ejecutar en: Supabase Dashboard → SQL Editor → New query
--  (se puede ejecutar más de una vez sin errores)
-- ============================================================

-- ============================================================
--  1. TABLAS
-- ============================================================

create table if not exists public.profiles (
    flippy_id   text primary key,
    username    text not null,
    avatar      text not null default '📻',
    -- Para que tus amigos te encuentren desde sus contactos del teléfono
    account_email text,
    phone       text,
    -- Un solo dispositivo activo por cuenta (último login gana)
    active_device text,
    created_at  timestamptz not null default now()
);

-- Si tu tabla profiles YA existía, ejecuta solo esto:
-- alter table public.profiles add column if not exists account_email text;
-- alter table public.profiles add column if not exists phone text;
-- alter table public.profiles add column if not exists active_device text;

create table if not exists public.channels (
    id          text primary key,
    name        text not null,
    frequency   text not null default '',
    description text not null default '',
    icon        text not null default 'Radio',
    is_public   boolean not null default true,
    pin         text,
    created_at  timestamptz not null default now()
);

create table if not exists public.contacts (
    id                  bigint generated always as identity primary key,
    owner_flippy_id     text not null,
    contact_flippy_id   text not null,
    created_at          timestamptz not null default now(),
    unique (owner_flippy_id, contact_flippy_id)
);

create table if not exists public.voice_messages (
    id               uuid primary key default gen_random_uuid(),
    sender_flippy_id text not null,
    sender_name      text not null,
    sender_avatar    text not null default '📻',
    target_type      text not null default 'channel' check (target_type in ('channel', 'direct')),
    target_id        text not null default 'ch-general',
    -- Path del audio dentro del bucket "flippy-audio"
    -- (ej. "FLIP-1234A/msg-1723500000000.wav")
    audio_url        text,
    duration         real not null default 0,
    filter           text not null default 'radio',
    transcription    text not null default '',
    created_at       timestamptz not null default now()
);

create table if not exists public.ptt_events (
    id               bigint generated always as identity primary key,
    event_type       text not null check (event_type in ('start', 'end')),
    sender_flippy_id text not null,
    sender_name      text not null,
    sender_avatar    text not null default '📻',
    target_type      text not null default 'channel',
    target_id        text not null default 'ch-general',
    created_at       timestamptz not null default now()
);

-- Canales por defecto (mismos que trae la app; se deduplican por id)
insert into public.channels (id, name, frequency, description, icon) values
    ('ch-general',  'General Radio',         '144.800 MHz', 'Frecuencia abierta general',         'Radio'),
    ('ch-tactical', 'Táctico & Emergencias', '433.920 MHz', 'Canal prioritario de emergencias',   'ShieldAlert'),
    ('ch-chill',    'Chill & Música',        '88.500 MHz',  'Charlas libres y audio',             'Headphones'),
    ('ch-gaming',   'Squad / Gaming',        '512.000 MHz', 'Coordinación rápida de juego',       'Gamepad2')
on conflict (id) do nothing;

-- ============================================================
--  2. SEGURIDAD (RLS) — acceso abierto con la anon key
--     (walkie-talkie sin login; ajusta si añades Auth en el futuro)
-- ============================================================

alter table public.profiles       enable row level security;
alter table public.channels       enable row level security;
alter table public.contacts       enable row level security;
alter table public.voice_messages enable row level security;
alter table public.ptt_events     enable row level security;

drop policy if exists flippy_public_all on public.profiles;
create policy flippy_public_all on public.profiles
    for all using (true) with check (true);

drop policy if exists flippy_public_all on public.channels;
create policy flippy_public_all on public.channels
    for all using (true) with check (true);

drop policy if exists flippy_public_all on public.contacts;
create policy flippy_public_all on public.contacts
    for all using (true) with check (true);

drop policy if exists flippy_public_all on public.voice_messages;
create policy flippy_public_all on public.voice_messages
    for all using (true) with check (true);

drop policy if exists flippy_public_all on public.ptt_events;
create policy flippy_public_all on public.ptt_events
    for all using (true) with check (true);

-- ============================================================
--  3. STORAGE — bucket público para los audios WAV
-- ============================================================

insert into storage.buckets (id, name, public)
values ('flippy-audio', 'flippy-audio', true)
on conflict (id) do nothing;

drop policy if exists flippy_audio_read on storage.objects;
create policy flippy_audio_read on storage.objects
    for select using (bucket_id = 'flippy-audio');

drop policy if exists flippy_audio_insert on storage.objects;
create policy flippy_audio_insert on storage.objects
    for insert with check (bucket_id = 'flippy-audio');

drop policy if exists flippy_audio_update on storage.objects;
create policy flippy_audio_update on storage.objects
    for update using (bucket_id = 'flippy-audio');

drop policy if exists flippy_audio_delete on storage.objects;
create policy flippy_audio_delete on storage.objects
    for delete using (bucket_id = 'flippy-audio');

-- ============================================================
--  4. REALTIME — recibir mensajes y eventos PTT al instante
-- ============================================================

do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'voice_messages'
    ) then
        alter publication supabase_realtime add table public.voice_messages;
    end if;

    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'ptt_events'
    ) then
        alter publication supabase_realtime add table public.ptt_events;
    end if;

    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'profiles'
    ) then
        alter publication supabase_realtime add table public.profiles;
    end if;
end $$;
