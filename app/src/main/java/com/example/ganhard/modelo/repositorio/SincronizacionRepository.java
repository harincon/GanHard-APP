package com.example.ganhard.modelo.repositorio;

import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.datos.MarcaSincronizacionDao;
import com.example.ganhard.modelo.entidad.MarcaSincronizacion;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.EjecutorRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.remoto.ServicioRemoto;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Respaldo en la nube (etapa 6.1): sube al servidor lo que cambió en la finca. Nunca modifica ni borra datos de
 * trabajo; solo lee las tablas y guarda sus "marcas" (hasta dónde ya se subió cada una).
 *
 * <p>Qué falta por subir: en cada tabla, lo que esté después de su marca (fecha e id del último registro confirmado).
 * La marca avanza solo cuando el servidor confirma un lote, así que un corte a medias se reintenta sin duplicar. Las
 * razas no tienen fechas ni cambian, por eso se envían completas cada vez.
 */
public final class SincronizacionRepository {
    /** Avisa, en el hilo principal, cuántos registros van subidos en esta sincronización. */
    public interface Progreso {
        void alAvanzar(int subidas);
    }

    static final int LOTE = 200;

    /** Una tabla del respaldo: cómo se lee y de qué finca es. {@code fecha} nulo es una tabla que siempre se envía completa. */
    private static final class Tabla {
        final String nombre, id, fecha, filtro;
        Tabla(String nombre, String id, String fecha, String filtro) {
            this.nombre = nombre;
            this.id = id;
            this.fecha = fecha;
            this.filtro = filtro;
        }
        boolean completa() { return fecha == null; }
    }

    private static final String DE_LA_FINCA = "id_finca = ?";
    private static final String DE_ANIMALES_DE_LA_FINCA = "id_animal IN (SELECT id_animal FROM animal WHERE id_finca = ?)";
    /** En este orden, para que lo que depende de otra cosa llegue después (pesajes y sanidad apuntan a tareas y animales). */
    private static final Tabla[] TABLAS = {
            new Tabla("raza", "id_raza", null, null),
            new Tabla("hierro", "id_hierro", "actualizado_en", DE_LA_FINCA),
            new Tabla("animal", "id_animal", "actualizado_en", DE_LA_FINCA),
            new Tabla("animal_identificador", "id_animal_identificador", "actualizado_en", DE_ANIMALES_DE_LA_FINCA),
            new Tabla("tarea", "id_tarea", "actualizado_en", DE_LA_FINCA),
            new Tabla("pesaje", "id_pesaje", "actualizado_en", DE_ANIMALES_DE_LA_FINCA),
            new Tabla("evento_sanitario", "id_evento_sanitario", "actualizado_en", DE_ANIMALES_DE_LA_FINCA),
            new Tabla("gasto", "id_gasto", "actualizado_en", DE_LA_FINCA),
            new Tabla("historial_cambio", "id_historial_cambio", "registrado_en", DE_LA_FINCA),
    };

    private final GanHardDatabase db;
    private final MarcaSincronizacionDao marcas;
    private final EjecutorRemoto ejecutor;
    private final ServicioRemoto servicio;
    private final CuentaRepository cuenta;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final AtomicBoolean enCurso = new AtomicBoolean(false);

    public SincronizacionRepository(GanHardDatabase db, EjecutorRemoto ejecutor, ServicioRemoto servicio, CuentaRepository cuenta) {
        this.db = db;
        this.marcas = db.marcaSincronizacionDao();
        this.ejecutor = ejecutor;
        this.servicio = servicio;
        this.cuenta = cuenta;
    }

    /** Cuántos cambios faltan por subir y cuándo se subió por última vez. Solo lee del teléfono. */
    public void estado(CallbackRemoto<EstadoSincronizacion> callback) {
        ejecutor.ejecutar(() -> {
            MarcaSincronizacion resumen = marcas.de(MarcaSincronizacion.RESUMEN);
            String ultima = resumen == null ? null : resumen.sincronizadoEn;
            String idFinca = db.configuracionDao().idFincaActiva();
            return new EstadoSincronizacion(idFinca == null ? 0 : pendientes(idFinca), ultima);
        }, callback);
    }

    private int pendientes(String idFinca) {
        int pendientes = 0;
        for (Tabla tabla : TABLAS) if (!tabla.completa()) pendientes += contarPendientes(tabla, idFinca);
        return pendientes;
    }

    /**
     * El intento silencioso al abrir la app: sube solo si hay algo pendiente y se puede (sesión, finca vinculada,
     * conexión). Cualquier problema se ignora, sin avisar: la persona siempre puede usar el botón de la pantalla.
     */
    public void sincronizarSiHaceFalta() {
        ejecutor.ejecutar(() -> {
            String idFinca = db.configuracionDao().idFincaActiva();
            if (idFinca == null || pendientes(idFinca) == 0) return null;
            if (!enCurso.compareAndSet(false, true)) return null;
            try {
                subirTodo(null);
            } finally {
                enCurso.set(false);
            }
            return null;
        }, new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) { /* En silencio. */ }
            @Override public void error(ErrorRemoto error) { /* En silencio: sin sesión, sin red o sin vincular no es motivo para avisar. */ }
        });
    }

    /**
     * Sube todo lo pendiente de la finca vinculada a la cuenta con la sesión abierta. Si ya hay una sincronización en
     * marcha, la segunda no se repite. Si algo falla, lo ya confirmado queda y se puede reintentar.
     */
    public void sincronizar(Progreso progreso, CallbackRemoto<ResultadoSincronizacion> callback) {
        ejecutor.ejecutar(() -> {
            if (!enCurso.compareAndSet(false, true)) throw ErrorRemoto.regla(ErrorRemoto.EN_CURSO);
            try {
                return subirTodo(progreso);
            } finally {
                enCurso.set(false);
            }
        }, callback);
    }

    /** Olvida lo subido: la próxima sincronización envía todo otra vez (es seguro, el servidor no duplica). */
    public void volverAEnviarTodo(CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            marcas.borrarMarcasDeDatos();
            return null;
        }, callback);
    }

    // --- Subir ---

    private ResultadoSincronizacion subirTodo(Progreso progreso) throws ErrorRemoto {
        String idFinca = cuenta.fincaDelDueno();
        int[] subidas = {0};
        for (Tabla tabla : TABLAS) {
            if (tabla.completa()) subirCompleta(tabla, idFinca);
            else subirPendientes(tabla, idFinca, subidas, progreso);
        }
        MarcaSincronizacion resumen = new MarcaSincronizacion();
        resumen.tabla = MarcaSincronizacion.RESUMEN;
        resumen.sincronizadoEn = Tiempo.ahoraUtc();
        marcas.guardar(resumen);
        return new ResultadoSincronizacion(subidas[0]);
    }

    /** Una tabla sin fechas (razas): todas sus filas, en lotes, cada vez. */
    private void subirCompleta(Tabla tabla, String idFinca) throws ErrorRemoto {
        JSONArray todas = leer("SELECT * FROM `" + tabla.nombre + "` ORDER BY `" + tabla.id + "`", new String[0]);
        for (int desde = 0; desde < todas.length(); desde += LOTE) {
            JSONArray lote = new JSONArray();
            for (int i = desde; i < Math.min(desde + LOTE, todas.length()); i++) lote.put(filaDe(todas, i));
            enviar(idFinca, tabla, lote);
        }
    }

    private void subirPendientes(Tabla tabla, String idFinca, int[] subidas, Progreso progreso) throws ErrorRemoto {
        MarcaSincronizacion marca = marcas.de(tabla.nombre);
        while (true) {
            List<String> args = new ArrayList<>();
            args.add(idFinca);
            String condicion = tabla.filtro + posterior(tabla, marca, args);
            JSONArray lote = leer("SELECT * FROM `" + tabla.nombre + "` WHERE " + condicion + " ORDER BY `" + tabla.fecha + "`, `" + tabla.id
                    + "` LIMIT " + LOTE, args.toArray(new String[0]));
            if (lote.length() == 0) return;
            enviar(idFinca, tabla, lote);
            JSONObject ultima = filaDe(lote, lote.length() - 1);
            marca = new MarcaSincronizacion();
            marca.tabla = tabla.nombre;
            marca.hastaEn = ultima.optString(tabla.fecha);
            marca.hastaId = ultima.optString(tabla.id);
            marca.sincronizadoEn = Tiempo.ahoraUtc();
            marcas.guardar(marca); // solo después de que el servidor confirmó el lote
            subidas[0] += lote.length();
            avisar(progreso, subidas[0]);
            if (lote.length() < LOTE) return;
        }
    }

    private void enviar(String idFinca, Tabla tabla, JSONArray lote) throws ErrorRemoto {
        cuenta.conSesion(sesion -> servicio.subirCambios(sesion.accesoToken, idFinca, tabla.nombre, lote));
    }

    private void avisar(Progreso progreso, int subidas) {
        if (progreso != null) principal.post(() -> progreso.alAvanzar(subidas));
    }

    // --- Leer ---

    /** "AND (fecha > ? OR (fecha = ? AND id > ?))" con los valores de la marca; vacío si la tabla nunca se subió. */
    private static String posterior(Tabla tabla, MarcaSincronizacion marca, List<String> args) {
        if (marca == null) return "";
        args.add(marca.hastaEn);
        args.add(marca.hastaEn);
        args.add(marca.hastaId);
        return " AND (`" + tabla.fecha + "` > ? OR (`" + tabla.fecha + "` = ? AND `" + tabla.id + "` > ?))";
    }

    private int contarPendientes(Tabla tabla, String idFinca) {
        List<String> args = new ArrayList<>();
        args.add(idFinca);
        String condicion = tabla.filtro + posterior(tabla, marcas.de(tabla.nombre), args);
        SupportSQLiteDatabase sql = db.getOpenHelper().getReadableDatabase();
        try (Cursor c = sql.query("SELECT COUNT(*) FROM `" + tabla.nombre + "` WHERE " + condicion, args.toArray(new Object[0]))) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    /** Las filas de una consulta como JSON, con los nombres de columna de la base (que son los del servidor). */
    private JSONArray leer(String consulta, String[] args) throws ErrorRemoto {
        JSONArray filas = new JSONArray();
        SupportSQLiteDatabase sql = db.getOpenHelper().getReadableDatabase();
        try (Cursor c = sql.query(consulta, (Object[]) args)) {
            String[] columnas = c.getColumnNames();
            while (c.moveToNext()) {
                JSONObject fila = new JSONObject();
                for (int i = 0; i < columnas.length; i++) {
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL: fila.put(columnas[i], JSONObject.NULL); break;
                        case Cursor.FIELD_TYPE_INTEGER: fila.put(columnas[i], c.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT: fila.put(columnas[i], c.getDouble(i)); break;
                        default: fila.put(columnas[i], c.getString(i));
                    }
                }
                filas.put(fila);
            }
        } catch (JSONException ex) {
            throw ErrorRemoto.de(ErrorRemoto.Tipo.LOCAL, null, "No se pudo preparar el respaldo", ex);
        }
        return filas;
    }

    private static JSONObject filaDe(JSONArray filas, int posicion) throws ErrorRemoto {
        try {
            return filas.getJSONObject(posicion);
        } catch (JSONException ex) {
            throw ErrorRemoto.de(ErrorRemoto.Tipo.LOCAL, null, "No se pudo preparar el respaldo", ex);
        }
    }
}
