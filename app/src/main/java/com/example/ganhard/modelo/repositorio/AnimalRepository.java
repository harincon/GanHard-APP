package com.example.ganhard.modelo.repositorio;

import android.database.sqlite.SQLiteConstraintException;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorAnimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

/**
 * Apoyo del Modelo para animales. Todo corre en el hilo del {@link EjecutorModelo}, que atiende una operación
 * a la vez; por eso resolver la finca y escribir dentro de una misma tarea no admite interferencias.
 * Cada escritura pasa por {@link RegistroCambios}, que la guarda junto con su entrada de historial.
 */
public final class AnimalRepository {
    private static final List<String> ESTADOS = Arrays.asList(Animal.ACTIVO, Animal.VENDIDO, Animal.FALLECIDO);

    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;
    private final RegistroCambios registro;

    public AnimalRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
        this.registro = new RegistroCambios(db);
    }

    public void registrar(EntradaAnimal entrada, Callback<Animal> callback) {
        registrar(UUID.randomUUID().toString(), entrada, callback);
    }

    /**
     * Registra con un id elegido por quien llama. Es idempotente: si ese id ya existe, devuelve el animal
     * guardado sin crear otro, de modo que repetir la solicitud (por ejemplo tras rotar la pantalla) no duplica.
     */
    public void registrar(String idAnimal, EntradaAnimal entrada, Callback<Animal> callback) {
        ejecutor.ejecutar(() -> {
            Animal existente = db.animalDao().porId(idAnimal);
            if (existente != null) return existente;
            validar(entrada);
            Animal animal = nuevo(idAnimal, entrada, idFincaActiva());
            HistorialCambio creacion = registro.preparar(HistorialCambio.ANIMAL, idAnimal, HistorialCambio.CREAR,
                    null, ValoresCambio.de(animal), null, null, animal.creadoEn);
            try {
                registro.guardar(() -> db.animalDao().insertar(animal), creacion);
            } catch (SQLiteConstraintException ex) {
                throw errorDeRestriccion(ex);
            }
            return animal;
        }, callback);
    }

    /**
     * Único camino para modificar un animal: actualiza los datos editables (RF06) y el estado activo, vendido o
     * fallecido con su fecha efectiva (RF07) sin crear otro registro; conserva id, finca, fecha de alta y pesajes.
     * Si el estado y la fecha son los que el animal ya tiene, el estado no se toca; si cambian, deben ser válidos
     * (estado conocido; fecha no futura ni anterior al ingreso, ya con el ingreso nuevo) o no se escribe nada.
     * Si cambia el ingreso, los pesajes se vuelven a clasificar como antecedente o no.
     * En el historial queda una entrada por los datos que cambiaron y otra por el estado, si cambió; cuando no
     * cambia nada, no se escribe nada.
     */
    public void actualizar(String idAnimal, EntradaAnimal entrada, String estado, String fechaEstado, Callback<Animal> callback) {
        ejecutor.ejecutar(() -> {
            Animal animal = animalExistente(idAnimal);
            JSONObject datosAntes = ValoresCambio.de(animal), estadoAntes = ValoresCambio.estado(animal);
            validar(entrada);
            boolean estadoSinCambio = estado != null && fechaEstado != null
                    && estado.equals(animal.estado) && fechaEstado.trim().equals(animal.fechaEstado);
            aplicarDatos(animal, entrada);
            if (!estadoSinCambio) {
                Map<String, ErrorCampo> errores = new LinkedHashMap<>();
                if (estado == null || !ESTADOS.contains(estado)) errores.put(Campos.ESTADO, new ErrorCampo(CodigoError.VALOR_INVALIDO));
                ErrorCampo fecha = ValidadorAnimal.fechaEstado(fechaEstado, animal, Tiempo.hoy());
                if (fecha != null) errores.put(Campos.FECHA_ESTADO, fecha);
                if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
                animal.estado = estado;
                animal.fechaEstado = fechaEstado.trim();
            }
            verificarCoherencia(animal);

            JSONObject[] datos = ValoresCambio.diferencia(datosAntes, ValoresCambio.de(animal));
            if (datos == null && estadoSinCambio) return animal;
            String ahora = Tiempo.ahoraUtc();
            List<HistorialCambio> cambios = new ArrayList<>();
            if (datos != null) {
                cambios.add(registro.preparar(HistorialCambio.ANIMAL, idAnimal, HistorialCambio.EDITAR, datos[0], datos[1], null, null, ahora));
            }
            // Si el estado no se tocó, su fecha solo pudo moverse siguiendo al ingreso, y eso ya consta en los datos.
            if (!estadoSinCambio) {
                cambios.add(registro.preparar(HistorialCambio.ANIMAL, idAnimal, HistorialCambio.CAMBIAR_ESTADO,
                        estadoAntes, ValoresCambio.estado(animal), null, animal.fechaEstado, ahora));
            }
            animal.actualizadoEn = ahora;
            // RN08: un animal vendido o fallecido deja de tener actividades programadas.
            List<Tarea> aCancelar = new ArrayList<>();
            if (!estadoSinCambio && !Animal.ACTIVO.equals(animal.estado)) {
                for (Tarea tarea : db.tareaDao().pendientesDelAnimal(idAnimal)) {
                    JSONObject antes = ValoresCambio.estadoTarea(tarea);
                    tarea.estado = Tarea.CANCELADA;
                    tarea.actualizadoEn = ahora;
                    cambios.add(registro.preparar(HistorialCambio.TAREA, tarea.idTarea, HistorialCambio.CAMBIAR_ESTADO,
                            antes, ValoresCambio.estadoTarea(tarea), null, animal.fechaEstado, ahora));
                    aCancelar.add(tarea);
                }
            }
            try {
                registro.guardar(() -> {
                    db.animalDao().actualizar(animal);
                    db.pesajeDao().recalcularAntecedentes(animal.idAnimal, animal.fechaIngreso);
                    for (Tarea tarea : aCancelar) db.tareaDao().actualizar(tarea);
                }, cambios.toArray(new HistorialCambio[0]));
            } catch (SQLiteConstraintException ex) {
                throw errorDeRestriccion(ex);
            }
            return animal;
        }, callback);
    }

    public void obtener(String idAnimal, Callback<Animal> callback) {
        ejecutor.ejecutar(() -> animalExistente(idAnimal), callback);
    }

    /**
     * Busca por código o nombre ignorando mayúsculas y tildes; {@code estado} nulo trae todos los estados.
     * La lista de una finca es corta, así que se filtra en memoria con el texto normalizado.
     */
    public void buscar(String texto, String estado, Callback<List<AnimalResumen>> callback) {
        ejecutor.ejecutar(() -> {
            List<AnimalResumen> animales = db.animalDao().buscar(idFincaActiva(), estado);
            String buscado = normalizar(texto == null ? "" : texto.trim());
            if (buscado.isEmpty()) return animales;
            List<AnimalResumen> coincidencias = new ArrayList<>();
            for (AnimalResumen animal : animales) {
                if (normalizar(animal.codigo).contains(buscado) || normalizar(animal.nombre).contains(buscado)) coincidencias.add(animal);
            }
            return coincidencias;
        }, callback);
    }

    public void contarActivos(Callback<Integer> callback) {
        ejecutor.ejecutar(() -> db.animalDao().contarActivos(idFincaActiva()), callback);
    }

    public void razas(Callback<List<Raza>> callback) {
        ejecutor.ejecutar(() -> db.razaDao().todas(), callback);
    }

    private void validar(EntradaAnimal entrada) throws ErrorModelo {
        Map<String, ErrorCampo> errores = ValidadorAnimal.validar(entrada, Tiempo.hoy());
        if (!entrada.idRaza.isEmpty() && db.razaDao().porId(entrada.idRaza) == null) {
            errores.put(Campos.ID_RAZA, new ErrorCampo(CodigoError.VALOR_INVALIDO));
        }
        if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
    }

    /** Copia los datos editables; un animal activo cuya fecha de estado era su ingreso la mueve con él. */
    private static void aplicarDatos(Animal animal, EntradaAnimal entrada) {
        boolean fechaDeEstadoSigueAlIngreso = Animal.ACTIVO.equals(animal.estado) && animal.fechaIngreso.equals(animal.fechaEstado);
        animal.idRaza = entrada.idRaza.isEmpty() ? null : entrada.idRaza;
        animal.codigo = ValidadorAnimal.normalizarCodigo(entrada.codigo);
        animal.nombre = entrada.nombre.isEmpty() ? null : entrada.nombre;
        animal.sexo = entrada.sexo;
        animal.fechaNacimiento = entrada.fechaNacimiento.isEmpty() ? null : entrada.fechaNacimiento;
        animal.fechaIngreso = entrada.fechaIngreso;
        animal.observaciones = entrada.observaciones.isEmpty() ? null : entrada.observaciones;
        if (fechaDeEstadoSigueAlIngreso) animal.fechaEstado = entrada.fechaIngreso;
    }

    /** La fecha del estado no puede quedar antes del ingreso: se rechaza un ingreso editado que la deje así. */
    private static void verificarCoherencia(Animal animal) throws ErrorModelo {
        if (animal.fechaEstado.compareTo(animal.fechaIngreso) < 0) {
            Map<String, ErrorCampo> errores = new LinkedHashMap<>();
            errores.put(Campos.FECHA_INGRESO, new ErrorCampo(CodigoError.FECHA_POSTERIOR));
            throw ErrorModelo.validacion(errores);
        }
    }

    private Animal animalExistente(String idAnimal) throws ErrorModelo {
        Animal animal = db.animalDao().porId(idAnimal);
        if (animal == null) throw ErrorModelo.noEncontrado(ErrorModelo.ANIMAL);
        return animal;
    }

    private String idFincaActiva() throws ErrorModelo {
        String idFinca = db.configuracionDao().idFincaActiva();
        if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
        return idFinca;
    }

    private static Animal nuevo(String idAnimal, EntradaAnimal entrada, String idFinca) {
        Animal animal = new Animal();
        animal.idAnimal = idAnimal;
        animal.idFinca = idFinca;
        aplicarDatos(animal, entrada);
        // Un animal nuevo entra activo desde su fecha de ingreso.
        animal.estado = Animal.ACTIVO;
        animal.fechaEstado = entrada.fechaIngreso;
        animal.creadoEn = Tiempo.ahoraUtc();
        animal.actualizadoEn = animal.creadoEn;
        return animal;
    }

    /** Solo la unicidad (finca, código) cuenta como duplicado; una referencia inválida es otro tipo de fallo. */
    private static ErrorModelo errorDeRestriccion(SQLiteConstraintException ex) {
        boolean repetido = ex.getMessage() != null && ex.getMessage().toUpperCase(Locale.ROOT).contains("UNIQUE");
        return repetido ? ErrorModelo.duplicado(Campos.CODIGO) : ErrorModelo.baseDeDatos(ex);
    }

    /** Minúsculas y sin tildes, para que "Lucía" coincida con "lucia" y "LUCÍA". */
    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
