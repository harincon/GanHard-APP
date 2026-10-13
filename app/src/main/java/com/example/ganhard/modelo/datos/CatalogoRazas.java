package com.example.ganhard.modelo.datos;

import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Lista inicial de razas de referencia. La usan la creación de la base y la migración 1 a 2, de modo que
 * una instalación nueva y una actualizada queden con el mismo catálogo.
 */
public final class CatalogoRazas {
    /** Filas {id_raza, nombre, descripcion}; los UUID son fijos para que coincidan en todos los dispositivos. */
    public static final String[][] RAZAS = {
            {"853193c3-3b52-4d5f-a4df-f73be27e5da1", "Brahman", "Cebuino de carne"},
            {"57ed1c04-8ed9-40aa-8d2a-1a77fe3ab5bb", "Gyr", "Cebuino lechero"},
            {"4cb82ea9-a291-4073-a0d8-a2d67098387c", "Girolando", "Cruce de Holstein y Gyr, lechero"},
            {"aa63c7d3-2840-4c36-a593-7dd8ecbb499b", "Holstein", "Lechera"},
            {"fecfee58-596b-42a9-a837-ea821d003b02", "Pardo Suizo", "Doble propósito"},
            {"4fda5aab-035d-4f2e-b1e1-c8159de431d4", "Angus", "Carne"},
            {"da528059-b672-4f72-8dd7-7dae7f27cad6", "Simmental", "Doble propósito"},
            {"98a92214-f591-4146-8e0a-b2e48db9b686", "Normando", "Doble propósito"},
            {"01e2ebcf-ce3e-486e-8ee3-0df716b5c72b", "Cebú", "Bos indicus, sin línea específica"},
            {"aa2e435e-86a7-4a4a-820a-0ec0b80e7f53", "Blanco Orejinegro", "Criollo colombiano"},
    };

    private CatalogoRazas() {}

    public static void insertar(SupportSQLiteDatabase db) {
        for (String[] raza : RAZAS) {
            db.execSQL("INSERT OR IGNORE INTO raza (id_raza, nombre, descripcion) VALUES (?, ?, ?)", new Object[]{raza[0], raza[1], raza[2]});
        }
    }
}
