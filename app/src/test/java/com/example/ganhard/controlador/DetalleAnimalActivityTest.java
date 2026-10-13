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
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.modelo.reglas.EntradaPesaje;
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
    private static final String BRAHMAN = "853193c3-3b52-4d5f-a4df-f73be27e5da1";
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
    private Animal luna() {
        return registrar(new EntradaAnimal("GH-014", "Luna", "H", BRAHMAN, "2025-03-05", "2026-01-15", "Compra reciente"));
    }
    private void pesar(Animal animal, String fecha, String hora, String peso) {
        Captura<Pesaje> captura = new Captura<>();
        app.pesajes().registrar(animal.idAnimal, new EntradaPesaje(fecha, hora, peso, ""), captura);
        assertTrue(captura.esperar().exito);
    }
    private ActivityController<DetalleAnimalActivity> abrir(String idAnimal) {
        Intent intento = new Intent();
        if (idAnimal != null) intento.putExtra(DetalleAnimalActivity.EXTRA_ID_ANIMAL, idAnimal);
        ActivityController<DetalleAnimalActivity> controlador = Robolectric.buildActivity(DetalleAnimalActivity.class, intento).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private String texto(DetalleAnimalActivity pantalla, int id) { return ((TextView) pantalla.findViewById(id)).getText().toString(); }
    private String tituloDe(DetalleAnimalActivity pantalla, int idFila) {
        return ((TextView) pantalla.findViewById(idFila).findViewById(R.id.filaTitulo)).getText().toString();
    }
    private String subtituloDe(DetalleAnimalActivity pantalla, int idFila) {
        return ((TextView) pantalla.findViewById(idFila).findViewById(R.id.filaSubtitulo)).getText().toString();
    }

    @Test public void laCabeceraMuestraOverlineNombreRazaYFechasCortas() {
        DetalleAnimalActivity pantalla = abrir(luna().idAnimal).get();
        assertEquals("Activo", texto(pantalla, R.id.fichaEstado));
        assertEquals("GH-014 · Luna", texto(pantalla, R.id.fichaTitulo));
        assertEquals("Hembra · Brahman", texto(pantalla, R.id.fichaRaza));
        assertEquals("Ingreso: 15 ene 2026", texto(pantalla, R.id.fichaIngreso));
        assertEquals("Nacimiento: 5 mar 2025", texto(pantalla, R.id.fichaNacimiento));
        assertEquals("Observaciones: Compra reciente", texto(pantalla, R.id.fichaObservaciones));
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.fichaObservaciones).getVisibility());
        assertEquals(View.GONE, pantalla.findViewById(R.id.fichaEstadoDesde).getVisibility());
        assertEquals(View.GONE, pantalla.findViewById(R.id.estadoError).getVisibility());
    }

    @Test public void sinNombreNiRazaNiNacimientoNiObservacionesSeMuestraCompleto() {
        Animal animal = registrar(new EntradaAnimal("GH-022", "", "M", "", "", "2026-01-10", ""));
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals("GH-022", texto(pantalla, R.id.fichaTitulo));
        assertEquals("Macho · Raza sin registrar", texto(pantalla, R.id.fichaRaza));
        assertEquals("Nacimiento: Sin registrar", texto(pantalla, R.id.fichaNacimiento));
        assertEquals(View.GONE, pantalla.findViewById(R.id.fichaObservaciones).getVisibility());
    }

    @Test public void unVendidoMuestraDesdeCuando() {
        Animal animal = luna();
        Animal vendido = db.animalDao().porId(animal.idAnimal);
        vendido.estado = Animal.VENDIDO;
        vendido.fechaEstado = "2026-06-01";
        db.animalDao().actualizar(vendido);
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals("Vendido", texto(pantalla, R.id.fichaEstado));
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.fichaEstadoDesde).getVisibility());
        assertEquals("Vendido desde 1 jun 2026", texto(pantalla, R.id.fichaEstadoDesde));
    }

    @Test public void elPesoRegistradoMuestraKilosYFechaConHora12h() {
        Animal animal = luna();
        pesar(animal, "2026-09-25", "07:40", "242");
        pesar(animal, "2026-10-10", "08:15", "250");
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals("250 kg", texto(pantalla, R.id.pesoKilos));
        assertEquals("Último pesaje · 10 oct 2026, 8:15 a. m.", texto(pantalla, R.id.pesoDetalle));
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.pesoDetalle).getVisibility());
    }

    @Test public void sinPesajesDiceSinPesajes() {
        DetalleAnimalActivity pantalla = abrir(luna().idAnimal).get();
        assertEquals(pantalla.getString(R.string.sin_pesajes), texto(pantalla, R.id.pesoKilos));
        assertEquals(View.GONE, pantalla.findViewById(R.id.pesoDetalle).getVisibility());
    }

    @Test public void elHistorialCuentaLasMedicionesConSingularYPlural() {
        Animal animal = luna();
        assertEquals(pantallaTexto(abrir(animal.idAnimal).get(), R.string.sin_mediciones), subtituloDe(abrir(animal.idAnimal).get(), R.id.filaHistorial));
        pesar(animal, "2026-09-25", "07:40", "242");
        assertEquals("1 medición vigente", subtituloDe(abrir(animal.idAnimal).get(), R.id.filaHistorial));
        pesar(animal, "2026-10-10", "08:15", "250");
        pesar(animal, "2026-10-11", "08:15", "251");
        assertEquals("3 mediciones vigentes", subtituloDe(abrir(animal.idAnimal).get(), R.id.filaHistorial));
        assertEquals("Historial de pesajes", tituloDe(abrir(animal.idAnimal).get(), R.id.filaHistorial));
    }
    private String pantallaTexto(DetalleAnimalActivity pantalla, int recurso) { return pantalla.getString(recurso); }

    @Test public void lasFilasDeSanidadYTareasEstanDeshabilitadasYNoNavegan() {
        DetalleAnimalActivity pantalla = abrir(luna().idAnimal).get();
        for (int id : new int[]{R.id.filaSanidad, R.id.filaTareas}) {
            assertFalse(pantalla.findViewById(id).isEnabled());
            assertTrue(subtituloDe(pantalla, id).contains(pantalla.getString(R.string.proximamente)));
            pantalla.findViewById(id).performClick();
        }
        assertEquals(null, shadowOf(pantalla).getNextStartedActivity());
        assertTrue(pantalla.findViewById(R.id.filaEditar).isEnabled());
        assertTrue(pantalla.findViewById(R.id.filaHistorial).isEnabled());
    }

    @Test public void registrarPesajeHistorialYEditarAbrenSusPantallas() {
        Animal animal = luna();
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        pantalla.findViewById(R.id.registrarPesaje).performClick();
        Intent registro = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(PesajeFormActivity.class.getName(), registro.getComponent().getClassName());
        assertEquals(animal.idAnimal, registro.getStringExtra(PesajeFormActivity.EXTRA_ID_ANIMAL));
        pantalla.findViewById(R.id.filaHistorial).performClick();
        Intent historial = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(PesajesActivity.class.getName(), historial.getComponent().getClassName());
        assertEquals(animal.idAnimal, historial.getStringExtra(PesajesActivity.EXTRA_ID_ANIMAL));
        pantalla.findViewById(R.id.filaEditar).performClick();
        Intent edicion = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(AnimalFormActivity.class.getName(), edicion.getComponent().getClassName());
        assertEquals(animal.idAnimal, edicion.getStringExtra(AnimalFormActivity.EXTRA_ID_ANIMAL));
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
        Animal animal = luna();
        db.close();
        DetalleAnimalActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoError).getVisibility());
        assertEquals(pantalla.getString(R.string.error_carga), texto(pantalla, R.id.mensajeError));
    }

    @Test public void rotarConservaLaFicha() {
        Animal animal = luna();
        ActivityController<DetalleAnimalActivity> primera = abrir(animal.idAnimal);
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        Intent intento = new Intent().putExtra(DetalleAnimalActivity.EXTRA_ID_ANIMAL, animal.idAnimal);
        ActivityController<DetalleAnimalActivity> segunda = Robolectric.buildActivity(DetalleAnimalActivity.class, intento)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("GH-014 · Luna", texto(segunda.get(), R.id.fichaTitulo));
        assertEquals("Hembra · Brahman", texto(segunda.get(), R.id.fichaRaza));
    }
}
