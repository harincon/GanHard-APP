package com.example.ganhard.modelo.remoto;

/** Una finca a la que la cuenta pertenece (o perteneció), con su rol y su estado. */
public final class PertenenciaRemota {
    public final String idFinca, nombreFinca, rol, estado;

    public PertenenciaRemota(String idFinca, String nombreFinca, String rol, String estado) {
        this.idFinca = idFinca;
        this.nombreFinca = nombreFinca;
        this.rol = rol;
        this.estado = estado;
    }
}
