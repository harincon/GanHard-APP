package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Ficha del animal. El código es único por finca sin distinguir mayúsculas; los estados conservan el historial. */
@Entity(tableName = "animal",
        foreignKeys = {
                @ForeignKey(entity = Finca.class, parentColumns = "id_finca", childColumns = "id_finca", onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = Raza.class, parentColumns = "id_raza", childColumns = "id_raza", onDelete = ForeignKey.SET_NULL)},
        indices = {
                @Index(value = {"id_finca", "codigo"}, unique = true),
                @Index(value = {"id_finca", "estado"}),
                @Index("id_raza")})
public class Animal {
    public static final String ACTIVO = "ACTIVO", VENDIDO = "VENDIDO", FALLECIDO = "FALLECIDO";
    public static final String HEMBRA = "H", MACHO = "M";

    @PrimaryKey @NonNull @ColumnInfo(name = "id_animal") public String idAnimal = "";
    @NonNull @ColumnInfo(name = "id_finca") public String idFinca = "";
    @ColumnInfo(name = "id_raza") public String idRaza;
    @NonNull @ColumnInfo(collate = ColumnInfo.NOCASE) public String codigo = "";
    public String nombre;
    @NonNull public String sexo = "";
    @ColumnInfo(name = "fecha_nacimiento") public String fechaNacimiento;
    @NonNull @ColumnInfo(name = "fecha_ingreso") public String fechaIngreso = "";
    @NonNull public String estado = ACTIVO;
    @NonNull @ColumnInfo(name = "fecha_estado") public String fechaEstado = "";
    public String observaciones;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
    public int version = 0;
}
