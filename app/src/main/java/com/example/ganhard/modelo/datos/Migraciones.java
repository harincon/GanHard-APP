package com.example.ganhard.modelo.datos;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

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

    /**
     * Agrega el historial de cambios y registra como creados los animales y pesajes que ya existían, con su fecha
     * de alta. Los valores son los actuales de cada registro: lo editado antes de esta versión no se conoce.
     * El JSON se arma aquí, con nombres de columna fijos, para que la migración no dependa de clases que cambien.
     */
    public static final Migration V2_V3 = new Migration(2, 3) {
        private final String[] datosAnimal = {"codigo", "nombre", "sexo", "id_raza", "fecha_nacimiento", "fecha_ingreso", "observaciones"};

        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `historial_cambio` (`id_historial_cambio` TEXT NOT NULL, `id_usuario` TEXT NOT NULL, "
                    + "`id_finca` TEXT, `entidad` TEXT NOT NULL, `id_registro` TEXT NOT NULL, `accion` TEXT NOT NULL, "
                    + "`registrado_en` TEXT NOT NULL, `recibido_en` TEXT, `fecha_efectiva` TEXT, `motivo` TEXT, `valores_anteriores` TEXT, "
                    + "`valores_nuevos` TEXT NOT NULL, PRIMARY KEY(`id_historial_cambio`), "
                    + "FOREIGN KEY(`id_usuario`) REFERENCES `usuario`(`id_usuario`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                    + "FOREIGN KEY(`id_finca`) REFERENCES `finca`(`id_finca`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_historial_cambio_id_finca_registrado_en` ON `historial_cambio` (`id_finca`, `registrado_en`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_historial_cambio_entidad_id_registro` ON `historial_cambio` (`entidad`, `id_registro`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_historial_cambio_id_usuario` ON `historial_cambio` (`id_usuario`)");
            try {
                registrarAnimales(db);
                registrarPesajes(db);
            } catch (JSONException ex) {
                throw new IllegalStateException("No se pudo registrar el historial de los datos existentes", ex);
            }
        }

        private void registrarAnimales(SupportSQLiteDatabase db) throws JSONException {
            try (Cursor c = db.query("SELECT a.id_animal, a.id_finca, f.id_propietario, a.creado_en, a.actualizado_en, a.estado, a.fecha_estado, "
                    + "a.codigo, a.nombre, a.sexo, a.id_raza, a.fecha_nacimiento, a.fecha_ingreso, a.observaciones "
                    + "FROM animal a JOIN finca f ON f.id_finca = a.id_finca")) {
                while (c.moveToNext()) {
                    JSONObject datos = new JSONObject();
                    for (int i = 0; i < datosAnimal.length; i++) {
                        if (!c.isNull(7 + i)) datos.put(datosAnimal[i], c.getString(7 + i));
                    }
                    insertar(db, c.getString(2), c.getString(1), "animal", c.getString(0), "CREAR", c.getString(3), null, datos);
                    if (!"ACTIVO".equals(c.getString(5))) {
                        JSONObject estado = new JSONObject().put("estado", c.getString(5)).put("fecha_estado", c.getString(6));
                        insertar(db, c.getString(2), c.getString(1), "animal", c.getString(0), "CAMBIAR_ESTADO", c.getString(4), c.getString(6), estado);
                    }
                }
            }
        }

        private void registrarPesajes(SupportSQLiteDatabase db) throws JSONException {
            try (Cursor c = db.query("SELECT p.id_pesaje, a.id_finca, f.id_propietario, p.creado_en, p.fecha, p.hora, p.peso_kg, p.observaciones "
                    + "FROM pesaje p JOIN animal a ON a.id_animal = p.id_animal JOIN finca f ON f.id_finca = a.id_finca")) {
                while (c.moveToNext()) {
                    JSONObject datos = new JSONObject().put("fecha", c.getString(4));
                    if (!c.isNull(5)) datos.put("hora", c.getString(5));
                    datos.put("peso_kg", c.getDouble(6));
                    if (!c.isNull(7)) datos.put("observaciones", c.getString(7));
                    insertar(db, c.getString(2), c.getString(1), "pesaje", c.getString(0), "CREAR", c.getString(3), null, datos);
                }
            }
        }

        private void insertar(SupportSQLiteDatabase db, String idUsuario, String idFinca, String entidad, String idRegistro,
                              String accion, String registradoEn, String fechaEfectiva, JSONObject nuevos) {
            db.execSQL("INSERT INTO historial_cambio (id_historial_cambio, id_usuario, id_finca, entidad, id_registro, accion, "
                            + "registrado_en, fecha_efectiva, valores_nuevos) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{UUID.randomUUID().toString(), idUsuario, idFinca, entidad, idRegistro, accion, registradoEn, fechaEfectiva, nuevos.toString()});
        }
    };

    /** Agrega las actividades sanitarias; no toca lo existente. */
    public static final Migration V3_V4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `evento_sanitario` (`id_evento_sanitario` TEXT NOT NULL, `id_animal` TEXT NOT NULL, "
                    + "`tipo` TEXT NOT NULL, `fecha` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `producto` TEXT, `observaciones` TEXT, "
                    + "`es_antecedente` INTEGER NOT NULL, `anulado` INTEGER NOT NULL, `anulado_en` TEXT, `motivo_anulacion` TEXT, "
                    + "`creado_en` TEXT NOT NULL, `actualizado_en` TEXT NOT NULL, `version` INTEGER NOT NULL, PRIMARY KEY(`id_evento_sanitario`), "
                    + "FOREIGN KEY(`id_animal`) REFERENCES `animal`(`id_animal`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_sanitario_id_animal_fecha` ON `evento_sanitario` (`id_animal`, `fecha`)");
        }
    };

    /** Agrega los gastos; no toca lo existente. */
    public static final Migration V4_V5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `gasto` (`id_gasto` TEXT NOT NULL, `id_finca` TEXT NOT NULL, `fecha` TEXT NOT NULL, "
                    + "`categoria` TEXT NOT NULL, `concepto` TEXT NOT NULL, `valor_centavos` INTEGER NOT NULL, `anulado` INTEGER NOT NULL, "
                    + "`anulado_en` TEXT, `motivo_anulacion` TEXT, `creado_en` TEXT NOT NULL, `actualizado_en` TEXT NOT NULL, "
                    + "`version` INTEGER NOT NULL, PRIMARY KEY(`id_gasto`), "
                    + "FOREIGN KEY(`id_finca`) REFERENCES `finca`(`id_finca`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_gasto_id_finca_fecha` ON `gasto` (`id_finca`, `fecha`)");
        }
    };

    /**
     * Agrega las tareas y el enlace de un pesaje o una actividad sanitaria con la tarea que cumplieron. Los
     * registros existentes quedan sin tarea.
     */
    public static final Migration V5_V6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `tarea` (`id_tarea` TEXT NOT NULL, `id_finca` TEXT NOT NULL, `id_animal` TEXT, "
                    + "`tipo` TEXT NOT NULL, `titulo` TEXT NOT NULL, `descripcion` TEXT, `fecha_programada` TEXT NOT NULL, `estado` TEXT NOT NULL, "
                    + "`fecha_completada` TEXT, `creado_en` TEXT NOT NULL, `actualizado_en` TEXT NOT NULL, `version` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`id_tarea`), "
                    + "FOREIGN KEY(`id_finca`) REFERENCES `finca`(`id_finca`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                    + "FOREIGN KEY(`id_animal`) REFERENCES `animal`(`id_animal`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tarea_id_finca_estado_fecha_programada` ON `tarea` (`id_finca`, `estado`, `fecha_programada`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tarea_id_animal` ON `tarea` (`id_animal`)");
            db.execSQL("ALTER TABLE `pesaje` ADD COLUMN `id_tarea` TEXT REFERENCES `tarea`(`id_tarea`) ON UPDATE NO ACTION ON DELETE RESTRICT");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pesaje_id_tarea` ON `pesaje` (`id_tarea`)");
            db.execSQL("ALTER TABLE `evento_sanitario` ADD COLUMN `id_tarea` TEXT REFERENCES `tarea`(`id_tarea`) ON UPDATE NO ACTION ON DELETE RESTRICT");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_sanitario_id_tarea` ON `evento_sanitario` (`id_tarea`)");
        }
    };

    /** Agrega a la finca el instante en que se vinculó a una cuenta; las fincas existentes quedan sin vincular. */
    public static final Migration V6_V7 = new Migration(6, 7) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `finca` ADD COLUMN `vinculada_en` TEXT");
        }
    };

    /** Agrega el catálogo de hierros y los identificadores de los animales; no toca lo existente (los animales quedan sin hierro). */
    public static final Migration V7_V8 = new Migration(7, 8) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `hierro` (`id_hierro` TEXT NOT NULL, `id_finca` TEXT NOT NULL, `nombre` TEXT NOT NULL COLLATE NOCASE, "
                    + "`descripcion` TEXT, `activo` INTEGER NOT NULL, `creado_en` TEXT NOT NULL, `actualizado_en` TEXT NOT NULL, `version` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`id_hierro`), FOREIGN KEY(`id_finca`) REFERENCES `finca`(`id_finca`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_hierro_id_finca_nombre` ON `hierro` (`id_finca`, `nombre`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `animal_identificador` (`id_animal_identificador` TEXT NOT NULL, `id_animal` TEXT NOT NULL, `tipo` TEXT NOT NULL, "
                    + "`id_hierro` TEXT, `valor` TEXT, `orden` INTEGER NOT NULL, `creado_en` TEXT NOT NULL, `actualizado_en` TEXT NOT NULL, "
                    + "PRIMARY KEY(`id_animal_identificador`), "
                    + "FOREIGN KEY(`id_animal`) REFERENCES `animal`(`id_animal`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                    + "FOREIGN KEY(`id_hierro`) REFERENCES `hierro`(`id_hierro`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identificador_id_animal` ON `animal_identificador` (`id_animal`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identificador_id_hierro` ON `animal_identificador` (`id_hierro`)");
        }
    };
}
