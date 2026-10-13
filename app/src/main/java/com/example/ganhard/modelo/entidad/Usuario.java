package com.example.ganhard.modelo.entidad;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuario", indices = {@Index(value = "id_cuenta_remota", unique = true)})
public class Usuario {
    @PrimaryKey @NonNull @ColumnInfo(name = "id_usuario") public String idUsuario = "";
    @ColumnInfo(name = "id_cuenta_remota") public String idCuentaRemota;
    @NonNull public String nombre = "";
    @NonNull public String correo = "";
    public String telefono;
    public int activo = 1;
    @NonNull @ColumnInfo(name = "creado_en") public String creadoEn = "";
    @NonNull @ColumnInfo(name = "actualizado_en") public String actualizadoEn = "";
    public int version = 0;
}
