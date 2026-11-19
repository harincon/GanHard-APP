package com.example.ganhard.controlador;

import android.content.Context;
import com.example.ganhard.R;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.remoto.IntegranteRemoto;
import com.example.ganhard.modelo.remoto.InvitacionRemota;

/** Traduce los errores y los estados de la cuenta a textos de recursos. */
public final class MensajesRemotos {
    private MensajesRemotos() {}

    public static String texto(Context contexto, ErrorRemoto error) {
        return contexto.getString(recurso(error));
    }

    private static int recurso(ErrorRemoto error) {
        switch (error.tipo) {
            case VALIDACION: return R.string.remoto_validacion;
            case SIN_CONFIGURAR: return R.string.remoto_sin_configurar;
            case SIN_SESION: return R.string.remoto_sin_sesion;
            case SIN_CONEXION: return R.string.remoto_sin_conexion;
            case CREDENCIALES: return R.string.remoto_credenciales;
            case CORREO_SIN_CONFIRMAR: return R.string.remoto_correo_sin_confirmar;
            case NO_AUTORIZADO: return R.string.remoto_no_autorizado;
            case NO_ENCONTRADO: return R.string.remoto_no_encontrado;
            case LOCAL: return R.string.remoto_local;
            case REGLA: return regla(error.codigo);
            case SERVIDOR:
            default: return ErrorRemoto.ESQUEMA_NO_INSTALADO.equals(error.codigo) ? R.string.remoto_esquema_no_instalado : R.string.remoto_servidor;
        }
    }

    private static int regla(String codigo) {
        if (codigo == null) return R.string.regla_desconocida;
        switch (codigo) {
            case ErrorRemoto.CODIGO_INVALIDO: return R.string.regla_codigo_invalido;
            case ErrorRemoto.DEMASIADOS_INTENTOS: return R.string.regla_demasiados_intentos;
            case ErrorRemoto.CONTRASENA_DEBIL: return R.string.regla_contrasena_debil;
            case ErrorRemoto.CORREO_REGISTRADO: return R.string.regla_correo_registrado;
            case ErrorRemoto.FINCA_DE_OTRA_CUENTA: return R.string.regla_finca_de_otra_cuenta;
            case ErrorRemoto.YA_TIENE_FINCA: return R.string.regla_ya_tiene_finca;
            case ErrorRemoto.NO_ES_DUENO: return R.string.regla_no_es_dueno;
            case ErrorRemoto.INVITACION_REPETIDA: return R.string.regla_invitacion_repetida;
            case ErrorRemoto.ES_INTEGRANTE: return R.string.regla_es_integrante;
            case ErrorRemoto.CORREO_PROPIO: return R.string.regla_correo_propio;
            case ErrorRemoto.INVITACION_NO_VIGENTE: return R.string.regla_invitacion_no_vigente;
            case ErrorRemoto.CORREO_NO_COINCIDE: return R.string.regla_correo_no_coincide;
            case ErrorRemoto.YA_ES_INTEGRANTE: return R.string.regla_ya_es_integrante;
            case ErrorRemoto.ES_ADMINISTRADOR: return R.string.regla_es_administrador;
            case ErrorRemoto.YA_RETIRADO: return R.string.regla_ya_retirado;
            case ErrorRemoto.MOTIVO_OBLIGATORIO: return R.string.regla_motivo_obligatorio;
            case ErrorRemoto.CORREO_INVALIDO: return R.string.regla_correo_invalido;
            case ErrorRemoto.SIN_FINCA_LOCAL: return R.string.regla_sin_finca_local;
            case ErrorRemoto.YA_VINCULADA: return R.string.regla_ya_vinculada;
            case ErrorRemoto.OTRA_CUENTA: return R.string.regla_otra_cuenta;
            case ErrorRemoto.NO_VINCULADA: return R.string.regla_no_vinculada;
            case ErrorRemoto.NO_ES_INTEGRANTE: return R.string.regla_no_es_integrante;
            case ErrorRemoto.YA_HAY_FINCA: return R.string.regla_ya_hay_finca;
            case ErrorRemoto.EN_CURSO: return R.string.regla_sincronizacion_en_curso;
            case ErrorRemoto.TABLA_INVALIDA:
            case ErrorRemoto.DEMASIADAS_FILAS:
            case ErrorRemoto.FILA_DE_OTRA_FINCA: return R.string.regla_respaldo_rechazado;
            default: return R.string.regla_desconocida;
        }
    }

    /** Fecha corta de un instante del servidor (AAAA-MM-DDTHH:MM...); si no tiene ese formato, se muestra tal cual. */
    public static String fecha(String instante) {
        if (instante == null) return "";
        return instante.matches("\\d{4}-\\d{2}-\\d{2}.*") ? FormatoFecha.corta(instante.substring(0, 10)) : instante;
    }

    public static String rol(Context contexto, String rol) {
        return contexto.getString(IntegranteRemoto.ADMINISTRADOR.equals(rol) ? R.string.rol_administrador : R.string.rol_encargado);
    }

    public static String estadoIntegrante(Context contexto, String estado) {
        return contexto.getString(IntegranteRemoto.REVOCADO.equals(estado) ? R.string.estado_revocado : R.string.integrante_con_acceso);
    }

    public static String estadoInvitacion(Context contexto, String estado) {
        if (InvitacionRemota.ACEPTADA.equals(estado)) return contexto.getString(R.string.invitacion_aceptada);
        if (InvitacionRemota.CANCELADA.equals(estado)) return contexto.getString(R.string.invitacion_cancelada);
        if (InvitacionRemota.VENCIDA.equals(estado)) return contexto.getString(R.string.invitacion_vencida);
        return contexto.getString(R.string.invitacion_pendiente);
    }
}
