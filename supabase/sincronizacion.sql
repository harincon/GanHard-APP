-- =============================================================================
-- GanHard · Etapa 6.1 · Respaldo en la nube (subir los datos de la finca)
--
-- Cómo usarlo: Supabase → SQL Editor → New query → pegar TODO este archivo → Run.
-- Antes hay que haber ejecutado `esquema.sql` (usa sus tablas y funciones de cuentas).
-- Se puede ejecutar más de una vez sin dañar lo que ya existe.
--
-- Igual que en `esquema.sql`: las tablas NO se pueden leer ni escribir desde la app (RLS activo, sin políticas y sin
-- permisos); la app solo llama a `subir_cambios`, que comprueba quién es la cuenta (auth.uid()) y que tenga acceso
-- ACTIVO a la finca. Los mensajes de RAISE EXCEPTION son códigos que la app traduce a textos; si cambias uno, cambia
-- también ErrorRemoto.java y MensajesRemotos.java.
--
-- Regla de conflictos: gana el último cambio. Una fila que ya existe solo se reemplaza si la que llega tiene
-- `actualizado_en` MAYOR. El historial y las razas nunca se reemplazan.
--
-- Los ids son texto (no uuid): así el servidor acepta cualquier id que haya generado el teléfono.
-- Las columnas son las mismas de las tablas del teléfono (app/schemas/.../11.json).
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Tablas (copia de lo que hay en el teléfono)
-- ---------------------------------------------------------------------------

create table if not exists public.datos_raza (
    id_raza     text primary key,
    nombre      text not null,
    descripcion text
);

create table if not exists public.datos_hierro (
    id_hierro      text primary key,
    id_finca       text not null,
    nombre         text not null,
    descripcion    text,
    activo         integer not null,
    creado_en      text not null,
    actualizado_en text not null,
    version        integer not null
);

create table if not exists public.datos_animal (
    id_animal         text primary key,
    id_finca          text not null,
    id_raza           text,
    codigo            text not null,
    nombre            text,
    sexo              text not null,
    fecha_nacimiento  text,
    fecha_ingreso     text not null,
    estado            text not null,
    fecha_estado      text not null,
    observaciones     text,
    creado_en         text not null,
    actualizado_en    text not null,
    version           integer not null
);

-- Esta tabla y las dos siguientes no traen la finca en el teléfono: el servidor la completa con la del animal.
create table if not exists public.datos_animal_identificador (
    id_animal_identificador text primary key,
    id_animal               text not null,
    id_finca                text not null,
    tipo                    text not null,
    id_hierro               text,
    valor                   text,
    orden                   integer not null,
    creado_en               text not null,
    actualizado_en          text not null
);

create table if not exists public.datos_tarea (
    id_tarea          text primary key,
    id_finca          text not null,
    id_animal         text,
    id_grupo          text,
    tipo              text not null,
    etiqueta          text,
    titulo            text not null,
    descripcion       text,
    fecha_programada  text not null,
    estado            text not null,
    fecha_completada  text,
    creado_en         text not null,
    actualizado_en    text not null,
    version           integer not null
);

create table if not exists public.datos_pesaje (
    id_pesaje        text primary key,
    id_animal        text not null,
    id_finca         text not null,
    fecha            text not null,
    hora             text,
    peso_kg          double precision not null,
    es_antecedente   integer not null,
    observaciones    text,
    anulado          integer not null,
    anulado_en       text,
    motivo_anulacion text,
    creado_en        text not null,
    actualizado_en   text not null,
    version          integer not null,
    id_tarea         text
);

create table if not exists public.datos_evento_sanitario (
    id_evento_sanitario text primary key,
    id_animal           text not null,
    id_finca            text not null,
    tipo                text not null,
    fecha               text not null,
    descripcion         text not null,
    producto            text,
    observaciones       text,
    es_antecedente      integer not null,
    anulado             integer not null,
    anulado_en          text,
    motivo_anulacion    text,
    creado_en           text not null,
    actualizado_en      text not null,
    version             integer not null,
    id_tarea            text,
    id_grupo            text
);

create table if not exists public.datos_gasto (
    id_gasto         text primary key,
    id_finca         text not null,
    fecha            text not null,
    categoria        text not null,
    concepto         text not null,
    valor_centavos   bigint not null,
    anulado          integer not null,
    anulado_en       text,
    motivo_anulacion text,
    creado_en        text not null,
    actualizado_en   text not null,
    version          integer not null
);

create table if not exists public.datos_historial_cambio (
    id_historial_cambio text primary key,
    id_usuario          text not null,
    id_finca            text,
    entidad             text not null,
    id_registro         text not null,
    accion              text not null,
    registrado_en       text not null,
    recibido_en         text,
    fecha_efectiva      text,
    motivo              text,
    valores_anteriores  text,
    valores_nuevos      text not null
);

-- Para cuando se bajen los datos a otro teléfono (etapa 6.2): leer por finca y por fecha de cambio.
create index if not exists datos_hierro_finca on public.datos_hierro (id_finca, actualizado_en);
create index if not exists datos_animal_finca on public.datos_animal (id_finca, actualizado_en);
create index if not exists datos_animal_identificador_finca on public.datos_animal_identificador (id_finca, actualizado_en);
create index if not exists datos_tarea_finca on public.datos_tarea (id_finca, actualizado_en);
create index if not exists datos_pesaje_finca on public.datos_pesaje (id_finca, actualizado_en);
create index if not exists datos_evento_sanitario_finca on public.datos_evento_sanitario (id_finca, actualizado_en);
create index if not exists datos_gasto_finca on public.datos_gasto (id_finca, actualizado_en);
create index if not exists datos_historial_cambio_finca on public.datos_historial_cambio (id_finca, registrado_en);

-- ---------------------------------------------------------------------------
-- Nadie toca las tablas directamente
-- ---------------------------------------------------------------------------

alter table public.datos_raza                 enable row level security;
alter table public.datos_hierro               enable row level security;
alter table public.datos_animal               enable row level security;
alter table public.datos_animal_identificador enable row level security;
alter table public.datos_tarea                enable row level security;
alter table public.datos_pesaje               enable row level security;
alter table public.datos_evento_sanitario     enable row level security;
alter table public.datos_gasto                enable row level security;
alter table public.datos_historial_cambio     enable row level security;

revoke all on public.datos_raza, public.datos_hierro, public.datos_animal, public.datos_animal_identificador, public.datos_tarea,
    public.datos_pesaje, public.datos_evento_sanitario, public.datos_gasto, public.datos_historial_cambio from anon, authenticated;

-- ---------------------------------------------------------------------------
-- La función que usa la app
-- ---------------------------------------------------------------------------

-- Recibe hasta 500 filas de UNA tabla de la finca y devuelve cuántas aplicó. Todo o nada: si una fila no sirve,
-- no se guarda ninguna del lote.
create or replace function public.subir_cambios(p_id_finca text, p_tabla text, p_filas jsonb)
returns integer language plpgsql security definer set search_path = '' as $$
declare
    v_cuenta uuid := public.cuenta_actual();
    v_aplicadas integer := 0;
begin
    -- Solo el dueño o un encargado con acceso ACTIVO a esa finca.
    if not exists (select 1 from public.usuario_finca uf
                   where uf.id_finca::text = p_id_finca and uf.id_cuenta = v_cuenta and uf.estado = 'ACTIVO') then
        raise exception 'no_es_integrante';
    end if;
    if p_tabla is null or p_tabla not in ('raza', 'hierro', 'animal', 'animal_identificador', 'tarea', 'pesaje',
                                          'evento_sanitario', 'gasto', 'historial_cambio') then
        raise exception 'tabla_invalida';
    end if;
    if p_filas is null or jsonb_typeof(p_filas) <> 'array' then raise exception 'filas_invalidas'; end if;
    if jsonb_array_length(p_filas) > 500 then raise exception 'demasiadas_filas'; end if;
    if jsonb_array_length(p_filas) = 0 then return 0; end if;

    if p_tabla = 'raza' then
        -- El catálogo no cambia: lo que ya existe se ignora.
        insert into public.datos_raza
        select r.* from jsonb_populate_recordset(null::public.datos_raza, p_filas) r
        on conflict (id_raza) do nothing;
        get diagnostics v_aplicadas = row_count;

    elsif p_tabla = 'hierro' then
        if exists (select 1 from jsonb_populate_recordset(null::public.datos_hierro, p_filas) r where r.id_finca is distinct from p_id_finca) then
            raise exception 'fila_de_otra_finca';
        end if;
        insert into public.datos_hierro
        select r.* from jsonb_populate_recordset(null::public.datos_hierro, p_filas) r
        on conflict (id_hierro) do update set
            nombre = excluded.nombre, descripcion = excluded.descripcion, activo = excluded.activo,
            creado_en = excluded.creado_en, actualizado_en = excluded.actualizado_en, version = excluded.version
        where excluded.actualizado_en > public.datos_hierro.actualizado_en and public.datos_hierro.id_finca = excluded.id_finca;
        get diagnostics v_aplicadas = row_count;

    elsif p_tabla = 'animal' then
        if exists (select 1 from jsonb_populate_recordset(null::public.datos_animal, p_filas) r where r.id_finca is distinct from p_id_finca) then
            raise exception 'fila_de_otra_finca';
        end if;
        insert into public.datos_animal
        select r.* from jsonb_populate_recordset(null::public.datos_animal, p_filas) r
        on conflict (id_animal) do update set
            id_raza = excluded.id_raza, codigo = excluded.codigo, nombre = excluded.nombre, sexo = excluded.sexo,
            fecha_nacimiento = excluded.fecha_nacimiento, fecha_ingreso = excluded.fecha_ingreso, estado = excluded.estado,
            fecha_estado = excluded.fecha_estado, observaciones = excluded.observaciones, creado_en = excluded.creado_en,
            actualizado_en = excluded.actualizado_en, version = excluded.version
        where excluded.actualizado_en > public.datos_animal.actualizado_en and public.datos_animal.id_finca = excluded.id_finca;
        get diagnostics v_aplicadas = row_count;

    elsif p_tabla = 'animal_identificador' then
        -- Cada animal debe ser de esta finca (por eso los animales se suben antes).
        if exists (select 1 from jsonb_populate_recordset(null::public.datos_animal_identificador, p_filas) r
                   where not exists (select 1 from public.datos_animal a where a.id_animal = r.id_animal and a.id_finca = p_id_finca)) then
            raise exception 'fila_de_otra_finca';
        end if;
        insert into public.datos_animal_identificador (id_animal_identificador, id_animal, id_finca, tipo, id_hierro, valor, orden, creado_en, actualizado_en)
        select r.id_animal_identificador, r.id_animal, p_id_finca, r.tipo, r.id_hierro, r.valor, r.orden, r.creado_en, r.actualizado_en
        from jsonb_populate_recordset(null::public.datos_animal_identificador, p_filas) r
        on conflict (id_animal_identificador) do update set
            id_animal = excluded.id_animal, tipo = excluded.tipo, id_hierro = excluded.id_hierro, valor = excluded.valor,
            orden = excluded.orden, creado_en = excluded.creado_en, actualizado_en = excluded.actualizado_en
        where excluded.actualizado_en > public.datos_animal_identificador.actualizado_en
          and public.datos_animal_identificador.id_finca = excluded.id_finca;
        get diagnostics v_aplicadas = row_count;

    elsif p_tabla = 'tarea' then
        if exists (select 1 from jsonb_populate_recordset(null::public.datos_tarea, p_filas) r where r.id_finca is distinct from p_id_finca) then
            raise exception 'fila_de_otra_finca';
        end if;
        insert into public.datos_tarea
        select r.* from jsonb_populate_recordset(null::public.datos_tarea, p_filas) r
        on conflict (id_tarea) do update set
            id_animal = excluded.id_animal, id_grupo = excluded.id_grupo, tipo = excluded.tipo, etiqueta = excluded.etiqueta,
            titulo = excluded.titulo, descripcion = excluded.descripcion, fecha_programada = excluded.fecha_programada,
            estado = excluded.estado, fecha_completada = excluded.fecha_completada, creado_en = excluded.creado_en,
            actualizado_en = excluded.actualizado_en, version = excluded.version
        where excluded.actualizado_en > public.datos_tarea.actualizado_en and public.datos_tarea.id_finca = excluded.id_finca;
        get diagnostics v_aplicadas = row_count;

    elsif p_tabla = 'pesaje' then
        if exists (select 1 from jsonb_populate_recordset(null::public.datos_pesaje, p_filas) r
                   where not exists (select 1 from public.datos_animal a where a.id_animal = r.id_animal and a.id_finca = p_id_finca)) then
            raise exception 'fila_de_otra_finca';
        end if;
        insert into public.datos_pesaje (id_pesaje, id_animal, id_finca, fecha, hora, peso_kg, es_antecedente, observaciones, anulado,
                                         anulado_en, motivo_anulacion, creado_en, actualizado_en, version, id_tarea)
        select r.id_pesaje, r.id_animal, p_id_finca, r.fecha, r.hora, r.peso_kg, r.es_antecedente, r.observaciones, r.anulado,
               r.anulado_en, r.motivo_anulacion, r.creado_en, r.actualizado_en, r.version, r.id_tarea
        from jsonb_populate_recordset(null::public.datos_pesaje, p_filas) r
        on conflict (id_pesaje) do update set
            id_animal = excluded.id_animal, fecha = excluded.fecha, hora = excluded.hora, peso_kg = excluded.peso_kg,
            es_antecedente = excluded.es_antecedente, observaciones = excluded.observaciones, anulado = excluded.anulado,
            anulado_en = excluded.anulado_en, motivo_anulacion = excluded.motivo_anulacion, creado_en = excluded.creado_en,
            actualizado_en = excluded.actualizado_en, version = excluded.version, id_tarea = excluded.id_tarea
        where excluded.actualizado_en > public.datos_pesaje.actualizado_en and public.datos_pesaje.id_finca = excluded.id_finca;
        get diagnostics v_aplicadas = row_count;

    elsif p_tabla = 'evento_sanitario' then
        if exists (select 1 from jsonb_populate_recordset(null::public.datos_evento_sanitario, p_filas) r
                   where not exists (select 1 from public.datos_animal a where a.id_animal = r.id_animal and a.id_finca = p_id_finca)) then
            raise exception 'fila_de_otra_finca';
        end if;
        insert into public.datos_evento_sanitario (id_evento_sanitario, id_animal, id_finca, tipo, fecha, descripcion, producto,
                                                   observaciones, es_antecedente, anulado, anulado_en, motivo_anulacion, creado_en,
                                                   actualizado_en, version, id_tarea, id_grupo)
        select r.id_evento_sanitario, r.id_animal, p_id_finca, r.tipo, r.fecha, r.descripcion, r.producto, r.observaciones,
               r.es_antecedente, r.anulado, r.anulado_en, r.motivo_anulacion, r.creado_en, r.actualizado_en, r.version,
               r.id_tarea, r.id_grupo
        from jsonb_populate_recordset(null::public.datos_evento_sanitario, p_filas) r
        on conflict (id_evento_sanitario) do update set
            id_animal = excluded.id_animal, tipo = excluded.tipo, fecha = excluded.fecha, descripcion = excluded.descripcion,
            producto = excluded.producto, observaciones = excluded.observaciones, es_antecedente = excluded.es_antecedente,
            anulado = excluded.anulado, anulado_en = excluded.anulado_en, motivo_anulacion = excluded.motivo_anulacion,
            creado_en = excluded.creado_en, actualizado_en = excluded.actualizado_en, version = excluded.version,
            id_tarea = excluded.id_tarea, id_grupo = excluded.id_grupo
        where excluded.actualizado_en > public.datos_evento_sanitario.actualizado_en
          and public.datos_evento_sanitario.id_finca = excluded.id_finca;
        get diagnostics v_aplicadas = row_count;

    elsif p_tabla = 'gasto' then
        if exists (select 1 from jsonb_populate_recordset(null::public.datos_gasto, p_filas) r where r.id_finca is distinct from p_id_finca) then
            raise exception 'fila_de_otra_finca';
        end if;
        insert into public.datos_gasto
        select r.* from jsonb_populate_recordset(null::public.datos_gasto, p_filas) r
        on conflict (id_gasto) do update set
            fecha = excluded.fecha, categoria = excluded.categoria, concepto = excluded.concepto,
            valor_centavos = excluded.valor_centavos, anulado = excluded.anulado, anulado_en = excluded.anulado_en,
            motivo_anulacion = excluded.motivo_anulacion, creado_en = excluded.creado_en,
            actualizado_en = excluded.actualizado_en, version = excluded.version
        where excluded.actualizado_en > public.datos_gasto.actualizado_en and public.datos_gasto.id_finca = excluded.id_finca;
        get diagnostics v_aplicadas = row_count;

    else
        -- historial_cambio: solo se agrega; lo que ya existe se ignora.
        if exists (select 1 from jsonb_populate_recordset(null::public.datos_historial_cambio, p_filas) r where r.id_finca is distinct from p_id_finca) then
            raise exception 'fila_de_otra_finca';
        end if;
        insert into public.datos_historial_cambio
        select r.* from jsonb_populate_recordset(null::public.datos_historial_cambio, p_filas) r
        on conflict (id_historial_cambio) do nothing;
        get diagnostics v_aplicadas = row_count;
    end if;

    return v_aplicadas;
end $$;

-- Solo una sesión iniciada puede llamarla.
revoke execute on function public.subir_cambios(text, text, jsonb) from public, anon;
grant execute on function public.subir_cambios(text, text, jsonb) to authenticated;

-- Que PostgREST reconozca la función nueva sin reiniciar el proyecto.
notify pgrst, 'reload schema';
