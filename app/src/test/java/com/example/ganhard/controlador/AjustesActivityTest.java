package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.PreferenciaTema;
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
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class AjustesActivityTest {
    private GanHardDatabase db;
    private GanHardApplication app;
    private final Context contexto = RuntimeEnvironment.getApplication();

    @Before public void preparar() {
        app = (GanHardApplication) RuntimeEnvironment.getApplication();
        db = GanHardDatabase.constructor(app, null).allowMainThreadQueries().build();
        app.instalar(db, Runnable::run);
    }
    @After public void limpiar() {
        db.close();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    private void fincaConfigurada(String ubicacion) {
        Usuario usuario = new Usuario();
        usuario.idUsuario = "usuario-1";
        usuario.nombre = "Helver Rincón";
        usuario.correo = "helver@ejemplo.com";
        Finca finca = new Finca();
        finca.idFinca = "finca-1";
        finca.idPropietario = usuario.idUsuario;
        finca.nombre = "La Esperanza";
        finca.ubicacion = ubicacion;
        db.configuracionDao().guardar(usuario, finca);
    }
    private AjustesActivity abrir() {
        AjustesActivity pantalla = Robolectric.buildActivity(AjustesActivity.class).create().start().resume().get();
        shadowOf(Looper.getMainLooper()).idle();
        return pantalla;
    }
    private String texto(AjustesActivity pantalla, int id) { return ((TextView) pantalla.findViewById(id)).getText().toString(); }
    private String tituloDe(AjustesActivity pantalla, int idFila) {
        return ((TextView) pantalla.findViewById(idFila).findViewById(R.id.filaTitulo)).getText().toString();
    }
    private String subtituloDe(AjustesActivity pantalla, int idFila) {
        return ((TextView) pantalla.findViewById(idFila).findViewById(R.id.filaSubtitulo)).getText().toString();
    }

    @Test public void muestraElPerfilYLaFincaConSuUbicacion() {
        fincaConfigurada("San Gil");
        AjustesActivity pantalla = abrir();
        assertEquals("Helver Rincón", texto(pantalla, R.id.ajustesNombre));
        assertEquals("helver@ejemplo.com", texto(pantalla, R.id.ajustesCorreo));
        assertEquals(pantalla.getString(R.string.ajustes_rol), texto(pantalla, R.id.ajustesRol));
        assertEquals("La Esperanza · San Gil", subtituloDe(pantalla, R.id.filaDatosFinca));
    }

    @Test public void sinUbicacionElSubtituloDeLaFincaEsSoloElNombre() {
        fincaConfigurada(null);
        assertEquals("La Esperanza", subtituloDe(abrir(), R.id.filaDatosFinca));
    }

    @Test public void sinConfiguracionAbreConfiguracion() {
        AjustesActivity pantalla = abrir();
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertNotNull(siguiente);
        assertEquals(ConfiguracionActivity.class.getName(), siguiente.getComponent().getClassName());
        assertTrue(pantalla.isFinishing());
    }

    @Test public void datosDeLaFincaAbreElFormulario() {
        fincaConfigurada("San Gil");
        AjustesActivity pantalla = abrir();
        pantalla.findViewById(R.id.filaDatosFinca).performClick();
        assertEquals(ConfiguracionActivity.class.getName(), shadowOf(pantalla).getNextStartedActivity().getComponent().getClassName());
    }

    @Test public void sinPreferenciaSeSeleccionaSeguirElSistema() {
        fincaConfigurada("San Gil");
        AjustesActivity pantalla = abrir();
        assertEquals("● " + pantalla.getString(R.string.tema_sistema), tituloDe(pantalla, R.id.filaTemaSistema));
        assertEquals(pantalla.getString(R.string.tema_seleccionado), subtituloDe(pantalla, R.id.filaTemaSistema));
        assertEquals("○ " + pantalla.getString(R.string.tema_claro), tituloDe(pantalla, R.id.filaTemaClaro));
        assertEquals(pantalla.getString(R.string.tema_ejemplo_claro), subtituloDe(pantalla, R.id.filaTemaClaro));
        assertEquals(pantalla.getString(R.string.tema_ejemplo_oscuro), subtituloDe(pantalla, R.id.filaTemaOscuro));
    }

    @Test public void laSeleccionInicialReflejaLoGuardado() {
        fincaConfigurada("San Gil");
        PreferenciaTema.guardar(contexto, AppCompatDelegate.MODE_NIGHT_NO);
        AjustesActivity pantalla = abrir();
        assertEquals("● " + pantalla.getString(R.string.tema_claro), tituloDe(pantalla, R.id.filaTemaClaro));
        assertEquals(pantalla.getString(R.string.tema_seleccionado), subtituloDe(pantalla, R.id.filaTemaClaro));
        assertEquals("○ " + pantalla.getString(R.string.tema_sistema), tituloDe(pantalla, R.id.filaTemaSistema));
    }

    @Test public void elegirOscuroPersisteAplicaYMarcaLaFila() {
        fincaConfigurada("San Gil");
        AjustesActivity pantalla = abrir();
        pantalla.findViewById(R.id.filaTemaOscuro).performClick();
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, PreferenciaTema.leer(contexto));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode());
        assertEquals("● " + pantalla.getString(R.string.tema_oscuro), tituloDe(pantalla, R.id.filaTemaOscuro));
        assertEquals(pantalla.getString(R.string.tema_seleccionado), subtituloDe(pantalla, R.id.filaTemaOscuro));
        assertEquals("○ " + pantalla.getString(R.string.tema_sistema), tituloDe(pantalla, R.id.filaTemaSistema));
    }

    @Test public void volverAElegirClaroLoPersiste() {
        fincaConfigurada("San Gil");
        AjustesActivity pantalla = abrir();
        pantalla.findViewById(R.id.filaTemaOscuro).performClick();
        pantalla.findViewById(R.id.filaTemaClaro).performClick();
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, PreferenciaTema.leer(contexto));
    }

    @Test public void elFormularioDeLaFincaYaNoTieneElSelectorDeTema() {
        fincaConfigurada("San Gil");
        ConfiguracionActivity formulario = Robolectric.buildActivity(ConfiguracionActivity.class).create().start().resume().get();
        assertEquals(0, formulario.getResources().getIdentifier("temas", "id", formulario.getPackageName()));
        assertEquals(0, formulario.getResources().getIdentifier("temaOscuro", "id", formulario.getPackageName()));
    }
}
