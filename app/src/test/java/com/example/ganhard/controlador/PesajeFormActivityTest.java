package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Looper;
import android.widget.EditText;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.pruebas.Captura;
import com.google.android.material.textfield.TextInputLayout;
import java.time.LocalTime;
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
public class PesajeFormActivityTest {
    private GanHardDatabase db;
    private GanHardApplication app;
    private Animal animal;

    @Before public void preparar() {
        app = (GanHardApplication) RuntimeEnvironment.getApplication();
        db = GanHardDatabase.constructor(app, null).allowMainThreadQueries().build();
        app.instalar(db, Runnable::run);
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
        app.animales().registrar(new EntradaAnimal("GH-001", "Lucero", "H", "", "2025-03-01", "2026-01-10", ""), captura);
        animal = captura.esperar().valor;
    }
    @After public void limpiar() { db.close(); }

    private Intent intento(String idAnimal) {
        Intent intento = new Intent();
        if (idAnimal != null) intento.putExtra(PesajeFormActivity.EXTRA_ID_ANIMAL, idAnimal);
        return intento;
    }
    private ActivityController<PesajeFormActivity> abrir() {
        ActivityController<PesajeFormActivity> controlador = Robolectric.buildActivity(PesajeFormActivity.class, intento(animal.idAnimal))
                .create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private void escribir(Activity pantalla, int id, String texto) { ((EditText) pantalla.findViewById(id)).setText(texto); }
    private String texto(Activity pantalla, int id) { return ((EditText) pantalla.findViewById(id)).getText().toString(); }
    private String error(Activity pantalla, int idCampo) {
        CharSequence error = ((TextInputLayout) pantalla.findViewById(idCampo)).getError();
        return error == null ? null : error.toString();
    }
    private void guardar(Activity pantalla) {
        pantalla.findViewById(R.id.guardar).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }
    private Cursor consulta(String sql) { return db.getOpenHelper().getWritableDatabase().query(sql); }
    private int contar() {
        try (Cursor c = consulta("SELECT COUNT(*) FROM pesaje")) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    @Test public void unAnimalInexistenteOSinIdSeCierraConMensaje() {
        PesajeFormActivity sinId = Robolectric.buildActivity(PesajeFormActivity.class, intento(null)).create().start().resume().get();
        assertTrue(sinId.isFinishing());
        PesajeFormActivity ajeno = Robolectric.buildActivity(PesajeFormActivity.class, intento("no-existe")).create().start().resume().get();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(ajeno.isFinishing());
        assertEquals(ajeno.getString(R.string.animal_no_encontrado), ShadowToast.getTextOfLatestToast());
    }

    @Test public void fechaYHoraComienzanEnElMomentoActual() {
        PesajeFormActivity pantalla = abrir().get();
        assertEquals(Tiempo.hoy().toString(), texto(pantalla, R.id.fecha));
        assertTrue(texto(pantalla, R.id.hora).matches("^\\d{2}:\\d{2}$"));
    }

    @Test public void pesoVacioMuestraErrorYNoGuarda() {
        PesajeFormActivity pantalla = abrir().get();
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_peso_obligatorio), error(pantalla, R.id.campoPeso));
        assertEquals(0, contar());
        assertFalse(pantalla.isFinishing());
    }

    @Test public void pesoCeroMuestraErrorYConservaLoEscrito() {
        PesajeFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.peso, "0");
        escribir(pantalla, R.id.observaciones, "Báscula de la entrada");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_peso_invalido), error(pantalla, R.id.campoPeso));
        assertEquals("Báscula de la entrada", texto(pantalla, R.id.observaciones));
        assertEquals("0", texto(pantalla, R.id.peso));
        assertEquals(0, contar());
    }

    @Test public void masDeDosDecimalesYPesoExageradoMuestranSuError() {
        PesajeFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.peso, "450.555");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_peso_decimales), error(pantalla, R.id.campoPeso));
        escribir(pantalla, R.id.peso, "5000");
        guardar(pantalla);
        assertTrue(error(pantalla, R.id.campoPeso).contains("2000"));
    }

    @Test public void pesoConComaSeGuardaYVuelve() {
        PesajeFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.fecha, "2026-02-01");
        escribir(pantalla, R.id.hora, "09:15");
        escribir(pantalla, R.id.peso, "450,5");
        guardar(pantalla);
        assertTrue(pantalla.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowOf(pantalla).getResultCode());
        assertTrue(ShadowToast.getTextOfLatestToast().contains("450.5"));
        try (Cursor c = consulta("SELECT peso_kg, fecha, hora FROM pesaje")) {
            assertTrue(c.moveToFirst());
            assertEquals(450.5, c.getDouble(0), 0.0001);
            assertEquals("2026-02-01", c.getString(1));
            assertEquals("09:15:00", c.getString(2));
        }
    }

    @Test public void fechaFuturaMuestraElErrorDeLaFecha() {
        PesajeFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.fecha, Tiempo.hoy().plusDays(1).toString());
        escribir(pantalla, R.id.peso, "450");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_fecha_futura), error(pantalla, R.id.campoFecha));
        assertEquals(0, contar());
    }

    @Test public void horaFuturaDeHoyMuestraElErrorDeLaHora() {
        assumeTrue("A las 23:59 la hora de prueba ya no es futura", Tiempo.ahora().toLocalTime().isBefore(LocalTime.of(23, 58)));
        PesajeFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.hora, "23:59");
        escribir(pantalla, R.id.peso, "450");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_hora_futura), error(pantalla, R.id.campoHora));
        assertEquals(0, contar());
    }

    @Test public void unAntecedenteSePuedeGuardarSinHora() {
        PesajeFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.fecha, "2025-12-31");
        escribir(pantalla, R.id.hora, "");
        escribir(pantalla, R.id.peso, "380");
        guardar(pantalla);
        assertTrue(pantalla.isFinishing());
        try (Cursor c = consulta("SELECT es_antecedente, hora FROM pesaje")) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
            assertTrue(c.isNull(1));
        }
    }

    @Test public void sinHoraEnUnPesajeNormalMuestraElErrorDeLaHora() {
        PesajeFormActivity pantalla = abrir().get();
        escribir(pantalla, R.id.hora, "");
        escribir(pantalla, R.id.peso, "450");
        guardar(pantalla);
        assertEquals(pantalla.getString(R.string.error_hora_obligatoria), error(pantalla, R.id.campoHora));
        assertEquals(0, contar());
    }

    @Test public void rotarConservaElFormularioYNoDuplicaElPesaje() {
        ActivityController<PesajeFormActivity> primera = abrir();
        escribir(primera.get(), R.id.fecha, "2026-02-01");
        escribir(primera.get(), R.id.hora, "09:15");
        escribir(primera.get(), R.id.peso, "450,5");
        escribir(primera.get(), R.id.observaciones, "Báscula de la entrada");
        primera.get().findViewById(R.id.guardar).performClick(); // el resultado llega a una pantalla ya destruida
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        ActivityController<PesajeFormActivity> segunda = Robolectric.buildActivity(PesajeFormActivity.class, intento(animal.idAnimal))
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("450,5", texto(segunda.get(), R.id.peso));
        assertEquals("Báscula de la entrada", texto(segunda.get(), R.id.observaciones));
        assertEquals("2026-02-01", texto(segunda.get(), R.id.fecha));
        assertNull("La pantalla destruida no debe navegar", shadowOf(primera.get()).getNextStartedActivity());
        assertEquals(1, contar());
    }

    @Test public void cancelarSinCambiosCierraYConCambiosPideConfirmar() {
        PesajeFormActivity sinCambios = abrir().get();
        sinCambios.findViewById(R.id.cancelar).performClick();
        assertTrue(sinCambios.isFinishing());
        PesajeFormActivity conCambios = abrir().get();
        escribir(conCambios, R.id.peso, "450");
        conCambios.findViewById(R.id.cancelar).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(ShadowDialog.getLatestDialog());
        assertTrue(ShadowDialog.getLatestDialog().isShowing());
        assertFalse(conCambios.isFinishing());
    }

    @Test public void rotarYVolverAGuardarNoDuplicaElPesaje() {
        ActivityController<PesajeFormActivity> primera = abrir();
        escribir(primera.get(), R.id.fecha, "2026-02-01");
        escribir(primera.get(), R.id.hora, "09:15");
        escribir(primera.get(), R.id.peso, "450,5");
        primera.get().findViewById(R.id.guardar).performClick(); // el resultado llega a una pantalla ya destruida
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        ActivityController<PesajeFormActivity> segunda = Robolectric.buildActivity(PesajeFormActivity.class, intento(animal.idAnimal))
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        guardar(segunda.get());
        assertEquals(1, contar());
        assertTrue(segunda.get().isFinishing());
    }
}
