package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.datos.HistorialResumen;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.List;

/** Apoyo del Modelo para consultar el historial de cambios (RF16). Solo lee: el historial no se edita ni restaura. */
public final class HistorialRepository {
    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;

    public HistorialRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
    }

    /** Todos los cambios de la finca activa, del más reciente al más antiguo. */
    public void deLaFinca(Callback<List<HistorialResumen>> callback) {
        ejecutor.ejecutar(() -> {
            String idFinca = db.configuracionDao().idFincaActiva();
            if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
            return db.historialDao().deLaFinca(idFinca);
        }, callback);
    }

    /** Cambios de un animal y de sus pesajes, incluidos los anulados. */
    public void delAnimal(String idAnimal, Callback<List<HistorialResumen>> callback) {
        ejecutor.ejecutar(() -> {
            if (db.animalDao().porId(idAnimal) == null) throw ErrorModelo.noEncontrado(ErrorModelo.ANIMAL);
            return db.historialDao().delAnimal(idAnimal);
        }, callback);
    }
}
