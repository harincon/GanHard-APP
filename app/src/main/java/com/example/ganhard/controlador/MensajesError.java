package com.example.ganhard.controlador;

import android.content.Context;
import com.example.ganhard.R;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.ErrorCampo;

/** Traduce los códigos de error del Modelo a los textos de la Vista (recursos, no cadenas escritas en código). */
public final class MensajesError {
    private MensajesError() {}

    /** Texto genérico del código; los que llevan límite lo incluyen. */
    public static String texto(Context contexto, ErrorCampo error) {
        switch (error.codigo) {
            case OBLIGATORIO: return contexto.getString(R.string.error_obligatorio);
            case LONGITUD_MAXIMA: return contexto.getString(R.string.error_longitud_maxima, error.limite);
            case FORMATO: return contexto.getString(R.string.error_formato);
            case VALOR_INVALIDO: return contexto.getString(R.string.error_valor_invalido);
            case FUERA_DE_RANGO: return contexto.getString(R.string.error_fuera_de_rango);
            case FECHA_FUTURA: return contexto.getString(R.string.error_fecha_futura);
            case FECHA_ANTERIOR: return contexto.getString(R.string.error_fecha_anterior);
            case DUPLICADO: return contexto.getString(R.string.error_duplicado);
            default: return contexto.getString(R.string.error_valor_invalido);
        }
    }

    /** Texto del campo: usa la indicación específica cuando existe y, si no, la genérica del código. */
    public static String texto(Context contexto, String campo, ErrorCampo error) {
        if (error.codigo == CodigoError.OBLIGATORIO) {
            if ("nombre".equals(campo)) return contexto.getString(R.string.error_nombre_obligatorio);
            if ("correo".equals(campo)) return contexto.getString(R.string.error_correo_obligatorio);
            if ("finca".equals(campo)) return contexto.getString(R.string.error_finca_obligatoria);
            if ("codigo".equals(campo)) return contexto.getString(R.string.error_codigo_obligatorio);
            if ("sexo".equals(campo)) return contexto.getString(R.string.error_sexo_obligatorio);
            if ("fechaIngreso".equals(campo)) return contexto.getString(R.string.error_ingreso_obligatorio);
        }
        if (error.codigo == CodigoError.DUPLICADO && "codigo".equals(campo)) return contexto.getString(R.string.error_codigo_duplicado);
        if (error.codigo == CodigoError.FORMATO) {
            if ("correo".equals(campo)) return contexto.getString(R.string.error_correo_formato);
            if ("fechaNacimiento".equals(campo) || "fechaIngreso".equals(campo)) return contexto.getString(R.string.error_fecha_formato);
        }
        if (error.codigo == CodigoError.VALOR_INVALIDO && "area".equals(campo)) return contexto.getString(R.string.error_area_invalida);
        return texto(contexto, error);
    }
}
