package com.example.ganhard.modelo.repositorio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.pruebas.Captura;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class AnimalRepositoryTest {
    private static final String INSTANTE = "2026-10-12T15:00:00.000Z";
    private GanHardDatabase db;
    private AnimalRepository repositorio;

    @Before public void abrir() {
        db = GanHardDatabase.constructor(RuntimeEnvironment.getApplication(), null).allowMainThreadQueries().build();
        repositorio = new AnimalRepository(db, new EjecutorModelo(Runnable::run));
    }
    @After public void cerrar() { db.close(); }

    private void fincaConfigurada(String idFinca, String creadoEn) {
        Usuario usuario = new Usuario();
        usuario.idUsuario = "usuario-" + idFinca;
        usuario.nombre = "Helver";
        usuario.correo = "h@ejemplo.com";
        Finca finca = new Finca();
        finca.idFinca = idFinca;
        finca.idPropietario = usuario.idUsuario;
        finca.nombre = "Finca " + idFinca;
        finca.creadoEn = creadoEn;
        finca.actualizadoEn = creadoEn;
        db.configuracionDao().guardar(usuario, finca);
    }
    private void fincaConfigurada() { fincaConfigurada("finca-1", "2026-01-01T00:00:00.000Z"); }

    private EntradaAnimal entrada(String codigo, String nombre) {
        return new EntradaAnimal(codigo, nombre, "H", "", "2025-03-01", "2026-01-10", "");
    }
    private Animal registrar(String codigo, String nombre) {
        Captura<Animal> captura = new Captura<>();
        repositorio.registrar(entrada(codigo, nombre), captura);
        captura.esperar();
        assertTrue(codigo, captura.exito);
        return captura.valor;
    }
    private List<AnimalResumen> buscar(String texto, String estado) {
        Captura<List<AnimalResumen>> captura = new Captura<>();
        repositorio.buscar(texto, estado, captura);
        captura.esperar();
        assertTrue(captura.exito);
        return captura.valor;
    }
    private int contar(String sql) {
        try (Cursor c = db.getOpenHelper().getWritableDatabase().query(sql)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }
    private void pesaje(String id, String idAnimal, String fecha, String hora, double peso, int anulado) {
        db.getOpenHelper().getWritableDatabase().execSQL("INSERT INTO pesaje (id_pesaje, id_animal, fecha, hora, peso_kg, es_antecedente, anulado, creado_en, actualizado_en, version) "
                + "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, 0)", new Object[]{id, idAnimal, fecha, hora, peso, anulado, INSTANTE, INSTANTE});
    }
    private void marcar(String idAnimal, String estado) {
        Animal animal = db.animalDao().porId(idAnimal);
        animal.estado = estado;
        db.animalDao().actualizar(animal);
    }

    @Test public void registraYObtieneConUuidFincaYFechasUtc() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Lucero");
        assertEquals(36, animal.idAnimal.length());
        assertEquals("finca-1", animal.idFinca);
        assertEquals(Animal.ACTIVO, animal.estado);
        assertEquals("2026-01-10", animal.fechaEstado);
        assertTrue(animal.creadoEn.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$"));
        Captura<Animal> leido = new Captura<>();
        repositorio.obtener(animal.idAnimal, leido);
        leido.esperar();
        assertEquals("GH-001", leido.valor.codigo);
        assertEquals("Lucero", leido.valor.nombre);
    }

    @Test public void obtenerUnIdInexistenteFallaConNoEncontrado() {
        fincaConfigurada();
        Captura<Animal> captura = new Captura<>();
        repositorio.obtener("no-existe", captura);
        captura.esperar();
        assertEquals(ErrorModelo.Tipo.NO_ENCONTRADO, captura.error.tipo);
    }

    @Test public void codigoConEspaciosYOtraCapitalizacionEsDuplicado() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        Captura<Animal> segundo = new Captura<>();
        repositorio.registrar(entrada(" gh-001 ", "Otro"), segundo);
        segundo.esperar();
        assertEquals(ErrorModelo.Tipo.DUPLICADO, segundo.error.tipo);
        assertEquals(CodigoError.DUPLICADO, segundo.error.campos.get("codigo").codigo);
        assertEquals(1, contar("SELECT COUNT(*) FROM animal"));
    }

    @Test public void mismoCodigoEnOtraFincaSeAcepta() {
        fincaConfigurada("finca-1", "2026-01-01T00:00:00.000Z");
        fincaConfigurada("finca-2", "2026-02-01T00:00:00.000Z");
        Animal ajeno = new Animal();
        ajeno.idAnimal = "animal-ajeno";
        ajeno.idFinca = "finca-2";
        ajeno.codigo = "GH-001";
        ajeno.sexo = "H";
        ajeno.fechaIngreso = "2026-01-10";
        ajeno.fechaEstado = "2026-01-10";
        ajeno.creadoEn = INSTANTE;
        ajeno.actualizadoEn = INSTANTE;
        db.animalDao().insertar(ajeno);
        Animal propio = registrar("GH-001", "Lucero");
        assertEquals("finca-1", propio.idFinca);
        assertEquals(2, contar("SELECT COUNT(*) FROM animal"));
    }

    @Test public void entradaInvalidaNoEscribe() {
        fincaConfigurada();
        Captura<Animal> captura = new Captura<>();
        repositorio.registrar(new EntradaAnimal("GH-001", "", "", "", "", "2026-01-10", ""), captura);
        captura.esperar();
        assertEquals(ErrorModelo.Tipo.VALIDACION, captura.error.tipo);
        assertTrue(captura.error.campos.containsKey("sexo"));
        assertEquals(0, contar("SELECT COUNT(*) FROM animal"));
    }

    @Test public void razaInexistenteEsErrorDeValidacionDeLaRaza() {
        fincaConfigurada();
        Captura<Animal> captura = new Captura<>();
        repositorio.registrar(new EntradaAnimal("GH-001", "", "H", "no-existe", "", "2026-01-10", ""), captura);
        captura.esperar();
        assertEquals(ErrorModelo.Tipo.VALIDACION, captura.error.tipo);
        assertEquals(CodigoError.VALOR_INVALIDO, captura.error.campos.get("idRaza").codigo);
        assertEquals(0, contar("SELECT COUNT(*) FROM animal"));
    }

    @Test public void registraConRazaDelCatalogo() {
        fincaConfigurada();
        Captura<List<Raza>> razas = new Captura<>();
        repositorio.razas(razas);
        razas.esperar();
        assertEquals(10, razas.valor.size());
        Captura<Animal> captura = new Captura<>();
        repositorio.registrar(new EntradaAnimal("GH-001", "", "H", razas.valor.get(0).idRaza, "", "2026-01-10", ""), captura);
        captura.esperar();
        assertEquals(razas.valor.get(0).idRaza, captura.valor.idRaza);
    }

    @Test public void sinFincaConfiguradaFallaConNoEncontrado() {
        Captura<Animal> captura = new Captura<>();
        repositorio.registrar(entrada("GH-001", "Lucero"), captura);
        captura.esperar();
        assertEquals(ErrorModelo.Tipo.NO_ENCONTRADO, captura.error.tipo);
        Captura<Integer> conteo = new Captura<>();
        repositorio.contarActivos(conteo);
        conteo.esperar();
        assertEquals(ErrorModelo.Tipo.NO_ENCONTRADO, conteo.error.tipo);
    }

    @Test public void buscarPorCodigoYPorNombreSinImportarMayusculas() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        registrar("GH-002", "Estrella");
        assertEquals(1, buscar("gh-001", null).size());
        assertEquals("GH-002", buscar("ESTRE", null).get(0).codigo);
        assertEquals(2, buscar("gh", null).size());
        assertEquals(2, buscar("   ", null).size());
        assertEquals(2, buscar(null, null).size());
    }

    @Test public void buscarConPorcentajeYGuionBajoYBarraLiteralesNoDevuelveTodo() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        registrar("GH_002", "Cincuenta%");
        registrar("GH-003", "Doble\\barra");
        assertEquals(1, buscar("%", null).size());
        assertEquals("GH_002", buscar("%", null).get(0).codigo);
        assertEquals(1, buscar("_", null).size());
        assertEquals("GH_002", buscar("_", null).get(0).codigo);
        assertEquals(1, buscar("\\", null).size());
        assertEquals("GH-003", buscar("\\", null).get(0).codigo);
    }

    @Test public void buscarSinCoincidenciasDevuelveListaVacia() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        assertTrue(buscar("zzz", null).isEmpty());
    }

    @Test public void buscarConTildesUsaElTextoExacto() {
        fincaConfigurada();
        registrar("GH-001", "Perla Pérez");
        assertEquals(1, buscar("Pérez", null).size());
        assertEquals(1, buscar("pérez", null).size());
    }

    @Test public void filtrarPorEstado() {
        fincaConfigurada();
        Animal activo = registrar("GH-001", "Lucero");
        Animal vendido = registrar("GH-002", "Estrella");
        marcar(vendido.idAnimal, Animal.VENDIDO);
        assertEquals(1, buscar(null, Animal.ACTIVO).size());
        assertEquals(activo.idAnimal, buscar(null, Animal.ACTIVO).get(0).idAnimal);
        assertEquals(1, buscar(null, Animal.VENDIDO).size());
        assertEquals(2, buscar(null, null).size());
    }

    @Test public void buscarSoloDevuelveLosAnimalesDeLaFincaActiva() {
        fincaConfigurada("finca-1", "2026-01-01T00:00:00.000Z");
        fincaConfigurada("finca-2", "2026-02-01T00:00:00.000Z");
        Animal ajeno = new Animal();
        ajeno.idAnimal = "animal-ajeno";
        ajeno.idFinca = "finca-2";
        ajeno.codigo = "OTRO-1";
        ajeno.sexo = "M";
        ajeno.fechaIngreso = "2026-01-10";
        ajeno.fechaEstado = "2026-01-10";
        ajeno.creadoEn = INSTANTE;
        ajeno.actualizadoEn = INSTANTE;
        db.animalDao().insertar(ajeno);
        registrar("GH-001", "Lucero");
        assertEquals(1, buscar(null, null).size());
        assertEquals("GH-001", buscar(null, null).get(0).codigo);
    }

    @Test public void contarActivosIgnoraVendidosYFallecidos() {
        fincaConfigurada();
        registrar("GH-001", "Uno");
        Animal vendido = registrar("GH-002", "Dos");
        Animal fallecido = registrar("GH-003", "Tres");
        registrar("GH-004", "Cuatro");
        marcar(vendido.idAnimal, Animal.VENDIDO);
        marcar(fallecido.idAnimal, Animal.FALLECIDO);
        Captura<Integer> conteo = new Captura<>();
        repositorio.contarActivos(conteo);
        conteo.esperar();
        assertEquals(Integer.valueOf(2), conteo.valor);
    }

    @Test public void resumenTraeElUltimoPesoVigenteONull() {
        fincaConfigurada();
        Animal con = registrar("GH-001", "Con pesajes");
        Animal sin = registrar("GH-002", "Sin pesajes");
        pesaje("p1", con.idAnimal, "2026-02-01", "08:00:00", 400, 0);
        pesaje("p2", con.idAnimal, "2026-03-01", "09:00:00", 420.5, 0);
        pesaje("p3", con.idAnimal, "2026-03-01", "18:00:00", 425, 0);
        pesaje("p4", con.idAnimal, "2026-04-01", "08:00:00", 500, 1);
        for (AnimalResumen resumen : buscar(null, null)) {
            if (resumen.idAnimal.equals(con.idAnimal)) assertEquals(Double.valueOf(425), resumen.ultimoPesoKg);
            if (resumen.idAnimal.equals(sin.idAnimal)) assertNull(resumen.ultimoPesoKg);
        }
        assertNotNull(buscar("Con", null).get(0).ultimoPesoKg);
    }

    @Test public void razasDevuelveElCatalogoOrdenadoPorNombre() {
        Captura<List<Raza>> razas = new Captura<>();
        repositorio.razas(razas);
        razas.esperar();
        assertEquals(10, razas.valor.size());
        assertEquals("Angus", razas.valor.get(0).nombre);
    }

    private Captura<Animal> actualizar(String idAnimal, EntradaAnimal entrada) {
        Captura<Animal> captura = new Captura<>();
        repositorio.actualizar(idAnimal, entrada, captura);
        return captura.esperar();
    }
    private Captura<Animal> cambiarEstado(String idAnimal, String estado, String fecha) {
        Captura<Animal> captura = new Captura<>();
        repositorio.cambiarEstado(idAnimal, estado, fecha, captura);
        return captura.esperar();
    }
    private int antecedente(String idPesaje) {
        try (Cursor c = db.getOpenHelper().getWritableDatabase().query("SELECT es_antecedente FROM pesaje WHERE id_pesaje = ?", new Object[]{idPesaje})) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    @Test public void editarConservaIdentidadYPesajes() {
        fincaConfigurada();
        Animal original = registrar("GH-001", "Lucero");
        pesaje("p1", original.idAnimal, "2026-02-01", "08:00:00", 400, 0);
        Captura<Animal> captura = actualizar(original.idAnimal, new EntradaAnimal("GH-001B", "Estrellita", "M", "", "2025-03-01", "2026-01-10", "Nota"));
        assertTrue(captura.exito);
        assertEquals(original.idAnimal, captura.valor.idAnimal);
        assertEquals(original.idFinca, captura.valor.idFinca);
        assertEquals(original.creadoEn, captura.valor.creadoEn);
        assertEquals("GH-001B", captura.valor.codigo);
        assertEquals("Estrellita", captura.valor.nombre);
        assertEquals("M", captura.valor.sexo);
        assertEquals("Nota", captura.valor.observaciones);
        assertEquals(Animal.ACTIVO, captura.valor.estado);
        assertEquals(1, contar("SELECT COUNT(*) FROM animal"));
        assertEquals(1, contar("SELECT COUNT(*) FROM pesaje"));
    }

    @Test public void editarElCodigoAOtroExistenteEsDuplicadoYNoCambiaNada() {
        fincaConfigurada();
        registrar("GH-001", "Uno");
        Animal segundo = registrar("GH-002", "Dos");
        Captura<Animal> captura = actualizar(segundo.idAnimal, entrada(" gh-001 ", "Dos"));
        assertEquals(ErrorModelo.Tipo.DUPLICADO, captura.error.tipo);
        assertEquals(CodigoError.DUPLICADO, captura.error.campos.get("codigo").codigo);
        assertEquals("GH-002", db.animalDao().porId(segundo.idAnimal).codigo);
    }

    @Test public void editarConservandoSuPropioCodigoAunqueCambieLaCapitalizacionNoEsDuplicado() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Uno");
        Captura<Animal> captura = actualizar(animal.idAnimal, entrada("gh-001 ", "Uno editado"));
        assertTrue(captura.exito);
        assertEquals("gh-001", captura.valor.codigo);
    }

    @Test public void editarUnAnimalInexistenteFallaConNoEncontrado() {
        fincaConfigurada();
        assertEquals(ErrorModelo.Tipo.NO_ENCONTRADO, actualizar("no-existe", entrada("GH-001", "Uno")).error.tipo);
    }

    @Test public void editarConDatosInvalidosNoEscribe() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Uno");
        Captura<Animal> captura = actualizar(animal.idAnimal, new EntradaAnimal("GH-001", "Otro nombre", "", "", "", "2026-01-10", ""));
        assertEquals(ErrorModelo.Tipo.VALIDACION, captura.error.tipo);
        assertEquals("Uno", db.animalDao().porId(animal.idAnimal).nombre);
    }

    @Test public void editarElIngresoRecalculaLosAntecedentesDeSusPesajes() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Uno");
        pesaje("p1", animal.idAnimal, "2026-01-05", "08:00:00", 380, 0);
        pesaje("p2", animal.idAnimal, "2026-02-01", "08:00:00", 400, 0);
        actualizar(animal.idAnimal, new EntradaAnimal("GH-001", "Uno", "H", "", "2025-03-01", "2026-03-01", ""));
        assertEquals(1, antecedente("p1"));
        assertEquals(1, antecedente("p2"));
        actualizar(animal.idAnimal, new EntradaAnimal("GH-001", "Uno", "H", "", "2025-03-01", "2026-01-01", ""));
        assertEquals(0, antecedente("p1"));
        assertEquals(0, antecedente("p2"));
    }

    @Test public void editarElIngresoDeUnAnimalActivoMueveSuFechaDeEstado() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Uno");
        Captura<Animal> captura = actualizar(animal.idAnimal, new EntradaAnimal("GH-001", "Uno", "H", "", "", "2026-01-02", ""));
        assertEquals("2026-01-02", captura.valor.fechaEstado);
    }

    @Test public void cambiarAVendidoConservaLaFichaYLosPesajes() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Uno");
        pesaje("p1", animal.idAnimal, "2026-02-01", "08:00:00", 400, 0);
        Captura<Animal> captura = cambiarEstado(animal.idAnimal, Animal.VENDIDO, "2026-06-01");
        assertTrue(captura.exito);
        assertEquals(Animal.VENDIDO, captura.valor.estado);
        assertEquals("2026-06-01", captura.valor.fechaEstado);
        assertEquals("GH-001", captura.valor.codigo);
        assertEquals(1, contar("SELECT COUNT(*) FROM pesaje"));
    }

    @Test public void cambiarElEstadoConFechaFuturaOAnteriorAlIngresoFallaYNoCambiaNada() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Uno");
        Captura<Animal> futura = cambiarEstado(animal.idAnimal, Animal.FALLECIDO, java.time.LocalDate.now().plusDays(1).toString());
        assertEquals(CodigoError.FECHA_FUTURA, futura.error.campos.get("fechaEstado").codigo);
        Captura<Animal> anterior = cambiarEstado(animal.idAnimal, Animal.FALLECIDO, "2026-01-09");
        assertEquals(CodigoError.FECHA_ANTERIOR, anterior.error.campos.get("fechaEstado").codigo);
        assertEquals(Animal.ACTIVO, db.animalDao().porId(animal.idAnimal).estado);
    }

    @Test public void cambiarAUnEstadoDesconocidoOUnAnimalInexistenteFalla() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Uno");
        Captura<Animal> desconocido = cambiarEstado(animal.idAnimal, "PERDIDO", "2026-06-01");
        assertEquals(CodigoError.VALOR_INVALIDO, desconocido.error.campos.get("estado").codigo);
        assertEquals(ErrorModelo.Tipo.NO_ENCONTRADO, cambiarEstado("no-existe", Animal.VENDIDO, "2026-06-01").error.tipo);
    }

    @Test public void elInventarioActivoSeActualizaTrasElCambioDeEstado() {
        fincaConfigurada();
        Animal uno = registrar("GH-001", "Uno");
        registrar("GH-002", "Dos");
        cambiarEstado(uno.idAnimal, Animal.FALLECIDO, "2026-06-01");
        Captura<Integer> tras = new Captura<>();
        repositorio.contarActivos(tras);
        assertEquals(Integer.valueOf(1), tras.esperar().valor);
        cambiarEstado(uno.idAnimal, Animal.ACTIVO, "2026-06-02");
        Captura<Integer> vuelta = new Captura<>();
        repositorio.contarActivos(vuelta);
        assertEquals(Integer.valueOf(2), vuelta.esperar().valor);
    }

    @Test public void buscarIgnoraTildesYMayusculas() {
        fincaConfigurada();
        registrar("GH-001", "Perla Pérez");
        registrar("GH-002", "LUCÍA");
        for (String texto : new String[]{"perez", "PÉREZ", "pérez"}) assertEquals(texto, "GH-001", buscar(texto, null).get(0).codigo);
        for (String texto : new String[]{"lucia", "LUCÍA", "lucía"}) assertEquals(texto, "GH-002", buscar(texto, null).get(0).codigo);
        assertEquals(0, buscar("zzz", null).size());
    }

    @Test public void registrarDosVecesConElMismoIdDevuelveElMismoAnimalSinDuplicar() {
        fincaConfigurada();
        Captura<Animal> primero = new Captura<>();
        repositorio.registrar("id-fijo", entrada("GH-001", "Uno"), primero);
        primero.esperar();
        Captura<Animal> segundo = new Captura<>();
        repositorio.registrar("id-fijo", entrada("GH-001", "Uno"), segundo);
        segundo.esperar();
        assertTrue(primero.exito);
        assertTrue(segundo.exito);
        assertEquals("id-fijo", segundo.valor.idAnimal);
        assertEquals(1, contar("SELECT COUNT(*) FROM animal"));
    }

    @Test public void cambiarAlMismoEstadoYFechaNoModificaNada() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Uno");
        assertTrue(cambiarEstado(animal.idAnimal, Animal.VENDIDO, "2026-06-01").exito);
        db.getOpenHelper().getWritableDatabase().execSQL("UPDATE animal SET actualizado_en = '2000-01-01T00:00:00.000Z'");
        assertTrue(cambiarEstado(animal.idAnimal, Animal.VENDIDO, "2026-06-01").exito);
        assertEquals("2000-01-01T00:00:00.000Z", db.animalDao().porId(animal.idAnimal).actualizadoEn);
    }

    @Test public void elResumenTraeLaRazaYLaFechaDelUltimoPesoVigente() {
        fincaConfigurada();
        Captura<Animal> captura = new Captura<>();
        repositorio.registrar(new EntradaAnimal("GH-014", "Luna", "H", "853193c3-3b52-4d5f-a4df-f73be27e5da1", "", "2026-01-10", ""), captura);
        Animal luna = captura.esperar().valor;
        pesaje("p1", luna.idAnimal, "2026-09-25", "07:40:00", 242, 0);
        pesaje("p2", luna.idAnimal, "2026-10-10", "08:15:00", 250, 0);
        pesaje("p3", luna.idAnimal, "2026-10-11", "08:15:00", 999, 1);
        AnimalResumen resumen = buscar(null, null).get(0);
        assertEquals("Brahman", resumen.razaNombre);
        assertEquals(Double.valueOf(250), resumen.ultimoPesoKg);
        assertEquals("2026-10-10", resumen.ultimoPesoFecha);
    }

    @Test public void unAnimalSinRazaNiPesajesTraeNulos() {
        fincaConfigurada();
        registrar("GH-022", "Mora");
        AnimalResumen resumen = buscar(null, null).get(0);
        assertNull(resumen.razaNombre);
        assertNull(resumen.ultimoPesoKg);
        assertNull(resumen.ultimoPesoFecha);
    }
}
