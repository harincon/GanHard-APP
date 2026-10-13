package com.example.ganhard.controlador;

import android.os.Bundle;
import android.os.Looper;
import android.widget.EditText;
import androidx.room.Room;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class ConfiguracionRotacionTest {
    @Test public void rotarDuranteCargaRecuperaDatosYLuegoConservaElFormularioEditado() {
        GanHardDatabase db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GanHardDatabase.class)
                .allowMainThreadQueries().build();
        ((GanHardApplication) RuntimeEnvironment.getApplication()).instalar(db, Runnable::run);
        try {
            Usuario usuario = new Usuario(); usuario.idUsuario = "usuario-prueba"; usuario.nombre = "Helver"; usuario.correo = "helver@ejemplo.com";
            Finca finca = new Finca(); finca.idFinca = "finca-prueba"; finca.idPropietario = usuario.idUsuario; finca.nombre = "La Esperanza";
            db.configuracionDao().guardar(usuario, finca);
            ActivityController<ConfiguracionActivity> inicial = Robolectric.buildActivity(ConfiguracionActivity.class).create().start().resume();
            Bundle duranteCarga = new Bundle();
            inicial.pause().saveInstanceState(duranteCarga).stop().destroy();
            ActivityController<ConfiguracionActivity> recreada = Robolectric.buildActivity(ConfiguracionActivity.class).create(duranteCarga).start().resume();
            esperarCarga(recreada.get());
            assertEquals("La Esperanza", texto(recreada.get(), R.id.nombreFinca));
            assertEquals("helver@ejemplo.com", texto(recreada.get(), R.id.correoAdministrador));
            ((EditText) recreada.get().findViewById(R.id.nombreFinca)).setText("Nombre todavía sin guardar");
            Bundle editando = new Bundle();
            recreada.pause().saveInstanceState(editando).stop().destroy();
            ActivityController<ConfiguracionActivity> tercera = Robolectric.buildActivity(ConfiguracionActivity.class).create(editando).start().resume();
            esperarCarga(tercera.get());
            assertEquals("Nombre todavía sin guardar", texto(tercera.get(), R.id.nombreFinca));
            tercera.get().findViewById(R.id.guardar).performClick();
            Bundle duranteGuardado = new Bundle();
            tercera.pause().saveInstanceState(duranteGuardado).stop().destroy();
            ActivityController<ConfiguracionActivity> cuarta = Robolectric.buildActivity(ConfiguracionActivity.class).create(duranteGuardado).start().resume();
            esperarCarga(cuarta.get());
            assertEquals("Nombre todavía sin guardar", texto(cuarta.get(), R.id.nombreFinca));
            assertNull("La pantalla destruida no debe navegar", shadowOf(tercera.get()).getNextStartedActivity());
            assertEquals("finca-prueba", db.configuracionDao().cargar().finca.idFinca);
            assertEquals("usuario-prueba", db.configuracionDao().cargar().usuario.idUsuario);
            cuarta.get().findViewById(R.id.guardar).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue("Debe guardar y regresar al inicio", cuarta.get().isFinishing());
            assertEquals("Nombre todavía sin guardar", db.configuracionDao().cargar().finca.nombre);
            assertEquals("finca-prueba", db.configuracionDao().cargar().finca.idFinca);
            cuarta.pause().stop().destroy();
        } finally { db.close(); }
    }
    private String texto(ConfiguracionActivity pantalla, int id) {
        return ((EditText) pantalla.findViewById(id)).getText().toString();
    }
    private void esperarCarga(ConfiguracionActivity pantalla) {
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue("La carga debe finalizar", pantalla.findViewById(R.id.guardar).isEnabled());
    }
}
