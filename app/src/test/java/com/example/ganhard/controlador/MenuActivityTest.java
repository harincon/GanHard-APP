package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class MenuActivityTest {
    private GanHardDatabase db;
    private GanHardApplication app;

    @Before public void preparar() {
        app = (GanHardApplication) RuntimeEnvironment.getApplication();
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
    private ActivityController<MenuActivity> abrir() {
        ActivityController<MenuActivity> controlador = Robolectric.buildActivity(MenuActivity.class).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private String claseSiguiente(MenuActivity pantalla) {
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertNotNull(siguiente);
        return siguiente.getComponent().getClassName();
    }
    private String texto(MenuActivity pantalla, int id) { return ((TextView) pantalla.findViewById(id)).getText().toString(); }
    private FilaNavegacion fila(MenuActivity pantalla, int id) { return pantalla.findViewById(id); }

    @Test public void sinConfiguracionAbreConfiguracion() {
        MenuActivity pantalla = abrir().get();
        assertEquals(ConfiguracionActivity.class.getName(), claseSiguiente(pantalla));
        assertTrue(pantalla.isFinishing());
    }

    @Test public void muestraLaFincaYElAdministrador() {
        fincaConfigurada();
        MenuActivity pantalla = abrir().get();
        assertEquals("La Esperanza", texto(pantalla, R.id.menuFinca));
        assertEquals("Helver · Administrador", texto(pantalla, R.id.menuAdministrador));
    }

    @Test public void muestraTodasLasFilasDelFigmaEnOrden() {
        fincaConfigurada();
        MenuActivity pantalla = abrir().get();
        int[] ids = {R.id.filaInicio, R.id.filaAnimales, R.id.filaSanidad, R.id.filaTareas, R.id.filaGastos,
                R.id.filaHistorial, R.id.filaSincronizacion, R.id.filaConfiguracion};
        String[] titulos = {"Inicio", "Animales", "Sanidad", "Tareas", "Gastos", "Historial de cambios", "Sincronización", "Configuración"};
        for (int i = 0; i < ids.length; i++) {
            assertEquals(titulos[i], ((TextView) fila(pantalla, ids[i]).findViewById(R.id.filaTitulo)).getText().toString());
        }
        assertTrue(fila(pantalla, R.id.filaInicio).getTop() <= fila(pantalla, R.id.filaConfiguracion).getTop());
    }

    @Test public void animalesYConfiguracionNavegan() {
        fincaConfigurada();
        MenuActivity pantalla = abrir().get();
        fila(pantalla, R.id.filaAnimales).performClick();
        assertEquals(AnimalesActivity.class.getName(), claseSiguiente(pantalla));
        fila(pantalla, R.id.filaConfiguracion).performClick();
        assertEquals(ConfiguracionActivity.class.getName(), claseSiguiente(pantalla));
    }

    @Test public void inicioVuelveAInicioYCierraElMenu() {
        fincaConfigurada();
        MenuActivity pantalla = abrir().get();
        fila(pantalla, R.id.filaInicio).performClick();
        assertEquals(InicioActivity.class.getName(), claseSiguiente(pantalla));
        assertTrue(pantalla.isFinishing());
    }

    @Test public void lasFilasPendientesEstanDeshabilitadasDicenProximamenteYNoNavegan() {
        fincaConfigurada();
        MenuActivity pantalla = abrir().get();
        for (int id : new int[]{R.id.filaSanidad, R.id.filaTareas, R.id.filaGastos, R.id.filaHistorial, R.id.filaSincronizacion}) {
            FilaNavegacion fila = fila(pantalla, id);
            assertFalse(fila.isEnabled());
            assertTrue(((TextView) fila.findViewById(R.id.filaSubtitulo)).getText().toString().contains(pantalla.getString(R.string.proximamente)));
            fila.performClick();
        }
        assertNull(shadowOf(pantalla).getNextStartedActivity());
        assertFalse(pantalla.isFinishing());
    }

    @Test public void lasFilasDisponiblesEstanHabilitadas() {
        fincaConfigurada();
        MenuActivity pantalla = abrir().get();
        for (int id : new int[]{R.id.filaInicio, R.id.filaAnimales, R.id.filaConfiguracion}) assertTrue(fila(pantalla, id).isEnabled());
    }

    @Test public void atrasCierraElMenu() {
        fincaConfigurada();
        MenuActivity pantalla = abrir().get();
        pantalla.getOnBackPressedDispatcher().onBackPressed();
        assertTrue(pantalla.isFinishing());
    }
}
