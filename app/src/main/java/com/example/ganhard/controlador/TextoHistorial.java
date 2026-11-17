package com.example.ganhard.controlador;

import android.content.Context;
import android.text.TextUtils;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.HistorialResumen;
import com.example.ganhard.modelo.entidad.AnimalIdentificador;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Tarea;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Convierte una entrada del historial en los textos de su tarjeta. Lo que no se reconoce (una clave nueva, un
 * JSON dañado) se muestra tal cual o se omite: el historial es de consulta y nunca debe cerrar la pantalla.
 */
final class TextoHistorial {
    private TextoHistorial() {}

    /** Tipo de cambio: Creación, Edición, Estado o Anulación. */
    static String etiqueta(Context contexto, HistorialCambio cambio) {
        switch (cambio.accion) {
            case HistorialCambio.CREAR: return contexto.getString(R.string.historial_creacion);
            case HistorialCambio.EDITAR: return contexto.getString(R.string.historial_edicion);
            case HistorialCambio.CAMBIAR_ESTADO: return contexto.getString(R.string.historial_estado);
            case HistorialCambio.ANULAR: return contexto.getString(R.string.historial_anulacion);
            default: return contexto.getString(R.string.historial_otro);
        }
    }

    /** "Pesaje corregido · GH-014": qué pasó y a qué animal. */
    static String titulo(Context contexto, HistorialResumen resumen) {
        String hecho = contexto.getString(hecho(resumen.cambio));
        return resumen.animalCodigo == null ? hecho : contexto.getString(R.string.codigo_nombre, hecho, resumen.animalCodigo);
    }

    private static int hecho(HistorialCambio cambio) {
        boolean pesaje = HistorialCambio.PESAJE.equals(cambio.entidad), animal = HistorialCambio.ANIMAL.equals(cambio.entidad),
                actividad = HistorialCambio.EVENTO_SANITARIO.equals(cambio.entidad), gasto = HistorialCambio.GASTO.equals(cambio.entidad),
                tarea = HistorialCambio.TAREA.equals(cambio.entidad), hierro = HistorialCambio.HIERRO.equals(cambio.entidad);
        switch (cambio.accion) {
            case HistorialCambio.CREAR:
                if (pesaje) return R.string.titulo_pesaje_registrado;
                if (animal) return R.string.titulo_animal_registrado;
                if (actividad) return R.string.titulo_sanidad_registrada;
                if (gasto) return R.string.titulo_gasto_registrado;
                if (tarea) return R.string.titulo_tarea_programada;
                if (hierro) return R.string.titulo_hierro_registrado;
                break;
            case HistorialCambio.EDITAR:
                if (pesaje) return R.string.titulo_pesaje_corregido;
                if (animal) return R.string.titulo_animal_editado;
                if (actividad) return R.string.titulo_sanidad_corregida;
                if (gasto) return R.string.titulo_gasto_corregido;
                if (tarea) return R.string.titulo_tarea_corregida;
                if (hierro) return R.string.titulo_hierro_editado;
                break;
            case HistorialCambio.CAMBIAR_ESTADO:
                if (tarea) {
                    JSONObject nuevos = leer(cambio.valoresNuevos);
                    String estado = nuevos == null ? "" : nuevos.optString("estado");
                    if (Tarea.COMPLETADA.equals(estado)) return R.string.titulo_tarea_completada;
                    if (Tarea.CANCELADA.equals(estado)) return R.string.titulo_tarea_cancelada;
                }
                if (hierro) {
                    JSONObject nuevos = leer(cambio.valoresNuevos);
                    return nuevos != null && nuevos.optBoolean("activo", true) ? R.string.titulo_hierro_reactivado : R.string.titulo_hierro_desactivado;
                }
                return R.string.titulo_estado_cambiado;
            case HistorialCambio.ANULAR:
                if (pesaje) return R.string.titulo_pesaje_anulado;
                if (actividad) return R.string.titulo_sanidad_anulada;
                if (gasto) return R.string.titulo_gasto_anulado;
                break;
            default: break;
        }
        return R.string.titulo_cambio;
    }

    /** Instante UTC del registro en la hora del teléfono: "12 oct 2026 · 9:20 a. m.". */
    static String registro(Context contexto, String registradoEn) {
        try {
            LocalDateTime local = Instant.parse(registradoEn).atZone(ZoneId.systemDefault()).toLocalDateTime();
            String hora = String.format(Locale.US, "%02d:%02d", local.getHour(), local.getMinute());
            return FormatoFecha.fechaYHora(local.toLocalDate().toString(), hora, "");
        } catch (DateTimeParseException | NullPointerException ex) {
            return registradoEn == null ? "" : registradoEn;
        }
    }

    /** Líneas de detalle: qué había antes y qué quedó, la medición afectada y el motivo. */
    static List<String> lineas(Context contexto, HistorialCambio cambio, Map<String, String> razas) {
        List<String> lineas = new ArrayList<>();
        JSONObject antes = leer(cambio.valoresAnteriores), ahora = leer(cambio.valoresNuevos);
        boolean pesaje = HistorialCambio.PESAJE.equals(cambio.entidad);
        boolean actividad = HistorialCambio.EVENTO_SANITARIO.equals(cambio.entidad);
        boolean gasto = HistorialCambio.GASTO.equals(cambio.entidad);
        boolean tarea = HistorialCambio.TAREA.equals(cambio.entidad), hierro = HistorialCambio.HIERRO.equals(cambio.entidad);
        switch (cambio.accion) {
            case HistorialCambio.EDITAR:
                if (antes != null && ahora != null) {
                    for (Iterator<String> claves = ahora.keys(); claves.hasNext(); ) {
                        String clave = claves.next();
                        String campo = campo(contexto, clave);
                        lineas.add(contexto.getString(R.string.historial_antes_campo, campo, valor(contexto, clave, antes.opt(clave), razas)));
                        lineas.add(contexto.getString(R.string.historial_ahora_campo, campo, valor(contexto, clave, ahora.opt(clave), razas)));
                    }
                }
                break;
            case HistorialCambio.CAMBIAR_ESTADO:
                if (antes != null && antes.has("estado")) {
                    lineas.add(contexto.getString(R.string.historial_antes, valor(contexto, tarea ? "estado_tarea" : "estado", antes.opt("estado"), razas)));
                }
                if (ahora != null && ahora.has("estado")) {
                    lineas.add(contexto.getString(R.string.historial_ahora, valor(contexto, tarea ? "estado_tarea" : "estado", ahora.opt("estado"), razas)));
                }
                if (cambio.fechaEfectiva != null) lineas.add(contexto.getString(R.string.historial_desde, FormatoFecha.corta(cambio.fechaEfectiva)));
                break;
            case HistorialCambio.ANULAR:
                if (pesaje) medicion(contexto, antes, lineas);
                else if (actividad) actividad(contexto, antes, lineas);
                else if (gasto) gasto(contexto, antes, lineas);
                break;
            case HistorialCambio.CREAR:
                if (pesaje) medicion(contexto, ahora, lineas);
                else if (actividad) actividad(contexto, ahora, lineas);
                else if (gasto) gasto(contexto, ahora, lineas);
                else if (tarea) tarea(contexto, ahora, lineas);
                else if (hierro) hierro(ahora, lineas);
                break;
            default: break;
        }
        if (cambio.motivo != null && !cambio.motivo.isEmpty()) lineas.add(contexto.getString(R.string.historial_motivo, cambio.motivo));
        return lineas;
    }

    /** "450 kg · 1 feb 2026, 8:15 a. m.": la medición a la que se refiere la entrada. */
    private static void medicion(Context contexto, JSONObject datos, List<String> lineas) {
        if (datos == null || !(datos.opt("peso_kg") instanceof Number)) return;
        String hora = datos.isNull("hora") ? null : datos.optString("hora");
        lineas.add(contexto.getString(R.string.anulacion_detalle, Presentacion.kilos(datos.optDouble("peso_kg")),
                FormatoFecha.fechaYHoraConComa(datos.optString("fecha"), hora)));
    }

    /** "Vacunación · 1 mar 2026": la actividad sanitaria a la que se refiere la entrada. */
    private static void actividad(Context contexto, JSONObject datos, List<String> lineas) {
        if (datos == null || datos.isNull("tipo")) return;
        lineas.add(contexto.getString(R.string.codigo_nombre, Presentacion.tipoSanidad(contexto, datos.optString("tipo")),
                FormatoFecha.corta(datos.optString("fecha"))));
    }

    /** "Compra de concentrado · $ 320.000" y "Alimentación · 10 oct 2026": el gasto al que se refiere la entrada. */
    private static void gasto(Context contexto, JSONObject datos, List<String> lineas) {
        if (datos == null || datos.isNull("concepto")) return;
        lineas.add(contexto.getString(R.string.codigo_nombre, datos.optString("concepto"),
                valor(contexto, "valor_centavos", datos.opt("valor_centavos"), Collections.<String, String>emptyMap())));
        lineas.add(contexto.getString(R.string.codigo_nombre, Presentacion.categoriaGasto(contexto, datos.optString("categoria")),
                FormatoFecha.corta(datos.optString("fecha"))));
    }

    /** "Pesaje de Luna" y "Pesaje · 13 oct 2026": la tarea a la que se refiere la entrada. */
    private static void tarea(Context contexto, JSONObject datos, List<String> lineas) {
        if (datos == null || datos.isNull("titulo")) return;
        lineas.add(datos.optString("titulo"));
        lineas.add(contexto.getString(R.string.codigo_nombre, Presentacion.tipoTarea(contexto, datos.optString("tipo")),
                FormatoFecha.corta(datos.optString("fecha_programada"))));
    }

    /** El nombre del hierro y, si tiene, su descripción. */
    private static void hierro(JSONObject datos, List<String> lineas) {
        if (datos == null || datos.isNull("nombre")) return;
        lineas.add(datos.optString("nombre"));
        if (!datos.isNull("descripcion")) lineas.add(datos.optString("descripcion"));
    }

    /** Los identificadores guardados como texto JSON ([{"t":"HIERRO","v":"X"}]) en "Hierro: X · Arete: 1234". */
    private static String identificadores(Context contexto, String json) {
        try {
            JSONArray arreglo = new JSONArray(json);
            List<String> partes = new ArrayList<>();
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject fila = arreglo.getJSONObject(i);
                String tipo = fila.optString("t");
                int etiqueta = AnimalIdentificador.HIERRO.equals(tipo) ? R.string.identificador_tipo_hierro
                        : AnimalIdentificador.ARETE.equals(tipo) ? R.string.identificador_tipo_arete : R.string.identificador_tipo_numero;
                partes.add(contexto.getString(R.string.identificador_linea, contexto.getString(etiqueta), fila.optString("v")));
            }
            return partes.isEmpty() ? contexto.getString(R.string.sin_registrar) : TextUtils.join(" · ", partes);
        } catch (JSONException ex) {
            return json;
        }
    }

    private static JSONObject leer(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return new JSONObject(json); }
        catch (JSONException ex) { return null; }
    }

    /** Nombre visible del campo; una clave que esta versión no conoce se muestra con su nombre. */
    private static String campo(Context contexto, String clave) {
        switch (clave) {
            case "codigo": return contexto.getString(R.string.campo_codigo);
            case "nombre": return contexto.getString(R.string.campo_nombre);
            case "sexo": return contexto.getString(R.string.campo_sexo);
            case "id_raza": return contexto.getString(R.string.campo_raza);
            case "fecha_nacimiento": return contexto.getString(R.string.campo_nacimiento);
            case "fecha_ingreso": return contexto.getString(R.string.campo_ingreso);
            case "observaciones": return contexto.getString(R.string.campo_observaciones);
            case "fecha": return contexto.getString(R.string.campo_fecha);
            case "hora": return contexto.getString(R.string.campo_hora);
            case "peso_kg": return contexto.getString(R.string.campo_peso);
            case "estado": return contexto.getString(R.string.campo_estado);
            case "fecha_estado": return contexto.getString(R.string.campo_fecha_estado);
            case "tipo": return contexto.getString(R.string.campo_tipo);
            case "descripcion": return contexto.getString(R.string.campo_descripcion);
            case "producto": return contexto.getString(R.string.campo_producto);
            case "categoria": return contexto.getString(R.string.campo_categoria);
            case "concepto": return contexto.getString(R.string.campo_concepto);
            case "valor_centavos": return contexto.getString(R.string.campo_valor);
            case "titulo": return contexto.getString(R.string.campo_titulo);
            case "identificadores": return contexto.getString(R.string.campo_identificadores);
            case "fecha_programada": return contexto.getString(R.string.campo_fecha_programada);
            default: return clave;
        }
    }

    /** Valor con el formato de pantalla del campo; lo vacío se muestra como "Sin registrar". */
    private static String valor(Context contexto, String clave, Object valor, Map<String, String> razas) {
        if (valor == null || valor == JSONObject.NULL || String.valueOf(valor).isEmpty()) return contexto.getString(R.string.sin_registrar);
        String texto = String.valueOf(valor);
        switch (clave) {
            case "peso_kg":
                return valor instanceof Number ? contexto.getString(R.string.peso_formato, Presentacion.kilos(((Number) valor).doubleValue())) : texto;
            case "fecha": case "fecha_nacimiento": case "fecha_ingreso": case "fecha_estado": case "fecha_programada": return FormatoFecha.corta(texto);
            case "estado_tarea": return Presentacion.estadoTarea(contexto, texto);
            case "hora": return FormatoFecha.hora12(texto);
            case "sexo": return Presentacion.sexo(contexto, texto);
            case "estado": return Presentacion.estado(contexto, texto);
            case "tipo": return Presentacion.tipoSanidad(contexto, texto);
            case "categoria": return Presentacion.categoriaGasto(contexto, texto);
            case "valor_centavos":
                return valor instanceof Number ? Presentacion.pesos(((Number) valor).longValue()) : texto;
            case "id_raza": return razas.containsKey(texto) ? razas.get(texto) : texto;
            case "identificadores": return identificadores(contexto, texto);
            default: return texto;
        }
    }
}
