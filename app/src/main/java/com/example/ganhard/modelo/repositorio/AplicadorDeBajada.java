package com.example.ganhard.modelo.repositorio;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.ganhard.modelo.datos.ConfiguracionDao;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.GeneradorCodigo;
import com.example.ganhard.modelo.remoto.DatosFincaRemota;
import com.example.ganhard.modelo.remoto.IntegranteRemoto;
import com.example.ganhard.modelo.remoto.PersonaRemota;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Guarda en el teléfono lo que se baja del servidor (etapa 6.2). Una fila se guarda si no existe o si la que llega es más
 * reciente que la local (gana el último cambio); las razas y el historial solo se agregan. Se usan únicamente las
 * columnas que existen en la tabla local (el servidor trae alguna de más). Dos animales distintos con el mismo código se
 * resuelven renumerando el más reciente, con una entrada de historial.
 */
final class AplicadorDeBajada {
    private final GanHardDatabase db;
    private final RegistroCambios registro;
    private final Map<String, List<String>> columnas = new HashMap<>();

    AplicadorDeBajada(GanHardDatabase db) {
        this.db = db;
        this.registro = new RegistroCambios(db);
    }

    /**
     * Deja una persona local por cada cuenta que usa la finca (las que no existen se crean con el id de su cuenta) y
     * devuelve id de cuenta → id de la persona local. {@code cuentaDeEsteTelefono}, si ya está en la lista, queda marcada
     * como quien usa este teléfono cuando se crea.
     */
    Map<String, String> registrarPersonas(DatosFincaRemota datos, String cuentaDeEsteTelefono) {
        ConfiguracionDao dao = db.configuracionDao();
        Map<String, String> mapa = new HashMap<>();
        String ahora = Tiempo.ahoraUtc();
        for (PersonaRemota persona : datos.personas) {
            Usuario local = dao.usuarioDeCuenta(persona.idCuenta);
            if (local == null) {
                local = new Usuario();
                local.idUsuario = persona.idCuenta;
                local.idCuentaRemota = persona.idCuenta;
                local.nombre = persona.nombre;
                local.correo = persona.correo;
                local.activo = IntegranteRemoto.ACTIVO.equals(persona.estado) ? 1 : 0;
                local.deEsteTelefono = persona.idCuenta.equals(cuentaDeEsteTelefono) ? 1 : 0;
                local.creadoEn = ahora;
                local.actualizadoEn = ahora;
                dao.guardarPersona(local);
            }
            mapa.put(persona.idCuenta, local.idUsuario);
        }
        return mapa;
    }

    /**
     * Aplica un lote de filas de una tabla, en una transacción. {@code columnaFecha} nulo es una tabla que solo agrega.
     * Devuelve cuántas filas guardó o cambió.
     */
    int aplicar(String tabla, String columnaId, String columnaFecha, JSONArray filas, String idFinca,
                Map<String, String> personasLocales, String autorPorOmision, java.util.Set<String> recibidas) {
        return db.runInTransaction(() -> {
            SupportSQLiteDatabase sql = db.getOpenHelper().getWritableDatabase();
            sql.execSQL("PRAGMA defer_foreign_keys = ON");
            List<String> locales = columnasDe(sql, tabla);
            int cambiadas = 0;
            for (int i = 0; i < filas.length(); i++) {
                ContentValues valores;
                try {
                    valores = valoresDe(filas.getJSONObject(i), locales);
                } catch (JSONException ex) {
                    throw new IllegalStateException("Fila inválida en " + tabla, ex);
                }
                if ("historial_cambio".equals(tabla)) {
                    valores.put("id_usuario", usuarioLocal(sql, valores.getAsString("id_usuario"), personasLocales, autorPorOmision));
                }
                if (columnaFecha == null) {
                    if (sql.insert(tabla, SQLiteDatabase.CONFLICT_IGNORE, valores) != -1) { cambiadas++; recibidas.add(tabla + "|" + valores.getAsString(columnaId)); }
                    continue;
                }
                String id = valores.getAsString(columnaId), fechaLocal = fechaLocal(sql, tabla, columnaId, columnaFecha, id);
                if (fechaLocal != null && valores.getAsString(columnaFecha).compareTo(fechaLocal) <= 0) continue; // la local es igual o más nueva
                if ("animal".equals(tabla)) resolverCodigoRepetido(sql, valores, idFinca);
                if (fechaLocal == null) sql.insert(tabla, SQLiteDatabase.CONFLICT_ABORT, valores);
                else sql.update(tabla, SQLiteDatabase.CONFLICT_NONE, valores, "`" + columnaId + "` = ?", new Object[]{id});
                cambiadas++;
                recibidas.add(tabla + "|" + id);
            }
            return cambiadas;
        });
    }

    // --- Apoyos ---

    private List<String> columnasDe(SupportSQLiteDatabase sql, String tabla) {
        List<String> lista = columnas.get(tabla);
        if (lista != null) return lista;
        lista = new ArrayList<>();
        try (Cursor c = sql.query("PRAGMA table_info(`" + tabla + "`)")) {
            while (c.moveToNext()) lista.add(c.getString(c.getColumnIndexOrThrow("name")));
        }
        columnas.put(tabla, lista);
        return lista;
    }

    private static ContentValues valoresDe(JSONObject fila, List<String> locales) throws JSONException {
        ContentValues valores = new ContentValues();
        for (String columna : locales) {
            if (!fila.has(columna)) continue;
            Object valor = fila.get(columna);
            if (valor == JSONObject.NULL) valores.putNull(columna);
            else if (valor instanceof Boolean) valores.put(columna, ((Boolean) valor) ? 1L : 0L);
            else if (valor instanceof Integer || valor instanceof Long) valores.put(columna, ((Number) valor).longValue());
            else if (valor instanceof Number) valores.put(columna, ((Number) valor).doubleValue());
            else valores.put(columna, valor.toString());
        }
        return valores;
    }

    private static String fechaLocal(SupportSQLiteDatabase sql, String tabla, String columnaId, String columnaFecha, String id) {
        try (Cursor c = sql.query("SELECT `" + columnaFecha + "` FROM `" + tabla + "` WHERE `" + columnaId + "` = ?", new Object[]{id})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    /** El servidor habla en ids de cuenta; el historial local apunta a personas locales. */
    private static String usuarioLocal(SupportSQLiteDatabase sql, String idCuenta, Map<String, String> personasLocales, String porOmision) {
        String local = personasLocales.get(idCuenta);
        if (local != null) return local;
        try (Cursor c = sql.query("SELECT 1 FROM usuario WHERE id_usuario = ?", new Object[]{idCuenta})) {
            return c.moveToFirst() ? idCuenta : porOmision;
        }
    }

    /** Si el animal que llega tiene el código de otro animal distinto, se renumera el más reciente (por fecha de creación). */
    private void resolverCodigoRepetido(SupportSQLiteDatabase sql, ContentValues animal, String idFinca) {
        String codigo = animal.getAsString("codigo"), id = animal.getAsString("id_animal");
        String idOtro = null, creadoOtro = null;
        try (Cursor c = sql.query("SELECT id_animal, creado_en FROM animal WHERE id_finca = ? AND codigo = ? COLLATE NOCASE AND id_animal <> ?",
                new Object[]{idFinca, codigo, id})) {
            if (c.moveToFirst()) { idOtro = c.getString(0); creadoOtro = c.getString(1); }
        }
        if (idOtro == null) return;
        List<String> codigos = new ArrayList<>();
        codigos.add(codigo);
        try (Cursor c = sql.query("SELECT codigo FROM animal WHERE id_finca = ?", new Object[]{idFinca})) {
            while (c.moveToNext()) codigos.add(c.getString(0));
        }
        String nuevo = GeneradorCodigo.siguiente(codigos), ahora = Tiempo.ahoraUtc();
        if (creadoOtro.compareTo(animal.getAsString("creado_en")) > 0) {
            // El animal local es el más reciente: es el que cambia de código.
            sql.execSQL("UPDATE animal SET codigo = ?, actualizado_en = ? WHERE id_animal = ?", new Object[]{nuevo, ahora, idOtro});
            anotarRenumeracion(idOtro, codigo, nuevo, ahora);
        } else {
            // Llega el más reciente: se guarda con otro código y con fecha nueva, para que el servidor también lo actualice.
            animal.put("codigo", nuevo);
            animal.put("actualizado_en", ahora);
            anotarRenumeracion(id, codigo, nuevo, ahora);
        }
    }

    private void anotarRenumeracion(String idAnimal, String viejo, String nuevo, String ahora) {
        try {
            JSONObject antes = new JSONObject().put("codigo", viejo), despues = new JSONObject().put("codigo", nuevo);
            HistorialCambio cambio = registro.preparar(HistorialCambio.ANIMAL, idAnimal, HistorialCambio.EDITAR, antes, despues,
                    "Código repetido: se renumeró al sincronizar", null, ahora);
            db.historialDao().insertar(cambio);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo anotar la renumeración", ex);
        }
    }
}
