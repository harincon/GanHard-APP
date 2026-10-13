package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Looper;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.CatalogoRazas;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.pruebas.Captura;
import com.google.android.material.textfield.TextInputLayout;
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
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class AnimalFormActivityTest {
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
    private ActivityController<AnimalFormActivity> abrir() {
        ActivityController<AnimalFormActivity> controlador = Robolectric.buildActivity(AnimalFormActivity.class).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private void escribir(Activity pantalla, int id, String texto) { ((EditText) pantalla.findViewById(id)).setText(texto); }
    private String texto(Activity pantalla, int id) { return ((EditText) pantalla.findViewById(id)).getText().toString(); }
    private CharSequence error(Activity pantalla, int idCampo) { return ((TextInputLayout) pantalla.findViewById(idCampo)).getError(); }
    private void elegirHembra(Activity pantalla) { ((RadioButton) pantalla.findViewById(R.id.sexoHembra)).setChecked(true); }
    private void guardar(Activity pantalla) {
        pantalla.findViewById(R.id.guardar).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }
    private int contarAnimales() {
        try (Cursor c = db.getOpenHelper().getWritableDatabase().query("SELECT COUNT(*) FROM animal")) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    @Test public void sinFincaConfiguradaAbreConfiguracion() {
        AnimalFormActivity pantalla = abrir().get();
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertNotNull(siguiente);
        assertEquals(ConfiguracionActivity.class.getName(), siguiente.getComponent().getClassName());
        assertTrue(pantalla.isFinishing());
    }

    @Test public void elIngresoComienzaEnLaFechaDeHoy() {
        fincaConfigurada();
        assertEquals(Tiempo.hoy().toString(), texto(abrir().get(), R.id.ingreso));
    }

    @Test public void codigoVacioMuestraErrorYNoGuarda() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        elegirHembra(pantalla);
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_codigo_obligatorio), String.valueOf(error(pantalla, R.id.campoCodigo)));
        assertEquals(0, contarAnimales());
        assertFalse(pantalla.isFinishing());
    }

    @Test public void sinSexoMuestraElErrorDelSexo() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        guardar(pantalla);
        assertEquals(android.view.View.VISIBLE, pantalla.findViewById(R.id.errorSexo).getVisibility());
        assertEquals(0, contarAnimales());
    }

    @Test public void guardarValidoMuestraConfirmacionYVuelve() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        escribir(pantalla, R.id.nombre, "Lucero");
        elegirHembra(pantalla);
        guardar(pantalla);
        assertTrue(pantalla.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowOf(pantalla).getResultCode());
        assertTrue(ShadowToast.getTextOfLatestToast().contains("GH-001"));
        assertEquals(1, contarAnimales());
    }

    @Test public void codigoDuplicadoMuestraErrorEnElCampoYConservaLoEscrito() {
        fincaConfigurada();
        Captura<Animal> previo = new Captura<>();
        app.animales().registrar(new EntradaAnimal("GH-001", "Uno", "H", "", "", "2026-01-10", ""), previo);
        previo.esperar();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, " gh-001 ");
        escribir(pantalla, R.id.nombre, "Otro");
        escribir(pantalla, R.id.observaciones, "Compra reciente");
        elegirHembra(pantalla);
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_codigo_duplicado), String.valueOf(error(pantalla, R.id.campoCodigo)));
        assertEquals("Otro", texto(pantalla, R.id.nombre));
        assertEquals("Compra reciente", texto(pantalla, R.id.observaciones));
        assertFalse(pantalla.isFinishing());
        assertEquals(1, contarAnimales());
    }

    @Test public void fechaDeIngresoFuturaMuestraElErrorDeLaFecha() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        elegirHembra(pantalla);
        escribir(pantalla, R.id.ingreso, Tiempo.hoy().plusDays(1).toString());
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_fecha_futura), String.valueOf(error(pantalla, R.id.campoIngreso)));
        assertEquals(0, contarAnimales());
    }

    @Test public void elDesplegableOfreceLasRazasDelCatalogo() {
        fincaConfigurada();
        AutoCompleteTextView raza = abrir().get().findViewById(R.id.raza);
        assertEquals(CatalogoRazas.RAZAS.length + 1, raza.getAdapter().getCount()); // más "Sin raza"
    }

    @Test public void laRazaElegidaSeGuardaEnElAnimal() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        elegirHembra(pantalla);
        AutoCompleteTextView raza = pantalla.findViewById(R.id.raza);
        raza.getOnItemClickListener().onItemClick(null, null, 1, 0); // primera raza por orden alfabético: Angus
        guardar(pantalla);
        try (Cursor c = db.getOpenHelper().getWritableDatabase().query("SELECT id_raza FROM animal")) {
            assertTrue(c.moveToFirst());
            assertEquals("4fda5aab-035d-4f2e-b1e1-c8159de431d4", c.getString(0));
        }
    }

    @Test public void rotarConservaElFormularioYNoDuplicaElRegistro() {
        fincaConfigurada();
        ActivityController<AnimalFormActivity> primera = abrir();
        escribir(primera.get(), R.id.codigo, "GH-001");
        escribir(primera.get(), R.id.nombre, "Lucero");
        elegirHembra(primera.get());
        primera.get().findViewById(R.id.guardar).performClick(); // el resultado llega a una pantalla ya destruida
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        ActivityController<AnimalFormActivity> segunda = Robolectric.buildActivity(AnimalFormActivity.class)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("GH-001", texto(segunda.get(), R.id.codigo));
        assertEquals("Lucero", texto(segunda.get(), R.id.nombre));
        assertTrue(((RadioButton) segunda.get().findViewById(R.id.sexoHembra)).isChecked());
        assertFalse(segunda.get().isFinishing());
        assertNull("La pantalla destruida no debe navegar", shadowOf(primera.get()).getNextStartedActivity());
        assertEquals(1, contarAnimales());
    }

    @Test public void cancelarSinCambiosCierraSinPreguntar() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        pantalla.findViewById(R.id.cancelar).performClick();
        assertTrue(pantalla.isFinishing());
    }

    @Test public void cancelarConCambiosPideConfirmar() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        pantalla.findViewById(R.id.cancelar).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(ShadowDialog.getLatestDialog());
        assertTrue(ShadowDialog.getLatestDialog().isShowing());
        assertFalse(pantalla.isFinishing());
        assertEquals(0, contarAnimales());
    }

    private ActivityController<AnimalFormActivity> abrirEditando(String idAnimal) {
        Intent intento = new Intent().putExtra(AnimalFormActivity.EXTRA_ID_ANIMAL, idAnimal);
        ActivityController<AnimalFormActivity> controlador = Robolectric.buildActivity(AnimalFormActivity.class, intento).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private Animal registrarCompleto(String codigo, String nombre) {
        Captura<Animal> captura = new Captura<>();
        app.animales().registrar(new EntradaAnimal(codigo, nombre, "H", "4fda5aab-035d-4f2e-b1e1-c8159de431d4", "2025-03-01", "2026-01-10", "Compra reciente"), captura);
        return captura.esperar().valor;
    }

    @Test public void enModoEdicionPrecargaLosDatosDelAnimal() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity pantalla = abrirEditando(animal.idAnimal).get();
        assertEquals("GH-001", texto(pantalla, R.id.codigo));
        assertEquals("Lucero", texto(pantalla, R.id.nombre));
        assertTrue(((RadioButton) pantalla.findViewById(R.id.sexoHembra)).isChecked());
        assertEquals("Angus", ((AutoCompleteTextView) pantalla.findViewById(R.id.raza)).getText().toString());
        assertEquals("2025-03-01", texto(pantalla, R.id.nacimiento));
        assertEquals("2026-01-10", texto(pantalla, R.id.ingreso));
        assertEquals("Compra reciente", texto(pantalla, R.id.observaciones));
        assertEquals(pantalla.getString(R.string.editar_animal), String.valueOf(((com.google.android.material.appbar.MaterialToolbar) pantalla.findViewById(R.id.toolbar)).getTitle()));
        assertEquals(pantalla.getString(R.string.guardar_cambios), ((android.widget.Button) pantalla.findViewById(R.id.guardar)).getText().toString());
    }

    @Test public void editarYGuardarActualizaElMismoRegistro() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity pantalla = abrirEditando(animal.idAnimal).get();
        escribir(pantalla, R.id.nombre, "Lucero editado");
        guardar(pantalla);
        assertTrue(pantalla.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowOf(pantalla).getResultCode());
        assertEquals(1, contarAnimales());
        Animal guardado = db.animalDao().porId(animal.idAnimal);
        assertEquals("Lucero editado", guardado.nombre);
        assertEquals("4fda5aab-035d-4f2e-b1e1-c8159de431d4", guardado.idRaza);
        assertTrue(ShadowToast.getTextOfLatestToast().contains("GH-001"));
    }

    @Test public void editarAUnCodigoExistenteMuestraDuplicadoYConservaLoEscrito() {
        fincaConfigurada();
        registrarCompleto("GH-001", "Uno");
        Animal segundo = registrarCompleto("GH-002", "Dos");
        AnimalFormActivity pantalla = abrirEditando(segundo.idAnimal).get();
        escribir(pantalla, R.id.codigo, "gh-001");
        escribir(pantalla, R.id.nombre, "Dos editado");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_codigo_duplicado), String.valueOf(error(pantalla, R.id.campoCodigo)));
        assertEquals("Dos editado", texto(pantalla, R.id.nombre));
        assertFalse(pantalla.isFinishing());
        assertEquals("GH-002", db.animalDao().porId(segundo.idAnimal).codigo);
    }

    @Test public void editarUnAnimalInexistenteSeCierraConMensaje() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrirEditando("no-existe").get();
        assertTrue(pantalla.isFinishing());
        assertEquals(pantalla.getString(R.string.animal_no_encontrado), ShadowToast.getTextOfLatestToast());
    }

    @Test public void enEdicionCancelarSinTocarNadaNoPregunta() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity pantalla = abrirEditando(animal.idAnimal).get();
        pantalla.findViewById(R.id.cancelar).performClick();
        assertTrue(pantalla.isFinishing());
    }

    @Test public void enEdicionCancelarConCambiosPideConfirmar() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity pantalla = abrirEditando(animal.idAnimal).get();
        escribir(pantalla, R.id.nombre, "Otro nombre");
        pantalla.findViewById(R.id.cancelar).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(ShadowDialog.getLatestDialog().isShowing());
        assertFalse(pantalla.isFinishing());
    }
}
