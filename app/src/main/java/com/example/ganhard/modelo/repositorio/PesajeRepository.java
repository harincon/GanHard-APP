package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.EntradaPesaje;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorPesaje;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

/**
 * Apoyo del Modelo para pesajes: registro e historial (RF08, RF09), corrección (RF14) y anulación (RF15).
 * Corre en el hilo del {@link EjecutorModelo}; cada escritura pasa por {@link RegistroCambios}.
 */
public final class PesajeRepository {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;
    private final RegistroCambios registro;

    public PesajeRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
        this.registro = new RegistroCambios(db);
    }

    public void registrar(String idAnimal, EntradaPesaje entrada, Callback<Pesaje> callback) {
        registrar(UUID.randomUUID().toString(), idAnimal, entrada, callback);
    }

    /**
     * Valida contra el animal (nacimiento e ingreso), calcula el antecedente y guarda el pesaje. Es idempotente:
     * si el id ya existe devuelve el pesaje guardado, así repetir la solicitud (por ejemplo tras rotar) no duplica.
     */
    public void registrar(String idPesaje, String idAnimal, EntradaPesaje entrada, Callback<Pesaje> callback) {
        ejecutor.ejecutar(() -> {
            Pesaje existente = db.pesajeDao().porId(idPesaje);
            if (existente != null) return existente;
            Animal animal = animalExistente(idAnimal);
            Map<String, ErrorCampo> errores = ValidadorPesaje.validar(entrada, animal, Tiempo.ahora());
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            String ahora = Tiempo.ahoraUtc();
            Pesaje pesaje = new Pesaje();
            pesaje.idPesaje = idPesaje;
            pesaje.idAnimal = idAnimal;
            aplicar(pesaje, entrada, animal);
            pesaje.creadoEn = ahora;
            pesaje.actualizadoEn = ahora;
            HistorialCambio creacion = registro.preparar(HistorialCambio.PESAJE, idPesaje, HistorialCambio.CREAR,
                    null, ValoresCambio.de(pesaje), null, null, ahora);
            registro.guardar(() -> db.pesajeDao().insertar(pesaje), creacion);
            return pesaje;
        }, callback);
    }

    /** El pesaje vigente con ese id; uno anulado ya no se puede abrir para corregirlo. */
    public void obtener(String idPesaje, Callback<Pesaje> callback) {
        ejecutor.ejecutar(() -> pesajeVigente(idPesaje), callback);
    }

    /**
     * Corrige un pesaje (RF14) con las mismas reglas del registro y un motivo obligatorio. Conserva su identidad
     * y deja en el historial los valores anteriores, los nuevos y el motivo. Si nada cambió, no se guarda.
     */
    public void corregir(String idPesaje, EntradaPesaje entrada, String motivo, Callback<Pesaje> callback) {
        ejecutor.ejecutar(() -> {
            Pesaje pesaje = pesajeVigente(idPesaje);
            Animal animal = animalExistente(pesaje.idAnimal);
            Map<String, ErrorCampo> errores = ValidadorPesaje.validar(entrada, animal, Tiempo.ahora());
            ErrorCampo errorMotivo = ValidadorPesaje.motivo(motivo);
            if (errorMotivo != null) errores.put(Campos.MOTIVO, errorMotivo);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            JSONObject antes = ValoresCambio.de(pesaje);
            aplicar(pesaje, entrada, animal);
            JSONObject[] diferencia = ValoresCambio.diferencia(antes, ValoresCambio.de(pesaje));
            if (diferencia == null) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.PESO, new ErrorCampo(CodigoError.SIN_CAMBIOS)));
            }
            String ahora = Tiempo.ahoraUtc();
            pesaje.actualizadoEn = ahora;
            HistorialCambio edicion = registro.preparar(HistorialCambio.PESAJE, idPesaje, HistorialCambio.EDITAR,
                    diferencia[0], diferencia[1], motivo.trim(), null, ahora);
            registro.guardar(() -> db.pesajeDao().actualizar(pesaje), edicion);
            return pesaje;
        }, callback);
    }

    /**
     * Anula un pesaje (RF15, RN07) con motivo obligatorio: se conserva en la base, pero deja de aparecer en el
     * historial de peso y de contar como último peso. No se puede deshacer; repetirla no cambia nada.
     */
    public void anular(String idPesaje, String motivo, Callback<Pesaje> callback) {
        ejecutor.ejecutar(() -> {
            Pesaje pesaje = db.pesajeDao().porId(idPesaje);
            if (pesaje == null) throw ErrorModelo.noEncontrado(ErrorModelo.PESAJE);
            if (pesaje.anulado == 1) return pesaje;
            ErrorCampo errorMotivo = ValidadorPesaje.motivo(motivo);
            if (errorMotivo != null) throw ErrorModelo.validacion(Collections.singletonMap(Campos.MOTIVO, errorMotivo));
            // La entrada guarda qué medición se anuló, para poder mostrarla cuando ya no esté en la lista.
            JSONObject medicion = new JSONObject();
            ValoresCambio.poner(medicion, "peso_kg", pesaje.pesoKg);
            ValoresCambio.poner(medicion, "fecha", pesaje.fecha);
            ValoresCambio.poner(medicion, "hora", pesaje.hora);
            JSONObject anulado = new JSONObject();
            ValoresCambio.poner(anulado, "anulado", 1);
            String ahora = Tiempo.ahoraUtc();
            pesaje.anulado = 1;
            pesaje.anuladoEn = ahora;
            pesaje.motivoAnulacion = motivo.trim();
            pesaje.actualizadoEn = ahora;
            HistorialCambio anulacion = registro.preparar(HistorialCambio.PESAJE, idPesaje, HistorialCambio.ANULAR,
                    medicion, anulado, pesaje.motivoAnulacion, null, ahora);
            registro.guardar(() -> db.pesajeDao().actualizar(pesaje), anulacion);
            return pesaje;
        }, callback);
    }

    public void historial(String idAnimal, Callback<List<Pesaje>> callback) {
        ejecutor.ejecutar(() -> {
            animalExistente(idAnimal);
            return db.pesajeDao().historial(idAnimal);
        }, callback);
    }

    /** Copia al pesaje los datos ya validados y lo clasifica como antecedente si es anterior al ingreso. */
    private static void aplicar(Pesaje pesaje, EntradaPesaje entrada, Animal animal) {
        pesaje.fecha = entrada.fecha;
        pesaje.hora = entrada.hora.isEmpty() ? null : LocalTime.parse(entrada.hora).format(HORA);
        pesaje.pesoKg = ValidadorPesaje.peso(entrada.pesoKg);
        pesaje.esAntecedente = ValidadorPesaje.esAntecedente(entrada.fecha, animal) ? 1 : 0;
        pesaje.observaciones = entrada.observaciones.isEmpty() ? null : entrada.observaciones;
    }

    private Pesaje pesajeVigente(String idPesaje) throws ErrorModelo {
        Pesaje pesaje = db.pesajeDao().porId(idPesaje);
        if (pesaje == null || pesaje.anulado == 1) throw ErrorModelo.noEncontrado(ErrorModelo.PESAJE);
        return pesaje;
    }

    private Animal animalExistente(String idAnimal) throws ErrorModelo {
        Animal animal = db.animalDao().porId(idAnimal);
        if (animal == null) throw ErrorModelo.noEncontrado(ErrorModelo.ANIMAL);
        return animal;
    }
}
