package com.example.ganhard.modelo.repositorio;

import android.database.sqlite.SQLiteConstraintException;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Hierro;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorHierro;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

/**
 * Catálogo de hierros de la finca. Un hierro se crea una vez y se elige en cada animal; no se borra, se desactiva.
 * Cada escritura pasa por {@link RegistroCambios}, que la guarda junto con su entrada de historial.
 */
public final class HierroRepository {
    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;
    private final RegistroCambios registro;

    public HierroRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
        this.registro = new RegistroCambios(db);
    }

    public void crear(String nombre, String descripcion, Callback<Hierro> callback) {
        ejecutor.ejecutar(() -> {
            String idFinca = idFincaActiva();
            validar(nombre, descripcion);
            Hierro hierro = new Hierro();
            hierro.idHierro = UUID.randomUUID().toString();
            hierro.idFinca = idFinca;
            aplicar(hierro, nombre, descripcion);
            if (db.hierroDao().porNombre(idFinca, hierro.nombre) != null) throw ErrorModelo.duplicado(Campos.NOMBRE);
            hierro.creadoEn = Tiempo.ahoraUtc();
            hierro.actualizadoEn = hierro.creadoEn;
            HistorialCambio creacion = registro.preparar(HistorialCambio.HIERRO, hierro.idHierro, HistorialCambio.CREAR,
                    null, ValoresCambio.de(hierro), null, null, hierro.creadoEn);
            try {
                registro.guardar(() -> db.hierroDao().insertar(hierro), creacion);
            } catch (SQLiteConstraintException ex) {
                throw ErrorModelo.duplicado(Campos.NOMBRE);
            }
            return hierro;
        }, callback);
    }

    /** Cambia el nombre y la descripción. Si no cambia nada, no escribe nada. */
    public void actualizar(String idHierro, String nombre, String descripcion, Callback<Hierro> callback) {
        ejecutor.ejecutar(() -> {
            Hierro hierro = existente(idHierro);
            JSONObject antes = ValoresCambio.de(hierro);
            validar(nombre, descripcion);
            aplicar(hierro, nombre, descripcion);
            Hierro mismoNombre = db.hierroDao().porNombre(hierro.idFinca, hierro.nombre);
            if (mismoNombre != null && !mismoNombre.idHierro.equals(hierro.idHierro)) throw ErrorModelo.duplicado(Campos.NOMBRE);
            JSONObject[] cambios = ValoresCambio.diferencia(antes, ValoresCambio.de(hierro));
            if (cambios == null) return hierro;
            hierro.actualizadoEn = Tiempo.ahoraUtc();
            HistorialCambio edicion = registro.preparar(HistorialCambio.HIERRO, idHierro, HistorialCambio.EDITAR,
                    cambios[0], cambios[1], null, null, hierro.actualizadoEn);
            try {
                registro.guardar(() -> db.hierroDao().actualizar(hierro), edicion);
            } catch (SQLiteConstraintException ex) {
                throw ErrorModelo.duplicado(Campos.NOMBRE);
            }
            return hierro;
        }, callback);
    }

    /** Desactiva o reactiva un hierro. Los animales que ya lo tienen lo conservan. Si ya está así, no escribe nada. */
    public void cambiarActivo(String idHierro, boolean activo, Callback<Hierro> callback) {
        ejecutor.ejecutar(() -> {
            Hierro hierro = existente(idHierro);
            if ((hierro.activo == 1) == activo) return hierro;
            JSONObject antes = ValoresCambio.estadoHierro(hierro);
            hierro.activo = activo ? 1 : 0;
            hierro.actualizadoEn = Tiempo.ahoraUtc();
            HistorialCambio cambio = registro.preparar(HistorialCambio.HIERRO, idHierro, HistorialCambio.CAMBIAR_ESTADO,
                    antes, ValoresCambio.estadoHierro(hierro), null, null, hierro.actualizadoEn);
            registro.guardar(() -> db.hierroDao().actualizar(hierro), cambio);
            return hierro;
        }, callback);
    }

    public void obtener(String idHierro, Callback<Hierro> callback) {
        ejecutor.ejecutar(() -> existente(idHierro), callback);
    }

    /** Los hierros de la finca: con {@code soloActivos}, los que se pueden elegir para un animal nuevo. */
    public void listar(boolean soloActivos, Callback<List<Hierro>> callback) {
        ejecutor.ejecutar(() -> {
            String idFinca = idFincaActiva();
            return soloActivos ? db.hierroDao().activos(idFinca) : db.hierroDao().todos(idFinca);
        }, callback);
    }

    private static void validar(String nombre, String descripcion) throws ErrorModelo {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        ErrorCampo errorNombre = ValidadorHierro.nombre(nombre);
        if (errorNombre != null) errores.put(Campos.NOMBRE, errorNombre);
        ErrorCampo errorDescripcion = ValidadorHierro.descripcion(descripcion);
        if (errorDescripcion != null) errores.put(Campos.DESCRIPCION, errorDescripcion);
        if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
    }

    private static void aplicar(Hierro hierro, String nombre, String descripcion) {
        hierro.nombre = nombre.trim();
        String texto = descripcion == null ? "" : descripcion.trim();
        hierro.descripcion = texto.isEmpty() ? null : texto;
    }

    private Hierro existente(String idHierro) throws ErrorModelo {
        Hierro hierro = db.hierroDao().porId(idHierro);
        if (hierro == null) throw ErrorModelo.noEncontrado(ErrorModelo.HIERRO);
        return hierro;
    }

    private String idFincaActiva() throws ErrorModelo {
        String idFinca = db.configuracionDao().idFincaActiva();
        if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
        return idFinca;
    }
}
