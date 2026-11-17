package com.example.ganhard.modelo.datos;

import androidx.room.ColumnInfo;

/** Identificador de un animal con el nombre de su hierro ya resuelto, listo para mostrarse. */
public class IdentificadorResumen {
    @ColumnInfo(name = "id_animal_identificador") public String idAnimalIdentificador;
    @ColumnInfo(name = "id_animal") public String idAnimal;
    public String tipo;
    @ColumnInfo(name = "id_hierro") public String idHierro;
    public String valor;
    public int orden;
    @ColumnInfo(name = "hierro_nombre") public String hierroNombre;
    /** 0 si el hierro está desactivado; 1 si está activo o el identificador no es un hierro. */
    @ColumnInfo(name = "hierro_activo") public int hierroActivo;

    /** Lo que se muestra: el nombre del hierro o el valor escrito. */
    public String texto() { return hierroNombre != null ? hierroNombre : (valor == null ? "" : valor); }
}
