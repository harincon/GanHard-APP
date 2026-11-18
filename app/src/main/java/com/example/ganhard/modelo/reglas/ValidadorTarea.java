package com.example.ganhard.modelo.reglas;

import com.example.ganhard.modelo.entidad.Tarea;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Reglas de la tarea (RF12, RF17), en Java puro. Devuelve códigos, no textos. */
public final class ValidadorTarea {
    public static final int TITULO_MAXIMO = 150, DESCRIPCION_MAXIMO = 500, TIPO_PROPIO_MAXIMO = 40;

    private ValidadorTarea() {}

    /** Clave para comparar lo escrito: sin espacios sobrantes, sin tildes y sin distinguir mayúsculas. */
    public static String clave(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    /**
     * El tipo normal (GENERAL, PESAJE o SANIDAD) al que equivale lo escrito o elegido, sin importar mayúsculas ni
     * tildes; nulo si es otro nombre, es decir, un tipo propio.
     */
    public static String tipoNormal(String texto) {
        String clave = clave(texto);
        for (String tipo : Tarea.TIPOS) if (clave.equals(clave(tipo))) return tipo;
        return null;
    }

    /**
     * Una tarea puede programarse para cualquier fecha, pasada o futura. Las de pesaje y sanidad exigen animal; que
     * ese animal exista, sea de la finca y esté activo lo comprueba el repositorio, que es quien lo consulta. El tipo
     * puede ser uno normal o uno propio escrito por la persona (hasta 40 caracteres), que se trata como una tarea general.
     */
    public static Map<String, ErrorCampo> validar(EntradaTarea entrada) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        String normal = tipoNormal(entrada.tipo);
        if (entrada.tipo.isEmpty()) errores.put(Campos.TIPO, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (normal == null && entrada.tipo.length() > TIPO_PROPIO_MAXIMO) {
            errores.put(Campos.TIPO, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, TIPO_PROPIO_MAXIMO));
        }
        if ((Tarea.PESAJE.equals(normal) || Tarea.SANIDAD.equals(normal)) && entrada.idAnimal.isEmpty()) {
            errores.put(Campos.ID_ANIMAL, new ErrorCampo(CodigoError.OBLIGATORIO));
        }
        if (entrada.titulo.isEmpty()) errores.put(Campos.TITULO, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (entrada.titulo.length() > TITULO_MAXIMO) errores.put(Campos.TITULO, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, TITULO_MAXIMO));
        if (entrada.fechaProgramada.isEmpty()) errores.put(Campos.FECHA_PROGRAMADA, new ErrorCampo(CodigoError.OBLIGATORIO));
        else if (Fechas.fecha(entrada.fechaProgramada) == null) errores.put(Campos.FECHA_PROGRAMADA, new ErrorCampo(CodigoError.FORMATO));
        if (entrada.descripcion.length() > DESCRIPCION_MAXIMO) {
            errores.put(Campos.DESCRIPCION, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, DESCRIPCION_MAXIMO));
        }
        return errores;
    }

    /** Fecha real de finalización de una tarea general: obligatoria, válida y no futura. Null si es válida. */
    public static ErrorCampo fechaCompletada(String fecha, LocalDate hoy) {
        String texto = fecha == null ? "" : fecha.trim();
        if (texto.isEmpty()) return new ErrorCampo(CodigoError.OBLIGATORIO);
        LocalDate valor = Fechas.fecha(texto);
        if (valor == null) return new ErrorCampo(CodigoError.FORMATO);
        if (valor.isAfter(hoy)) return new ErrorCampo(CodigoError.FECHA_FUTURA);
        return null;
    }
}
