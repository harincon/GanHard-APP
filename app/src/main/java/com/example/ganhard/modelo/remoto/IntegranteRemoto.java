package com.example.ganhard.modelo.remoto;

/** Persona con acceso (o que lo tuvo) a la finca. Un acceso retirado se conserva con su fecha y su motivo. */
public final class IntegranteRemoto {
    public static final String ADMINISTRADOR = "ADMINISTRADOR", ENCARGADO = "ENCARGADO";
    public static final String ACTIVO = "ACTIVO", REVOCADO = "REVOCADO";

    public final String idUsuarioFinca, nombre, correo, rol, estado, fechaIngreso, fechaRetiro, motivoRetiro;

    public IntegranteRemoto(String idUsuarioFinca, String nombre, String correo, String rol, String estado,
                            String fechaIngreso, String fechaRetiro, String motivoRetiro) {
        this.idUsuarioFinca = idUsuarioFinca;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.estado = estado;
        this.fechaIngreso = fechaIngreso;
        this.fechaRetiro = fechaRetiro;
        this.motivoRetiro = motivoRetiro;
    }
}
