-- FLIPPY: migración inicial
create table if not exists public.profiles (
  id uuid primary key default gen_random_uuid(),
  flippy_id text unique not null,
  username text not null,
  avatar text not null default '📻',
  created_at timestamptz not null default now()
);

create table if not exists public.channels (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  frequency text not null default '',
  description text not null default '',
  icon text not null default 'Radio',
  is_public boolean not null default true,
  pin text,
  created_at timestamptz not null default now()
);

create table if not exists public.contacts (
  id uuid primary key default gen_random_uuid(),
  owner_flippy_id text not null,
  contact_flippy_id text not null,
  created_at timestamptz not null default now(),
  unique (owner_flippy_id, contact_flippy_id)
);

create table if not exists public.voice_messages (
  id uuid primary key default gen_random_uuid(),
  sender_flippy_id text not null,
  sender_name text not null,
  sender_avatar text not null default '📻',
  target_type text not null default 'channel',
  target_id text not null default 'ch-general',
  audio_url text,
  duration real not null default 0,
  filter text not null default 'radio',
  transcription text not null default '',
  created_at timestamptz not null default now()
);

create table if not exists public.ptt_events (
  id uuid primary key default gen_random_uuid(),
  event_type text not null,
  sender_flippy_id text not null,
  sender_name text not null,
  sender_avatar text not null default '📻',
  target_type text not null default 'channel',
  target_id text not null default 'ch-general',
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.channels enable row level security;
alter table public.contacts enable row level security;
alter table public.voice_messages enable row level security;
alter table public.ptt_events enable row level security;

drop policy if exists "profiles_select" on public.profiles;
create policy "profiles_select" on public.profiles for select using (true);
drop policy if exists "profiles_insert" on public.profiles;
create policy "profiles_insert" on public.profiles for insert with check (true);

drop policy if exists "channels_select" on public.channels;
create policy "channels_select" on public.channels for select using (true);
drop policy if exists "channels_insert" on public.channels;
create policy "channels_insert" on public.channels for insert with check (true);

drop policy if exists "contacts_select" on public.contacts;
create policy "contacts_select" on public.contacts for select using (true);
drop policy if exists "contacts_insert" on public.contacts;
create policy "contacts_insert" on public.contacts for insert with check (true);

drop policy if exists "messages_select" on public.voice_messages;
create policy "messages_select" on public.voice_messages for select using (true);
drop policy if exists "messages_insert" on public.voice_messages;
create policy "messages_insert" on public.voice_messages for insert with check (true);

drop policy if exists "ptt_select" on public.ptt_events;
create policy "ptt_select" on public.ptt_events for select using (true);
drop policy if exists "ptt_insert" on public.ptt_events;
create policy "ptt_insert" on public.ptt_events for insert with check (true);

insert into storage.buckets (id, name, public)
values ('flippy-audio', 'flippy-audio', true)
on conflict (id) do nothing;

alter publication supabase_realtime add table public.voice_messages;
alter publication supabase_realtime add table public.ptt_events;

insert into public.profiles (flippy_id, username, avatar)
select 'FLIP-BOT', 'Flippy AI Walkie', '🤖'
where not exists (select 1 from public.profiles where flippy_id = 'FLIP-BOT');
insert into public.profiles (flippy_id, username, avatar)
select 'FLIP-8921', 'Alex Patrol', '🐺'
where not exists (select 1 from public.profiles where flippy_id = 'FLIP-8921');
insert into public.profiles (flippy_id, username, avatar)
select 'FLIP-3142', 'Echo Base', '📡'
where not exists (select 1 from public.profiles where flippy_id = 'FLIP-3142');

insert into public.channels (name, frequency, description, icon)
select 'General Radio', '144.800 MHz', 'Frecuencia abierta para todos los operadores Flippy', 'Radio'
where not exists (select 1 from public.channels where name = 'General Radio');
insert into public.channels (name, frequency, description, icon)
select 'Táctico & Emergencias', '433.920 MHz', 'Canal prioritario de emergencias', 'ShieldAlert'
where not exists (select 1 from public.channels where name = 'Táctico & Emergencias');
insert into public.channels (name, frequency, description, icon)
select 'Chill & Música', '88.500 MHz', 'Charlas libres y audio', 'Headphones'
where not exists (select 1 from public.channels where name = 'Chill & Música');
insert into public.channels (name, frequency, description, icon)
select 'Squad / Gaming', '512.000 MHz', 'Coordinación rápida de juego', 'Gamepad2'
where not exists (select 1 from public.channels where name = 'Squad / Gaming');