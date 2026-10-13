package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
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
public class PesajesActivityTest {
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

    private void pesar(String fecha, String hora, String peso, String observaciones) {
        Captura<Pesaje> captura = new Captura<>();
        app.pesajes().registrar(animal.idAnimal, new EntradaPesaje(fecha, hora, peso, observaciones), captura);
        assertTrue(captura.esperar().exito);
    }
    private ActivityController<PesajesActivity> abrir(String idAnimal) {
        Intent intento = new Intent();
        if (idAnimal != null) intento.putExtra(PesajesActivity.EXTRA_ID_ANIMAL, idAnimal);
        ActivityController<PesajesActivity> controlador = Robolectric.buildActivity(PesajesActivity.class, intento).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private PesajeAdapter adaptador(PesajesActivity pantalla) {
        return (PesajeAdapter) ((RecyclerView) pantalla.findViewById(R.id.lista)).getAdapter();
    }
    private PesajeAdapter.Fila fila(PesajesActivity pantalla, int posicion) {
        RecyclerView lista = pantalla.findViewById(R.id.lista);
        PesajeAdapter.Fila fila = adaptador(pantalla).onCreateViewHolder(lista, 0);
        adaptador(pantalla).onBindViewHolder(fila, posicion);
        return fila;
    }
    private String texto(android.view.View vista, int id) { return ((TextView) vista.findViewById(id)).getText().toString(); }

    @Test public void unAnimalInexistenteOSinIdSeCierraConMensaje() {
        assertTrue(abrir(null).get().isFinishing());
        PesajesActivity ajeno = abrir("no-existe").get();
        assertTrue(ajeno.isFinishing());
        assertEquals(ajeno.getString(R.string.animal_no_encontrado), ShadowToast.getTextOfLatestToast());
    }

    @Test public void historialVacioMuestraAccionParaRegistrar() {
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoVacio).getVisibility());
        assertEquals(View.GONE, pantalla.findViewById(R.id.lista).getVisibility());
        assertEquals(pantalla.getString(R.string.sin_pesajes_historial), texto(pantalla.findViewById(R.id.estadoVacio), R.id.mensajeVacio));
        pantalla.findViewById(R.id.accionVacio).performClick();
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(PesajeFormActivity.class.getName(), siguiente.getComponent().getClassName());
        assertEquals(animal.idAnimal, siguiente.getStringExtra(PesajeFormActivity.EXTRA_ID_ANIMAL));
    }

    @Test public void muestraLosPesajesDelMasRecienteAlMasAntiguo() {
        pesar("2026-02-01", "08:00", "400", "");
        pesar("2026-03-01", "18:00", "425", "Después de la lluvia");
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(2, adaptador(pantalla).getItemCount());
        PesajeAdapter.Fila primera = fila(pantalla, 0);
        assertEquals("425 kg", texto(primera.itemView, R.id.itemPeso));
        assertEquals("2026-03-01 · 18:00", texto(primera.itemView, R.id.itemFechaHora));
        assertEquals("Después de la lluvia", texto(primera.itemView, R.id.itemObservaciones));
        assertEquals(View.GONE, primera.itemView.findViewById(R.id.itemAntecedente).getVisibility());
        PesajeAdapter.Fila segunda = fila(pantalla, 1);
        assertEquals("400 kg", texto(segunda.itemView, R.id.itemPeso));
        assertEquals(View.GONE, segunda.itemView.findViewById(R.id.itemObservaciones).getVisibility());
    }

    @Test public void unAntecedenteMuestraSuMarcaYSinHora() {
        pesar("2025-12-31", "", "380", "");
        PesajeAdapter.Fila fila = fila(abrir(animal.idAnimal).get(), 0);
        assertEquals(View.VISIBLE, fila.itemView.findViewById(R.id.itemAntecedente).getVisibility());
        assertTrue(texto(fila.itemView, R.id.itemFechaHora).contains(RuntimeEnvironment.getApplication().getString(R.string.sin_hora)));
    }

    @Test public void volverDelFormularioRefrescaElHistorial() {
        ActivityController<PesajesActivity> controlador = abrir(animal.idAnimal);
        assertEquals(0, adaptador(controlador.get()).getItemCount());
        pesar("2026-03-01", "09:00", "420,5", "");
        controlador.pause().resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, adaptador(controlador.get()).getItemCount());
        assertEquals(View.GONE, controlador.get().findViewById(R.id.estadoVacio).getVisibility());
        assertNotNull(shadowOf(controlador.get()));
    }

    @Test public void errorDeBaseDeDatosMuestraReintentar() {
        db.close();
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoError).getVisibility());
        assertEquals(pantalla.getString(R.string.error_carga), texto(pantalla.findViewById(R.id.estadoError), R.id.mensajeError));
    }

    @Test public void elBotonDeAgregarAbreElFormulario() {
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        pantalla.findViewById(R.id.agregar).performClick();
        assertEquals(PesajeFormActivity.class.getName(), shadowOf(pantalla).getNextStartedActivity().getComponent().getClassName());
    }
}
