package com.example.ganhard.modelo.repositorio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.modelo.reglas.EntradaPesaje;
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
public class PesajeRepositoryTest {
    private GanHardDatabase db;
    private PesajeRepository pesajes;
    private Animal animal;

    @Before public void abrir() {
        db = GanHardDatabase.constructor(RuntimeEnvironment.getApplication(), null).allowMainThreadQueries().build();
        EjecutorModelo ejecutor = new EjecutorModelo(Runnable::run);
        pesajes = new PesajeRepository(db, ejecutor);
        Usuario usuario = new Usuario();
        usuario.idUsuario = "usuario-1";
        usuario.nombre = "Helver";
        usuario.correo = "h@ejemplo.com";
        Finca finca = new Finca();
        finca.idFinca = "finca-1";
        finca.idPropietario = usuario.idUsuario;
        finca.nombre = "La Esperanza";
        db.configuracionDao().guardar(usuario, finca);
        Captura<Animal> captura = new Captura<>();
        new AnimalRepository(db, ejecutor).registrar(new EntradaAnimal("GH-001", "Lucero", "H", "", "2025-03-01", "2026-01-10", ""), captura);
        animal = captura.esperar().valor;
    }
    @After public void cerrar() { db.close(); }

    private Captura<Pesaje> registrar(String fecha, String hora, String peso) {
        Captura<Pesaje> captura = new Captura<>();
        pesajes.registrar(animal.idAnimal, new EntradaPesaje(fecha, hora, peso, ""), captura);
        return captura.esperar();
    }
    private List<Pesaje> historial() {
        Captura<List<Pesaje>> captura = new Captura<>();
        pesajes.historial(animal.idAnimal, captura);
        assertTrue(captura.esperar().exito);
        return captura.valor;
    }
    private int contar() {
        try (Cursor c = db.getOpenHelper().getWritableDatabase().query("SELECT COUNT(*) FROM pesaje")) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    @Test public void registraConPesoFechaHoraYSinAntecedente() {
        Captura<Pesaje> captura = registrar("2026-02-01", "09:15", "450,5");
        assertTrue(captura.exito);
        Pesaje pesaje = captura.valor;
        assertEquals(36, pesaje.idPesaje.length());
        assertEquals(animal.idAnimal, pesaje.idAnimal);
        assertEquals(450.5, pesaje.pesoKg, 0.0001);
        assertEquals("2026-02-01", pesaje.fecha);
        assertEquals("09:15:00", pesaje.hora);
        assertEquals(0, pesaje.esAntecedente);
        assertEquals(0, pesaje.anulado);
        assertTrue(pesaje.creadoEn.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$"));
    }

    @Test public void unPesajeAnteriorAlIngresoSeMarcaAntecedenteYPuedeOmitirLaHora() {
        Pesaje pesaje = registrar("2025-12-31", "", "380").valor;
        assertEquals(1, pesaje.esAntecedente);
        assertNull(pesaje.hora);
    }

    @Test public void variosPesajesIgualesSeConservanYSeDistinguenPorId() {
        Pesaje primero = registrar("2026-02-01", "09:15", "450").valor;
        Pesaje segundo = registrar("2026-02-01", "09:15", "450").valor;
        assertNotEquals(primero.idPesaje, segundo.idPesaje);
        assertEquals(2, historial().size());
    }

    @Test public void historialOrdenadoPorFechaYHoraDeLaMasRecienteALaMasAntigua() {
        registrar("2026-03-01", "18:00", "425");
        registrar("2026-02-01", "08:00", "400");
        registrar("2026-03-01", "09:00", "420,5");
        List<Pesaje> historial = historial();
        assertEquals(425, historial.get(0).pesoKg, 0.0001);
        assertEquals(420.5, historial.get(1).pesoKg, 0.0001);
        assertEquals(400, historial.get(2).pesoKg, 0.0001);
    }

    @Test public void ultimoEntregaElMasRecienteYEsNullSinPesajes() {
        Captura<Pesaje> vacio = new Captura<>();
        pesajes.ultimo(animal.idAnimal, vacio);
        assertTrue(vacio.esperar().exito);
        assertNull(vacio.valor);
        registrar("2026-02-01", "08:00", "400");
        registrar("2026-03-01", "09:00", "420,5");
        Captura<Pesaje> ultimo = new Captura<>();
        pesajes.ultimo(animal.idAnimal, ultimo);
        assertEquals(420.5, ultimo.esperar().valor.pesoKg, 0.0001);
    }

    @Test public void elHistorialYElUltimoIgnoranLosPesajesAnulados() {
        registrar("2026-02-01", "08:00", "400");
        Pesaje anulado = registrar("2026-03-01", "09:00", "500").valor;
        db.getOpenHelper().getWritableDatabase().execSQL("UPDATE pesaje SET anulado = 1 WHERE id_pesaje = ?", new Object[]{anulado.idPesaje});
        assertEquals(1, historial().size());
        Captura<Pesaje> ultimo = new Captura<>();
        pesajes.ultimo(animal.idAnimal, ultimo);
        assertEquals(400, ultimo.esperar().valor.pesoKg, 0.0001);
    }

    @Test public void animalInexistenteFallaConNoEncontrado() {
        Captura<Pesaje> captura = new Captura<>();
        pesajes.registrar("no-existe", new EntradaPesaje("2026-02-01", "09:15", "450", ""), captura);
        assertEquals(ErrorModelo.Tipo.NO_ENCONTRADO, captura.esperar().error.tipo);
        Captura<List<Pesaje>> lista = new Captura<>();
        pesajes.historial("no-existe", lista);
        assertEquals(ErrorModelo.Tipo.NO_ENCONTRADO, lista.esperar().error.tipo);
        assertEquals(0, contar());
    }

    @Test public void pesoInvalidoNoEscribe() {
        for (String peso : new String[]{"0", "-5", "abc", "450.555", "2001"}) {
            Captura<Pesaje> captura = registrar("2026-02-01", "09:15", peso);
            assertEquals(peso, ErrorModelo.Tipo.VALIDACION, captura.error.tipo);
            assertTrue(peso, captura.error.campos.containsKey("peso"));
        }
        assertEquals(0, contar());
    }

    @Test public void unaFechaFuturaNoEscribe() {
        Captura<Pesaje> captura = registrar(Tiempo.hoy().plusDays(1).toString(), "09:15", "450");
        assertEquals(CodigoError.FECHA_FUTURA, captura.error.campos.get("fecha").codigo);
        assertEquals(0, contar());
    }

    @Test public void unaFechaAnteriorAlNacimientoNoEscribe() {
        Captura<Pesaje> captura = registrar("2025-02-28", "", "40");
        assertEquals(CodigoError.FECHA_ANTERIOR, captura.error.campos.get("fecha").codigo);
        assertEquals(0, contar());
    }

    @Test public void hoyConUnaHoraYaTranscurridaSeGuarda() {
        assertTrue(registrar(Tiempo.hoy().toString(), "00:00", "450").exito);
    }

    @Test public void tambienSePuedePesarUnAnimalVendido() {
        Animal vendido = db.animalDao().porId(animal.idAnimal);
        vendido.estado = Animal.VENDIDO;
        db.animalDao().actualizar(vendido);
        assertTrue(registrar("2026-02-01", "09:15", "450").exito);
    }
}
