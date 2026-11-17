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
    @Query("SELECT id_finca FROM finca WHERE activa = 1 ORDER BY creado_en, id_finca LIMIT 1")
    public abstract String idFincaActiva();
    @Query("SELECT * FROM usuario WHERE id_usuario = :id")
    public abstract Usuario usuario(String id);
    @Query("UPDATE usuario SET id_cuenta_remota = :idCuenta WHERE id_usuario = :idUsuario")
    protected abstract void asignarCuenta(String idUsuario, String idCuenta);
    @Query("UPDATE finca SET vinculada_en = :fecha WHERE id_finca = :idFinca")
    protected abstract void marcarVinculada(String idFinca, String fecha);
    @Upsert protected abstract void guardarUsuario(Usuario usuario);
    @Upsert protected abstract void guardarFinca(Finca finca);

    @Transaction public void guardar(Usuario usuario, Finca finca) {
        guardarUsuario(usuario);
        guardarFinca(finca);
    }
    /** Enlaza el dueño local y su finca con una cuenta; las dos escrituras ocurren juntas o ninguna. */
    @Transaction public void vincular(String idUsuario, String idFinca, String idCuenta, String fecha) {
        asignarCuenta(idUsuario, idCuenta);
        marcarVinculada(idFinca, fecha);
    }
    @Transaction public ConfiguracionLocal cargar() {
        Finca finca = fincaActiva();
        if (finca == null) return null;
        Usuario usuario = usuario(finca.idPropietario);
        if (usuario == null) throw new IllegalStateException("La finca no tiene un propietario válido");
        return new ConfiguracionLocal(usuario, finca);
    }
}
