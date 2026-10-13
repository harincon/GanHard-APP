package com.example.ganhard.modelo.datos;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Migraciones del esquema. Nunca se usa una migración destructiva. */
public final class Migraciones {
    private Migraciones() {}

    /** Agrega el catálogo de razas, los animales y los pesajes sin tocar usuario ni finca. */
    public static final Migration V1_V2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `raza` (`id_raza` TEXT NOT NULL, `nombre` TEXT NOT NULL, `descripcion` TEXT, PRIMARY KEY(`id_raza`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `animal` (`id_animal` TEXT NOT NULL, `id_finca` TEXT NOT NULL, `id_raza` TEXT, "
                    + "`codigo` TEXT NOT NULL COLLATE NOCASE, `nombre` TEXT, `sexo` TEXT NOT NULL, `fecha_nacimiento` TEXT, "
                    + "`fecha_ingreso` TEXT NOT NULL, `estado` TEXT NOT NULL, `fecha_estado` TEXT NOT NULL, `observaciones` TEXT, "
                    + "`creado_en` TEXT NOT NULL, `actualizado_en` TEXT NOT NULL, `version` INTEGER NOT NULL, PRIMARY KEY(`id_animal`), "
                    + "FOREIGN KEY(`id_finca`) REFERENCES `finca`(`id_finca`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                    + "FOREIGN KEY(`id_raza`) REFERENCES `raza`(`id_raza`) ON UPDATE NO ACTION ON DELETE SET NULL )");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_id_finca_codigo` ON `animal` (`id_finca`, `codigo`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_id_finca_estado` ON `animal` (`id_finca`, `estado`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_id_raza` ON `animal` (`id_raza`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `pesaje` (`id_pesaje` TEXT NOT NULL, `id_animal` TEXT NOT NULL, `fecha` TEXT NOT NULL, `hora` TEXT, "
                    + "`peso_kg` REAL NOT NULL, `es_antecedente` INTEGER NOT NULL, `observaciones` TEXT, `anulado` INTEGER NOT NULL, "
                    + "`anulado_en` TEXT, `motivo_anulacion` TEXT, `creado_en` TEXT NOT NULL, `actualizado_en` TEXT NOT NULL, "
                    + "`version` INTEGER NOT NULL, PRIMARY KEY(`id_pesaje`), "
                    + "FOREIGN KEY(`id_animal`) REFERENCES `animal`(`id_animal`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pesaje_id_animal_fecha_hora` ON `pesaje` (`id_animal`, `fecha`, `hora`)");
            CatalogoRazas.insertar(db);
        }
    };
}
