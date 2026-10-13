package com.example.ganhard.modelo.datos;

import androidx.room.Dao;
import androidx.room.Query;
import com.example.ganhard.modelo.entidad.Raza;
import java.util.List;

@Dao
public interface RazaDao {
    @Query("SELECT * FROM raza ORDER BY nombre")
    List<Raza> todas();

    @Query("SELECT * FROM raza WHERE id_raza = :idRaza")
    Raza porId(String idRaza);
}
