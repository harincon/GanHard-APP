package com.example.ganhard.modelo.remoto;

import com.example.ganhard.modelo.reglas.ErrorCampo;
import java.util.Collections;
import java.util.Map;

/**
 * Fallo de una operación con la cuenta o con el servidor. El tipo (y, en una regla, el código) permite a la Vista
 * mostrar un texto propio sin leer el mensaje del servidor.
 */
public final class ErrorRemoto extends Exception {
    public enum Tipo {
        /** Datos del formulario inválidos; el detalle está en {@link #campos}. */
        VALIDACION,
        /** La app se compiló sin los datos de Supabase. */
        SIN_CONFIGURAR,
        /** No hay sesión iniciada o ya no es válida. */
        SIN_SESION,
        SIN_CONEXION,
        CREDENCIALES,
        CORREO_SIN_CONFIRMAR,
        /** El servidor rechazó el token o el permiso. */
        NO_AUTORIZADO,
        NO_ENCONTRADO,
        /** Una regla del servidor o de la app; el motivo está en {@link #codigo}. */
        REGLA,
        SERVIDOR,
        /** Falló la base de datos del teléfono. */
        LOCAL
    }

    // Códigos de regla que llegan del servidor (los mismos de supabase/esquema.sql).
    public static final String DEMASIADOS_INTENTOS = "demasiados_intentos", CONTRASENA_DEBIL = "contrasena_debil";
    public static final String CORREO_REGISTRADO = "correo_registrado", ESQUEMA_NO_INSTALADO = "esquema_no_instalado";
    public static final String FINCA_DE_OTRA_CUENTA = "finca_de_otra_cuenta", YA_TIENE_FINCA = "ya_tiene_finca";
    public static final String NO_ES_DUENO = "no_es_dueno", INVITACION_REPETIDA = "invitacion_repetida";
    public static final String ES_INTEGRANTE = "es_integrante", CORREO_PROPIO = "correo_propio";
    public static final String INVITACION_NO_VIGENTE = "invitacion_no_vigente", CORREO_NO_COINCIDE = "correo_no_coincide";
    public static final String YA_ES_INTEGRANTE = "ya_es_integrante", ES_ADMINISTRADOR = "es_administrador";
    public static final String YA_RETIRADO = "ya_retirado", MOTIVO_OBLIGATORIO = "motivo_obligatorio", CORREO_INVALIDO = "correo_invalido";
    // Códigos que decide la app antes de llamar al servidor.
    public static final String SIN_FINCA_LOCAL = "sin_finca_local", YA_VINCULADA = "ya_vinculada";
    public static final String OTRA_CUENTA = "finca_vinculada_a_otra_cuenta", NO_VINCULADA = "finca_no_vinculada";

    public final Tipo tipo;
    /** En una REGLA, el motivo; en SERVIDOR, a veces un código conocido; nulo en los demás casos. */
    public final String codigo;
    /** Errores por campo en una VALIDACION; vacío en los demás tipos. */
    public final Map<String, ErrorCampo> campos;

    private ErrorRemoto(Tipo tipo, String codigo, String mensaje, Throwable causa, Map<String, ErrorCampo> campos) {
        super(mensaje == null ? tipo.name() + (codigo == null ? "" : ": " + codigo) : mensaje, causa);
        this.tipo = tipo;
        this.codigo = codigo;
        this.campos = Collections.unmodifiableMap(campos);
    }

    public static ErrorRemoto de(Tipo tipo, String codigo) {
        return new ErrorRemoto(tipo, codigo, null, null, Collections.emptyMap());
    }
    public static ErrorRemoto de(Tipo tipo, String codigo, String detalle, Throwable causa) {
        return new ErrorRemoto(tipo, codigo, detalle, causa, Collections.emptyMap());
    }
    public static ErrorRemoto regla(String codigo) { return de(Tipo.REGLA, codigo); }
    public static ErrorRemoto validacion(Map<String, ErrorCampo> campos) {
        return new ErrorRemoto(Tipo.VALIDACION, null, "Datos inválidos", null, campos);
    }
}
