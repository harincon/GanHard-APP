-- =============================================================================
-- GanHard · Etapas 6.2 y 6.3 · Bajar los datos al teléfono y varias personas en la misma finca
--
-- Cómo usarlo: Supabase → SQL Editor → New query → pegar TODO este archivo → Run.
-- Antes hay que haber ejecutado `esquema.sql` y `sincronizacion.sql`.
-- Se puede ejecutar más de una vez sin dañar lo que ya existe.
--
-- Igual que en los demás archivos: las tablas siguen cerradas; la app solo llama a estas funciones, que comprueban
-- quién es la cuenta (auth.uid()) y que tenga acceso ACTIVO a la finca (la dueña o una persona encargada).
-- Los mensajes de RAISE EXCEPTION son códigos que la app traduce a textos.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Los datos de la finca y de las personas que la usan
-- ---------------------------------------------------------------------------

-- Con esto un teléfono vacío arma la finca local y conoce a las personas (nombre y correo) para mostrar quién hizo cada
-- cambio. Incluye también a quien ya perdió el acceso, porque sus cambios siguen en el historial.
create or replace function public.datos_de_finca(p_id_finca text)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
    v_cuenta uuid := public.cuenta_actual();
    v_finca public.finca;
begin
    if not exists (select 1 from public.usuario_finca uf
                   where uf.id_finca::text = p_id_finca and uf.id_cuenta = v_cuenta and uf.estado = 'ACTIVO') then
        raise exception 'no_es_integrante';
    end if;
    select * into v_finca from public.finca f where f.id_finca::text = p_id_finca;
    return jsonb_build_object(
        'finca', jsonb_build_object(
            'id_finca', v_finca.id_finca, 'nombre', v_finca.nombre, 'ubicacion', v_finca.ubicacion,
            'area_hectareas', v_finca.area_hectareas, 'id_propietario', v_finca.id_propietario,
            'creado_en', to_char(v_finca.creado_en at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')),
        'personas', coalesce((
            select jsonb_agg(jsonb_build_object(
                       'id_cuenta', uf.id_cuenta, 'nombre', p.nombre, 'correo', p.correo, 'rol', uf.rol, 'estado', uf.estado)
                   order by uf.fecha_ingreso, uf.id_usuario_finca)
            from public.usuario_finca uf join public.perfil p on p.id_cuenta = uf.id_cuenta
            where uf.id_finca::text = p_id_finca), '[]'::jsonb));
end $$;

-- ---------------------------------------------------------------------------
-- Bajar lo que cambió
-- ---------------------------------------------------------------------------

-- Devuelve hasta 500 filas de UNA tabla de la finca POSTERIORES a la marca (fecha e id), ordenadas por fecha e id.
-- Sin marca (nulos) trae desde el principio. Las razas no pertenecen a una finca y se piden por id.
-- Las fechas son texto ISO (AAAA-MM-DDTHH:MM:SS.mmmZ), así que comparar como texto es comparar en el tiempo.
create or replace function public.bajar_cambios(p_id_finca text, p_tabla text, p_desde_en text, p_desde_id text, p_limite integer)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
    v_cuenta uuid := public.cuenta_actual();
    v_limite integer := least(greatest(coalesce(p_limite, 500), 1), 500);
    v_filas jsonb;
begin
    if not exists (select 1 from public.usuario_finca uf
                   where uf.id_finca::text = p_id_finca and uf.id_cuenta = v_cuenta and uf.estado = 'ACTIVO') then
        raise exception 'no_es_integrante';
    end if;
    if p_tabla is null or p_tabla not in ('raza', 'hierro', 'animal', 'animal_identificador', 'tarea', 'pesaje',
                                          'evento_sanitario', 'gasto', 'historial_cambio') then
        raise exception 'tabla_invalida';
    end if;

    if p_tabla = 'raza' then
        select coalesce(jsonb_agg(to_jsonb(t) order by t.id_raza), '[]'::jsonb) into v_filas
        from (select * from public.datos_raza where p_desde_id is null or id_raza > p_desde_id order by id_raza limit v_limite) t;

    elsif p_tabla = 'hierro' then
        select coalesce(jsonb_agg(to_jsonb(t) order by t.actualizado_en, t.id_hierro), '[]'::jsonb) into v_filas
        from (select * from public.datos_hierro
              where id_finca = p_id_finca and (p_desde_en is null or actualizado_en > p_desde_en
                                               or (actualizado_en = p_desde_en and id_hierro > p_desde_id))
              order by actualizado_en, id_hierro limit v_limite) t;

    elsif p_tabla = 'animal' then
        select coalesce(jsonb_agg(to_jsonb(t) order by t.actualizado_en, t.id_animal), '[]'::jsonb) into v_filas
        from (select * from public.datos_animal
              where id_finca = p_id_finca and (p_desde_en is null or actualizado_en > p_desde_en
                                               or (actualizado_en = p_desde_en and id_animal > p_desde_id))
              order by actualizado_en, id_animal limit v_limite) t;

    elsif p_tabla = 'animal_identificador' then
        select coalesce(jsonb_agg(to_jsonb(t) order by t.actualizado_en, t.id_animal_identificador), '[]'::jsonb) into v_filas
        from (select * from public.datos_animal_identificador
              where id_finca = p_id_finca and (p_desde_en is null or actualizado_en > p_desde_en
                                               or (actualizado_en = p_desde_en and id_animal_identificador > p_desde_id))
              order by actualizado_en, id_animal_identificador limit v_limite) t;

    elsif p_tabla = 'tarea' then
        select coalesce(jsonb_agg(to_jsonb(t) order by t.actualizado_en, t.id_tarea), '[]'::jsonb) into v_filas
        from (select * from public.datos_tarea
              where id_finca = p_id_finca and (p_desde_en is null or actualizado_en > p_desde_en
                                               or (actualizado_en = p_desde_en and id_tarea > p_desde_id))
              order by actualizado_en, id_tarea limit v_limite) t;

    elsif p_tabla = 'pesaje' then
        select coalesce(jsonb_agg(to_jsonb(t) order by t.actualizado_en, t.id_pesaje), '[]'::jsonb) into v_filas
        from (select * from public.datos_pesaje
              where id_finca = p_id_finca and (p_desde_en is null or actualizado_en > p_desde_en
                                               or (actualizado_en = p_desde_en and id_pesaje > p_desde_id))
              order by actualizado_en, id_pesaje limit v_limite) t;

    elsif p_tabla = 'evento_sanitario' then
        select coalesce(jsonb_agg(to_jsonb(t) order by t.actualizado_en, t.id_evento_sanitario), '[]'::jsonb) into v_filas
        from (select * from public.datos_evento_sanitario
              where id_finca = p_id_finca and (p_desde_en is null or actualizado_en > p_desde_en
                                               or (actualizado_en = p_desde_en and id_evento_sanitario > p_desde_id))
              order by actualizado_en, id_evento_sanitario limit v_limite) t;

    elsif p_tabla = 'gasto' then
        select coalesce(jsonb_agg(to_jsonb(t) order by t.actualizado_en, t.id_gasto), '[]'::jsonb) into v_filas
        from (select * from public.datos_gasto
              where id_finca = p_id_finca and (p_desde_en is null or actualizado_en > p_desde_en
                                               or (actualizado_en = p_desde_en and id_gasto > p_desde_id))
              order by actualizado_en, id_gasto limit v_limite) t;

    else
        select coalesce(jsonb_agg(to_jsonb(t) order by t.registrado_en, t.id_historial_cambio), '[]'::jsonb) into v_filas
        from (select * from public.datos_historial_cambio
              where id_finca = p_id_finca and (p_desde_en is null or registrado_en > p_desde_en
                                               or (registrado_en = p_desde_en and id_historial_cambio > p_desde_id))
              order by registrado_en, id_historial_cambio limit v_limite) t;
    end if;

    return v_filas;
end $$;

-- Solo una sesión iniciada puede llamarlas.
revoke execute on function public.datos_de_finca(text), public.bajar_cambios(text, text, text, text, integer) from public, anon;
grant execute on function public.datos_de_finca(text), public.bajar_cambios(text, text, text, text, integer) to authenticated;

-- Que PostgREST reconozca las funciones nuevas sin reiniciar el proyecto.
notify pgrst, 'reload schema';
