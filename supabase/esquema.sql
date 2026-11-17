-- =============================================================================
-- GanHard · Etapa 5 · Cuentas, fincas, invitaciones e integrantes
--
-- Cómo usarlo: Supabase → SQL Editor → New query → pegar TODO este archivo → Run.
-- Se puede ejecutar más de una vez sin dañar lo que ya existe.
--
-- Principio de seguridad: las tablas NO se pueden leer ni escribir desde la app
-- (RLS activo, sin políticas y sin permisos). La app solo llama a las funciones
-- de abajo, y cada función comprueba quién es la cuenta (auth.uid()) y qué le
-- toca hacer. Los mensajes de RAISE EXCEPTION son códigos que la app traduce a
-- textos; si cambias uno, cambia también ErrorRemoto.java y MensajesRemotos.java.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Tablas
-- ---------------------------------------------------------------------------

-- Una fila por cuenta (la crea un disparador al registrarse).
create table if not exists public.perfil (
    id_cuenta uuid primary key references auth.users (id) on delete cascade,
    nombre    text not null check (char_length(btrim(nombre)) between 1 and 100),
    correo    text not null,
    creado_en timestamptz not null default now()
);

-- La finca registrada en el servidor. Su id es el mismo que usa el teléfono.
create table if not exists public.finca (
    id_finca       uuid primary key,
    id_propietario uuid not null references public.perfil (id_cuenta),
    nombre         text not null check (char_length(btrim(nombre)) between 1 and 100),
    ubicacion      text check (ubicacion is null or char_length(ubicacion) <= 200),
    area_hectareas numeric(10, 2) check (area_hectareas is null or area_hectareas > 0),
    creado_en      timestamptz not null default now()
);
-- En esta etapa cada cuenta tiene una sola finca.
create unique index if not exists finca_un_propietario on public.finca (id_propietario);

-- Quién tiene (o tuvo) acceso a una finca. Retirar un acceso no borra la fila.
create table if not exists public.usuario_finca (
    id_usuario_finca uuid primary key default gen_random_uuid(),
    id_finca         uuid not null references public.finca (id_finca),
    id_cuenta        uuid not null references public.perfil (id_cuenta),
    rol              text not null check (rol in ('ADMINISTRADOR', 'ENCARGADO')),
    estado           text not null default 'ACTIVO' check (estado in ('ACTIVO', 'REVOCADO')),
    fecha_ingreso    timestamptz not null default now(),
    fecha_retiro     timestamptz,
    motivo_retiro    text check (motivo_retiro is null or char_length(motivo_retiro) <= 500),
    check ((estado = 'REVOCADO') = (fecha_retiro is not null))
);
-- Una persona no puede tener dos accesos activos a la misma finca; sí puede volver después de un retiro.
create unique index if not exists usuario_finca_activo on public.usuario_finca (id_finca, id_cuenta) where estado = 'ACTIVO';
create index if not exists usuario_finca_cuenta on public.usuario_finca (id_cuenta);

create table if not exists public.invitacion_finca (
    id_invitacion_finca uuid primary key default gen_random_uuid(),
    id_finca            uuid not null references public.finca (id_finca),
    invitado_por        uuid not null references public.perfil (id_cuenta),
    correo_invitado     text not null,
    estado              text not null default 'PENDIENTE' check (estado in ('PENDIENTE', 'ACEPTADA', 'CANCELADA', 'VENCIDA')),
    creada_en           timestamptz not null default now(),
    vence_en            timestamptz not null default now() + interval '7 days',
    aceptada_por        uuid references public.perfil (id_cuenta)
);
-- Una sola invitación pendiente por finca y correo.
create unique index if not exists invitacion_pendiente_unica on public.invitacion_finca (id_finca, correo_invitado) where estado = 'PENDIENTE';
create index if not exists invitacion_correo on public.invitacion_finca (correo_invitado);

-- ---------------------------------------------------------------------------
-- Nadie toca las tablas directamente
-- ---------------------------------------------------------------------------

alter table public.perfil           enable row level security;
alter table public.finca            enable row level security;
alter table public.usuario_finca    enable row level security;
alter table public.invitacion_finca enable row level security;

revoke all on public.perfil, public.finca, public.usuario_finca, public.invitacion_finca from anon, authenticated;

-- ---------------------------------------------------------------------------
-- Perfil automático al crear la cuenta
-- ---------------------------------------------------------------------------

create or replace function public.crear_perfil() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    insert into public.perfil (id_cuenta, nombre, correo)
    values (new.id,
            left(coalesce(nullif(btrim(new.raw_user_meta_data ->> 'nombre'), ''), split_part(new.email, '@', 1)), 100),
            lower(new.email))
    on conflict (id_cuenta) do nothing;
    return new;
end $$;

drop trigger if exists al_crear_cuenta on auth.users;
create trigger al_crear_cuenta after insert on auth.users
    for each row execute function public.crear_perfil();

-- Cuentas que ya existieran antes de instalar este esquema.
insert into public.perfil (id_cuenta, nombre, correo)
select u.id, left(coalesce(nullif(btrim(u.raw_user_meta_data ->> 'nombre'), ''), split_part(u.email, '@', 1)), 100), lower(u.email)
from auth.users u
on conflict (id_cuenta) do nothing;

-- ---------------------------------------------------------------------------
-- Apoyos internos (la app no puede llamarlos)
-- ---------------------------------------------------------------------------

create or replace function public.cuenta_actual() returns uuid
language plpgsql stable security definer set search_path = '' as $$
declare v_cuenta uuid := auth.uid();
begin
    if v_cuenta is null then raise exception 'sin_sesion'; end if;
    return v_cuenta;
end $$;

-- Correo de la cuenta, solo si ya lo confirmó; si no, 'correo_sin_confirmar'.
create or replace function public.correo_confirmado(p_cuenta uuid) returns text
language plpgsql stable security definer set search_path = '' as $$
declare v_correo text; v_confirmado timestamptz;
begin
    select lower(email), email_confirmed_at into v_correo, v_confirmado from auth.users where id = p_cuenta;
    if v_correo is null or v_confirmado is null then raise exception 'correo_sin_confirmar'; end if;
    return v_correo;
end $$;

create or replace function public.exigir_dueno(p_finca uuid, p_cuenta uuid) returns void
language plpgsql stable security definer set search_path = '' as $$
begin
    if not exists (select 1 from public.finca where id_finca = p_finca and id_propietario = p_cuenta) then
        raise exception 'no_es_dueno';
    end if;
end $$;

revoke execute on function public.cuenta_actual(), public.correo_confirmado(uuid), public.exigir_dueno(uuid, uuid), public.crear_perfil()
    from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- Funciones que usa la app
-- ---------------------------------------------------------------------------

-- Registra la finca del teléfono a nombre de la cuenta. Repetir la llamada con la misma finca no hace nada.
create or replace function public.vincular_finca(p_id_finca uuid, p_nombre text, p_ubicacion text, p_area_hectareas numeric)
returns void language plpgsql security definer set search_path = '' as $$
declare v_cuenta uuid := public.cuenta_actual(); v_dueno uuid;
begin
    select id_propietario into v_dueno from public.finca where id_finca = p_id_finca;
    if found then
        if v_dueno = v_cuenta then return; end if;
        raise exception 'finca_de_otra_cuenta';
    end if;
    if exists (select 1 from public.finca where id_propietario = v_cuenta) then raise exception 'ya_tiene_finca'; end if;
    insert into public.finca (id_finca, id_propietario, nombre, ubicacion, area_hectareas)
    values (p_id_finca, v_cuenta, btrim(p_nombre), nullif(btrim(p_ubicacion), ''), p_area_hectareas);
    insert into public.usuario_finca (id_finca, id_cuenta, rol) values (p_id_finca, v_cuenta, 'ADMINISTRADOR');
end $$;

-- Las fincas a las que pertenece la cuenta (también las que perdió, con estado REVOCADO).
create or replace function public.mis_fincas()
returns table (id_finca uuid, nombre text, rol text, estado text)
language plpgsql stable security definer set search_path = '' as $$
declare v_cuenta uuid := public.cuenta_actual();
begin
    return query
        select f.id_finca, f.nombre, uf.rol, uf.estado
        from public.usuario_finca uf join public.finca f on f.id_finca = uf.id_finca
        where uf.id_cuenta = v_cuenta
        order by uf.fecha_ingreso, uf.id_usuario_finca;
end $$;

-- Invitaciones vigentes dirigidas al correo confirmado de la cuenta.
create or replace function public.mis_invitaciones()
returns table (id_invitacion_finca uuid, id_finca uuid, nombre_finca text, invitado_por text, creada_en timestamptz, vence_en timestamptz)
language plpgsql stable security definer set search_path = '' as $$
declare v_cuenta uuid := public.cuenta_actual(); v_correo text := public.correo_confirmado(v_cuenta);
begin
    return query
        select i.id_invitacion_finca, i.id_finca, f.nombre, p.nombre, i.creada_en, i.vence_en
        from public.invitacion_finca i
        join public.finca f on f.id_finca = i.id_finca
        join public.perfil p on p.id_cuenta = i.invitado_por
        where i.correo_invitado = v_correo and i.estado = 'PENDIENTE' and i.vence_en > now()
        order by i.creada_en;
end $$;

create or replace function public.aceptar_invitacion(p_id_invitacion uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare v_cuenta uuid := public.cuenta_actual(); v_correo text := public.correo_confirmado(v_cuenta); v_inv public.invitacion_finca;
begin
    select * into v_inv from public.invitacion_finca where id_invitacion_finca = p_id_invitacion for update;
    if not found or v_inv.estado <> 'PENDIENTE' or v_inv.vence_en <= now() then raise exception 'invitacion_no_vigente'; end if;
    if v_inv.correo_invitado <> v_correo then raise exception 'correo_no_coincide'; end if;
    if exists (select 1 from public.usuario_finca where id_finca = v_inv.id_finca and id_cuenta = v_cuenta and estado = 'ACTIVO') then
        raise exception 'ya_es_integrante';
    end if;
    insert into public.usuario_finca (id_finca, id_cuenta, rol) values (v_inv.id_finca, v_cuenta, 'ENCARGADO');
    update public.invitacion_finca set estado = 'ACEPTADA', aceptada_por = v_cuenta where id_invitacion_finca = p_id_invitacion;
end $$;

-- Solo el dueño de la finca. Quién tiene acceso y qué invitaciones envió, con las vencidas marcadas.
create or replace function public.integrantes(p_id_finca uuid)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare v_cuenta uuid := public.cuenta_actual();
begin
    perform public.exigir_dueno(p_id_finca, v_cuenta);
    return jsonb_build_object(
        'integrantes', coalesce((
            select jsonb_agg(jsonb_build_object(
                       'id_usuario_finca', uf.id_usuario_finca, 'nombre', p.nombre, 'correo', p.correo, 'rol', uf.rol,
                       'estado', uf.estado, 'fecha_ingreso', uf.fecha_ingreso, 'fecha_retiro', uf.fecha_retiro,
                       'motivo_retiro', uf.motivo_retiro) order by uf.fecha_ingreso, uf.id_usuario_finca)
            from public.usuario_finca uf join public.perfil p on p.id_cuenta = uf.id_cuenta
            where uf.id_finca = p_id_finca), '[]'::jsonb),
        'invitaciones', coalesce((
            select jsonb_agg(jsonb_build_object(
                       'id_invitacion_finca', i.id_invitacion_finca, 'correo_invitado', i.correo_invitado,
                       'estado', case when i.estado = 'PENDIENTE' and i.vence_en <= now() then 'VENCIDA' else i.estado end,
                       'creada_en', i.creada_en, 'vence_en', i.vence_en) order by i.creada_en desc)
            from public.invitacion_finca i where i.id_finca = p_id_finca), '[]'::jsonb));
end $$;

create or replace function public.invitar_encargado(p_id_finca uuid, p_correo text)
returns void language plpgsql security definer set search_path = '' as $$
declare v_cuenta uuid := public.cuenta_actual(); v_correo text := lower(btrim(coalesce(p_correo, '')));
begin
    perform public.exigir_dueno(p_id_finca, v_cuenta);
    if v_correo !~ '^[^\s@]+@[^\s@]+\.[^\s@]+$' or char_length(v_correo) > 150 then raise exception 'correo_invalido'; end if;
    if v_correo = (select lower(email) from auth.users where id = v_cuenta) then raise exception 'correo_propio'; end if;
    if exists (select 1 from public.usuario_finca uf join auth.users u on u.id = uf.id_cuenta
               where uf.id_finca = p_id_finca and uf.estado = 'ACTIVO' and lower(u.email) = v_correo) then
        raise exception 'es_integrante';
    end if;
    -- Una invitación vencida no estorba una nueva.
    update public.invitacion_finca set estado = 'VENCIDA'
    where id_finca = p_id_finca and correo_invitado = v_correo and estado = 'PENDIENTE' and vence_en <= now();
    if exists (select 1 from public.invitacion_finca where id_finca = p_id_finca and correo_invitado = v_correo and estado = 'PENDIENTE') then
        raise exception 'invitacion_repetida';
    end if;
    insert into public.invitacion_finca (id_finca, invitado_por, correo_invitado) values (p_id_finca, v_cuenta, v_correo);
end $$;

create or replace function public.cancelar_invitacion(p_id_invitacion uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare v_cuenta uuid := public.cuenta_actual(); v_inv public.invitacion_finca;
begin
    select * into v_inv from public.invitacion_finca where id_invitacion_finca = p_id_invitacion for update;
    if not found then raise exception 'invitacion_no_vigente'; end if;
    perform public.exigir_dueno(v_inv.id_finca, v_cuenta);
    if v_inv.estado <> 'PENDIENTE' then raise exception 'invitacion_no_vigente'; end if;
    update public.invitacion_finca set estado = 'CANCELADA' where id_invitacion_finca = p_id_invitacion;
end $$;

-- Retira el acceso de un encargado. La fila se conserva con su fecha y su motivo.
create or replace function public.retirar_acceso(p_id_usuario_finca uuid, p_motivo text)
returns void language plpgsql security definer set search_path = '' as $$
declare v_cuenta uuid := public.cuenta_actual(); v_uf public.usuario_finca;
begin
    if p_motivo is null or btrim(p_motivo) = '' then raise exception 'motivo_obligatorio'; end if;
    select * into v_uf from public.usuario_finca where id_usuario_finca = p_id_usuario_finca for update;
    if not found then raise exception 'no_es_dueno'; end if;
    perform public.exigir_dueno(v_uf.id_finca, v_cuenta);
    if v_uf.rol = 'ADMINISTRADOR' then raise exception 'es_administrador'; end if;
    if v_uf.estado <> 'ACTIVO' then raise exception 'ya_retirado'; end if;
    update public.usuario_finca set estado = 'REVOCADO', fecha_retiro = now(), motivo_retiro = btrim(p_motivo)
    where id_usuario_finca = p_id_usuario_finca;
end $$;

-- Solo una sesión iniciada puede llamar a las funciones de la app.
revoke execute on function
    public.vincular_finca(uuid, text, text, numeric), public.mis_fincas(), public.mis_invitaciones(), public.aceptar_invitacion(uuid),
    public.integrantes(uuid), public.invitar_encargado(uuid, text), public.cancelar_invitacion(uuid), public.retirar_acceso(uuid, text)
    from public, anon;
grant execute on function
    public.vincular_finca(uuid, text, text, numeric), public.mis_fincas(), public.mis_invitaciones(), public.aceptar_invitacion(uuid),
    public.integrantes(uuid), public.invitar_encargado(uuid, text), public.cancelar_invitacion(uuid), public.retirar_acceso(uuid, text)
    to authenticated;

-- Que PostgREST reconozca las funciones nuevas sin reiniciar el proyecto.
notify pgrst, 'reload schema';
