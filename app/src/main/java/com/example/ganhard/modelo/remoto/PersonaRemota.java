package com.example.ganhard.modelo.remoto;

/** Una persona con acceso (o que tuvo acceso) a la finca, tal como la conoce el servidor. */
public final class PersonaRemota {
    public final String idCuenta, nombre, correo, rol, estado;

    public PersonaRemota(String idCuenta, String nombre, String correo, String rol, String estado) {
        this.idCuenta = idCuenta;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.estado = estado;
    }
}
