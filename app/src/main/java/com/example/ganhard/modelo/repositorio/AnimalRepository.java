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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
        ejecutor.ejecutar(() -> {
            validar(entrada);
            Animal animal = nuevo(entrada, idFincaActiva());
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
            try {
                db.runInTransaction(() -> {
                    db.animalDao().actualizar(animal);
                    db.pesajeDao().recalcularAntecedentes(animal.idAnimal, animal.fechaIngreso);
                });
            } catch (SQLiteConstraintException ex) {
                throw errorDeRestriccion(ex);
            }
            return animal;
        }, callback);
    }

    /** Marca el animal como activo, vendido o fallecido (RF07) con su fecha efectiva; conserva ficha y pesajes. */
    public void cambiarEstado(String idAnimal, String estado, String fechaEstado, Callback<Animal> callback) {
        ejecutor.ejecutar(() -> {
            Animal animal = animalExistente(idAnimal);
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

    /** Busca por código o nombre (sin distinguir mayúsculas ASCII); {@code estado} nulo trae todos los estados. */
    public void buscar(String texto, String estado, Callback<List<AnimalResumen>> callback) {
        ejecutor.ejecutar(() -> {
            String limpio = texto == null ? "" : texto.trim();
            String patron = limpio.isEmpty() ? null : "%" + escaparLike(limpio) + "%";
            return db.animalDao().buscar(idFincaActiva(), estado, patron);
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

    private static Animal nuevo(EntradaAnimal entrada, String idFinca) {
        String ahora = Tiempo.ahoraUtc();
        Animal animal = new Animal();
        animal.idAnimal = UUID.randomUUID().toString();
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
        boolean repetido = ex.getMessage() != null && ex.getMessage().toUpperCase(java.util.Locale.ROOT).contains("UNIQUE");
        return repetido ? ErrorModelo.duplicado("codigo") : ErrorModelo.baseDeDatos(ex);
    }

    /** Hace literales los comodines de LIKE (%, _) y el propio carácter de escape. */
    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
