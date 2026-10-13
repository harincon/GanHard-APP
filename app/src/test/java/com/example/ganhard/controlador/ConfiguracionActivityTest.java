package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.widget.EditText;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import com.google.android.material.appbar.MaterialToolbar;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class ConfiguracionActivityTest {
    private GanHardDatabase db;

    @Before public void preparar() {
        GanHardApplication app = (GanHardApplication) RuntimeEnvironment.getApplication();
        db = GanHardDatabase.constructor(app, null).allowMainThreadQueries().build();
        app.instalar(db, Runnable::run);
    }
    @After public void limpiar() { db.close(); }

    private void fincaConfigurada() {
        Usuario usuario = new Usuario();
        usuario.idUsuario = "usuario-1";
        usuario.nombre = "Helver";
        usuario.correo = "h@ejemplo.com";
        Finca finca = new Finca();
        finca.idFinca = "finca-1";
        finca.idPropietario = usuario.idUsuario;
        finca.nombre = "La Esperanza";
        db.configuracionDao().guardar(usuario, finca);
    }
    private ConfiguracionActivity abrir() {
        ConfiguracionActivity pantalla = Robolectric.buildActivity(ConfiguracionActivity.class).create().start().resume().get();
        shadowOf(Looper.getMainLooper()).idle();
        return pantalla;
    }
    private void guardar(ConfiguracionActivity pantalla) {
        pantalla.findViewById(R.id.guardar).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    @Test public void guardarEnEdicionVuelveAlOrigenSinAbrirInicio() {
        fincaConfigurada();
        ConfiguracionActivity pantalla = abrir();
        ((EditText) pantalla.findViewById(R.id.nombreFinca)).setText("La Esperanza II");
        guardar(pantalla);
        assertTrue(pantalla.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowOf(pantalla).getResultCode());
        assertNull("No debe borrar la pila de Menú y Configuración", shadowOf(pantalla).getNextStartedActivity());
        assertEquals("La Esperanza II", db.configuracionDao().cargar().finca.nombre);
    }

    @Test public void laPrimeraConfiguracionAbreInicio() {
        ConfiguracionActivity pantalla = abrir();
        ((EditText) pantalla.findViewById(R.id.nombreAdministrador)).setText("Helver");
        ((EditText) pantalla.findViewById(R.id.correoAdministrador)).setText("h@ejemplo.com");
        ((EditText) pantalla.findViewById(R.id.nombreFinca)).setText("La Esperanza");
        guardar(pantalla);
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertNotNull(siguiente);
        assertEquals(InicioActivity.class.getName(), siguiente.getComponent().getClassName());
        assertTrue(pantalla.isFinishing());
    }

    @Test public void enEdicionElTituloEsDatosDeLaFinca() {
        fincaConfigurada();
        ConfiguracionActivity pantalla = abrir();
        assertEquals("Datos de la finca", String.valueOf(((MaterialToolbar) pantalla.findViewById(R.id.toolbar)).getTitle()));
    }
}
