package com.example.ganhard.modelo.datos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Migración 1 a 2 con el ayudante oficial de Room: crea la base con el esquema exportado de la versión 1,
 * la migra y deja que Room valide el resultado contra el esquema de la versión 2. Se ejecuta en un
 * dispositivo o emulador (connectedDebugAndroidTest); las pruebas locales la cubren con EsquemaV2Test.
 */
@RunWith(AndroidJUnit4.class)
public class MigracionInstrumentadaTest {
    private static final String ARCHIVO = "migracion_instrumentada.db";
    private static final String INSTANTE = "2026-10-12T15:00:00.000Z";

    @Rule public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), GanHardDatabase.class);

    @Test public void migrarDeLaVersion1ALa2ConservaLosDatosYValidaElEsquema() throws IOException {
        SupportSQLiteDatabase v1 = helper.createDatabase(ARCHIVO, 1);
        v1.execSQL("INSERT INTO usuario (id_usuario, nombre, correo, activo, creado_en, actualizado_en, version) VALUES (?, ?, ?, 1, ?, ?, 0)",
                new Object[]{"u1", "Helver", "h@ejemplo.com", INSTANTE, INSTANTE});
        v1.execSQL("INSERT INTO finca (id_finca, id_propietario, nombre, activa, creado_en, actualizado_en, version) VALUES (?, ?, ?, 1, ?, ?, 0)",
                new Object[]{"f1", "u1", "La Esperanza", INSTANTE, INSTANTE});
        v1.close();

        SupportSQLiteDatabase v2 = helper.runMigrationsAndValidate(ARCHIVO, 2, true, Migraciones.V1_V2);
        try (Cursor c = v2.query("SELECT nombre FROM finca WHERE id_finca = 'f1'")) {
            assertTrue(c.moveToFirst());
            assertEquals("La Esperanza", c.getString(0));
        }
        try (Cursor c = v2.query("SELECT COUNT(*) FROM raza")) {
            c.moveToFirst();
            assertEquals(CatalogoRazas.RAZAS.length, c.getInt(0));
        }
        v2.close();
    }
}
