package com.example.ganhard.modelo.datos;

import androidx.room.ColumnInfo;

/** Fila de la lista de animales: lo necesario para mostrarla, con el último peso vigente (null si no hay). */
public class AnimalResumen {
    @ColumnInfo(name = "id_animal") public String idAnimal;
    public String codigo;
    public String nombre;
    public String sexo;
    public String estado;
    @ColumnInfo(name = "ultimo_peso_kg") public Double ultimoPesoKg;
}
