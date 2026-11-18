package com.example.ganhard.modelo.remoto;

import java.util.List;

/** Servicio de una app compilada sin los datos de Supabase: la app funciona sin cuenta y todo lo remoto avisa que falta configurar. */
public final class ServicioNoConfigurado implements ServicioRemoto {
    private static ErrorRemoto falta() { return ErrorRemoto.de(ErrorRemoto.Tipo.SIN_CONFIGURAR, null); }

    @Override public boolean configurado() { return false; }
    @Override public void registrar(String correo, String contrasena, String nombre) throws ErrorRemoto { throw falta(); }
    @Override public SesionRemota iniciarSesion(String correo, String contrasena) throws ErrorRemoto { throw falta(); }
    @Override public SesionRemota renovar(String tokenRenovacion) throws ErrorRemoto { throw falta(); }
    @Override public SesionRemota confirmarCorreo(String correo, String codigo) throws ErrorRemoto { throw falta(); }
    @Override public void reenviarConfirmacion(String correo) throws ErrorRemoto { throw falta(); }
    @Override public void recuperarAcceso(String correo) throws ErrorRemoto { throw falta(); }
    @Override public void cerrarSesion(String tokenAcceso) throws ErrorRemoto { throw falta(); }
    @Override public void vincularFinca(String tokenAcceso, FincaRemota finca) throws ErrorRemoto { throw falta(); }
    @Override public List<PertenenciaRemota> misFincas(String tokenAcceso) throws ErrorRemoto { throw falta(); }
    @Override public List<InvitacionRemota> misInvitaciones(String tokenAcceso) throws ErrorRemoto { throw falta(); }
    @Override public void aceptarInvitacion(String tokenAcceso, String idInvitacion) throws ErrorRemoto { throw falta(); }
    @Override public IntegrantesRemotos integrantes(String tokenAcceso, String idFinca) throws ErrorRemoto { throw falta(); }
    @Override public void invitarEncargado(String tokenAcceso, String idFinca, String correo) throws ErrorRemoto { throw falta(); }
    @Override public void cancelarInvitacion(String tokenAcceso, String idInvitacion) throws ErrorRemoto { throw falta(); }
    @Override public void retirarAcceso(String tokenAcceso, String idUsuarioFinca, String motivo) throws ErrorRemoto { throw falta(); }
}
