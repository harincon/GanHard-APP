package com.example.ganhard.modelo.repositorio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.EntradaConfiguracion;
import com.example.ganhard.pruebas.Captura;
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
public class ConfiguracionRepositoryTest {
    private GanHardDatabase db;
    private ConfiguracionRepository repositorio;

    @Before public void abrir() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GanHardDatabase.class)
                .allowMainThreadQueries().build();
        repositorio = new ConfiguracionRepository(db.configuracionDao(), new EjecutorModelo(Runnable::run));
    }
    @After public void cerrar() { db.close(); }

    private EntradaConfiguracion entrada(String nombreFinca) {
        return new EntradaConfiguracion("Helver", "helver@ejemplo.com", nombreFinca, "Vereda El Rosal", "24,5");
    }

    @Test public void creaPerfilYFincaConUuidNuevos() {
        Captura<ConfiguracionLocal> captura = new Captura<>();
        repositorio.guardar(entrada("La Esperanza"), captura);
        captura.esperar();
        assertTrue(captura.exito);
        assertEquals(36, captura.valor.usuario.idUsuario.length());
        assertEquals(36, captura.valor.finca.idFinca.length());
        assertEquals(captura.valor.usuario.idUsuario, captura.valor.finca.idPropietario);
        assertTrue(captura.valor.finca.creadoEn.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$"));
        assertEquals(Double.valueOf(24.5), captura.valor.finca.areaHectareas);
    }

    @Test public void editarConservaIdsYCreadoEn() {
        Captura<ConfiguracionLocal> primera = new Captura<>();
        repositorio.guardar(entrada("La Esperanza"), primera);
        primera.esperar();
        Captura<ConfiguracionLocal> segunda = new Captura<>();
        repositorio.guardar(entrada("La Esperanza II"), segunda);
        segunda.esperar();
        assertEquals(primera.valor.usuario.idUsuario, segunda.valor.usuario.idUsuario);
        assertEquals(primera.valor.finca.idFinca, segunda.valor.finca.idFinca);
        assertEquals(primera.valor.finca.creadoEn, segunda.valor.finca.creadoEn);
        assertEquals("La Esperanza II", segunda.valor.finca.nombre);
        assertNotEquals("", segunda.valor.finca.actualizadoEn);
    }

    @Test public void entradaInvalidaFallaConValidacionYNoEscribe() {
        Captura<ConfiguracionLocal> captura = new Captura<>();
        repositorio.guardar(new EntradaConfiguracion("", "helver@ejemplo.com", "La Esperanza", "", ""), captura);
        captura.esperar();
        assertEquals(ErrorModelo.Tipo.VALIDACION, captura.error.tipo);
        assertTrue(captura.error.campos.containsKey("nombre"));
        assertNull(db.configuracionDao().cargar());
    }

    @Test public void cargarSinDatosEntregaNull() {
        Captura<ConfiguracionLocal> captura = new Captura<>();
        repositorio.cargar(captura);
        captura.esperar();
        assertTrue(captura.exito);
        assertNull(captura.valor);
    }

    @Test public void idFincaActivaCorrespondeALaFincaGuardada() {
        assertNull(db.configuracionDao().idFincaActiva());
        Captura<ConfiguracionLocal> captura = new Captura<>();
        repositorio.guardar(entrada("La Esperanza"), captura);
        captura.esperar();
        assertNotNull(db.configuracionDao().idFincaActiva());
        assertEquals(captura.valor.finca.idFinca, db.configuracionDao().idFincaActiva());
    }
}
