package com.example.ganhard.modelo.remoto;

/** Datos de la finca que se envían al servidor al vincularla a la cuenta. */
public final class FincaRemota {
    public final String idFinca, nombre, ubicacion;
    public final Double areaHectareas;

    public FincaRemota(String idFinca, String nombre, String ubicacion, Double areaHectareas) {
        this.idFinca = idFinca;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.areaHectareas = areaHectareas;
    }
}
