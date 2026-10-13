package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
import com.google.android.material.appbar.MaterialToolbar;
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
        animal = registrar("GH-014", "Luna");
    }
    @After public void limpiar() { db.close(); }

    private Animal registrar(String codigo, String nombre) {
        Captura<Animal> captura = new Captura<>();
        app.animales().registrar(new EntradaAnimal(codigo, nombre, "H", "", "2025-03-01", "2026-01-10", ""), captura);
        return captura.esperar().valor;
    }
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
    private String texto(View vista, int id) { return ((TextView) vista.findViewById(id)).getText().toString(); }

    @Test public void unAnimalInexistenteOSinIdSeCierraConMensaje() {
        assertTrue(abrir(null).get().isFinishing());
        PesajesActivity ajeno = abrir("no-existe").get();
        assertTrue(ajeno.isFinishing());
        assertEquals(ajeno.getString(R.string.animal_no_encontrado), ShadowToast.getTextOfLatestToast());
    }

    @Test public void elTituloUsaElNombreOElCodigo() {
        PesajesActivity conNombre = abrir(animal.idAnimal).get();
        assertEquals("Pesajes de Luna", String.valueOf(((MaterialToolbar) conNombre.findViewById(R.id.toolbar)).getTitle()));
        Animal sinNombre = registrar("GH-022", "");
        PesajesActivity sinNombrePantalla = abrir(sinNombre.idAnimal).get();
        assertEquals("Pesajes de GH-022", String.valueOf(((MaterialToolbar) sinNombrePantalla.findViewById(R.id.toolbar)).getTitle()));
    }

    @Test public void laCabeceraDiceElCodigoYElOrden() {
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals("GH-014 · Historial de peso", texto(pantalla.findViewById(R.id.raiz), R.id.pesajesCabecera));
        assertEquals(pantalla.getString(R.string.pesajes_orden), texto(pantalla.findViewById(R.id.raiz), R.id.pesajesOrden));
    }

    @Test public void historialVacioMuestraMensajeYElBotonDeRegistrarSigueDisponible() {
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoVacio).getVisibility());
        assertEquals(View.GONE, pantalla.findViewById(R.id.lista).getVisibility());
        assertEquals(pantalla.getString(R.string.sin_pesajes_historial), texto(pantalla.findViewById(R.id.estadoVacio), R.id.mensajeVacio));
        pantalla.findViewById(R.id.registrarPesaje).performClick();
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(PesajeFormActivity.class.getName(), siguiente.getComponent().getClassName());
        assertEquals(animal.idAnimal, siguiente.getStringExtra(PesajeFormActivity.EXTRA_ID_ANIMAL));
    }

    @Test public void lasTarjetasMuestranFechaConHora12hYSoloLaPrimeraDiceVigente() {
        pesar("2026-09-10", "08:00", "235", "");
        pesar("2026-09-25", "07:40", "242", "");
        pesar("2026-10-10", "08:15", "250", "Después de la lluvia");
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(3, adaptador(pantalla).getItemCount());
        PesajeAdapter.Fila primera = fila(pantalla, 0);
        assertEquals(View.VISIBLE, primera.itemView.findViewById(R.id.itemVigente).getVisibility());
        assertEquals("Vigente", texto(primera.itemView, R.id.itemVigente));
        assertEquals("250 kg", texto(primera.itemView, R.id.itemPeso));
        assertEquals("10 oct 2026 · 8:15 a. m.", texto(primera.itemView, R.id.itemFechaHora));
        assertEquals("Después de la lluvia", texto(primera.itemView, R.id.itemObservaciones));
        PesajeAdapter.Fila segunda = fila(pantalla, 1);
        assertEquals(View.GONE, segunda.itemView.findViewById(R.id.itemVigente).getVisibility());
        assertEquals("242 kg", texto(segunda.itemView, R.id.itemPeso));
        assertEquals("25 sep 2026 · 7:40 a. m.", texto(segunda.itemView, R.id.itemFechaHora));
        assertEquals(View.GONE, segunda.itemView.findViewById(R.id.itemObservaciones).getVisibility());
    }

    @Test public void unAntecedenteSinHoraMuestraSuMarcaYSinHora() {
        pesar("2025-12-31", "", "380", "");
        PesajeAdapter.Fila fila = fila(abrir(animal.idAnimal).get(), 0);
        assertEquals(View.VISIBLE, fila.itemView.findViewById(R.id.itemAntecedente).getVisibility());
        assertEquals("31 dic 2025 · " + RuntimeEnvironment.getApplication().getString(R.string.sin_hora), texto(fila.itemView, R.id.itemFechaHora));
    }

    @Test public void noHayBotonFlotanteYProgramarProximoPesajeEstaDeshabilitado() {
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertNull(pantalla.findViewById(pantalla.getResources().getIdentifier("agregar", "id", pantalla.getPackageName())));
        FilaNavegacion programar = pantalla.findViewById(R.id.filaProgramar);
        assertFalse(programar.isEnabled());
        assertEquals(pantalla.getString(R.string.programar_pesaje), texto(programar, R.id.filaTitulo));
        assertTrue(texto(programar, R.id.filaSubtitulo).contains(pantalla.getString(R.string.proximamente)));
        programar.performClick();
        assertNull(shadowOf(pantalla).getNextStartedActivity());
    }

    @Test public void volverDelFormularioRefrescaElHistorial() {
        ActivityController<PesajesActivity> controlador = abrir(animal.idAnimal);
        assertEquals(0, adaptador(controlador.get()).getItemCount());
        pesar("2026-03-01", "09:00", "420,5", "");
        controlador.pause().resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, adaptador(controlador.get()).getItemCount());
        assertEquals(View.GONE, controlador.get().findViewById(R.id.estadoVacio).getVisibility());
    }

    @Test public void errorDeBaseDeDatosMuestraReintentar() {
        db.close();
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoError).getVisibility());
        assertEquals(pantalla.getString(R.string.error_carga), texto(pantalla.findViewById(R.id.estadoError), R.id.mensajeError));
    }

    @Test public void elHistorialMuestraLaNotaDeLosRegistrosAnulados() {
        PesajesActivity pantalla = abrir(animal.idAnimal).get();
        assertEquals(pantalla.getString(R.string.nota_pesajes_anulados), texto(pantalla.findViewById(R.id.raiz), R.id.notaAnulados));
    }
}
