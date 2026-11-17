package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.datos.ConfiguracionDao;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorCuenta;
import com.example.ganhard.modelo.reglas.ValidadorMotivo;
import com.example.ganhard.modelo.remoto.AlmacenSesion;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.EjecutorRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.remoto.FincaRemota;
import com.example.ganhard.modelo.remoto.IntegrantesRemotos;
import com.example.ganhard.modelo.remoto.InvitacionRemota;
import com.example.ganhard.modelo.remoto.PertenenciaRemota;
import com.example.ganhard.modelo.remoto.ServicioRemoto;
import com.example.ganhard.modelo.remoto.SesionRemota;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * La cuenta es opcional: sin ella la app funciona igual con los datos del teléfono. Con ella se puede vincular la
 * finca y gestionar quién tiene acceso. Aquí se decide qué se comprueba en el teléfono (formularios, si la finca ya
 * está vinculada y de quién) y qué decide el servidor (todas las reglas de invitaciones y accesos).
 *
 * <p>Esta clase nunca modifica ni borra datos de trabajo: cerrar sesión o perder el acceso solo afecta a la sesión.
 */
public final class CuentaRepository {
    private interface Operacion<T> {
        T con(SesionRemota sesion) throws ErrorRemoto;
    }

    private final ConfiguracionDao dao;
    private final EjecutorRemoto ejecutor;
    private final ServicioRemoto servicio;
    private final AlmacenSesion almacen;

    public CuentaRepository(GanHardDatabase db, EjecutorRemoto ejecutor, ServicioRemoto servicio, AlmacenSesion almacen) {
        this.dao = db.configuracionDao();
        this.ejecutor = ejecutor;
        this.servicio = servicio;
        this.almacen = almacen;
    }

    // --- Estado y cuenta ---

    /** Lo que se sabe sin salir a la red: la sesión guardada y el vínculo de la finca local. */
    public void estado(CallbackRemoto<EstadoCuenta> callback) {
        ejecutor.ejecutar(() -> {
            SesionRemota sesion = almacen.leer();
            ConfiguracionLocal local = dao.cargar();
            boolean vinculada = local != null && local.finca.vinculadaEn != null;
            boolean deEstaCuenta = vinculada && sesion != null && sesion.idCuenta.equals(local.usuario.idCuentaRemota);
            return new EstadoCuenta(servicio.configurado(), sesion != null, sesion == null ? null : sesion.correo,
                    sesion != null && sesion.correoConfirmado, local != null, local == null ? null : local.finca.nombre, vinculada, deEstaCuenta);
        }, callback);
    }

    /** Crea la cuenta. No abre sesión: el correo debe confirmarse primero. */
    public void registrar(String nombre, String correo, String contrasena, String repetida, CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            exigirValido(ValidadorCuenta.registro(nombre, correo, contrasena, repetida));
            servicio.registrar(ValidadorCuenta.normalizarCorreo(correo), contrasena, nombre.trim());
            return null;
        }, callback);
    }

    public void iniciarSesion(String correo, String contrasena, CallbackRemoto<SesionRemota> callback) {
        ejecutor.ejecutar(() -> {
            exigirValido(ValidadorCuenta.inicio(correo, contrasena));
            SesionRemota sesion = servicio.iniciarSesion(ValidadorCuenta.normalizarCorreo(correo), contrasena);
            almacen.guardar(sesion);
            return sesion;
        }, callback);
    }

    public void recuperarAcceso(String correo, CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            exigirCorreo(correo);
            servicio.recuperarAcceso(ValidadorCuenta.normalizarCorreo(correo));
            return null;
        }, callback);
    }

    /** Cierra la sesión en el teléfono aunque el servidor no responda. Los datos locales y el vínculo de la finca se conservan. */
    public void cerrarSesion(CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            SesionRemota sesion = almacen.leer();
            try {
                if (sesion != null) servicio.cerrarSesion(sesion.accesoToken);
            } catch (ErrorRemoto ignorado) {
                // Sin red o con el token vencido: la sesión local se borra igual.
            } finally {
                almacen.borrar();
            }
            return null;
        }, callback);
    }

    // --- Finca ---

    /** Registra en el servidor la finca de este teléfono a nombre de la cuenta y la marca como vinculada. */
    public void vincularFinca(CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            SesionRemota guardada = sesionGuardada();
            ConfiguracionLocal local = dao.cargar();
            if (local == null) throw ErrorRemoto.regla(ErrorRemoto.SIN_FINCA_LOCAL);
            if (local.finca.vinculadaEn != null) {
                boolean esDeEstaCuenta = guardada.idCuenta.equals(local.usuario.idCuentaRemota);
                throw ErrorRemoto.regla(esDeEstaCuenta ? ErrorRemoto.YA_VINCULADA : ErrorRemoto.OTRA_CUENTA);
            }
            Finca finca = local.finca;
            String idCuenta = conSesion(sesion -> {
                servicio.vincularFinca(sesion.accesoToken, new FincaRemota(finca.idFinca, finca.nombre, finca.ubicacion, finca.areaHectareas));
                return sesion.idCuenta;
            });
            // Solo con el servidor ya de acuerdo se anota el vínculo; si esta escritura fallara, reintentar es seguro.
            dao.vincular(local.usuario.idUsuario, finca.idFinca, idCuenta, Tiempo.ahoraUtc());
            return null;
        }, callback);
    }

    public void misFincas(CallbackRemoto<List<PertenenciaRemota>> callback) {
        ejecutor.ejecutar(() -> conSesion(sesion -> servicio.misFincas(sesion.accesoToken)), callback);
    }

    // --- Invitaciones recibidas ---

    public void misInvitaciones(CallbackRemoto<List<InvitacionRemota>> callback) {
        ejecutor.ejecutar(() -> conSesion(sesion -> servicio.misInvitaciones(sesion.accesoToken)), callback);
    }

    /** Acepta una invitación. No cambia nada local: la persona entra a una finca ajena, no crea una propia. */
    public void aceptarInvitacion(String idInvitacion, CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> conSesion(sesion -> {
            servicio.aceptarInvitacion(sesion.accesoToken, idInvitacion);
            return null;
        }), callback);
    }

    // --- Gestión de la finca propia ---

    public void integrantes(CallbackRemoto<IntegrantesRemotos> callback) {
        ejecutor.ejecutar(() -> {
            String idFinca = fincaDelDueno();
            return conSesion(sesion -> servicio.integrantes(sesion.accesoToken, idFinca));
        }, callback);
    }

    public void invitar(String correo, CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            exigirCorreo(correo);
            String idFinca = fincaDelDueno();
            return conSesion(sesion -> {
                servicio.invitarEncargado(sesion.accesoToken, idFinca, ValidadorCuenta.normalizarCorreo(correo));
                return null;
            });
        }, callback);
    }

    public void cancelarInvitacion(String idInvitacion, CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            fincaDelDueno();
            return conSesion(sesion -> {
                servicio.cancelarInvitacion(sesion.accesoToken, idInvitacion);
                return null;
            });
        }, callback);
    }

    /** Retira el acceso de un integrante; el servidor conserva su fila con la fecha y el motivo. */
    public void retirarAcceso(String idUsuarioFinca, String motivo, CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            ErrorCampo error = ValidadorMotivo.validar(motivo);
            if (error != null) throw ErrorRemoto.validacion(Collections.singletonMap(Campos.MOTIVO, error));
            fincaDelDueno();
            return conSesion(sesion -> {
                servicio.retirarAcceso(sesion.accesoToken, idUsuarioFinca, motivo.trim());
                return null;
            });
        }, callback);
    }

    // --- Apoyos ---

    private static void exigirValido(Map<String, ErrorCampo> errores) throws ErrorRemoto {
        if (!errores.isEmpty()) throw ErrorRemoto.validacion(errores);
    }

    private static void exigirCorreo(String correo) throws ErrorRemoto {
        ErrorCampo error = ValidadorCuenta.correo(correo);
        if (error != null) throw ErrorRemoto.validacion(Collections.singletonMap(Campos.CORREO, error));
    }

    private SesionRemota sesionGuardada() throws ErrorRemoto {
        SesionRemota sesion = almacen.leer();
        if (sesion == null) throw ErrorRemoto.de(ErrorRemoto.Tipo.SIN_SESION, null);
        return sesion;
    }

    /** El id de la finca local, solo si está vinculada y a la cuenta que tiene la sesión abierta. */
    private String fincaDelDueno() throws ErrorRemoto {
        SesionRemota sesion = sesionGuardada();
        ConfiguracionLocal local = dao.cargar();
        if (local == null) throw ErrorRemoto.regla(ErrorRemoto.SIN_FINCA_LOCAL);
        if (local.finca.vinculadaEn == null) throw ErrorRemoto.regla(ErrorRemoto.NO_VINCULADA);
        if (!sesion.idCuenta.equals(local.usuario.idCuentaRemota)) throw ErrorRemoto.regla(ErrorRemoto.NO_ES_DUENO);
        return local.finca.idFinca;
    }

    /**
     * Ejecuta una operación con la sesión guardada. Si el token venció se renueva antes; si el servidor igual lo
     * rechaza, se renueva y se intenta una sola vez más. Si ya no se puede renovar, la sesión se borra.
     */
    private <T> T conSesion(Operacion<T> operacion) throws ErrorRemoto {
        SesionRemota sesion = sesionGuardada();
        if (sesion.caducada(System.currentTimeMillis() / 1000)) sesion = renovar(sesion);
        try {
            return operacion.con(sesion);
        } catch (ErrorRemoto ex) {
            if (ex.tipo != ErrorRemoto.Tipo.NO_AUTORIZADO) throw ex;
            return operacion.con(renovar(sesion));
        }
    }

    private SesionRemota renovar(SesionRemota sesion) throws ErrorRemoto {
        try {
            SesionRemota nueva = servicio.renovar(sesion.renovacionToken);
            almacen.guardar(nueva);
            return nueva;
        } catch (ErrorRemoto ex) {
            // Solo un rechazo del servidor cierra la sesión; sin red se conserva para reintentar luego.
            if (ex.tipo == ErrorRemoto.Tipo.NO_AUTORIZADO) {
                almacen.borrar();
                throw ErrorRemoto.de(ErrorRemoto.Tipo.SIN_SESION, null);
            }
            throw ex;
        }
    }
}
