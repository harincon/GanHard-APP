package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.pruebas.Captura;
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
public class InicioActivityTest {
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
        finca.ubicacion = "Vereda El Rosal";
        db.configuracionDao().guardar(usuario, finca);
    }
    private Animal registrar(String codigo) {
        Captura<Animal> captura = new Captura<>();
        app.animales().registrar(new EntradaAnimal(codigo, "", "H", "", "", "2026-01-10", ""), captura);
        captura.esperar();
        return captura.valor;
    }
    private void marcar(Animal animal, String estado) {
        Animal guardado = db.animalDao().porId(animal.idAnimal);
        guardado.estado = estado;
        db.animalDao().actualizar(guardado);
    }
    private ActivityController<InicioActivity> abrir() {
        ActivityController<InicioActivity> controlador = Robolectric.buildActivity(InicioActivity.class).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private String texto(InicioActivity pantalla, int id) { return ((TextView) pantalla.findViewById(id)).getText().toString(); }
    private String activos(InicioActivity pantalla, int cantidad) {
        return pantalla.getResources().getQuantityString(R.plurals.animales_activos, cantidad, cantidad);
    }
    private String claseSiguiente(InicioActivity pantalla) {
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertNotNull(siguiente);
        return siguiente.getComponent().getClassName();
    }

    @Test public void sinConfiguracionAbreConfiguracion() {
        InicioActivity pantalla = abrir().get();
        assertEquals(ConfiguracionActivity.class.getName(), claseSiguiente(pantalla));
        assertTrue(pantalla.isFinishing());
    }

    @Test public void muestraLaFincaElAdministradorYElConteoReal() {
        fincaConfigurada();
        registrar("GH-001");
        registrar("GH-002");
        marcar(registrar("GH-003"), Animal.VENDIDO);
        registrar("GH-004");
        InicioActivity pantalla = abrir().get();
        assertEquals("La Esperanza", texto(pantalla, R.id.nombreFinca));
        assertEquals("Helver · Administrador", texto(pantalla, R.id.administrador));
        assertEquals(activos(pantalla, 3), texto(pantalla, R.id.inventarioActivos));
        assertEquals(View.GONE, pantalla.findViewById(R.id.estadoError).getVisibility());
    }

    @Test public void sinAnimalesElConteoEsCero() {
        fincaConfigurada();
        assertEquals(activos(abrir().get(), 0), texto(abrir().get(), R.id.inventarioActivos));
    }

    @Test public void conUnSoloAnimalUsaElSingular() {
        fincaConfigurada();
        registrar("GH-001");
        InicioActivity pantalla = abrir().get();
        assertEquals("1 animal activo", texto(pantalla, R.id.inventarioActivos));
    }

    @Test public void altaYCambioDeEstadoActualizanAlVolver() {
        fincaConfigurada();
        Animal primero = registrar("GH-001");
        ActivityController<InicioActivity> controlador = abrir();
        assertEquals(activos(controlador.get(), 1), texto(controlador.get(), R.id.inventarioActivos));
        registrar("GH-002");
        controlador.pause().resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(activos(controlador.get(), 2), texto(controlador.get(), R.id.inventarioActivos));
        marcar(primero, Animal.FALLECIDO);
        controlador.pause().resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(activos(controlador.get(), 1), texto(controlador.get(), R.id.inventarioActivos));
    }

    @Test public void errorDeCargaMuestraReintentar() {
        fincaConfigurada();
        db.close();
        InicioActivity pantalla = abrir().get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoError).getVisibility());
        assertEquals(pantalla.getString(R.string.error_carga), texto(pantalla, R.id.mensajeError));
    }

    @Test public void soloLosModulosImplementadosEstanHabilitados() {
        fincaConfigurada();
        InicioActivity pantalla = abrir().get();
        assertTrue(pantalla.findViewById(R.id.moduloAnimales).isEnabled());
        assertTrue(pantalla.findViewById(R.id.moduloConfiguracion).isEnabled());
        for (int id : new int[]{R.id.moduloSanidad, R.id.moduloTareas, R.id.moduloGastos}) {
            assertFalse(pantalla.findViewById(id).isEnabled());
            assertTrue(texto(pantalla, id).contains(pantalla.getString(R.string.proximamente)));
        }
    }

    @Test public void losAccesosAbrenSusPantallas() {
        fincaConfigurada();
        InicioActivity pantalla = abrir().get();
        pantalla.findViewById(R.id.registrarAnimal).performClick();
        assertEquals(AnimalFormActivity.class.getName(), claseSiguiente(pantalla));
        pantalla.findViewById(R.id.moduloAnimales).performClick();
        assertEquals(AnimalesActivity.class.getName(), claseSiguiente(pantalla));
        pantalla.findViewById(R.id.moduloConfiguracion).performClick();
        assertEquals(ConfiguracionActivity.class.getName(), claseSiguiente(pantalla));
    }
}
