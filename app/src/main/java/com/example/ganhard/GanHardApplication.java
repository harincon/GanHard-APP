package com.example.ganhard;

import android.app.Application;
import androidx.annotation.VisibleForTesting;
import com.example.ganhard.modelo.PreferenciaTema;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.remoto.AlmacenSesion;
import com.example.ganhard.modelo.remoto.EjecutorRemoto;
import com.example.ganhard.modelo.remoto.ServicioNoConfigurado;
import com.example.ganhard.modelo.remoto.ServicioRemoto;
import com.example.ganhard.modelo.remoto.SupabaseServicio;
import com.example.ganhard.modelo.repositorio.AnimalRepository;
import com.example.ganhard.modelo.repositorio.ConfiguracionRepository;
import com.example.ganhard.modelo.repositorio.CuentaRepository;
import com.example.ganhard.modelo.repositorio.EstadisticasRepository;
import com.example.ganhard.modelo.repositorio.GastoRepository;
import com.example.ganhard.modelo.repositorio.HierroRepository;
import com.example.ganhard.modelo.repositorio.HistorialRepository;
import com.example.ganhard.modelo.repositorio.PesajeRepository;
import com.example.ganhard.modelo.repositorio.SanidadRepository;
import com.example.ganhard.modelo.repositorio.TareaRepository;
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
    private TareaRepository tareas;
    private HierroRepository hierros;
    private EstadisticasRepository estadisticas;
    private CuentaRepository cuenta;
    @Override public void onCreate() {
        super.onCreate();
        PreferenciaTema.aplicar(this);
        instalar(GanHardDatabase.constructor(this, "ganhard.db").build(), Executors.newSingleThreadExecutor(),
                Executors.newSingleThreadExecutor(), servicioDeLaCompilacion(), new AlmacenSesion(this));
    }
    /** Arma los repositorios sobre una base y un hilo de trabajo; las pruebas lo usan con una base en memoria y sin cuenta. */
    @VisibleForTesting public void instalar(GanHardDatabase database, Executor trabajo) {
        instalar(database, trabajo, trabajo, new ServicioNoConfigurado(), new AlmacenSesion(this));
    }
    /** Igual, con el servicio remoto y el almacén de sesión que se indiquen (las pruebas usan un servidor simulado). */
    @VisibleForTesting public void instalar(GanHardDatabase database, Executor trabajo, Executor trabajoRemoto,
                                            ServicioRemoto servicio, AlmacenSesion sesiones) {
        ejecutor = new EjecutorModelo(trabajo);
        configuracion = new ConfiguracionRepository(database.configuracionDao(), ejecutor);
        animales = new AnimalRepository(database, ejecutor);
        pesajes = new PesajeRepository(database, ejecutor);
        historial = new HistorialRepository(database, ejecutor);
        sanidad = new SanidadRepository(database, ejecutor);
        gastos = new GastoRepository(database, ejecutor);
        tareas = new TareaRepository(database, ejecutor);
        hierros = new HierroRepository(database, ejecutor);
        estadisticas = new EstadisticasRepository(database, ejecutor);
        cuenta = new CuentaRepository(database, new EjecutorRemoto(trabajoRemoto), servicio, sesiones);
    }
    /** Con los datos de Supabase de local.properties usa el servidor real; sin ellos, la app funciona sin cuenta. */
    private static ServicioRemoto servicioDeLaCompilacion() {
        boolean completo = BuildConfig.SUPABASE_URL.startsWith("https://") && !BuildConfig.SUPABASE_ANON_KEY.trim().isEmpty();
        return completo ? new SupabaseServicio(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) : new ServicioNoConfigurado();
    }
    public EjecutorModelo ejecutor() { return ejecutor; }
    public ConfiguracionRepository configuracion() { return configuracion; }
    public AnimalRepository animales() { return animales; }
    public PesajeRepository pesajes() { return pesajes; }
    public HistorialRepository historial() { return historial; }
    public SanidadRepository sanidad() { return sanidad; }
    public GastoRepository gastos() { return gastos; }
    public TareaRepository tareas() { return tareas; }
    public HierroRepository hierros() { return hierros; }
    public EstadisticasRepository estadisticas() { return estadisticas; }
    public CuentaRepository cuenta() { return cuenta; }
}
