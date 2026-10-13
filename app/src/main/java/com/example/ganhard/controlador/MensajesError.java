package com.example.ganhard.controlador;

import android.content.Context;
import com.example.ganhard.R;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorPesaje;

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
        String especifico = especifico(contexto, campo, error);
        return especifico != null ? especifico : texto(contexto, error);
    }

    private static String especifico(Context contexto, String campo, ErrorCampo error) {
        CodigoError codigo = error.codigo;
        switch (campo) {
            case "nombre": if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_nombre_obligatorio); break;
            case "finca": if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_finca_obligatoria); break;
            case "correo":
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_correo_obligatorio);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_correo_formato);
                break;
            case "area": if (codigo == CodigoError.VALOR_INVALIDO) return contexto.getString(R.string.error_area_invalida); break;
            case "codigo":
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_codigo_obligatorio);
                if (codigo == CodigoError.DUPLICADO) return contexto.getString(R.string.error_codigo_duplicado);
                break;
            case "sexo": if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_sexo_obligatorio); break;
            case "fechaIngreso":
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_ingreso_obligatorio);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato);
                break;
            case "fechaNacimiento": if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato); break;
            case "fecha":
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_fecha_pesaje_obligatoria);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato);
                if (codigo == CodigoError.FECHA_ANTERIOR) return contexto.getString(R.string.error_fecha_antes_nacimiento);
                break;
            case "hora":
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_hora_obligatoria);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_hora_formato);
                if (codigo == CodigoError.FECHA_FUTURA) return contexto.getString(R.string.error_hora_futura);
                break;
            case "fechaEstado":
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato);
                if (codigo == CodigoError.FECHA_ANTERIOR) return contexto.getString(R.string.error_estado_antes_ingreso);
                break;
            case "peso":
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_peso_obligatorio);
                if (codigo == CodigoError.VALOR_INVALIDO) return contexto.getString(R.string.error_peso_invalido);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_peso_decimales);
                if (codigo == CodigoError.FUERA_DE_RANGO) return contexto.getString(R.string.error_peso_fuera_rango, ValidadorPesaje.PESO_MAXIMO_KG);
                break;
            default: break;
        }
        return null;
    }
}
