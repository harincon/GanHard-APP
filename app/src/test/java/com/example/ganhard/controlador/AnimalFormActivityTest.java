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
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
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
import com.google.android.material.appbar.MaterialToolbar;
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
    private static final String ANGUS = "4fda5aab-035d-4f2e-b1e1-c8159de431d4";
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
    private ActivityController<AnimalFormActivity> abrirEditando(String idAnimal) {
        Intent intento = new Intent().putExtra(AnimalFormActivity.EXTRA_ID_ANIMAL, idAnimal);
        ActivityController<AnimalFormActivity> controlador = Robolectric.buildActivity(AnimalFormActivity.class, intento).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private Animal registrarCompleto(String codigo, String nombre) {
        Captura<Animal> captura = new Captura<>();
        app.animales().registrar(new EntradaAnimal(codigo, nombre, "H", ANGUS, "2025-03-01", "2026-01-10", "Compra reciente"), captura);
        return captura.esperar().valor;
    }
    private void escribir(Activity pantalla, int id, String texto) { ((EditText) pantalla.findViewById(id)).setText(texto); }
    private String texto(Activity pantalla, int id) { return ((TextView) pantalla.findViewById(id)).getText().toString(); }
    private String error(Activity pantalla, int idCampo) {
        CharSequence error = ((TextInputLayout) pantalla.findViewById(idCampo)).getError();
        return error == null ? null : error.toString();
    }
    private void elegirSexo(Activity pantalla, String sexo) { ((AutoCompleteTextView) pantalla.findViewById(R.id.sexo)).setText(sexo, false); }
    private void elegirEstado(Activity pantalla, String estado) { ((AutoCompleteTextView) pantalla.findViewById(R.id.estado)).setText(estado, false); }
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
    private String hoyEnPantalla() { return FormatoFecha.aPantalla(Tiempo.hoy().toString()); }

    @Test public void sinFincaConfiguradaAbreConfiguracion() {
        AnimalFormActivity pantalla = abrir().get();
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertNotNull(siguiente);
        assertEquals(ConfiguracionActivity.class.getName(), siguiente.getComponent().getClassName());
        assertTrue(pantalla.isFinishing());
    }

    @Test public void muestraLaNotaDeCamposObligatoriosYLaAyudaDelCodigo() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        assertEquals(pantalla.getString(R.string.campos_obligatorios), texto(pantalla, R.id.notaObligatorios));
        assertEquals(pantalla.getString(R.string.ayuda_codigo), String.valueOf(((TextInputLayout) pantalla.findViewById(R.id.campoCodigo)).getHelperText()));
    }

    @Test public void laFechaDeIngresoComienzaEnHoyConFormatoDiaMesAnio() {
        fincaConfigurada();
        String hoy = texto(abrir().get(), R.id.ingreso);
        assertEquals(hoyEnPantalla(), hoy);
        assertTrue(hoy.matches("^\\d{2}/\\d{2}/\\d{4}$"));
    }

    @Test public void enRegistroNoSeVenEstadoNiFechaDelEstado() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        assertEquals(View.GONE, pantalla.findViewById(R.id.campoEstado).getVisibility());
        assertEquals(View.GONE, pantalla.findViewById(R.id.campoFechaEstado).getVisibility());
    }

    @Test public void codigoVacioMuestraErrorYNoGuarda() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        elegirSexo(pantalla, "Hembra");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_codigo_obligatorio), error(pantalla, R.id.campoCodigo));
        assertEquals(0, contarAnimales());
        assertFalse(pantalla.isFinishing());
    }

    @Test public void sinSexoMuestraElErrorDelSexo() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_sexo_obligatorio), error(pantalla, R.id.campoSexo));
        assertEquals(0, contarAnimales());
    }

    @Test public void elSexoSeEligeEnUnaListaYSeGuarda() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        AutoCompleteTextView lista = pantalla.findViewById(R.id.sexo);
        assertEquals(2, lista.getAdapter().getCount());
        escribir(pantalla, R.id.codigo, "GH-001");
        elegirSexo(pantalla, "Macho");
        guardar(pantalla);
        try (Cursor c = db.getOpenHelper().getWritableDatabase().query("SELECT sexo FROM animal")) {
            assertTrue(c.moveToFirst());
            assertEquals("M", c.getString(0));
        }
    }

    @Test public void guardarValidoMuestraConfirmacionYVuelve() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        escribir(pantalla, R.id.nombre, "Lucero");
        elegirSexo(pantalla, "Hembra");
        guardar(pantalla);
        assertTrue(pantalla.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowOf(pantalla).getResultCode());
        assertTrue(ShadowToast.getTextOfLatestToast().contains("GH-001"));
        assertEquals(1, contarAnimales());
    }

    @Test public void unaFechaEscritaDdMmAaaaSeGuardaEnIso() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        elegirSexo(pantalla, "Hembra");
        escribir(pantalla, R.id.nacimiento, "05/03/2025");
        escribir(pantalla, R.id.ingreso, "5/1/2026");
        guardar(pantalla);
        assertTrue(pantalla.isFinishing());
        try (Cursor c = db.getOpenHelper().getWritableDatabase().query("SELECT fecha_nacimiento, fecha_ingreso FROM animal")) {
            assertTrue(c.moveToFirst());
            assertEquals("2025-03-05", c.getString(0));
            assertEquals("2026-01-05", c.getString(1));
        }
    }

    @Test public void fechasConFormatoIncorrectoMuestranElErrorDeFormatoYConservanLoEscrito() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        elegirSexo(pantalla, "Hembra");
        escribir(pantalla, R.id.nacimiento, "5-3-2025");
        escribir(pantalla, R.id.ingreso, "31/02/2026");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_fecha_formato), error(pantalla, R.id.campoNacimiento));
        assertEquals(pantalla.getString(R.string.error_fecha_formato), error(pantalla, R.id.campoIngreso));
        assertEquals("5-3-2025", texto(pantalla, R.id.nacimiento));
        assertEquals("31/02/2026", texto(pantalla, R.id.ingreso));
        assertEquals(0, contarAnimales());
    }

    @Test public void codigoDuplicadoMuestraErrorEnElCampoYConservaLoEscrito() {
        fincaConfigurada();
        registrarCompleto("GH-001", "Uno");
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, " gh-001 ");
        escribir(pantalla, R.id.nombre, "Otro");
        escribir(pantalla, R.id.observaciones, "Compra reciente");
        elegirSexo(pantalla, "Hembra");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_codigo_duplicado), error(pantalla, R.id.campoCodigo));
        assertEquals("Otro", texto(pantalla, R.id.nombre));
        assertEquals("Compra reciente", texto(pantalla, R.id.observaciones));
        assertFalse(pantalla.isFinishing());
        assertEquals(1, contarAnimales());
    }

    @Test public void fechaDeIngresoFuturaMuestraElErrorDeLaFecha() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        elegirSexo(pantalla, "Hembra");
        escribir(pantalla, R.id.ingreso, FormatoFecha.aPantalla(Tiempo.hoy().plusDays(1).toString()));
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_fecha_futura), error(pantalla, R.id.campoIngreso));
        assertEquals(0, contarAnimales());
    }

    @Test public void elDesplegableDeRazaOfreceLasRazasDelCatalogo() {
        fincaConfigurada();
        AutoCompleteTextView raza = abrir().get().findViewById(R.id.raza);
        assertEquals(CatalogoRazas.RAZAS.length + 1, raza.getAdapter().getCount()); // más "Sin raza"
    }

    @Test public void laRazaElegidaSeGuardaEnElAnimal() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.codigo, "GH-001");
        elegirSexo(pantalla, "Hembra");
        AutoCompleteTextView raza = pantalla.findViewById(R.id.raza);
        raza.getOnItemClickListener().onItemClick(null, null, 1, 0); // primera raza por orden alfabético: Angus
        guardar(pantalla);
        try (Cursor c = db.getOpenHelper().getWritableDatabase().query("SELECT id_raza FROM animal")) {
            assertTrue(c.moveToFirst());
            assertEquals(ANGUS, c.getString(0));
        }
    }

    @Test public void rotarConservaElFormularioYNoDuplicaElRegistro() {
        fincaConfigurada();
        ActivityController<AnimalFormActivity> primera = abrir();
        escribir(primera.get(), R.id.codigo, "GH-001");
        escribir(primera.get(), R.id.nombre, "Lucero");
        elegirSexo(primera.get(), "Hembra");
        primera.get().findViewById(R.id.guardar).performClick(); // el resultado llega a una pantalla ya destruida
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        ActivityController<AnimalFormActivity> segunda = Robolectric.buildActivity(AnimalFormActivity.class)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("GH-001", texto(segunda.get(), R.id.codigo));
        assertEquals("Lucero", texto(segunda.get(), R.id.nombre));
        assertEquals("Hembra", texto(segunda.get(), R.id.sexo));
        assertFalse(segunda.get().isFinishing());
        assertNull("La pantalla destruida no debe navegar", shadowOf(primera.get()).getNextStartedActivity());
        assertEquals(1, contarAnimales());
    }

    @Test public void rotarYVolverAGuardarNoDuplicaNiMuestraElErrorDeCodigo() {
        fincaConfigurada();
        ActivityController<AnimalFormActivity> primera = abrir();
        escribir(primera.get(), R.id.codigo, "GH-001");
        elegirSexo(primera.get(), "Hembra");
        primera.get().findViewById(R.id.guardar).performClick();
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        ActivityController<AnimalFormActivity> segunda = Robolectric.buildActivity(AnimalFormActivity.class)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        guardar(segunda.get());
        assertEquals(1, contarAnimales());
        assertNull(error(segunda.get(), R.id.campoCodigo));
        assertTrue(segunda.get().isFinishing());
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

    // ----- Edición -----

    @Test public void enModoEdicionPrecargaLosDatosYMuestraEstadoYFechaDelEstado() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity pantalla = abrirEditando(animal.idAnimal).get();
        assertEquals("GH-001", texto(pantalla, R.id.codigo));
        assertEquals("Lucero", texto(pantalla, R.id.nombre));
        assertEquals("Hembra", texto(pantalla, R.id.sexo));
        assertEquals("Angus", texto(pantalla, R.id.raza));
        assertEquals("01/03/2025", texto(pantalla, R.id.nacimiento));
        assertEquals("10/01/2026", texto(pantalla, R.id.ingreso));
        assertEquals("Compra reciente", texto(pantalla, R.id.observaciones));
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.campoEstado).getVisibility());
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.campoFechaEstado).getVisibility());
        assertEquals("Activo", texto(pantalla, R.id.estado));
        assertEquals("10/01/2026", texto(pantalla, R.id.fechaEstado));
        assertEquals(pantalla.getString(R.string.editar_animal), String.valueOf(((MaterialToolbar) pantalla.findViewById(R.id.toolbar)).getTitle()));
        assertEquals(pantalla.getString(R.string.guardar_cambios), texto(pantalla, R.id.guardar));
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
        assertEquals(ANGUS, guardado.idRaza);
        assertEquals(Animal.ACTIVO, guardado.estado);
        assertTrue(ShadowToast.getTextOfLatestToast().contains("GH-001"));
    }

    @Test public void enEdicionCambiarAVendidoGuardaEstadoYFecha() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity pantalla = abrirEditando(animal.idAnimal).get();
        elegirEstado(pantalla, "Vendido");
        escribir(pantalla, R.id.fechaEstado, "01/06/2026");
        guardar(pantalla);
        assertTrue(pantalla.isFinishing());
        Animal guardado = db.animalDao().porId(animal.idAnimal);
        assertEquals(Animal.VENDIDO, guardado.estado);
        assertEquals("2026-06-01", guardado.fechaEstado);
    }

    @Test public void enEdicionVendidoConFechaAnteriorAlIngresoMuestraElError() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity pantalla = abrirEditando(animal.idAnimal).get();
        elegirEstado(pantalla, "Vendido");
        escribir(pantalla, R.id.fechaEstado, "09/01/2026");
        guardar(pantalla);
        assertFalse(pantalla.isFinishing());
        assertEquals(pantalla.getString(R.string.error_estado_antes_ingreso), error(pantalla, R.id.campoFechaEstado));
        assertEquals(Animal.ACTIVO, db.animalDao().porId(animal.idAnimal).estado);
    }

    @Test public void enEdicionUnaFechaDeEstadoFuturaMuestraElError() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity pantalla = abrirEditando(animal.idAnimal).get();
        elegirEstado(pantalla, "Fallecido");
        escribir(pantalla, R.id.fechaEstado, FormatoFecha.aPantalla(Tiempo.hoy().plusDays(1).toString()));
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_fecha_futura), error(pantalla, R.id.campoFechaEstado));
        assertEquals(Animal.ACTIVO, db.animalDao().porId(animal.idAnimal).estado);
    }

    @Test public void editarAUnCodigoExistenteMuestraDuplicadoYConservaLoEscrito() {
        fincaConfigurada();
        registrarCompleto("GH-001", "Uno");
        Animal segundo = registrarCompleto("GH-002", "Dos");
        AnimalFormActivity pantalla = abrirEditando(segundo.idAnimal).get();
        escribir(pantalla, R.id.codigo, "gh-001");
        escribir(pantalla, R.id.nombre, "Dos editado");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_codigo_duplicado), error(pantalla, R.id.campoCodigo));
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

    @Test public void enEdicionCancelarSinTocarNadaNoPreguntaYConCambiosSi() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        AnimalFormActivity sinCambios = abrirEditando(animal.idAnimal).get();
        sinCambios.findViewById(R.id.cancelar).performClick();
        assertTrue(sinCambios.isFinishing());
        AnimalFormActivity conCambios = abrirEditando(animal.idAnimal).get();
        escribir(conCambios, R.id.nombre, "Otro nombre");
        conCambios.findViewById(R.id.cancelar).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(ShadowDialog.getLatestDialog().isShowing());
        assertFalse(conCambios.isFinishing());
    }

    @Test public void rotarAntesDeQueCargueElAnimalDeEdicionAunLoRellena() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        Intent intento = new Intent().putExtra(AnimalFormActivity.EXTRA_ID_ANIMAL, animal.idAnimal);
        ActivityController<AnimalFormActivity> primera = Robolectric.buildActivity(AnimalFormActivity.class, intento).create().start().resume();
        Bundle estado = new Bundle(); // la carga del animal todavía no ha llegado
        primera.pause().saveInstanceState(estado).stop().destroy();
        ActivityController<AnimalFormActivity> segunda = Robolectric.buildActivity(AnimalFormActivity.class, intento)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("GH-001", texto(segunda.get(), R.id.codigo));
        assertEquals("Lucero", texto(segunda.get(), R.id.nombre));
        assertEquals("Compra reciente", texto(segunda.get(), R.id.observaciones));
        assertEquals("Angus", texto(segunda.get(), R.id.raza));
        assertEquals("Hembra", texto(segunda.get(), R.id.sexo));
    }

    @Test public void rotarDespuesDeEditarConservaLoEditado() {
        fincaConfigurada();
        Animal animal = registrarCompleto("GH-001", "Lucero");
        ActivityController<AnimalFormActivity> primera = abrirEditando(animal.idAnimal);
        escribir(primera.get(), R.id.nombre, "Cambiado");
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        Intent intento = new Intent().putExtra(AnimalFormActivity.EXTRA_ID_ANIMAL, animal.idAnimal);
        ActivityController<AnimalFormActivity> segunda = Robolectric.buildActivity(AnimalFormActivity.class, intento)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Cambiado", texto(segunda.get(), R.id.nombre));
    }

    @Test public void escribirSoloDigitosInsertaLasBarrasDeLaFecha() {
        fincaConfigurada();
        AnimalFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.nacimiento, "05032025");
        assertEquals("05/03/2025", texto(pantalla, R.id.nacimiento));
        escribir(pantalla, R.id.nacimiento, "5/3/2025");
        assertEquals("5/3/2025", texto(pantalla, R.id.nacimiento));
    }
}
