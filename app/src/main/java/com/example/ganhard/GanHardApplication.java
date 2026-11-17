package com.example.ganhard;

import android.app.Application;
import androidx.annotation.VisibleForTesting;
import com.example.ganhard.modelo.PreferenciaTema;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.repositorio.AnimalRepository;
import com.example.ganhard.modelo.repositorio.ConfiguracionRepository;
import com.example.ganhard.modelo.repositorio.GastoRepository;
import com.example.ganhard.modelo.repositorio.HistorialRepository;
import com.example.ganhard.modelo.repositorio.PesajeRepository;
import com.example.ganhard.modelo.repositorio.SanidadRepository;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Una base, un ejecutor y los repositorios compartidos durante la vida del proceso. */
public class GanHardApplication extends Application {
    private EjecutorModelo ejecutor;
    private ConfiguracionRepository configuracion;
    private AnimalRepository animales;
    private PesajeRepository pesajes;
    private HistorialRepository historial;
    private SanidadRepository sanidad;
    private GastoRepository gastos;
    @Override public void onCreate() {
        super.onCreate();
        PreferenciaTema.aplicar(this);
        instalar(GanHardDatabase.constructor(this, "ganhard.db").build(), Executors.newSingleThreadExecutor());
    }
    /** Arma los repositorios sobre una base y un hilo de trabajo; las pruebas lo usan con una base en memoria. */
    @VisibleForTesting public void instalar(GanHardDatabase database, Executor trabajo) {
        ejecutor = new EjecutorModelo(trabajo);
        configuracion = new ConfiguracionRepository(database.configuracionDao(), ejecutor);
        animales = new AnimalRepository(database, ejecutor);
        pesajes = new PesajeRepository(database, ejecutor);
        historial = new HistorialRepository(database, ejecutor);
        sanidad = new SanidadRepository(database, ejecutor);
        gastos = new GastoRepository(database, ejecutor);
    }
    public EjecutorModelo ejecutor() { return ejecutor; }
    public ConfiguracionRepository configuracion() { return configuracion; }
    public AnimalRepository animales() { return animales; }
    public PesajeRepository pesajes() { return pesajes; }
    public HistorialRepository historial() { return historial; }
    public SanidadRepository sanidad() { return sanidad; }
    public GastoRepository gastos() { return gastos; }
}
