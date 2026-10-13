package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.EntradaPesaje;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorPesaje;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Apoyo del Modelo para pesajes (RF08, RF09). Corre en el hilo del {@link EjecutorModelo}. */
public final class PesajeRepository {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;

    public PesajeRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
    }

    /** Valida contra el animal (nacimiento e ingreso), calcula el antecedente y guarda el pesaje. */
    public void registrar(String idAnimal, EntradaPesaje entrada, Callback<Pesaje> callback) {
        ejecutor.ejecutar(() -> {
            Animal animal = animalExistente(idAnimal);
            Map<String, ErrorCampo> errores = ValidadorPesaje.validar(entrada, animal, Tiempo.ahora());
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            String ahora = Tiempo.ahoraUtc();
            Pesaje pesaje = new Pesaje();
            pesaje.idPesaje = UUID.randomUUID().toString();
            pesaje.idAnimal = idAnimal;
            pesaje.fecha = entrada.fecha;
            pesaje.hora = entrada.hora.isEmpty() ? null : LocalTime.parse(entrada.hora).format(HORA);
            pesaje.pesoKg = ValidadorPesaje.peso(entrada.pesoKg);
            pesaje.esAntecedente = ValidadorPesaje.esAntecedente(entrada.fecha, animal) ? 1 : 0;
            pesaje.observaciones = entrada.observaciones.isEmpty() ? null : entrada.observaciones;
            pesaje.creadoEn = ahora;
            pesaje.actualizadoEn = ahora;
            db.pesajeDao().insertar(pesaje);
            return pesaje;
        }, callback);
    }

    public void historial(String idAnimal, Callback<List<Pesaje>> callback) {
        ejecutor.ejecutar(() -> {
            animalExistente(idAnimal);
            return db.pesajeDao().historial(idAnimal);
        }, callback);
    }

    /** El pesaje vigente más reciente, o null si el animal no tiene pesajes. */
    public void ultimo(String idAnimal, Callback<Pesaje> callback) {
        ejecutor.ejecutar(() -> {
            animalExistente(idAnimal);
            return db.pesajeDao().ultimo(idAnimal);
        }, callback);
    }

    private Animal animalExistente(String idAnimal) throws ErrorModelo {
        Animal animal = db.animalDao().porId(idAnimal);
        if (animal == null) throw ErrorModelo.noEncontrado("animal");
        return animal;
    }
}
