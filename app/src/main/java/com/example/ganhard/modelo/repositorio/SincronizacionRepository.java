package com.example.ganhard.modelo.repositorio;

import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.ganhard.modelo.datos.ConfiguracionDao;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.datos.MarcaSincronizacionDao;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.MarcaSincronizacion;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.DatosFincaRemota;
import com.example.ganhard.modelo.remoto.EjecutorRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.remoto.ServicioRemoto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Respaldo en la nube y trabajo entre varios teléfonos (etapas 6.1 a 6.3). Sube lo que cambió en la finca, baja lo que
 * cambió en otros teléfonos y puede recuperar una finca completa en un teléfono vacío. Gana siempre el último cambio.
 *
 * <p>Qué falta por subir: en cada tabla, lo que esté después de su marca (fecha e id del último registro confirmado). Qué
 * falta por bajar: lo mismo, con otra marca (la de la tabla precedida por {@code bajada:}). Las marcas avanzan solo cuando
 * un lote se confirma o se aplica, así que un corte a medias se reintenta sin duplicar. Las razas no tienen fechas ni
 * cambian: se envían completas cada vez y se bajan por id.
 */
public final class SincronizacionRepository {
    /** Avisa, en el hilo principal, cuántos registros van subidos y bajados en esta sincronización. */
    public interface Progreso {
        void alAvanzar(int registros);
    }

    static final int LOTE = 200, LOTE_DE_BAJADA = 500;
    private static final String PREFIJO_BAJADA = "bajada:";

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

    private static final class Contador {
        int subidas, bajadas;
        /** Lo que se acaba de bajar (tabla|id): se vuelve a enviar una vez, pero no cuenta como subido por esta persona. */
        final java.util.Set<String> recibidas = new java.util.HashSet<>();
        int total() { return subidas + bajadas; }
    }

    private static final class DatosYCuenta {
        final String idCuenta;
        final DatosFincaRemota datos;
        DatosYCuenta(String idCuenta, DatosFincaRemota datos) { this.idCuenta = idCuenta; this.datos = datos; }
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
    private final AplicadorDeBajada aplicador;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final AtomicBoolean enCurso = new AtomicBoolean(false);

    public SincronizacionRepository(GanHardDatabase db, EjecutorRemoto ejecutor, ServicioRemoto servicio, CuentaRepository cuenta) {
        this.db = db;
        this.marcas = db.marcaSincronizacionDao();
        this.ejecutor = ejecutor;
        this.servicio = servicio;
        this.cuenta = cuenta;
        this.aplicador = new AplicadorDeBajada(db);
    }

    /** Cuántos cambios faltan por subir y cuándo se sincronizó por última vez. Solo lee del teléfono. */
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
     * Sincroniza la finca vinculada a la cuenta con la sesión abierta: primero baja lo que cambió en otros teléfonos y
     * después sube lo propio. Si ya hay una sincronización en marcha, la segunda no se repite. Si algo falla, lo ya
     * confirmado queda y se puede reintentar.
     */
    public void sincronizar(Progreso progreso, CallbackRemoto<ResultadoSincronizacion> callback) {
        ejecutor.ejecutar(() -> {
            if (!enCurso.compareAndSet(false, true)) throw ErrorRemoto.regla(ErrorRemoto.EN_CURSO);
            try {
                String idFinca = cuenta.fincaDelDueno();
                Contador contador = new Contador();
                bajarTodo(idFinca, contador, progreso);
                subirTodo(idFinca, contador, progreso);
                terminar();
                return new ResultadoSincronizacion(contador.subidas, contador.bajadas);
            } finally {
                enCurso.set(false);
            }
        }, callback);
    }

    /**
     * El intento silencioso al abrir la app: baja siempre lo que haya y sube solo si hay algo pendiente. Cualquier problema
     * se ignora, sin avisar: la persona siempre puede usar el botón de la pantalla.
     */
    public void sincronizarSiHaceFalta() {
        ejecutor.ejecutar(() -> {
            if (db.configuracionDao().idFincaActiva() == null) return null;
            if (!enCurso.compareAndSet(false, true)) return null;
            try {
                String idFinca = cuenta.fincaDelDueno();
                Contador contador = new Contador();
                bajarTodo(idFinca, contador, null);
                if (pendientes(idFinca) > 0) subirTodo(idFinca, contador, null);
                terminar();
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
     * Recupera una finca de la cuenta en un teléfono vacío: crea la finca y las personas que la usan, y baja todos sus
     * datos. Quien usa el teléfono queda como la persona de este teléfono (administrador si es la dueña; si no, encargado).
     * Solo funciona si el teléfono no tiene ninguna finca.
     */
    public void recuperarFinca(String idFinca, Progreso progreso, CallbackRemoto<ResultadoSincronizacion> callback) {
        ejecutor.ejecutar(() -> {
            if (!enCurso.compareAndSet(false, true)) throw ErrorRemoto.regla(ErrorRemoto.EN_CURSO);
            try {
                if (db.configuracionDao().idFincaActiva() != null) throw ErrorRemoto.regla(ErrorRemoto.YA_HAY_FINCA);
                DatosYCuenta recibido = cuenta.conSesion(sesion -> new DatosYCuenta(sesion.idCuenta, servicio.datosDeFinca(sesion.accesoToken, idFinca)));
                crearFincaLocal(recibido);
                Contador contador = new Contador();
                bajarTodo(idFinca, contador, progreso);
                // El teléfono estaba vacío: lo que bajó es justo lo que tiene el servidor, así que nada de eso falta por subir.
                for (Tabla tabla : TABLAS) {
                    MarcaSincronizacion bajada = marcas.de(PREFIJO_BAJADA + tabla.nombre);
                    if (tabla.completa() || bajada == null) continue;
                    MarcaSincronizacion subida = new MarcaSincronizacion();
                    subida.tabla = tabla.nombre;
                    subida.hastaEn = bajada.hastaEn;
                    subida.hastaId = bajada.hastaId;
                    subida.sincronizadoEn = bajada.sincronizadoEn;
                    marcas.guardar(subida);
                }
                terminar();
                return new ResultadoSincronizacion(0, contador.bajadas);
            } finally {
                enCurso.set(false);
            }
        }, callback);
    }

    /** Olvida lo subido: la próxima sincronización envía todo otra vez (es seguro, el servidor no duplica). Lo bajado no se toca. */
    public void volverAEnviarTodo(CallbackRemoto<Void> callback) {
        ejecutor.ejecutar(() -> {
            marcas.borrarMarcasDeSubida();
            return null;
        }, callback);
    }

    private void terminar() {
        MarcaSincronizacion resumen = new MarcaSincronizacion();
        resumen.tabla = MarcaSincronizacion.RESUMEN;
        resumen.sincronizadoEn = Tiempo.ahoraUtc();
        marcas.guardar(resumen);
    }

    // --- Recuperar ---

    private void crearFincaLocal(DatosYCuenta recibido) throws ErrorRemoto {
        ConfiguracionDao dao = db.configuracionDao();
        try {
            db.runInTransaction(() -> {
                Map<String, String> personas = aplicador.registrarPersonas(recibido.datos, recibido.idCuenta);
                Usuario dueno = dao.usuario(personas.get(recibido.datos.idPropietario));
                if (dueno == null) throw new IllegalStateException("La finca no tiene dueño en el servidor");
                Finca finca = new Finca();
                finca.idFinca = recibido.datos.finca.idFinca;
                finca.idPropietario = dueno.idUsuario;
                finca.nombre = recibido.datos.finca.nombre;
                finca.ubicacion = recibido.datos.finca.ubicacion;
                finca.areaHectareas = recibido.datos.finca.areaHectareas;
                finca.vinculadaEn = Tiempo.ahoraUtc();
                finca.creadoEn = recibido.datos.creadoEn;
                finca.actualizadoEn = recibido.datos.creadoEn;
                dao.guardar(dueno, finca);
            });
        } catch (IllegalStateException ex) {
            throw ErrorRemoto.de(ErrorRemoto.Tipo.LOCAL, null, ex.getMessage(), ex);
        }
    }

    // --- Bajar ---

    private void bajarTodo(String idFinca, Contador contador, Progreso progreso) throws ErrorRemoto {
        String cuentaDeEsteTelefono = db.configuracionDao().usuarioDeEsteTelefono() == null ? null
                : db.configuracionDao().usuarioDeEsteTelefono().idCuentaRemota;
        DatosFincaRemota datos = cuenta.conSesion(sesion -> servicio.datosDeFinca(sesion.accesoToken, idFinca));
        Map<String, String> personas = aplicador.registrarPersonas(datos, cuentaDeEsteTelefono);
        String autorPorOmision = autorPorOmision();
        for (Tabla tabla : TABLAS) {
            MarcaSincronizacion[] marca = {marcas.de(PREFIJO_BAJADA + tabla.nombre)};
            while (true) {
                String desdeEn = marca[0] == null || tabla.completa() ? null : marca[0].hastaEn;
                String desdeId = marca[0] == null ? null : marca[0].hastaId;
                JSONArray lote = cuenta.conSesion(sesion -> servicio.bajarCambios(sesion.accesoToken, idFinca, tabla.nombre, desdeEn, desdeId, LOTE_DE_BAJADA));
                if (lote.length() == 0) break;
                int cambiadas = aplicador.aplicar(tabla.nombre, tabla.id, tabla.fecha, lote, idFinca, personas, autorPorOmision, contador.recibidas);
                JSONObject ultima = fila(lote, lote.length() - 1);
                MarcaSincronizacion nueva = new MarcaSincronizacion();
                nueva.tabla = PREFIJO_BAJADA + tabla.nombre;
                nueva.hastaEn = tabla.completa() ? "" : ultima.optString(tabla.fecha);
                nueva.hastaId = ultima.optString(tabla.id);
                nueva.sincronizadoEn = Tiempo.ahoraUtc();
                marcas.guardar(nueva); // solo después de aplicar el lote
                marca[0] = nueva;
                contador.bajadas += cambiadas;
                if (cambiadas > 0) avisar(progreso, contador.total());
                if (lote.length() < LOTE_DE_BAJADA) break;
            }
        }
    }

    private String autorPorOmision() {
        Usuario esteTelefono = db.configuracionDao().usuarioDeEsteTelefono();
        if (esteTelefono != null) return esteTelefono.idUsuario;
        Finca finca = db.configuracionDao().fincaActiva();
        return finca == null ? "" : finca.idPropietario;
    }

    // --- Subir ---

    private void subirTodo(String idFinca, Contador contador, Progreso progreso) throws ErrorRemoto {
        for (Tabla tabla : TABLAS) {
            if (tabla.completa()) subirCompleta(tabla, idFinca);
            else subirPendientes(tabla, idFinca, contador, progreso);
        }
    }

    /** Una tabla sin fechas (razas): todas sus filas, en lotes, cada vez. */
    private void subirCompleta(Tabla tabla, String idFinca) throws ErrorRemoto {
        JSONArray todas = leer(tabla, "SELECT * FROM `" + tabla.nombre + "` ORDER BY `" + tabla.id + "`", new String[0]);
        for (int desde = 0; desde < todas.length(); desde += LOTE) {
            JSONArray lote = new JSONArray();
            for (int i = desde; i < Math.min(desde + LOTE, todas.length()); i++) lote.put(fila(todas, i));
            enviar(idFinca, tabla, lote);
        }
    }

    private void subirPendientes(Tabla tabla, String idFinca, Contador contador, Progreso progreso) throws ErrorRemoto {
        MarcaSincronizacion marca = marcas.de(tabla.nombre);
        while (true) {
            List<String> args = new ArrayList<>();
            args.add(idFinca);
            String condicion = tabla.filtro + posterior(tabla, marca, args);
            JSONArray lote = leer(tabla, "SELECT * FROM `" + tabla.nombre + "` WHERE " + condicion + " ORDER BY `" + tabla.fecha + "`, `" + tabla.id
                    + "` LIMIT " + LOTE, args.toArray(new String[0]));
            if (lote.length() == 0) return;
            enviar(idFinca, tabla, lote);
            JSONObject ultima = fila(lote, lote.length() - 1);
            marca = new MarcaSincronizacion();
            marca.tabla = tabla.nombre;
            marca.hastaEn = ultima.optString(tabla.fecha);
            marca.hastaId = ultima.optString(tabla.id);
            marca.sincronizadoEn = Tiempo.ahoraUtc();
            marcas.guardar(marca); // solo después de que el servidor confirmó el lote
            for (int i = 0; i < lote.length(); i++) {
                if (!contador.recibidas.contains(tabla.nombre + "|" + fila(lote, i).optString(tabla.id))) contador.subidas++;
            }
            avisar(progreso, contador.total());
            if (lote.length() < LOTE) return;
        }
    }

    private void enviar(String idFinca, Tabla tabla, JSONArray lote) throws ErrorRemoto {
        cuenta.conSesion(sesion -> servicio.subirCambios(sesion.accesoToken, idFinca, tabla.nombre, lote));
    }

    private void avisar(Progreso progreso, int registros) {
        if (progreso != null) principal.post(() -> progreso.alAvanzar(registros));
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

    /**
     * Las filas de una consulta como JSON, con los nombres de columna de la base (que son los del servidor). El servidor
     * habla en ids de cuenta, así que el autor del historial se envía como el id de la cuenta de esa persona.
     */
    private JSONArray leer(Tabla tabla, String consulta, String[] args) throws ErrorRemoto {
        Map<String, String> cuentas = "historial_cambio".equals(tabla.nombre) ? cuentasDeLasPersonas() : new HashMap<>();
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
                if ("historial_cambio".equals(tabla.nombre)) {
                    String cuentaDelAutor = cuentas.get(fila.optString("id_usuario"));
                    if (cuentaDelAutor != null) fila.put("id_usuario", cuentaDelAutor);
                }
                filas.put(fila);
            }
        } catch (JSONException ex) {
            throw ErrorRemoto.de(ErrorRemoto.Tipo.LOCAL, null, "No se pudo preparar el respaldo", ex);
        }
        return filas;
    }

    /** id de la persona local → id de su cuenta (solo las que ya tienen cuenta). */
    private Map<String, String> cuentasDeLasPersonas() {
        Map<String, String> cuentas = new HashMap<>();
        SupportSQLiteDatabase sql = db.getOpenHelper().getReadableDatabase();
        try (Cursor c = sql.query("SELECT id_usuario, id_cuenta_remota FROM usuario WHERE id_cuenta_remota IS NOT NULL")) {
            while (c.moveToNext()) cuentas.put(c.getString(0), c.getString(1));
        }
        return cuentas;
    }

    private static JSONObject fila(JSONArray filas, int posicion) throws ErrorRemoto {
        try {
            return filas.getJSONObject(posicion);
        } catch (JSONException ex) {
            throw ErrorRemoto.de(ErrorRemoto.Tipo.LOCAL, null, "No se pudo preparar el respaldo", ex);
        }
    }
}
