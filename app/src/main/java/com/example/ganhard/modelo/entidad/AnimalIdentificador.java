package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Dato que identifica a un animal: un hierro del catálogo, un arete o un número cualquiera. No hay regla de
 * unicidad: los valores pueden repetirse entre animales o dentro de uno. El único valor único es el código del animal.
 */
@Entity(tableName = "animal_identificador",
        foreignKeys = {
                @ForeignKey(entity = Animal.class, parentColumns = "id_animal", childColumns = "id_animal", onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = Hierro.class, parentColumns = "id_hierro", childColumns = "id_hierro", onDelete = ForeignKey.RESTRICT)},
        indices = {@Index("id_animal"), @Index("id_hierro")})
public class AnimalIdentificador {
    public static final String HIERRO = "HIERRO", ARETE = "ARETE", NUMERO = "NUMERO";

    @PrimaryKey @NonNull @ColumnInfo(name = "id_animal_identificador") public String idAnimalIdentificador = "";
    @NonNull @ColumnInfo(name = "id_animal") public String idAnimal = "";
    @NonNull public String tipo = HIERRO;
    /** Solo en un HIERRO: el hierro del catálogo. */
    @ColumnInfo(name = "id_hierro") public String idHierro;
    /** Solo en un ARETE o un NUMERO: el valor escrito. */
    public String valor;
    /** Posición en la lista del animal; el primer hierro es el principal. */
    public int orden;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
}
