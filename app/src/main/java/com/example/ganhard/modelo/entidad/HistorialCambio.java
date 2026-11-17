package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bitácora de consulta (RN06): quién hizo un cambio, cuándo, qué modificó y por qué. No restaura versiones.
 * {@code idRegistro} apunta al animal o al pesaje afectado según {@code entidad}; es una referencia lógica, sin
 * clave foránea, porque señala a tablas distintas.
 */
@Entity(tableName = "historial_cambio",
        foreignKeys = {
                @ForeignKey(entity = Usuario.class, parentColumns = "id_usuario", childColumns = "id_usuario", onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = Finca.class, parentColumns = "id_finca", childColumns = "id_finca", onDelete = ForeignKey.RESTRICT)},
        indices = {
                @Index(value = {"id_finca", "registrado_en"}),
                @Index(value = {"entidad", "id_registro"}),
                @Index("id_usuario")})
public class HistorialCambio {
    public static final String ANIMAL = "animal", PESAJE = "pesaje", EVENTO_SANITARIO = "evento_sanitario", GASTO = "gasto", TAREA = "tarea";
    public static final String CREAR = "CREAR", EDITAR = "EDITAR", ANULAR = "ANULAR", CAMBIAR_ESTADO = "CAMBIAR_ESTADO";

    @PrimaryKey @NonNull @ColumnInfo(name = "id_historial_cambio") public String idHistorialCambio = "";
    @NonNull @ColumnInfo(name = "id_usuario") public String idUsuario = "";
    @ColumnInfo(name = "id_finca") public String idFinca;
    @NonNull public String entidad = "";
    @NonNull @ColumnInfo(name = "id_registro") public String idRegistro = "";
    @NonNull public String accion = "";
    /** Instante UTC en que se registró el cambio en este teléfono. */
    @NonNull @ColumnInfo(name = "registrado_en") public String registradoEn = "";
    /** Lo asignará el servidor al sincronizar; nulo mientras tanto. */
    @ColumnInfo(name = "recibido_en") public String recibidoEn;
    /** Fecha del hecho cuando difiere del registro, como en un cambio de estado. */
    @ColumnInfo(name = "fecha_efectiva") public String fechaEfectiva;
    public String motivo;
    /** JSON con los valores previos de los campos que cambiaron; nulo en una creación. */
    @ColumnInfo(name = "valores_anteriores") public String valoresAnteriores;
    /** JSON con los valores resultantes. */
    @NonNull @ColumnInfo(name = "valores_nuevos") public String valoresNuevos = "";
}
