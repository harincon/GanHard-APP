package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.EventoSanitarioResumen;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.EntradaSanidad;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorMotivo;
import com.example.ganhard.modelo.reglas.ValidadorSanidad;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONObject;

/**
 * Apoyo del Modelo para las actividades sanitarias: registro y consulta (RF11), corrección (RF14) y anulación
 * (RF15). Corre en el hilo del {@link EjecutorModelo}; cada escritura pasa por {@link RegistroCambios}.
 */
public final class SanidadRepository {
    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;
    private final RegistroCambios registro;

    public SanidadRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
        this.registro = new RegistroCambios(db);
    }

    /**
     * Guarda una actividad realizada. El id lo elige quien llama y la operación es idempotente: si ya existe,
     * devuelve la guardada, de modo que repetir la solicitud (por ejemplo tras rotar) no duplica.
     */
    public void registrar(String idEvento, String idAnimal, EntradaSanidad entrada, Callback<EventoSanitario> callback) {
        registrar(idEvento, idAnimal, entrada, null, callback);
    }

    /**
     * Igual que el anterior, pero para cumplir una tarea de sanidad (RF17): en la misma transacción la actividad se
     * guarda con su {@code id_tarea} y la tarea queda completada en la fecha de la actividad. Si la tarea no existe, ya
     * no está pendiente o no es de ese animal y de tipo sanidad, no se guarda nada.
     */
    public void registrar(String idEvento, String idAnimal, EntradaSanidad entrada, String idTarea, Callback<EventoSanitario> callback) {
        ejecutor.ejecutar(() -> {
            EventoSanitario existente = db.eventoSanitarioDao().porId(idEvento);
            if (existente != null) return existente;
            if (idAnimal == null || idAnimal.trim().isEmpty()) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.ID_ANIMAL, new ErrorCampo(CodigoError.OBLIGATORIO)));
            }
            Animal animal = animalExistente(idAnimal);
            Map<String, ErrorCampo> errores = ValidadorSanidad.validar(entrada, animal, Tiempo.hoy());
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            String ahora = Tiempo.ahoraUtc();
            EventoSanitario evento = new EventoSanitario();
            evento.idEventoSanitario = idEvento;
            evento.idAnimal = idAnimal;
            aplicar(evento, entrada, animal);
            evento.creadoEn = ahora;
            evento.actualizadoEn = ahora;
            HistorialCambio creacion = registro.preparar(HistorialCambio.EVENTO_SANITARIO, idEvento, HistorialCambio.CREAR,
                    null, ValoresCambio.de(evento), null, null, ahora);
            if (idTarea == null || idTarea.trim().isEmpty()) {
                registro.guardar(() -> db.eventoSanitarioDao().insertar(evento), creacion);
                return evento;
            }
            evento.idTarea = idTarea;
            CierreTarea cierre = CierreTarea.preparar(db, registro, idTarea, Tarea.SANIDAD, idAnimal, evento.fecha, ahora);
            registro.guardar(() -> {
                db.eventoSanitarioDao().insertar(evento);
                db.tareaDao().actualizar(cierre.tarea);
            }, creacion, cierre.cambio);
            return evento;
        }, callback);
    }

    /** La actividad vigente con ese id; una anulada ya no se puede abrir para corregirla. */
    public void obtener(String idEvento, Callback<EventoSanitario> callback) {
        ejecutor.ejecutar(() -> eventoVigente(idEvento), callback);
    }

    /** Corrige una actividad con las reglas del registro y un motivo obligatorio. El animal no cambia. */
    public void corregir(String idEvento, EntradaSanidad entrada, String motivo, Callback<EventoSanitario> callback) {
        ejecutor.ejecutar(() -> {
            EventoSanitario evento = eventoVigente(idEvento);
            Animal animal = animalExistente(evento.idAnimal);
            Map<String, ErrorCampo> errores = ValidadorSanidad.validar(entrada, animal, Tiempo.hoy());
            ErrorCampo errorMotivo = ValidadorMotivo.validar(motivo);
            if (errorMotivo != null) errores.put(Campos.MOTIVO, errorMotivo);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            JSONObject antes = ValoresCambio.de(evento);
            aplicar(evento, entrada, animal);
            JSONObject[] diferencia = ValoresCambio.diferencia(antes, ValoresCambio.de(evento));
            if (diferencia == null) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.DESCRIPCION, new ErrorCampo(CodigoError.SIN_CAMBIOS)));
            }
            String ahora = Tiempo.ahoraUtc();
            evento.actualizadoEn = ahora;
            HistorialCambio edicion = registro.preparar(HistorialCambio.EVENTO_SANITARIO, idEvento, HistorialCambio.EDITAR,
                    diferencia[0], diferencia[1], motivo.trim(), null, ahora);
            registro.guardar(() -> db.eventoSanitarioDao().actualizar(evento), edicion);
            return evento;
        }, callback);
    }

    /** Anula una actividad con motivo obligatorio: se conserva, pero sale de las listas. No se puede deshacer. */
    public void anular(String idEvento, String motivo, Callback<EventoSanitario> callback) {
        ejecutor.ejecutar(() -> {
            EventoSanitario evento = db.eventoSanitarioDao().porId(idEvento);
            if (evento == null) throw ErrorModelo.noEncontrado(ErrorModelo.EVENTO);
            if (evento.anulado == 1) return evento;
            ErrorCampo errorMotivo = ValidadorMotivo.validar(motivo);
            if (errorMotivo != null) throw ErrorModelo.validacion(Collections.singletonMap(Campos.MOTIVO, errorMotivo));
            HistorialCambio anulacion = marcarAnulada(evento, motivo, Tiempo.ahoraUtc());
            registro.guardar(() -> db.eventoSanitarioDao().actualizar(evento), anulacion);
            return evento;
        }, callback);
    }

    /** Deja la actividad anulada en memoria y devuelve su entrada de historial; quien llama la guarda. */
    private HistorialCambio marcarAnulada(EventoSanitario evento, String motivo, String ahora) throws ErrorModelo {
        // La entrada guarda qué actividad se anuló, para mostrarla cuando ya no esté en la lista.
        JSONObject actividad = new JSONObject();
        ValoresCambio.poner(actividad, "tipo", evento.tipo);
        ValoresCambio.poner(actividad, "fecha", evento.fecha);
        JSONObject anulado = new JSONObject();
        ValoresCambio.poner(anulado, "anulado", 1);
        evento.anulado = 1;
        evento.anuladoEn = ahora;
        evento.motivoAnulacion = motivo.trim();
        evento.actualizadoEn = ahora;
        return registro.preparar(HistorialCambio.EVENTO_SANITARIO, evento.idEventoSanitario, HistorialCambio.ANULAR,
                actividad, anulado, evento.motivoAnulacion, null, ahora);
    }

    // --- Aplicación a varios animales (grupos) ---

    /**
     * Registra la misma actividad en varios animales a la vez: una actividad por animal, unidas por {@code idGrupo}.
     * Solo entran animales activos; si alguno no existe, no está activo o la entrada es inválida, no se guarda nada.
     * Es idempotente por {@code idGrupo}: repetir la solicitud devuelve lo ya guardado. Cada animal recibe su propia
     * entrada de historial.
     */
    public void registrarGrupo(String idGrupo, List<String> idsAnimales, EntradaSanidad entrada, Callback<List<EventoSanitario>> callback) {
        ejecutor.ejecutar(() -> {
            List<EventoSanitario> existentes = db.eventoSanitarioDao().todosDelGrupo(idGrupo);
            if (!existentes.isEmpty()) return existentes;
            Set<String> ids = new LinkedHashSet<>();
            if (idsAnimales != null) for (String id : idsAnimales) if (id != null && !id.trim().isEmpty()) ids.add(id.trim());
            if (ids.isEmpty()) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.ID_ANIMAL, new ErrorCampo(CodigoError.OBLIGATORIO)));
            }
            String idFinca = db.configuracionDao().idFincaActiva();
            if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
            String ahora = Tiempo.ahoraUtc();
            List<EventoSanitario> eventos = new ArrayList<>();
            List<HistorialCambio> cambios = new ArrayList<>();
            for (String idAnimal : ids) {
                Animal animal = animalExistente(idAnimal);
                if (!animal.idFinca.equals(idFinca)) throw ErrorModelo.noEncontrado(ErrorModelo.ANIMAL);
                if (!Animal.ACTIVO.equals(animal.estado)) {
                    throw ErrorModelo.validacion(Collections.singletonMap(Campos.ID_ANIMAL, new ErrorCampo(CodigoError.ANIMAL_INACTIVO)));
                }
                Map<String, ErrorCampo> errores = ValidadorSanidad.validar(entrada, animal, Tiempo.hoy());
                if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
                EventoSanitario evento = new EventoSanitario();
                evento.idEventoSanitario = UUID.randomUUID().toString();
                evento.idAnimal = idAnimal;
                evento.idGrupo = idGrupo;
                aplicar(evento, entrada, animal);
                evento.creadoEn = ahora;
                evento.actualizadoEn = ahora;
                eventos.add(evento);
                cambios.add(registro.preparar(HistorialCambio.EVENTO_SANITARIO, evento.idEventoSanitario, HistorialCambio.CREAR,
                        null, ValoresCambio.de(evento), null, null, ahora));
            }
            registro.guardar(() -> {
                for (EventoSanitario evento : eventos) db.eventoSanitarioDao().insertar(evento);
            }, cambios.toArray(new HistorialCambio[0]));
            return eventos;
        }, callback);
    }

    /** El grupo con sus actividades vigentes; si ya no queda ninguna (todas anuladas), no existe. */
    public void grupo(String idGrupo, Callback<GrupoSanitario> callback) {
        ejecutor.ejecutar(() -> cargarGrupo(idGrupo), callback);
    }

    /**
     * Corrige todo el grupo con las reglas del registro y un motivo obligatorio: cambia las actividades vigentes del
     * grupo (también las que ya se habían corregido aparte) y deja una entrada de historial por animal. Si en ninguna
     * cambia nada, se rechaza como una corrección individual sin cambios.
     */
    public void corregirGrupo(String idGrupo, EntradaSanidad entrada, String motivo, Callback<GrupoSanitario> callback) {
        ejecutor.ejecutar(() -> {
            List<EventoSanitario> vigentes = vigentesDelGrupo(idGrupo);
            Map<String, ErrorCampo> errores = new LinkedHashMap<>();
            Map<String, Animal> animalesDe = new HashMap<>();
            for (EventoSanitario evento : vigentes) {
                Animal animal = animalExistente(evento.idAnimal);
                animalesDe.put(evento.idAnimal, animal);
                if (errores.isEmpty()) errores.putAll(ValidadorSanidad.validar(entrada, animal, Tiempo.hoy()));
            }
            ErrorCampo errorMotivo = ValidadorMotivo.validar(motivo);
            if (errorMotivo != null) errores.put(Campos.MOTIVO, errorMotivo);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            String ahora = Tiempo.ahoraUtc();
            List<EventoSanitario> cambiados = new ArrayList<>();
            List<HistorialCambio> cambios = new ArrayList<>();
            for (EventoSanitario evento : vigentes) {
                JSONObject antes = ValoresCambio.de(evento);
                aplicar(evento, entrada, animalesDe.get(evento.idAnimal));
                JSONObject[] diferencia = ValoresCambio.diferencia(antes, ValoresCambio.de(evento));
                if (diferencia == null) continue;
                evento.actualizadoEn = ahora;
                cambiados.add(evento);
                cambios.add(registro.preparar(HistorialCambio.EVENTO_SANITARIO, evento.idEventoSanitario, HistorialCambio.EDITAR,
                        diferencia[0], diferencia[1], motivo.trim(), null, ahora));
            }
            if (cambiados.isEmpty()) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.DESCRIPCION, new ErrorCampo(CodigoError.SIN_CAMBIOS)));
            }
            registro.guardar(() -> {
                for (EventoSanitario evento : cambiados) db.eventoSanitarioDao().actualizar(evento);
            }, cambios.toArray(new HistorialCambio[0]));
            return cargarGrupo(idGrupo);
        }, callback);
    }

    /** Anula las actividades vigentes del grupo con un motivo obligatorio; devuelve cuántas anuló. No se puede deshacer. */
    public void anularGrupo(String idGrupo, String motivo, Callback<Integer> callback) {
        ejecutor.ejecutar(() -> {
            List<EventoSanitario> vigentes = vigentesDelGrupo(idGrupo);
            ErrorCampo errorMotivo = ValidadorMotivo.validar(motivo);
            if (errorMotivo != null) throw ErrorModelo.validacion(Collections.singletonMap(Campos.MOTIVO, errorMotivo));
            String ahora = Tiempo.ahoraUtc();
            List<HistorialCambio> cambios = new ArrayList<>();
            for (EventoSanitario evento : vigentes) cambios.add(marcarAnulada(evento, motivo, ahora));
            registro.guardar(() -> {
                for (EventoSanitario evento : vigentes) db.eventoSanitarioDao().actualizar(evento);
            }, cambios.toArray(new HistorialCambio[0]));
            return vigentes.size();
        }, callback);
    }

    /**
     * La lista general: cada actividad individual como una tarjeta y las de un mismo grupo juntas en una sola, con su
     * total, en el lugar de la más reciente del grupo.
     */
    public void listaAgrupada(Callback<List<ElementoSanidad>> callback) {
        ejecutor.ejecutar(() -> {
            String idFinca = db.configuracionDao().idFincaActiva();
            if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
            List<EventoSanitarioResumen> filas = db.eventoSanitarioDao().deLaFinca(idFinca);
            Map<String, Integer> totales = new HashMap<>();
            for (EventoSanitarioResumen fila : filas) {
                if (fila.evento.idGrupo != null) totales.merge(fila.evento.idGrupo, 1, Integer::sum);
            }
            List<ElementoSanidad> elementos = new ArrayList<>();
            Set<String> yaPuestos = new HashSet<>();
            for (EventoSanitarioResumen fila : filas) {
                String grupo = fila.evento.idGrupo;
                if (grupo == null) elementos.add(ElementoSanidad.individual(fila));
                else if (yaPuestos.add(grupo)) elementos.add(ElementoSanidad.grupo(grupo, fila.evento, totales.get(grupo)));
            }
            return elementos;
        }, callback);
    }

    private GrupoSanitario cargarGrupo(String idGrupo) throws ErrorModelo {
        List<EventoSanitarioResumen> actividades = db.eventoSanitarioDao().delGrupo(idGrupo);
        if (actividades.isEmpty()) throw ErrorModelo.noEncontrado(ErrorModelo.EVENTO);
        return new GrupoSanitario(idGrupo, actividades.get(0).evento, actividades);
    }

    private List<EventoSanitario> vigentesDelGrupo(String idGrupo) throws ErrorModelo {
        List<EventoSanitario> vigentes = db.eventoSanitarioDao().vigentesDelGrupo(idGrupo);
        if (vigentes.isEmpty()) throw ErrorModelo.noEncontrado(ErrorModelo.EVENTO);
        return vigentes;
    }

    /** Actividades vigentes de toda la finca, de la más reciente a la más antigua. */
    public void deLaFinca(Callback<List<EventoSanitarioResumen>> callback) {
        ejecutor.ejecutar(() -> {
            String idFinca = db.configuracionDao().idFincaActiva();
            if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
            return db.eventoSanitarioDao().deLaFinca(idFinca);
        }, callback);
    }

    public void delAnimal(String idAnimal, Callback<List<EventoSanitarioResumen>> callback) {
        ejecutor.ejecutar(() -> {
            animalExistente(idAnimal);
            return db.eventoSanitarioDao().delAnimal(idAnimal);
        }, callback);
    }

    private static void aplicar(EventoSanitario evento, EntradaSanidad entrada, Animal animal) {
        evento.tipo = entrada.tipo;
        evento.fecha = entrada.fecha;
        evento.descripcion = entrada.descripcion;
        evento.producto = entrada.producto.isEmpty() ? null : entrada.producto;
        evento.observaciones = entrada.observaciones.isEmpty() ? null : entrada.observaciones;
        evento.esAntecedente = ValidadorSanidad.esAntecedente(entrada.fecha, animal) ? 1 : 0;
    }

    private EventoSanitario eventoVigente(String idEvento) throws ErrorModelo {
        EventoSanitario evento = db.eventoSanitarioDao().porId(idEvento);
        if (evento == null || evento.anulado == 1) throw ErrorModelo.noEncontrado(ErrorModelo.EVENTO);
        return evento;
    }

    private Animal animalExistente(String idAnimal) throws ErrorModelo {
        Animal animal = db.animalDao().porId(idAnimal);
        if (animal == null) throw ErrorModelo.noEncontrado(ErrorModelo.ANIMAL);
        return animal;
    }
}
