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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
            aplicarTipo(tarea, entrada.tipo, idFinca);
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
        ejecutor.ejecutar(() -> consultar(estado, idAnimal), callback);
    }

    private List<TareaResumen> consultar(String estado, String idAnimal) throws ErrorModelo {
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
    }

    // --- Tipos propios ---

    /** Los tipos propios que la finca ya usó (sin repetir y en orden), para ofrecerlos otra vez al programar. */
    public void tiposPropios(Callback<List<String>> callback) {
        ejecutor.ejecutar(() -> db.tareaDao().etiquetas(idFincaActiva()), callback);
    }

    /**
     * Deja el tipo de la tarea: uno de los normales si lo escrito coincide con alguno, o general con la etiqueta propia.
     * Un tipo propio ya usado se escribe igual que la primera vez, aunque se haya tecleado distinto.
     */
    private void aplicarTipo(Tarea tarea, String texto, String idFinca) {
        String normal = ValidadorTarea.tipoNormal(texto);
        if (normal != null) {
            tarea.tipo = normal;
            tarea.etiqueta = null;
            return;
        }
        tarea.tipo = Tarea.GENERAL;
        tarea.etiqueta = texto;
        String clave = ValidadorTarea.clave(texto);
        for (String usada : db.tareaDao().etiquetas(idFinca)) {
            if (ValidadorTarea.clave(usada).equals(clave)) {
                tarea.etiqueta = usada;
                return;
            }
        }
    }

    // --- Tareas para varios animales (grupos) ---

    /**
     * Programa la misma tarea para varios animales a la vez: una tarea por animal, unidas por {@code idGrupo}. Solo
     * entran animales activos de la finca; si alguno no existe o no está activo, o la entrada es inválida, no se guarda
     * nada. Es idempotente por {@code idGrupo}. Cada tarea recibe su propia entrada de historial. El animal de la entrada
     * se ignora: lo ponen los elegidos.
     */
    public void programarGrupo(String idGrupo, List<String> idsAnimales, EntradaTarea entrada, Callback<List<Tarea>> callback) {
        ejecutor.ejecutar(() -> {
            List<Tarea> existentes = db.tareaDao().todosDelGrupo(idGrupo);
            if (!existentes.isEmpty()) return existentes;
            Set<String> ids = new LinkedHashSet<>();
            if (idsAnimales != null) for (String id : idsAnimales) if (id != null && !id.trim().isEmpty()) ids.add(id.trim());
            if (ids.isEmpty()) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.ID_ANIMAL, new ErrorCampo(CodigoError.OBLIGATORIO)));
            }
            String idFinca = idFincaActiva();
            // Se valida una sola vez, con un animal de los elegidos para que pesaje y sanidad no pidan animal.
            EntradaTarea conAnimal = new EntradaTarea(entrada.tipo, ids.iterator().next(), entrada.titulo, entrada.descripcion, entrada.fechaProgramada);
            Map<String, ErrorCampo> errores = ValidadorTarea.validar(conAnimal);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            for (String idAnimal : ids) animalActivo(idAnimal, idFinca);
            String ahora = Tiempo.ahoraUtc();
            List<Tarea> creadas = new ArrayList<>();
            List<HistorialCambio> cambios = new ArrayList<>();
            for (String idAnimal : ids) {
                Tarea tarea = new Tarea();
                tarea.idTarea = UUID.randomUUID().toString();
                tarea.idFinca = idFinca;
                tarea.idAnimal = idAnimal;
                tarea.idGrupo = idGrupo;
                aplicarTipo(tarea, entrada.tipo, idFinca);
                aplicar(tarea, entrada);
                tarea.estado = Tarea.PENDIENTE;
                tarea.creadoEn = ahora;
                tarea.actualizadoEn = ahora;
                creadas.add(tarea);
                cambios.add(registro.preparar(HistorialCambio.TAREA, tarea.idTarea, HistorialCambio.CREAR,
                        null, ValoresCambio.de(tarea), null, null, ahora));
            }
            registro.guardar(() -> {
                for (Tarea tarea : creadas) db.tareaDao().insertar(tarea);
            }, cambios.toArray(new HistorialCambio[0]));
            return creadas;
        }, callback);
    }

    /** El grupo con las tareas de cada animal, en cualquier estado; si no existe, no se encuentra. */
    public void grupo(String idGrupo, Callback<GrupoTarea> callback) {
        ejecutor.ejecutar(() -> {
            List<TareaResumen> tareas = db.tareaDao().delGrupo(idGrupo);
            if (tareas.isEmpty()) throw ErrorModelo.noEncontrado(ErrorModelo.TAREA);
            Tarea referencia = tareas.get(0).tarea;
            for (TareaResumen fila : tareas) {
                if (Tarea.PENDIENTE.equals(fila.tarea.estado)) { referencia = fila.tarea; break; }
            }
            return new GrupoTarea(idGrupo, referencia, tareas);
        }, callback);
    }

    /**
     * Cancela las tareas pendientes del grupo (las completadas o ya canceladas no se tocan) y devuelve cuántas canceló.
     * Cada una deja su entrada de historial. No se puede deshacer.
     */
    public void cancelarGrupo(String idGrupo, Callback<Integer> callback) {
        ejecutor.ejecutar(() -> {
            if (db.tareaDao().todosDelGrupo(idGrupo).isEmpty()) throw ErrorModelo.noEncontrado(ErrorModelo.TAREA);
            List<Tarea> pendientes = db.tareaDao().pendientesDelGrupo(idGrupo);
            if (pendientes.isEmpty()) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.ESTADO, new ErrorCampo(CodigoError.NO_PENDIENTE)));
            }
            String ahora = Tiempo.ahoraUtc();
            List<HistorialCambio> cambios = new ArrayList<>();
            for (Tarea tarea : pendientes) {
                JSONObject antes = ValoresCambio.estadoTarea(tarea);
                tarea.estado = Tarea.CANCELADA;
                tarea.actualizadoEn = ahora;
                cambios.add(registro.preparar(HistorialCambio.TAREA, tarea.idTarea, HistorialCambio.CAMBIAR_ESTADO,
                        antes, ValoresCambio.estadoTarea(tarea), null, null, ahora));
            }
            registro.guardar(() -> {
                for (Tarea tarea : pendientes) db.tareaDao().actualizar(tarea);
            }, cambios.toArray(new HistorialCambio[0]));
            return pendientes.size();
        }, callback);
    }

    /**
     * La lista de un estado con las tareas de un mismo grupo juntas en una tarjeta con su total (una por grupo y estado),
     * en el lugar de la primera del grupo. Con un animal elegido se ven sus tareas una por una.
     */
    public void listaAgrupada(String estado, String idAnimal, Callback<List<ElementoTarea>> callback) {
        ejecutor.ejecutar(() -> {
            List<TareaResumen> filas = consultar(estado, idAnimal);
            boolean unAnimal = idAnimal != null && !idAnimal.trim().isEmpty();
            Map<String, Integer> totales = new HashMap<>();
            if (!unAnimal) for (TareaResumen fila : filas) if (fila.tarea.idGrupo != null) totales.merge(fila.tarea.idGrupo, 1, Integer::sum);
            List<ElementoTarea> elementos = new ArrayList<>();
            Set<String> yaPuestos = new HashSet<>();
            for (TareaResumen fila : filas) {
                String grupo = fila.tarea.idGrupo;
                if (unAnimal || grupo == null) elementos.add(ElementoTarea.individual(fila));
                else if (yaPuestos.add(grupo)) elementos.add(ElementoTarea.grupo(grupo, fila.tarea, totales.get(grupo)));
            }
            return elementos;
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
