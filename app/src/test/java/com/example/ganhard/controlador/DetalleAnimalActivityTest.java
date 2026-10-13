package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
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
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class DetalleAnimalActivityTest {
    private static final String ANGUS = "4fda5aab-035d-4f2e-b1e1-c8159de431d4";
    private GanHardDatabase db;
    private GanHardApplication app;

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
    }
    @After public void limpiar() { db.close(); }

    private Animal registrar(EntradaAnimal entrada) {
        Captura<Animal> captura = new Captura<>();
        app.animales().registrar(entrada, captura);
        captura.esperar();
        assertTrue(captura.exito);
        return captura.valor;
    }
    private Animal completo() {
        return registrar(new EntradaAnimal("GH-001", "Lucero", "H", ANGUS, "2025-03-01", "2026-01-10", "Compra reciente"));
    }
    private ActivityController<DetalleAnimalActivity> abrir(String idAnimal) {
        Intent intento = new Intent();
        if (idAnimal != null) intento.putExtra(DetalleAnimalActivity.EXTRA_ID_ANIMAL, idAnimal);
        ActivityController<DetalleAnimalActivity> controlador = Robolectric.buildActivity(DetalleAnimalActivity.class, intento).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private String texto(DetalleAnimalActivity pantalla, int id) { return ((TextView) pantalla.findViewById(id)).getText().toString(); }

    @Test public void muestraLosDatosDelAnimal() {
        DetalleAnimalActivity pantalla = abrir(completo().idAnimal).get();
        assertEquals("GH-001", texto(pantalla, R.id.fichaCodigo));
        assertEquals("Lucero", texto(pantalla, R.id.fichaNombre));
        assertEquals("Hembra", texto(pantalla, R.id.datoSexo));
        assertEquals("Angus", texto(pantalla, R.id.datoRaza));
        assertEquals("2025-03-01", texto(pantalla, R.id.datoNacimiento));
        assertEquals("2026-01-10", texto(pantalla, R.id.datoIngreso));
        assertEquals("Activo desde 2026-01-10", texto(pantalla, R.id.datoEstado));
        assertEquals("Compra reciente", texto(pantalla, R.id.datoObservaciones));
        assertEquals(View.GONE, pantalla.findViewById(R.id.estadoError).getVisibility());
    }

    @Test public void losDatosOpcionalesAusentesDicenSinRegistrar() {
        Animal animal = registrar(new EntradaAnimal("GH-002", "", "M", "", "", "2026-01-10", ""));
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        String sinRegistrar = pantalla.getString(R.string.sin_registrar);
        assertEquals(pantalla.getString(R.string.sin_nombre), texto(pantalla, R.id.fichaNombre));
        assertEquals("Macho", texto(pantalla, R.id.datoSexo));
        assertEquals(sinRegistrar, texto(pantalla, R.id.datoRaza));
        assertEquals(sinRegistrar, texto(pantalla, R.id.datoNacimiento));
        assertEquals(sinRegistrar, texto(pantalla, R.id.datoObservaciones));
    }

    @Test public void sinPesajesMuestraIndicacion() {
        DetalleAnimalActivity pantalla = abrir(completo().idAnimal).get();
        assertEquals(pantalla.getString(R.string.sin_pesajes), texto(pantalla, R.id.datoUltimoPeso));
    }

    @Test public void unAnimalVendidoMuestraSuEstado() {
        Animal animal = completo();
        Animal guardado = db.animalDao().porId(animal.idAnimal);
        guardado.estado = Animal.VENDIDO;
        guardado.fechaEstado = "2026-06-01";
        db.animalDao().actualizar(guardado);
        assertEquals("Vendido desde 2026-06-01", texto(abrir(animal.idAnimal).get(), R.id.datoEstado));
    }

    @Test public void idInexistenteMuestraMensajeYSeCierra() {
        DetalleAnimalActivity pantalla = abrir("no-existe").get();
        assertTrue(pantalla.isFinishing());
        assertEquals(pantalla.getString(R.string.animal_no_encontrado), ShadowToast.getTextOfLatestToast());
    }

    @Test public void sinIdEnElIntentoSeCierra() {
        assertTrue(abrir(null).get().isFinishing());
    }

    @Test public void errorDeBaseDeDatosMuestraReintentar() {
        Animal animal = completo();
        db.close();
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoError).getVisibility());
        assertEquals(pantalla.getString(R.string.error_carga), texto(pantalla, R.id.mensajeError));
    }

    @Test public void rotarConservaLaFicha() {
        Animal animal = completo();
        ActivityController<DetalleAnimalActivity> primera = abrir(animal.idAnimal);
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        Intent intento = new Intent().putExtra(DetalleAnimalActivity.EXTRA_ID_ANIMAL, animal.idAnimal);
        ActivityController<DetalleAnimalActivity> segunda = Robolectric.buildActivity(DetalleAnimalActivity.class, intento)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("GH-001", texto(segunda.get(), R.id.fichaCodigo));
        assertEquals("Angus", texto(segunda.get(), R.id.datoRaza));
    }

    @Test public void fichaMuestraElUltimoPesoRealYSuFecha() {
        Animal animal = completo();
        Captura<com.example.ganhard.modelo.entidad.Pesaje> captura = new Captura<>();
        app.pesajes().registrar(animal.idAnimal, new com.example.ganhard.modelo.reglas.EntradaPesaje("2026-03-01", "09:00", "420,5", ""), captura);
        captura.esperar();
        assertEquals("420.5 kg · 2026-03-01", texto(abrir(animal.idAnimal).get(), R.id.datoUltimoPeso));
    }

    @Test public void registrarPesajeYVerPesosAbrenSusPantallas() {
        Animal animal = completo();
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        pantalla.findViewById(R.id.registrarPesaje).performClick();
        Intent registro = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(PesajeFormActivity.class.getName(), registro.getComponent().getClassName());
        assertEquals(animal.idAnimal, registro.getStringExtra(PesajeFormActivity.EXTRA_ID_ANIMAL));
        pantalla.findViewById(R.id.verPesos).performClick();
        Intent historial = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(PesajesActivity.class.getName(), historial.getComponent().getClassName());
        assertEquals(animal.idAnimal, historial.getStringExtra(PesajesActivity.EXTRA_ID_ANIMAL));
    }

    private androidx.appcompat.app.AlertDialog abrirDialogoDeEstado(DetalleAnimalActivity pantalla) {
        pantalla.findViewById(R.id.cambiarEstado).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        return (androidx.appcompat.app.AlertDialog) org.robolectric.shadows.ShadowDialog.getLatestDialog();
    }

    @Test public void editarAbreElFormularioConElId() {
        Animal animal = completo();
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        pantalla.findViewById(R.id.editar).performClick();
        Intent edicion = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(AnimalFormActivity.class.getName(), edicion.getComponent().getClassName());
        assertEquals(animal.idAnimal, edicion.getStringExtra(AnimalFormActivity.EXTRA_ID_ANIMAL));
    }

    @Test public void elDialogoDeEstadoParteDelEstadoActual() {
        Animal animal = completo();
        Animal vendido = db.animalDao().porId(animal.idAnimal);
        vendido.estado = Animal.VENDIDO;
        vendido.fechaEstado = "2026-06-01";
        db.animalDao().actualizar(vendido);
        androidx.appcompat.app.AlertDialog dialogo = abrirDialogoDeEstado(abrir(animal.idAnimal).get());
        assertTrue(((android.widget.RadioButton) dialogo.findViewById(R.id.estadoVendido)).isChecked());
    }

    @Test public void cambiarElEstadoAVendidoLoMuestraEnLaFicha() {
        Animal animal = completo();
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        androidx.appcompat.app.AlertDialog dialogo = abrirDialogoDeEstado(pantalla);
        ((android.widget.RadioButton) dialogo.findViewById(R.id.estadoVendido)).setChecked(true);
        dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Vendido desde " + com.example.ganhard.modelo.nucleo.Tiempo.hoy(), texto(pantalla, R.id.datoEstado));
        assertFalse(dialogo.isShowing());
    }

    @Test public void unaFechaFuturaMuestraElErrorYMantieneElDialogo() {
        Animal animal = completo();
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        androidx.appcompat.app.AlertDialog dialogo = abrirDialogoDeEstado(pantalla);
        ((android.widget.RadioButton) dialogo.findViewById(R.id.estadoFallecido)).setChecked(true);
        ((android.widget.EditText) dialogo.findViewById(R.id.estadoFecha)).setText(com.example.ganhard.modelo.nucleo.Tiempo.hoy().plusDays(1).toString());
        dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(dialogo.isShowing());
        assertEquals(pantalla.getString(R.string.error_fecha_futura),
                String.valueOf(((com.google.android.material.textfield.TextInputLayout) dialogo.findViewById(R.id.campoEstadoFecha)).getError()));
        assertEquals("Activo desde 2026-01-10", texto(pantalla, R.id.datoEstado));
    }

    @Test public void cancelarElDialogoNoCambiaNada() {
        Animal animal = completo();
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        androidx.appcompat.app.AlertDialog dialogo = abrirDialogoDeEstado(pantalla);
        ((android.widget.RadioButton) dialogo.findViewById(R.id.estadoVendido)).setChecked(true);
        dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(dialogo.isShowing());
        assertEquals("Activo desde 2026-01-10", texto(pantalla, R.id.datoEstado));
    }

    @Test public void aplicarSinCambiarNadaConservaLaFechaDelEstado() {
        Animal animal = completo();
        Animal vendido = db.animalDao().porId(animal.idAnimal);
        vendido.estado = Animal.VENDIDO;
        vendido.fechaEstado = "2026-06-01";
        db.animalDao().actualizar(vendido);
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        androidx.appcompat.app.AlertDialog dialogo = abrirDialogoDeEstado(pantalla);
        assertEquals("2026-06-01", ((android.widget.EditText) dialogo.findViewById(R.id.estadoFecha)).getText().toString());
        dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Vendido desde 2026-06-01", texto(pantalla, R.id.datoEstado));
    }
}
