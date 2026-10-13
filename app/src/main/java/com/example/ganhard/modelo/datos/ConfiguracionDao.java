package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Upsert;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;

@Dao
public abstract class ConfiguracionDao {
    @Query("SELECT * FROM finca WHERE activa = 1 ORDER BY creado_en, id_finca LIMIT 1")
    public abstract Finca fincaActiva();
    @Query("SELECT * FROM usuario WHERE id_usuario = :id")
    public abstract Usuario usuario(String id);
    @Upsert protected abstract void guardarUsuario(Usuario usuario);
    @Upsert protected abstract void guardarFinca(Finca finca);

    @Transaction public void guardar(Usuario usuario, Finca finca) {
        guardarUsuario(usuario);
        guardarFinca(finca);
    }
    @Transaction public ConfiguracionLocal cargar() {
        Finca finca = fincaActiva();
        if (finca == null) return null;
        Usuario usuario = usuario(finca.idPropietario);
        if (usuario == null) throw new IllegalStateException("La finca no tiene un propietario válido");
        return new ConfiguracionLocal(usuario, finca);
    }
}
