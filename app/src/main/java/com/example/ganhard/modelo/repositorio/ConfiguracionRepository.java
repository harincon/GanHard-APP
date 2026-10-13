package com.example.ganhard.modelo.repositorio;

import android.os.Handler;
import android.os.Looper;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.datos.ConfiguracionDao;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.reglas.EntradaConfiguracion;
import com.example.ganhard.modelo.reglas.ValidadorConfiguracion;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Apoyo del Modelo: coordina persistencia; nunca realiza consultas en el hilo de UI. */
public final class ConfiguracionRepository {
    public interface Callback {
        void exito(ConfiguracionLocal configuracion);
        void error(Exception error);
    }
    private final ConfiguracionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());
    public ConfiguracionRepository(ConfiguracionDao dao) { this.dao = dao; }
    public void cargar(Callback callback) {
        executor.execute(() -> {
            try { entregar(callback, dao.cargar()); }
            catch (Exception ex) { principal.post(() -> callback.error(ex)); }
        });
    }
    public void guardar(EntradaConfiguracion entrada, Callback callback) {
        executor.execute(() -> {
            try {
                if (!ValidadorConfiguracion.validar(entrada).isEmpty()) throw new IllegalArgumentException("Datos inválidos");
                ConfiguracionLocal existente = dao.cargar();
                Usuario usuario = existente == null ? new Usuario() : existente.usuario;
                Finca finca = existente == null ? new Finca() : existente.finca;
                String fecha = ahoraUtc();
                if (existente == null) {
                    usuario.idUsuario = UUID.randomUUID().toString();
                    finca.idFinca = UUID.randomUUID().toString();
                    usuario.creadoEn = fecha;
                    finca.creadoEn = fecha;
                    finca.idPropietario = usuario.idUsuario;
                }
                usuario.nombre = entrada.nombre;
                usuario.correo = entrada.correo;
                usuario.actualizadoEn = fecha;
                finca.nombre = entrada.finca;
                finca.ubicacion = entrada.ubicacion.isEmpty() ? null : entrada.ubicacion;
                finca.areaHectareas = ValidadorConfiguracion.area(entrada.area);
                finca.actualizadoEn = fecha;
                dao.guardar(usuario, finca);
                entregar(callback, new ConfiguracionLocal(usuario, finca));
            } catch (Exception ex) { principal.post(() -> callback.error(ex)); }
        });
    }
    private void entregar(Callback callback, ConfiguracionLocal datos) { principal.post(() -> callback.exito(datos)); }
    private String ahoraUtc() {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formato.format(new Date());
    }
}
