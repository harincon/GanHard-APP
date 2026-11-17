package com.example.ganhard.modelo.repositorio;

import android.database.sqlite.SQLiteConstraintException;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.datos.IdentificadorResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.AnimalIdentificador;
import com.example.ganhard.modelo.entidad.Hierro;
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
import com.example.ganhard.modelo.reglas.EntradaIdentificador;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.GeneradorCodigo;
import com.example.ganhard.modelo.reglas.ValidadorAnimal;
import com.example.ganhard.modelo.reglas.ValidadorIdentificadores;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
            String idFinca = idFincaActiva();
            validar(entrada, true, idFinca, Collections.<String>emptySet(), entrada.identificadores != null);
            Animal animal = nuevo(idAnimal, entrada, idFinca);
            // Sin código escrito se asigna el siguiente; se calcula en esta misma tarea, así que no hay carreras.
            if (animal.codigo.isEmpty()) animal.codigo = GeneradorCodigo.siguiente(db.animalDao().codigos(idFinca));
            List<AnimalIdentificador> filas = entrada.identificadores == null
                    ? Collections.<AnimalIdentificador>emptyList() : filasNuevas(idAnimal, entrada.identificadores, animal.creadoEn);
            HistorialCambio creacion = registro.preparar(HistorialCambio.ANIMAL, idAnimal, HistorialCambio.CREAR,
                    null, ValoresCambio.de(animal, ValoresCambio.identificadores(resumenes(filas))), null, null, animal.creadoEn);
            try {
                registro.guardar(() -> {
                    db.animalDao().insertar(animal);
                    for (AnimalIdentificador fila : filas) db.identificadorDao().insertar(fila);
                }, creacion);
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
            List<IdentificadorResumen> actuales = db.identificadorDao().delAnimal(idAnimal);
            String identificadoresAntes = ValoresCambio.identificadores(actuales);
            JSONObject datosAntes = ValoresCambio.de(animal, identificadoresAntes), estadoAntes = ValoresCambio.estado(animal);
            // Un animal que ya tiene hierro no puede quedarse sin ninguno; uno viejo sin hierro se edita sin exigirle uno.
            boolean exigirHierro = entrada.identificadores != null && tieneHierro(actuales);
            Set<String> hierrosDelAnimal = new HashSet<>();
            for (IdentificadorResumen actual : actuales) if (actual.idHierro != null) hierrosDelAnimal.add(actual.idHierro);
            validar(entrada, false, animal.idFinca, hierrosDelAnimal, exigirHierro);
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

            Plan identificadores = entrada.identificadores == null ? Plan.VACIO : planear(idAnimal, actuales, entrada.identificadores);
            String identificadoresDespues = entrada.identificadores == null ? identificadoresAntes
                    : ValoresCambio.identificadores(resumenes(identificadores.resultado));
            JSONObject[] datos = ValoresCambio.diferencia(datosAntes, ValoresCambio.de(animal, identificadoresDespues));
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
                    for (String id : identificadores.eliminar) db.identificadorDao().eliminar(id);
                    for (Map.Entry<String, Integer> nuevoOrden : identificadores.reordenar.entrySet()) {
                        db.identificadorDao().cambiarOrden(nuevoOrden.getKey(), nuevoOrden.getValue(), ahora);
                    }
                    for (AnimalIdentificador fila : identificadores.insertar) db.identificadorDao().insertar(fila);
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
            String idFinca = idFincaActiva();
            List<AnimalResumen> animales = db.animalDao().buscar(idFinca, estado);
            String buscado = normalizar(texto == null ? "" : texto.trim());
            if (buscado.isEmpty()) return animales;
            // También se busca en los identificadores: el nombre del hierro, el arete y los números.
            Map<String, StringBuilder> identificadores = new HashMap<>();
            for (IdentificadorResumen identificador : db.identificadorDao().deLaFinca(idFinca)) {
                identificadores.computeIfAbsent(identificador.idAnimal, id -> new StringBuilder()).append(normalizar(identificador.texto())).append('\n');
            }
            List<AnimalResumen> coincidencias = new ArrayList<>();
            for (AnimalResumen animal : animales) {
                StringBuilder propios = identificadores.get(animal.idAnimal);
                if (normalizar(animal.codigo).contains(buscado) || normalizar(animal.nombre).contains(buscado)
                        || (propios != null && propios.indexOf(buscado) >= 0)) coincidencias.add(animal);
            }
            return coincidencias;
        }, callback);
    }

    /** Los identificadores del animal en su orden, con el nombre de cada hierro ya resuelto. */
    public void identificadores(String idAnimal, Callback<List<IdentificadorResumen>> callback) {
        ejecutor.ejecutar(() -> {
            animalExistente(idAnimal);
            return db.identificadorDao().delAnimal(idAnimal);
        }, callback);
    }

    public void contarActivos(Callback<Integer> callback) {
        ejecutor.ejecutar(() -> db.animalDao().contarActivos(idFincaActiva()), callback);
    }

    public void razas(Callback<List<Raza>> callback) {
        ejecutor.ejecutar(() -> db.razaDao().todas(), callback);
    }

    /**
     * Valida el animal y, si la entrada trae identificadores, también ellos: que el hierro elegido exista en la
     * finca y esté activo (uno desactivado solo vale si el animal ya lo tenía) y, si se pide, que haya al menos uno.
     * {@code creacion} permite dejar el código vacío, porque el Modelo asigna el siguiente.
     */
    private void validar(EntradaAnimal entrada, boolean creacion, String idFinca, Set<String> hierrosDelAnimal, boolean exigirHierro) throws ErrorModelo {
        Map<String, ErrorCampo> errores = ValidadorAnimal.validar(entrada, Tiempo.hoy(), !creacion);
        if (!entrada.idRaza.isEmpty() && db.razaDao().porId(entrada.idRaza) == null) {
            errores.put(Campos.ID_RAZA, new ErrorCampo(CodigoError.VALOR_INVALIDO));
        }
        if (entrada.identificadores != null) {
            errores.putAll(ValidadorIdentificadores.validar(entrada.identificadores, exigirHierro));
            if (!errores.containsKey(Campos.HIERRO)) {
                for (EntradaIdentificador identificador : entrada.identificadores) {
                    if (!identificador.esHierro() || identificador.idHierro.isEmpty()) continue;
                    Hierro hierro = db.hierroDao().porId(identificador.idHierro);
                    boolean utilizable = hierro != null && hierro.idFinca.equals(idFinca)
                            && (hierro.activo == 1 || hierrosDelAnimal.contains(identificador.idHierro));
                    if (!utilizable) {
                        errores.put(Campos.HIERRO, new ErrorCampo(CodigoError.VALOR_INVALIDO));
                        break;
                    }
                }
            }
        }
        if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
    }

    private static boolean tieneHierro(List<IdentificadorResumen> identificadores) {
        for (IdentificadorResumen identificador : identificadores) if (AnimalIdentificador.HIERRO.equals(identificador.tipo)) return true;
        return false;
    }

    /** Lo que hay que escribir para que los identificadores guardados queden como la lista nueva. */
    private static final class Plan {
        static final Plan VACIO = new Plan();
        final List<AnimalIdentificador> insertar = new ArrayList<>(), resultado = new ArrayList<>();
        final List<String> eliminar = new ArrayList<>();
        final Map<String, Integer> reordenar = new LinkedHashMap<>();
    }

    /**
     * Compara la lista nueva con la guardada: lo que no cambió conserva su fila (y su id), lo nuevo se inserta y lo
     * que ya no está se elimina. Un hierro sin elegir no se guarda.
     */
    private Plan planear(String idAnimal, List<IdentificadorResumen> actuales, List<EntradaIdentificador> nuevas) {
        Plan plan = new Plan();
        List<IdentificadorResumen> libres = new ArrayList<>(actuales);
        String ahora = Tiempo.ahoraUtc();
        int orden = 0;
        for (EntradaIdentificador entrada : nuevas) {
            if (entrada.esHierro() && entrada.idHierro.isEmpty()) continue;
            IdentificadorResumen igual = null;
            for (IdentificadorResumen candidato : libres) {
                if (candidato.tipo.equals(entrada.tipo) && entrada.idHierro.equals(vacio(candidato.idHierro)) && entrada.valor.equals(vacio(candidato.valor))) {
                    igual = candidato;
                    break;
                }
            }
            AnimalIdentificador fila;
            if (igual != null) {
                libres.remove(igual);
                fila = deResumen(igual);
                if (igual.orden != orden) {
                    plan.reordenar.put(igual.idAnimalIdentificador, orden);
                    fila.orden = orden;
                }
            } else {
                fila = nueva(idAnimal, entrada, orden, ahora);
                plan.insertar.add(fila);
            }
            plan.resultado.add(fila);
            orden++;
        }
        for (IdentificadorResumen sobrante : libres) plan.eliminar.add(sobrante.idAnimalIdentificador);
        return plan;
    }

    private static String vacio(String texto) { return texto == null ? "" : texto; }

    private static AnimalIdentificador deResumen(IdentificadorResumen resumen) {
        AnimalIdentificador fila = new AnimalIdentificador();
        fila.idAnimalIdentificador = resumen.idAnimalIdentificador;
        fila.idAnimal = resumen.idAnimal;
        fila.tipo = resumen.tipo;
        fila.idHierro = resumen.idHierro;
        fila.valor = resumen.valor;
        fila.orden = resumen.orden;
        return fila;
    }

    private static AnimalIdentificador nueva(String idAnimal, EntradaIdentificador entrada, int orden, String ahora) {
        AnimalIdentificador fila = new AnimalIdentificador();
        fila.idAnimalIdentificador = UUID.randomUUID().toString();
        fila.idAnimal = idAnimal;
        fila.tipo = entrada.tipo;
        fila.idHierro = entrada.esHierro() ? entrada.idHierro : null;
        fila.valor = entrada.esHierro() ? null : entrada.valor;
        fila.orden = orden;
        fila.creadoEn = ahora;
        fila.actualizadoEn = ahora;
        return fila;
    }

    /** Las filas de un animal nuevo, en el orden de la lista y sin los hierros sin elegir. */
    private static List<AnimalIdentificador> filasNuevas(String idAnimal, List<EntradaIdentificador> entradas, String ahora) {
        List<AnimalIdentificador> filas = new ArrayList<>();
        for (EntradaIdentificador entrada : entradas) {
            if (entrada.esHierro() && entrada.idHierro.isEmpty()) continue;
            filas.add(nueva(idAnimal, entrada, filas.size(), ahora));
        }
        return filas;
    }

    /** Las filas con el nombre de su hierro, para escribirlas en el historial. */
    private List<IdentificadorResumen> resumenes(List<AnimalIdentificador> filas) {
        List<IdentificadorResumen> resumenes = new ArrayList<>();
        for (AnimalIdentificador fila : filas) {
            IdentificadorResumen resumen = new IdentificadorResumen();
            resumen.tipo = fila.tipo;
            resumen.valor = fila.valor;
            resumen.idHierro = fila.idHierro;
            if (fila.idHierro != null) {
                Hierro hierro = db.hierroDao().porId(fila.idHierro);
                resumen.hierroNombre = hierro == null ? fila.idHierro : hierro.nombre;
            }
            resumenes.add(resumen);
        }
        return resumenes;
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
