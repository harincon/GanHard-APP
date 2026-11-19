package com.example.ganhard.modelo.remoto;

import java.util.List;

/**
 * Lo que la app necesita del servidor. Las llamadas son bloqueantes y se hacen siempre fuera del hilo principal. Las
 * reglas (quién puede invitar, qué invitación vale, etc.) las aplica el servidor; la app solo las pide y las muestra.
 */
public interface ServicioRemoto {
    /** Falso si la app se compiló sin los datos del proyecto de Supabase. */
    boolean configurado();

    void registrar(String correo, String contrasena, String nombre) throws ErrorRemoto;
    SesionRemota iniciarSesion(String correo, String contrasena) throws ErrorRemoto;
    SesionRemota renovar(String tokenRenovacion) throws ErrorRemoto;
    /** Confirma el correo con el código que llegó por correo; el servidor entrega la sesión ya iniciada. */
    SesionRemota confirmarCorreo(String correo, String codigo) throws ErrorRemoto;
    /** Pide un código de confirmación nuevo; no revela si el correo tiene cuenta. */
    void reenviarConfirmacion(String correo) throws ErrorRemoto;
    void recuperarAcceso(String correo) throws ErrorRemoto;
    void cerrarSesion(String tokenAcceso) throws ErrorRemoto;

    void vincularFinca(String tokenAcceso, FincaRemota finca) throws ErrorRemoto;
    List<PertenenciaRemota> misFincas(String tokenAcceso) throws ErrorRemoto;
    List<InvitacionRemota> misInvitaciones(String tokenAcceso) throws ErrorRemoto;
    void aceptarInvitacion(String tokenAcceso, String idInvitacion) throws ErrorRemoto;

    IntegrantesRemotos integrantes(String tokenAcceso, String idFinca) throws ErrorRemoto;
    void invitarEncargado(String tokenAcceso, String idFinca, String correo) throws ErrorRemoto;
    void cancelarInvitacion(String tokenAcceso, String idInvitacion) throws ErrorRemoto;
    void retirarAcceso(String tokenAcceso, String idUsuarioFinca, String motivo) throws ErrorRemoto;

    /**
     * Sube un lote de filas (hasta 500) de una tabla de la finca para el respaldo. Es idempotente: el servidor solo
     * reemplaza una fila si la que llega es más reciente. Devuelve cuántas filas aplicó.
     */
    int subirCambios(String tokenAcceso, String idFinca, String tabla, org.json.JSONArray filas) throws ErrorRemoto;
}
