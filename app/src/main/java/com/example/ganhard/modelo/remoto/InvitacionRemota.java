package com.example.ganhard.modelo.remoto;

/** Invitación a una finca. Quien la recibe ve la finca y quién invita; el dueño ve el correo invitado y su estado. */
public final class InvitacionRemota {
    public static final String PENDIENTE = "PENDIENTE", ACEPTADA = "ACEPTADA", CANCELADA = "CANCELADA", VENCIDA = "VENCIDA";

    public final String idInvitacion, idFinca, nombreFinca, invitadoPor, correoInvitado, estado, creadaEn, venceEn;

    public InvitacionRemota(String idInvitacion, String idFinca, String nombreFinca, String invitadoPor, String correoInvitado,
                            String estado, String creadaEn, String venceEn) {
        this.idInvitacion = idInvitacion;
        this.idFinca = idFinca;
        this.nombreFinca = nombreFinca;
        this.invitadoPor = invitadoPor;
        this.correoInvitado = correoInvitado;
        this.estado = estado;
        this.creadaEn = creadaEn;
        this.venceEn = venceEn;
    }
}
