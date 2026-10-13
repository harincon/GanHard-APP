package com.example.ganhard.modelo.datos;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConfiguracionPersistenciaTest {
    private static final String ARCHIVO = "ganhard_prueba_configuracion.db";
    private Context context;
    private GanHardDatabase database;
    @Before public void abrir() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(ARCHIVO);
        database = Room.databaseBuilder(context, GanHardDatabase.class, ARCHIVO).build();
    }
    @After public void cerrar() { database.close(); context.deleteDatabase(ARCHIVO); }
    @Test public void conservaDatosAlCerrarYReabrirYPermiteEditarSinCambiarIds() {
        Usuario usuario = new Usuario(); usuario.idUsuario = "usuario-prueba"; usuario.nombre = "Helver"; usuario.correo = "h@ejemplo.com";
        Finca finca = new Finca(); finca.idFinca = "finca-prueba"; finca.idPropietario = usuario.idUsuario; finca.nombre = "La Esperanza";
        finca.areaHectareas = 24.5;
        database.configuracionDao().guardar(usuario, finca);
        database.close();
        database = Room.databaseBuilder(context, GanHardDatabase.class, ARCHIVO).build();
        ConfiguracionLocal recuperado = database.configuracionDao().cargar();
        assertNotNull(recuperado);
        assertEquals("La Esperanza", recuperado.finca.nombre);
        assertEquals(24.5, recuperado.finca.areaHectareas, 0.0001);
        recuperado.usuario.nombre = "Helver Rincón"; recuperado.finca.nombre = "La Esperanza actualizada";
        database.configuracionDao().guardar(recuperado.usuario, recuperado.finca);
        ConfiguracionLocal editado = database.configuracionDao().cargar();
        assertEquals("usuario-prueba", editado.usuario.idUsuario);
        assertEquals("finca-prueba", editado.finca.idFinca);
        assertEquals("Helver Rincón", editado.usuario.nombre);
    }
    @Test public void rechazaFincaSinPropietarioYRevierteLaTransaccionCompleta() {
        Usuario usuario = new Usuario(); usuario.idUsuario = "usuario-prueba";
        Finca finca = new Finca(); finca.idFinca = "finca-prueba"; finca.idPropietario = "no-existe";
        try { database.configuracionDao().guardar(usuario, finca); fail("Debía rechazar la referencia inválida"); }
        catch (android.database.sqlite.SQLiteConstraintException esperado) { /* Integridad y atomicidad. */ }
        assertNull(database.configuracionDao().usuario(usuario.idUsuario));
        assertNull(database.configuracionDao().cargar());
    }
}
