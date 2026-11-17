package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Egreso de la finca. El valor se guarda como entero en centavos de COP: 12500000 es COP 125.000,00. */
@Entity(tableName = "gasto",
        foreignKeys = @ForeignKey(entity = Finca.class, parentColumns = "id_finca", childColumns = "id_finca", onDelete = ForeignKey.RESTRICT),
        indices = @Index(value = {"id_finca", "fecha"}))
public class Gasto {
    public static final String ALIMENTACION = "ALIMENTACION", SANIDAD = "SANIDAD", MANTENIMIENTO = "MANTENIMIENTO",
            TRANSPORTE = "TRANSPORTE", OTRO = "OTRO";
    /** Categorías admitidas, en el orden en que se ofrecen. */
    public static final List<String> CATEGORIAS = Collections.unmodifiableList(
            Arrays.asList(ALIMENTACION, SANIDAD, MANTENIMIENTO, TRANSPORTE, OTRO));

    @PrimaryKey @NonNull @ColumnInfo(name = "id_gasto") public String idGasto = "";
    @NonNull @ColumnInfo(name = "id_finca") public String idFinca = "";
    @NonNull public String fecha = "";
    @NonNull public String categoria = "";
    @NonNull public String concepto = "";
    @ColumnInfo(name = "valor_centavos") public long valorCentavos;
    public int anulado = 0;
    @ColumnInfo(name = "anulado_en") public String anuladoEn;
    @ColumnInfo(name = "motivo_anulacion") public String motivoAnulacion;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
    public int version = 0;
}
