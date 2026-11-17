package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.EventoSanitarioResumen;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.HistorialCambio;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
            registro.guardar(() -> db.eventoSanitarioDao().insertar(evento), creacion);
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
            // La entrada guarda qué actividad se anuló, para mostrarla cuando ya no esté en la lista.
            JSONObject actividad = new JSONObject();
            ValoresCambio.poner(actividad, "tipo", evento.tipo);
            ValoresCambio.poner(actividad, "fecha", evento.fecha);
            JSONObject anulado = new JSONObject();
            ValoresCambio.poner(anulado, "anulado", 1);
            String ahora = Tiempo.ahoraUtc();
            evento.anulado = 1;
            evento.anuladoEn = ahora;
            evento.motivoAnulacion = motivo.trim();
            evento.actualizadoEn = ahora;
            HistorialCambio anulacion = registro.preparar(HistorialCambio.EVENTO_SANITARIO, idEvento, HistorialCambio.ANULAR,
                    actividad, anulado, evento.motivoAnulacion, null, ahora);
            registro.guardar(() -> db.eventoSanitarioDao().actualizar(evento), anulacion);
            return evento;
        }, callback);
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
