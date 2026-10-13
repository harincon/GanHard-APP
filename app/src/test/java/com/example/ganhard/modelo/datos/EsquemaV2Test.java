package com.example.ganhard.modelo.datos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.ganhard.modelo.entidad.Raza;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public class EsquemaV2Test {
    private static final String INSTANTE = "2026-10-12T15:00:00.000Z";
    private static final String CARPETA_ESQUEMAS = "schemas/com.example.ganhard.modelo.datos.GanHardDatabase/";

    private final Context contexto = RuntimeEnvironment.getApplication();
    private GanHardDatabase nueva;

    @Before public void abrir() {
        nueva = GanHardDatabase.constructor(contexto, null).allowMainThreadQueries().build();
    }
    @After public void cerrar() { nueva.close(); }

    private SupportSQLiteDatabase sql() { return nueva.getOpenHelper().getWritableDatabase(); }

    private void usuario(SupportSQLiteDatabase db, String id) {
        db.execSQL("INSERT INTO usuario (id_usuario, nombre, correo, activo, creado_en, actualizado_en, version) VALUES (?, ?, ?, 1, ?, ?, 0)",
                new Object[]{id, "Helver", "h@ejemplo.com", INSTANTE, INSTANTE});
    }
    private void finca(SupportSQLiteDatabase db, String id, String propietario) {
        db.execSQL("INSERT INTO finca (id_finca, id_propietario, nombre, activa, creado_en, actualizado_en, version) VALUES (?, ?, ?, 1, ?, ?, 0)",
                new Object[]{id, propietario, "La Esperanza", INSTANTE, INSTANTE});
    }
    private void animal(SupportSQLiteDatabase db, String id, String idFinca, String codigo) {
        db.execSQL("INSERT INTO animal (id_animal, id_finca, codigo, sexo, fecha_ingreso, estado, fecha_estado, creado_en, actualizado_en, version) "
                + "VALUES (?, ?, ?, 'H', '2026-01-10', 'ACTIVO', '2026-01-10', ?, ?, 0)", new Object[]{id, idFinca, codigo, INSTANTE, INSTANTE});
    }

    /** Lee las sentencias CREATE de una versión exportada del esquema: {nombre, sql}. */
    private List<String[]> esquema(int version) throws Exception {
        String json = new String(Files.readAllBytes(new File(CARPETA_ESQUEMAS + version + ".json").toPath()), StandardCharsets.UTF_8);
        JSONArray entidades = new JSONObject(json).getJSONObject("database").getJSONArray("entities");
        List<String[]> sentencias = new ArrayList<>();
        for (int i = 0; i < entidades.length(); i++) {
            JSONObject entidad = entidades.getJSONObject(i);
            String tabla = entidad.getString("tableName");
            sentencias.add(new String[]{tabla, entidad.getString("createSql").replace("${TABLE_NAME}", tabla)});
            JSONArray indices = entidad.optJSONArray("indices");
            for (int j = 0; indices != null && j < indices.length(); j++) {
                JSONObject indice = indices.getJSONObject(j);
                sentencias.add(new String[]{indice.getString("name"), indice.getString("createSql").replace("${TABLE_NAME}", tabla)});
            }
        }
        return sentencias;
    }

    /** Base en memoria con exactamente el esquema exportado de la versión 1. */
    private SupportSQLiteDatabase baseV1() throws Exception {
        final List<String[]> v1 = esquema(1);
        SupportSQLiteOpenHelper.Configuration configuracion = SupportSQLiteOpenHelper.Configuration.builder(contexto)
                .callback(new SupportSQLiteOpenHelper.Callback(1) {
                    @Override public void onCreate(SupportSQLiteDatabase db) { for (String[] s : v1) db.execSQL(s[1]); }
                    @Override public void onUpgrade(SupportSQLiteDatabase db, int anterior, int nueva) {}
                }).build();
        SupportSQLiteDatabase db = new FrameworkSQLiteOpenHelperFactory().create(configuracion).getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys = ON");
        return db;
    }

    private static String normalizar(String sql) { return sql.replace("IF NOT EXISTS ", "").replaceAll("\\s+", " ").trim(); }

    private String sqlGuardado(SupportSQLiteDatabase db, String nombre) {
        try (Cursor c = db.query("SELECT sql FROM sqlite_master WHERE name = ?", new Object[]{nombre})) {
            assertTrue("No existe " + nombre, c.moveToFirst());
            return c.getString(0);
        }
    }

    @Test public void migrarV1AV2ConservaUsuarioYFinca() throws Exception {
        SupportSQLiteDatabase db = baseV1();
        usuario(db, "u1");
        finca(db, "f1", "u1");
        Migraciones.V1_V2.migrate(db);
        try (Cursor c = db.query("SELECT nombre FROM finca WHERE id_finca = 'f1'")) {
            assertTrue(c.moveToFirst());
            assertEquals("La Esperanza", c.getString(0));
        }
        try (Cursor c = db.query("SELECT COUNT(*) FROM usuario")) {
            c.moveToFirst();
            assertEquals(1, c.getInt(0));
        }
        animal(db, "a1", "f1", "GH-001"); // las tablas nuevas ya aceptan datos que referencian a la finca migrada
        db.close();
    }

    @Test public void laMigracionCreaElMismoEsquemaQueRoomExporta() throws Exception {
        SupportSQLiteDatabase db = baseV1();
        Migraciones.V1_V2.migrate(db);
        Set<String> anteriores = new HashSet<>();
        for (String[] s : esquema(1)) anteriores.add(s[0]);
        int comparados = 0;
        for (String[] s : esquema(2)) {
            if (anteriores.contains(s[0])) continue;
            assertEquals(s[0], normalizar(s[1]), normalizar(sqlGuardado(db, s[0])));
            comparados++;
        }
        assertEquals("raza + animal + pesaje y sus 4 índices", 3 + 4, comparados);
        db.close();
    }

    @Test public void baseNuevaTieneCatalogoDeRazas() {
        List<Raza> razas = nueva.razaDao().todas();
        assertEquals(CatalogoRazas.RAZAS.length, razas.size());
        Set<String> nombres = new HashSet<>();
        for (Raza raza : razas) nombres.add(raza.nombre);
        assertEquals(razas.size(), nombres.size());
    }

    @Test public void migracionYBaseNuevaSiembranLasMismasRazas() throws Exception {
        SupportSQLiteDatabase db = baseV1();
        Migraciones.V1_V2.migrate(db);
        Set<String> migradas = new HashSet<>();
        try (Cursor c = db.query("SELECT id_raza FROM raza")) { while (c.moveToNext()) migradas.add(c.getString(0)); }
        db.close();
        Set<String> nuevas = new HashSet<>();
        for (Raza raza : nueva.razaDao().todas()) nuevas.add(raza.idRaza);
        assertEquals(nuevas, migradas);
    }

    @Test public void codigoDuplicadoEnLaMismaFincaLanzaConstraint() {
        usuario(sql(), "u1");
        finca(sql(), "f1", "u1");
        animal(sql(), "a1", "f1", "GH-001");
        try { animal(sql(), "a2", "f1", "gh-001"); fail("Debía rechazar el código repetido sin distinguir mayúsculas"); }
        catch (SQLiteConstraintException esperado) { /* Unicidad por finca. */ }
    }

    @Test public void mismoCodigoEnOtraFincaEsValido() {
        usuario(sql(), "u1");
        finca(sql(), "f1", "u1");
        finca(sql(), "f2", "u1");
        animal(sql(), "a1", "f1", "GH-001");
        animal(sql(), "a2", "f2", "GH-001");
        try (Cursor c = sql().query("SELECT COUNT(*) FROM animal")) {
            c.moveToFirst();
            assertEquals(2, c.getInt(0));
        }
    }

    @Test public void pesajeConAnimalInexistenteLanzaConstraint() {
        try {
            sql().execSQL("INSERT INTO pesaje (id_pesaje, id_animal, fecha, peso_kg, es_antecedente, anulado, creado_en, actualizado_en, version) "
                    + "VALUES ('p1', 'no-existe', '2026-02-01', 350.5, 0, 0, ?, ?, 0)", new Object[]{INSTANTE, INSTANTE});
            fail("Debía rechazar el pesaje de un animal inexistente");
        } catch (SQLiteConstraintException esperado) { /* Integridad referencial. */ }
    }
}
