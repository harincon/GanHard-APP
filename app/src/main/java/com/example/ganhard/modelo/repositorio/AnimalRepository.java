package com.example.ganhard.modelo.repositorio;

import android.database.sqlite.SQLiteConstraintException;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
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

/**
 * Apoyo del Modelo para animales. Todo corre en el hilo del {@link EjecutorModelo}, que atiende una operación
 * a la vez; por eso resolver la finca y escribir dentro de una misma tarea no admite interferencias.
 */
public final class AnimalRepository {
    private static final List<String> ESTADOS = Arrays.asList(Animal.ACTIVO, Animal.VENDIDO, Animal.FALLECIDO);

    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;

    public AnimalRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
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
            try {
                db.animalDao().insertar(animal);
            } catch (SQLiteConstraintException ex) {
                throw errorDeRestriccion(ex);
            }
            return animal;
        }, callback);
    }

    /**
     * Actualiza los datos editables (RF06) sin crear otro registro: conserva id, finca, estado, fecha de alta
     * y los pesajes. Si cambia el ingreso, los pesajes se vuelven a clasificar como antecedente o no.
     */
    public void actualizar(String idAnimal, EntradaAnimal entrada, Callback<Animal> callback) {
        ejecutor.ejecutar(() -> {
            Animal animal = animalExistente(idAnimal);
            validar(entrada);
            aplicarDatos(animal, entrada);
            guardarCambios(animal);
            return animal;
        }, callback);
    }

    /**
     * Actualiza los datos y el estado (RF06, RF07) en una sola operación. Si el estado y la fecha son los que el
     * animal ya tiene, el estado no se toca; si cambian, deben ser válidos (estado conocido; fecha no futura ni
     * anterior al ingreso, ya con el ingreso nuevo) o no se escribe nada.
     */
    public void actualizar(String idAnimal, EntradaAnimal entrada, String estado, String fechaEstado, Callback<Animal> callback) {
        ejecutor.ejecutar(() -> {
            Animal animal = animalExistente(idAnimal);
            validar(entrada);
            boolean estadoSinCambio = estado != null && fechaEstado != null
                    && estado.equals(animal.estado) && fechaEstado.trim().equals(animal.fechaEstado);
            aplicarDatos(animal, entrada);
            if (!estadoSinCambio) {
                Map<String, ErrorCampo> errores = new LinkedHashMap<>();
                if (estado == null || !ESTADOS.contains(estado)) errores.put("estado", new ErrorCampo(CodigoError.VALOR_INVALIDO));
                ErrorCampo fecha = ValidadorAnimal.fechaEstado(fechaEstado, animal, Tiempo.hoy());
                if (fecha != null) errores.put("fechaEstado", fecha);
                if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
                animal.estado = estado;
                animal.fechaEstado = fechaEstado.trim();
            }
            guardarCambios(animal);
            return animal;
        }, callback);
    }

    /**
     * Marca el animal como activo, vendido o fallecido (RF07) con su fecha efectiva; conserva ficha y pesajes.
     * Repetir el estado y la fecha que ya tiene no cambia nada.
     */
    public void cambiarEstado(String idAnimal, String estado, String fechaEstado, Callback<Animal> callback) {
        ejecutor.ejecutar(() -> {
            Animal animal = animalExistente(idAnimal);
            if (estado != null && fechaEstado != null && estado.equals(animal.estado) && fechaEstado.trim().equals(animal.fechaEstado)) {
                return animal;
            }
            Map<String, ErrorCampo> errores = new LinkedHashMap<>();
            if (estado == null || !ESTADOS.contains(estado)) errores.put("estado", new ErrorCampo(CodigoError.VALOR_INVALIDO));
            ErrorCampo fecha = ValidadorAnimal.fechaEstado(fechaEstado, animal, Tiempo.hoy());
            if (fecha != null) errores.put("fechaEstado", fecha);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            animal.estado = estado;
            animal.fechaEstado = fechaEstado.trim();
            animal.actualizadoEn = Tiempo.ahoraUtc();
            db.animalDao().actualizar(animal);
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
            errores.put("idRaza", new ErrorCampo(CodigoError.VALOR_INVALIDO));
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
        animal.actualizadoEn = Tiempo.ahoraUtc();
    }

    /** Escribe el animal y reclasifica sus pesajes en una transacción. */
    private void guardarCambios(Animal animal) throws ErrorModelo {
        try {
            db.runInTransaction(() -> {
                db.animalDao().actualizar(animal);
                db.pesajeDao().recalcularAntecedentes(animal.idAnimal, animal.fechaIngreso);
            });
        } catch (SQLiteConstraintException ex) {
            throw errorDeRestriccion(ex);
        }
    }

    private Animal animalExistente(String idAnimal) throws ErrorModelo {
        Animal animal = db.animalDao().porId(idAnimal);
        if (animal == null) throw ErrorModelo.noEncontrado("animal");
        return animal;
    }

    private String idFincaActiva() throws ErrorModelo {
        String idFinca = db.configuracionDao().idFincaActiva();
        if (idFinca == null) throw ErrorModelo.noEncontrado("finca");
        return idFinca;
    }

    private static Animal nuevo(String idAnimal, EntradaAnimal entrada, String idFinca) {
        String ahora = Tiempo.ahoraUtc();
        Animal animal = new Animal();
        animal.idAnimal = idAnimal;
        animal.idFinca = idFinca;
        animal.idRaza = entrada.idRaza.isEmpty() ? null : entrada.idRaza;
        animal.codigo = ValidadorAnimal.normalizarCodigo(entrada.codigo);
        animal.nombre = entrada.nombre.isEmpty() ? null : entrada.nombre;
        animal.sexo = entrada.sexo;
        animal.fechaNacimiento = entrada.fechaNacimiento.isEmpty() ? null : entrada.fechaNacimiento;
        animal.fechaIngreso = entrada.fechaIngreso;
        animal.estado = Animal.ACTIVO;
        animal.fechaEstado = entrada.fechaIngreso;
        animal.observaciones = entrada.observaciones.isEmpty() ? null : entrada.observaciones;
        animal.creadoEn = ahora;
        animal.actualizadoEn = ahora;
        return animal;
    }

    /** Solo la unicidad (finca, código) cuenta como duplicado; una referencia inválida es otro tipo de fallo. */
    private static ErrorModelo errorDeRestriccion(SQLiteConstraintException ex) {
        boolean repetido = ex.getMessage() != null && ex.getMessage().toUpperCase(Locale.ROOT).contains("UNIQUE");
        return repetido ? ErrorModelo.duplicado("codigo") : ErrorModelo.baseDeDatos(ex);
    }

    /** Minúsculas y sin tildes, para que "Lucía" coincida con "lucia" y "LUCÍA". */
    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
