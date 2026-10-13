package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.pruebas.Captura;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.TimeUnit;
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
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class AnimalesActivityTest {
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
    private Animal registrar(String codigo, String nombre) {
        Captura<Animal> captura = new Captura<>();
        app.animales().registrar(new EntradaAnimal(codigo, nombre, "H", "", "", "2026-01-10", ""), captura);
        captura.esperar();
        return captura.valor;
    }
    private void marcar(Animal animal, String estado) {
        Animal guardado = db.animalDao().porId(animal.idAnimal);
        guardado.estado = estado;
        db.animalDao().actualizar(guardado);
    }
    private ActivityController<AnimalesActivity> abrir() {
        ActivityController<AnimalesActivity> controlador = Robolectric.buildActivity(AnimalesActivity.class).create().start().resume();
        shadowOf(Looper.getMainLooper()).idle();
        return controlador;
    }
    private void esperarBusqueda() { ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS); }
    private void buscar(AnimalesActivity pantalla, String texto) {
        ((TextInputEditText) pantalla.findViewById(R.id.buscar)).setText(texto);
        esperarBusqueda();
    }
    private AnimalAdapter adaptador(AnimalesActivity pantalla) {
        return (AnimalAdapter) ((RecyclerView) pantalla.findViewById(R.id.lista)).getAdapter();
    }
    private String textoDe(AnimalesActivity pantalla, int id) { return ((TextView) pantalla.findViewById(id)).getText().toString(); }

    @Test public void sinFincaConfiguradaAbreConfiguracion() {
        AnimalesActivity pantalla = abrir().get();
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertNotNull(siguiente);
        assertEquals(ConfiguracionActivity.class.getName(), siguiente.getComponent().getClassName());
        assertTrue(pantalla.isFinishing());
    }

    @Test public void listaVaciaMuestraMensajeYAccionParaRegistrar() {
        fincaConfigurada();
        AnimalesActivity pantalla = abrir().get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoVacio).getVisibility());
        assertEquals(View.GONE, pantalla.findViewById(R.id.lista).getVisibility());
        assertEquals(pantalla.getString(R.string.sin_animales), textoDe(pantalla, R.id.mensajeVacio));
        assertEquals(pantalla.getString(R.string.registrar_animal), textoDe(pantalla, R.id.accionVacio));
        pantalla.findViewById(R.id.accionVacio).performClick();
        assertEquals(AnimalFormActivity.class.getName(), shadowOf(pantalla).getNextStartedActivity().getComponent().getClassName());
    }

    @Test public void muestraLosAnimalesRegistrados() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        registrar("GH-002", null);
        AnimalesActivity pantalla = abrir().get();
        assertEquals(View.GONE, pantalla.findViewById(R.id.estadoVacio).getVisibility());
        AnimalAdapter adaptador = adaptador(pantalla);
        assertEquals(2, adaptador.getItemCount());
        RecyclerView lista = pantalla.findViewById(R.id.lista);
        AnimalAdapter.Fila fila = adaptador.onCreateViewHolder(lista, 0);
        adaptador.onBindViewHolder(fila, 0);
        assertEquals("GH-001", ((TextView) fila.itemView.findViewById(R.id.itemCodigo)).getText().toString());
        assertEquals("Lucero", ((TextView) fila.itemView.findViewById(R.id.itemNombre)).getText().toString());
        assertEquals(pantalla.getString(R.string.sin_pesajes), ((TextView) fila.itemView.findViewById(R.id.itemPeso)).getText().toString());
        AnimalAdapter.Fila sinNombre = adaptador.onCreateViewHolder(lista, 0);
        adaptador.onBindViewHolder(sinNombre, 1);
        assertEquals(pantalla.getString(R.string.sin_nombre), ((TextView) sinNombre.itemView.findViewById(R.id.itemNombre)).getText().toString());
    }

    @Test public void busquedaSinCoincidenciasMuestraMensajeYLimpiarRestauraLaLista() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        AnimalesActivity pantalla = abrir().get();
        buscar(pantalla, "zzz");
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoVacio).getVisibility());
        assertEquals(pantalla.getString(R.string.sin_coincidencias), textoDe(pantalla, R.id.mensajeVacio));
        assertEquals(pantalla.getString(R.string.limpiar_busqueda), textoDe(pantalla, R.id.accionVacio));
        pantalla.findViewById(R.id.accionVacio).performClick();
        esperarBusqueda();
        assertEquals("", textoDe(pantalla, R.id.buscar));
        assertEquals(1, adaptador(pantalla).getItemCount());
        assertEquals(View.GONE, pantalla.findViewById(R.id.estadoVacio).getVisibility());
    }

    @Test public void buscarPorCodigoOPorNombreFiltraLaLista() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        registrar("GH-002", "Estrella");
        AnimalesActivity pantalla = abrir().get();
        buscar(pantalla, "estre");
        assertEquals(1, adaptador(pantalla).getItemCount());
        buscar(pantalla, "gh-00");
        assertEquals(2, adaptador(pantalla).getItemCount());
    }

    @Test public void buscarConPorcentajeNoMuestraTodo() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        registrar("GH-002", "Estrella");
        AnimalesActivity pantalla = abrir().get();
        buscar(pantalla, "%");
        assertEquals(0, adaptador(pantalla).getItemCount());
        assertEquals(pantalla.getString(R.string.sin_coincidencias), textoDe(pantalla, R.id.mensajeVacio));
    }

    @Test public void filtroDeEstadoOcultaLosVendidos() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        marcar(registrar("GH-002", "Estrella"), Animal.VENDIDO);
        AnimalesActivity pantalla = abrir().get();
        assertEquals(2, adaptador(pantalla).getItemCount());
        ((Chip) pantalla.findViewById(R.id.chipActivos)).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, adaptador(pantalla).getItemCount());
        ((Chip) pantalla.findViewById(R.id.chipVendidos)).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, adaptador(pantalla).getItemCount());
        ((Chip) pantalla.findViewById(R.id.chipTodos)).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(2, adaptador(pantalla).getItemCount());
    }

    @Test public void volverDelRegistroRefrescaLaLista() {
        fincaConfigurada();
        ActivityController<AnimalesActivity> controlador = abrir();
        assertEquals(0, adaptador(controlador.get()).getItemCount());
        registrar("GH-001", "Lucero");
        controlador.pause().resume();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, adaptador(controlador.get()).getItemCount());
    }

    @Test public void rotarConservaElTextoDeBusquedaYElFiltro() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        ActivityController<AnimalesActivity> primera = abrir();
        buscar(primera.get(), "GH");
        ((Chip) primera.get().findViewById(R.id.chipActivos)).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        ActivityController<AnimalesActivity> segunda = Robolectric.buildActivity(AnimalesActivity.class)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        esperarBusqueda();
        assertEquals("GH", textoDe(segunda.get(), R.id.buscar));
        assertTrue(((Chip) segunda.get().findViewById(R.id.chipActivos)).isChecked());
        assertEquals(1, adaptador(segunda.get()).getItemCount());
    }

    @Test public void errorDeBaseDeDatosMuestraReintentar() {
        fincaConfigurada();
        db.close();
        AnimalesActivity pantalla = abrir().get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoError).getVisibility());
        assertEquals(pantalla.getString(R.string.error_carga), textoDe(pantalla, R.id.mensajeError));
    }
}
