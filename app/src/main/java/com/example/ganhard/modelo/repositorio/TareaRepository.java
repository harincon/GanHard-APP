package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.datos.TareaResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.EntradaTarea;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorMotivo;
import com.example.ganhard.modelo.reglas.ValidadorTarea;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * Apoyo del Modelo para las tareas (RF12): programar, consultar, corregir (RF14), cancelar y completar las
 * generales. Completar una de pesaje o de sanidad ocurre al guardar el registro realizado, desde los repositorios
 * de pesajes y de sanidad (RF17). Corre en el hilo del {@link EjecutorModelo}; cada escritura pasa por
 * {@link RegistroCambios}.
 */
public final class TareaRepository {
    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;
    private final RegistroCambios registro;

    public TareaRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
        this.registro = new RegistroCambios(db);
    }

    /**
     * Programa una tarea pendiente. El id lo elige quien llama y la operación es idempotente: si ya existe,
     * devuelve la guardada, de modo que repetir la solicitud (por ejemplo tras rotar) no duplica. El animal, si lo
     * hay, debe ser de la finca y estar activo (RN08).
     */
    public void programar(String idTarea, EntradaTarea entrada, Callback<Tarea> callback) {
        ejecutor.ejecutar(() -> {
            Tarea existente = db.tareaDao().porId(idTarea);
            if (existente != null) return existente;
            String idFinca = idFincaActiva();
            Map<String, ErrorCampo> errores = ValidadorTarea.validar(entrada);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            if (!entrada.idAnimal.isEmpty()) animalActivo(entrada.idAnimal, idFinca);
            String ahora = Tiempo.ahoraUtc();
            Tarea tarea = new Tarea();
            tarea.idTarea = idTarea;
            tarea.idFinca = idFinca;
            tarea.idAnimal = entrada.idAnimal.isEmpty() ? null : entrada.idAnimal;
            tarea.tipo = entrada.tipo;
            aplicar(tarea, entrada);
            tarea.estado = Tarea.PENDIENTE;
            tarea.creadoEn = ahora;
            tarea.actualizadoEn = ahora;
            HistorialCambio creacion = registro.preparar(HistorialCambio.TAREA, idTarea, HistorialCambio.CREAR,
                    null, ValoresCambio.de(tarea), null, null, ahora);
            registro.guardar(() -> db.tareaDao().insertar(tarea), creacion);
            return tarea;
        }, callback);
    }

    /** La tarea con ese id, en cualquier estado: la pantalla de detalle también muestra las cerradas. */
    public void obtener(String idTarea, Callback<Tarea> callback) {
        ejecutor.ejecutar(() -> tareaExistente(idTarea), callback);
    }

    /**
     * Corrige una tarea pendiente (RF14): título, descripción y fecha prevista. El tipo y el animal no cambian.
     * Pide un motivo y deja en el historial los valores anteriores y los nuevos; si nada cambió, no guarda.
     */
    public void corregir(String idTarea, EntradaTarea entrada, String motivo, Callback<Tarea> callback) {
        ejecutor.ejecutar(() -> {
            Tarea tarea = tareaPendiente(idTarea);
            EntradaTarea completa = new EntradaTarea(tarea.tipo, tarea.idAnimal, entrada.titulo, entrada.descripcion, entrada.fechaProgramada);
            Map<String, ErrorCampo> errores = ValidadorTarea.validar(completa);
            ErrorCampo errorMotivo = ValidadorMotivo.validar(motivo);
            if (errorMotivo != null) errores.put(Campos.MOTIVO, errorMotivo);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            JSONObject antes = ValoresCambio.de(tarea);
            aplicar(tarea, completa);
            JSONObject[] diferencia = ValoresCambio.diferencia(antes, ValoresCambio.de(tarea));
            if (diferencia == null) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.TITULO, new ErrorCampo(CodigoError.SIN_CAMBIOS)));
            }
            String ahora = Tiempo.ahoraUtc();
            tarea.actualizadoEn = ahora;
            HistorialCambio edicion = registro.preparar(HistorialCambio.TAREA, idTarea, HistorialCambio.EDITAR,
                    diferencia[0], diferencia[1], motivo.trim(), null, ahora);
            registro.guardar(() -> db.tareaDao().actualizar(tarea), edicion);
            return tarea;
        }, callback);
    }

    /** Cancela una tarea pendiente. No pide motivo, como el cambio de estado de un animal; no se puede deshacer. */
    public void cancelar(String idTarea, Callback<Tarea> callback) {
        ejecutor.ejecutar(() -> {
            Tarea tarea = tareaPendiente(idTarea);
            JSONObject antes = ValoresCambio.estadoTarea(tarea);
            String ahora = Tiempo.ahoraUtc();
            tarea.estado = Tarea.CANCELADA;
            tarea.actualizadoEn = ahora;
            HistorialCambio cambio = registro.preparar(HistorialCambio.TAREA, idTarea, HistorialCambio.CAMBIAR_ESTADO,
                    antes, ValoresCambio.estadoTarea(tarea), null, null, ahora);
            registro.guardar(() -> db.tareaDao().actualizar(tarea), cambio);
            return tarea;
        }, callback);
    }

    /**
     * Completa una tarea general pendiente en la fecha indicada (obligatoria, válida y no futura). Las de pesaje y
     * sanidad se completan al guardar su registro, no a mano.
     */
    public void completar(String idTarea, String fechaCompletada, Callback<Tarea> callback) {
        ejecutor.ejecutar(() -> {
            Tarea tarea = tareaPendiente(idTarea);
            if (!Tarea.GENERAL.equals(tarea.tipo)) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.TIPO, new ErrorCampo(CodigoError.VALOR_INVALIDO)));
            }
            ErrorCampo error = ValidadorTarea.fechaCompletada(fechaCompletada, Tiempo.hoy());
            if (error != null) throw ErrorModelo.validacion(Collections.singletonMap(Campos.FECHA, error));
            String fecha = fechaCompletada.trim();
            JSONObject antes = ValoresCambio.estadoTarea(tarea);
            String ahora = Tiempo.ahoraUtc();
            tarea.estado = Tarea.COMPLETADA;
            tarea.fechaCompletada = fecha;
            tarea.actualizadoEn = ahora;
            HistorialCambio cambio = registro.preparar(HistorialCambio.TAREA, idTarea, HistorialCambio.CAMBIAR_ESTADO,
                    antes, ValoresCambio.estadoTarea(tarea), null, fecha, ahora);
            registro.guardar(() -> db.tareaDao().actualizar(tarea), cambio);
            return tarea;
        }, callback);
    }

    /** Tareas de la finca en un estado; {@code idAnimal} nulo trae las de todos, con uno trae solo las suyas. */
    public void listar(String estado, String idAnimal, Callback<List<TareaResumen>> callback) {
        ejecutor.ejecutar(() -> {
            if (estado == null || !Tarea.ESTADOS.contains(estado)) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.ESTADO, new ErrorCampo(CodigoError.VALOR_INVALIDO)));
            }
            String idFinca = idFincaActiva();
            String animal = idAnimal == null || idAnimal.trim().isEmpty() ? null : idAnimal.trim();
            if (animal != null) {
                Animal existente = db.animalDao().porId(animal);
                if (existente == null || !idFinca.equals(existente.idFinca)) throw ErrorModelo.noEncontrado(ErrorModelo.ANIMAL);
            }
            return Tarea.PENDIENTE.equals(estado) ? db.tareaDao().pendientes(idFinca, animal) : db.tareaDao().cerradas(idFinca, animal, estado);
        }, callback);
    }

    private static void aplicar(Tarea tarea, EntradaTarea entrada) {
        tarea.titulo = entrada.titulo;
        tarea.descripcion = entrada.descripcion.isEmpty() ? null : entrada.descripcion;
        tarea.fechaProgramada = entrada.fechaProgramada;
    }

    private Tarea tareaExistente(String idTarea) throws ErrorModelo {
        Tarea tarea = db.tareaDao().porId(idTarea);
        if (tarea == null) throw ErrorModelo.noEncontrado(ErrorModelo.TAREA);
        return tarea;
    }

    private Tarea tareaPendiente(String idTarea) throws ErrorModelo {
        Tarea tarea = tareaExistente(idTarea);
        if (!Tarea.PENDIENTE.equals(tarea.estado)) {
            throw ErrorModelo.validacion(Collections.singletonMap(Campos.ESTADO, new ErrorCampo(CodigoError.NO_PENDIENTE)));
        }
        return tarea;
    }

    /** El animal de una tarea debe ser de la finca y seguir activo (RN08). */
    private void animalActivo(String idAnimal, String idFinca) throws ErrorModelo {
        Animal animal = db.animalDao().porId(idAnimal);
        if (animal == null || !idFinca.equals(animal.idFinca)) throw ErrorModelo.noEncontrado(ErrorModelo.ANIMAL);
        if (!Animal.ACTIVO.equals(animal.estado)) {
            throw ErrorModelo.validacion(Collections.singletonMap(Campos.ID_ANIMAL, new ErrorCampo(CodigoError.ANIMAL_INACTIVO)));
        }
    }

    private String idFincaActiva() throws ErrorModelo {
        String idFinca = db.configuracionDao().idFincaActiva();
        if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
        return idFinca;
    }
}
