package com.example.ganhard.controlador;

import android.content.Context;
import com.example.ganhard.R;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorIdentificadores;
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
            case FECHA_POSTERIOR: return contexto.getString(R.string.error_fecha_posterior);
            case DUPLICADO: return contexto.getString(R.string.error_duplicado);
            case SIN_CAMBIOS: return contexto.getString(R.string.error_sin_cambios);
            case NO_PENDIENTE: return contexto.getString(R.string.error_no_pendiente);
            case ANIMAL_INACTIVO: return contexto.getString(R.string.error_animal_inactivo);
            case LONGITUD_MINIMA: return contexto.getString(R.string.error_longitud_minima, error.limite);
            case NO_COINCIDE: return contexto.getString(R.string.error_no_coincide);
            default: return contexto.getString(R.string.error_valor_invalido);
        }
    }

    /** Mensaje general de los errores de la lista de identificadores del animal. */
    public static String identificadores(Context contexto, ErrorCampo error) {
        if (error.codigo == CodigoError.LONGITUD_MAXIMA && error.limite == ValidadorIdentificadores.MAXIMO) {
            return contexto.getString(R.string.error_identificadores_maximo, error.limite);
        }
        if (error.codigo == CodigoError.LONGITUD_MAXIMA) return contexto.getString(R.string.error_identificador_largo, error.limite);
        if (error.codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_identificador_valor);
        return contexto.getString(R.string.error_identificador_invalido);
    }

    /** Texto del campo: usa la indicación específica cuando existe y, si no, la genérica del código. */
    public static String texto(Context contexto, String campo, ErrorCampo error) {
        String especifico = especifico(contexto, campo, error);
        return especifico != null ? especifico : texto(contexto, error);
    }

    private static String especifico(Context contexto, String campo, ErrorCampo error) {
        CodigoError codigo = error.codigo;
        switch (campo) {
            case Campos.NOMBRE: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_nombre_obligatorio); break;
            case Campos.FINCA: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_finca_obligatoria); break;
            case Campos.CORREO:
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_correo_obligatorio);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_correo_formato);
                break;
            case Campos.AREA: if (codigo == CodigoError.VALOR_INVALIDO) return contexto.getString(R.string.error_area_invalida); break;
            case Campos.CODIGO:
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_codigo_obligatorio);
                if (codigo == CodigoError.DUPLICADO) return contexto.getString(R.string.error_codigo_duplicado);
                break;
            case Campos.SEXO: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_sexo_obligatorio); break;
            case Campos.FECHA_INGRESO:
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_ingreso_obligatorio);
                if (codigo == CodigoError.FECHA_POSTERIOR) return contexto.getString(R.string.error_ingreso_despues_estado);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato);
                break;
            case Campos.FECHA_NACIMIENTO: if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato); break;
            case Campos.FECHA:
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_fecha_obligatoria);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato);
                if (codigo == CodigoError.FECHA_ANTERIOR) return contexto.getString(R.string.error_fecha_antes_nacimiento);
                break;
            case Campos.HORA:
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_hora_obligatoria);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_hora_formato);
                if (codigo == CodigoError.FECHA_FUTURA) return contexto.getString(R.string.error_hora_futura);
                break;
            case Campos.FECHA_ESTADO:
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato);
                if (codigo == CodigoError.FECHA_ANTERIOR) return contexto.getString(R.string.error_estado_antes_ingreso);
                break;
            case Campos.MOTIVO: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_motivo_obligatorio); break;
            case Campos.HIERRO:
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_hierro_obligatorio);
                if (codigo == CodigoError.VALOR_INVALIDO) return contexto.getString(R.string.error_hierro_invalido);
                break;
            case Campos.CONTRASENA: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_contrasena_obligatoria); break;
            case Campos.CONTRASENA_REPETIDA: if (codigo == CodigoError.NO_COINCIDE) return contexto.getString(R.string.error_contrasenas_distintas); break;
            case Campos.ID_ANIMAL: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_animal_obligatorio); break;
            case Campos.TIPO: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_tipo_obligatorio); break;
            case Campos.DESCRIPCION: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_descripcion_obligatoria); break;
            case Campos.TITULO: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_titulo_obligatorio); break;
            case Campos.FECHA_PROGRAMADA:
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_fecha_obligatoria);
                if (codigo == CodigoError.FORMATO) return contexto.getString(R.string.error_fecha_formato);
                break;
            case Campos.CATEGORIA: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_categoria_obligatoria); break;
            case Campos.CONCEPTO: if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_concepto_obligatorio); break;
            case Campos.VALOR:
                if (codigo == CodigoError.OBLIGATORIO) return contexto.getString(R.string.error_valor_obligatorio);
                if (codigo == CodigoError.VALOR_INVALIDO) return contexto.getString(R.string.error_valor_no_valido);
                if (codigo == CodigoError.FUERA_DE_RANGO) return contexto.getString(R.string.error_valor_fuera_rango);
                break;
            case Campos.PESO:
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
