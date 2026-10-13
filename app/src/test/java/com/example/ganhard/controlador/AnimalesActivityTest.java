package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.RadioButton;
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
    private static final String BRAHMAN = "853193c3-3b52-4d5f-a4df-f73be27e5da1";
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
    private Animal registrar(String codigo, String nombre) { return registrar(codigo, nombre, "H", ""); }
    private Animal registrar(String codigo, String nombre, String sexo, String idRaza) {
        Captura<Animal> captura = new Captura<>();
        app.animales().registrar(new EntradaAnimal(codigo, nombre, sexo, idRaza, "", "2026-01-10", ""), captura);
        captura.esperar();
        return captura.valor;
    }
    private void pesar(Animal animal, String fecha, String hora, String peso) {
        Captura<Pesaje> captura = new Captura<>();
        app.pesajes().registrar(animal.idAnimal, new EntradaPesaje(fecha, hora, peso, ""), captura);
        assertTrue(captura.esperar().exito);
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
    private void elegirFiltro(AnimalesActivity pantalla, int id) {
        ((RadioButton) pantalla.findViewById(id)).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }
    private AnimalAdapter adaptador(AnimalesActivity pantalla) {
        return (AnimalAdapter) ((RecyclerView) pantalla.findViewById(R.id.lista)).getAdapter();
    }
    private AnimalAdapter.Fila fila(AnimalesActivity pantalla, int posicion) {
        RecyclerView lista = pantalla.findViewById(R.id.lista);
        AnimalAdapter.Fila fila = adaptador(pantalla).onCreateViewHolder(lista, 0);
        adaptador(pantalla).onBindViewHolder(fila, posicion);
        return fila;
    }
    private String texto(View vista, int id) { return ((TextView) vista.findViewById(id)).getText().toString(); }
    private String textoDe(AnimalesActivity pantalla, int id) { return texto(pantalla.findViewById(android.R.id.content), id); }

    @Test public void sinFincaConfiguradaAbreConfiguracion() {
        AnimalesActivity pantalla = abrir().get();
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertNotNull(siguiente);
        assertEquals(ConfiguracionActivity.class.getName(), siguiente.getComponent().getClassName());
        assertTrue(pantalla.isFinishing());
    }

    @Test public void abreConActivosSeleccionadoYSoloMuestraActivos() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        marcar(registrar("GH-002", "Estrella"), Animal.VENDIDO);
        AnimalesActivity pantalla = abrir().get();
        assertTrue(((RadioButton) pantalla.findViewById(R.id.filtroActivos)).isChecked());
        assertEquals(1, adaptador(pantalla).getItemCount());
        assertEquals("GH-001 · Lucero", texto(fila(pantalla, 0).itemView, R.id.itemTitulo));
    }

    @Test public void yaNoHayOpcionTodosNiChipsNiBotonFlotante() {
        fincaConfigurada();
        AnimalesActivity pantalla = abrir().get();
        for (String nombre : new String[]{"chipTodos", "chipActivos", "agregar"}) {
            assertEquals(nombre, null, pantalla.findViewById(pantalla.getResources().getIdentifier(nombre, "id", pantalla.getPackageName())));
        }
    }

    @Test public void elContadorUsaSingularPluralYCero() {
        fincaConfigurada();
        registrar("GH-001", "Uno");
        AnimalesActivity pantalla = abrir().get();
        assertEquals("1 activo en la finca", texto(pantalla.findViewById(R.id.contadorFiltro).getRootView(), R.id.contadorFiltro));
        registrar("GH-002", "Dos");
        buscar(pantalla, "");
        pantalla.findViewById(R.id.filtroActivos).performClick();
        ActivityController<AnimalesActivity> otra = abrir();
        assertEquals("2 activos en la finca", texto(otra.get().findViewById(R.id.contadorFiltro).getRootView(), R.id.contadorFiltro));
        elegirFiltro(otra.get(), R.id.filtroVendidos);
        assertEquals("0 vendidos en la finca", texto(otra.get().findViewById(R.id.contadorFiltro).getRootView(), R.id.contadorFiltro));
        elegirFiltro(otra.get(), R.id.filtroFallecidos);
        assertEquals("0 fallecidos en la finca", texto(otra.get().findViewById(R.id.contadorFiltro).getRootView(), R.id.contadorFiltro));
    }

    @Test public void cambiarAVendidosYFallecidosMuestraEsosAnimales() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        marcar(registrar("GH-002", "Estrella"), Animal.VENDIDO);
        marcar(registrar("GH-003", "Luna"), Animal.FALLECIDO);
        AnimalesActivity pantalla = abrir().get();
        elegirFiltro(pantalla, R.id.filtroVendidos);
        assertEquals(1, adaptador(pantalla).getItemCount());
        assertEquals("GH-002 · Estrella", texto(fila(pantalla, 0).itemView, R.id.itemTitulo));
        assertEquals("1 vendido en la finca", textoDe(pantalla, R.id.contadorFiltro));
        elegirFiltro(pantalla, R.id.filtroFallecidos);
        assertEquals("GH-003 · Luna", texto(fila(pantalla, 0).itemView, R.id.itemTitulo));
        elegirFiltro(pantalla, R.id.filtroActivos);
        assertEquals("GH-001 · Lucero", texto(fila(pantalla, 0).itemView, R.id.itemTitulo));
    }

    @Test public void laTarjetaMuestraOverlineTituloRazaYUltimoPesaje() {
        fincaConfigurada();
        Animal luna = registrar("GH-014", "Luna", "H", BRAHMAN);
        pesar(luna, "2026-10-10", "08:15", "250");
        AnimalesActivity pantalla = abrir().get();
        View tarjeta = fila(pantalla, 0).itemView;
        assertEquals("Activo", texto(tarjeta, R.id.itemEstado));
        assertEquals("GH-014 · Luna", texto(tarjeta, R.id.itemTitulo));
        assertEquals("Hembra · Brahman", texto(tarjeta, R.id.itemDetalle));
        assertEquals("Último pesaje: 250 kg · 10 oct 2026", texto(tarjeta, R.id.itemPeso));
    }

    @Test public void unAnimalSinNombreNiRazaNiPesajesSeMuestraCompleto() {
        fincaConfigurada();
        registrar("GH-022", "", "M", "");
        AnimalesActivity pantalla = abrir().get();
        View tarjeta = fila(pantalla, 0).itemView;
        assertEquals("GH-022", texto(tarjeta, R.id.itemTitulo));
        assertEquals("Macho · Raza sin registrar", texto(tarjeta, R.id.itemDetalle));
        assertEquals(pantalla.getString(R.string.sin_pesajes), texto(tarjeta, R.id.itemPeso));
    }

    @Test public void sinAnimalesActivosDiceQueNoHayYElBotonDeRegistrarSigueDisponible() {
        fincaConfigurada();
        AnimalesActivity pantalla = abrir().get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoVacio).getVisibility());
        assertEquals(View.GONE, pantalla.findViewById(R.id.lista).getVisibility());
        assertEquals(pantalla.getString(R.string.sin_animales_activos), textoDe(pantalla, R.id.mensajeVacio));
        assertEquals(View.GONE, pantalla.findViewById(R.id.accionVacio).getVisibility());
        pantalla.findViewById(R.id.registrarAnimal).performClick();
        assertEquals(AnimalFormActivity.class.getName(), shadowOf(pantalla).getNextStartedActivity().getComponent().getClassName());
    }

    @Test public void sinVendidosNiFallecidosDiceQueNoHay() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        AnimalesActivity pantalla = abrir().get();
        elegirFiltro(pantalla, R.id.filtroVendidos);
        assertEquals(pantalla.getString(R.string.sin_animales_vendidos), textoDe(pantalla, R.id.mensajeVacio));
        elegirFiltro(pantalla, R.id.filtroFallecidos);
        assertEquals(pantalla.getString(R.string.sin_animales_fallecidos), textoDe(pantalla, R.id.mensajeVacio));
    }

    @Test public void elBotonDeRegistrarEstaSiempreYLaNotaTambien() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        AnimalesActivity pantalla = abrir().get();
        assertEquals(pantalla.getString(R.string.registrar_animal), textoDe(pantalla, R.id.registrarAnimal));
        assertEquals(pantalla.getString(R.string.nota_animales_inactivos), textoDe(pantalla, R.id.notaAnimales));
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
        assertTrue(((RadioButton) pantalla.findViewById(R.id.filtroActivos)).isChecked());
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

    @Test public void busquedaYFiltroSeCombinan() {
        fincaConfigurada();
        registrar("GH-001", "Lucero");
        marcar(registrar("GH-002", "Lucía"), Animal.VENDIDO);
        AnimalesActivity pantalla = abrir().get();
        buscar(pantalla, "lu");
        assertEquals(1, adaptador(pantalla).getItemCount());
        elegirFiltro(pantalla, R.id.filtroVendidos);
        assertEquals(1, adaptador(pantalla).getItemCount());
        assertEquals("GH-002 · Lucía", texto(fila(pantalla, 0).itemView, R.id.itemTitulo));
        buscar(pantalla, "lucero");
        assertEquals(0, adaptador(pantalla).getItemCount());
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
        marcar(registrar("GH-001", "Lucero"), Animal.VENDIDO);
        ActivityController<AnimalesActivity> primera = abrir();
        buscar(primera.get(), "GH");
        elegirFiltro(primera.get(), R.id.filtroVendidos);
        Bundle estado = new Bundle();
        primera.pause().saveInstanceState(estado).stop().destroy();
        ActivityController<AnimalesActivity> segunda = Robolectric.buildActivity(AnimalesActivity.class)
                .create(estado).start().restoreInstanceState(estado).resume();
        shadowOf(Looper.getMainLooper()).idle();
        esperarBusqueda();
        assertEquals("GH", textoDe(segunda.get(), R.id.buscar));
        assertTrue(((RadioButton) segunda.get().findViewById(R.id.filtroVendidos)).isChecked());
        assertEquals(1, adaptador(segunda.get()).getItemCount());
    }

    @Test public void errorDeBaseDeDatosMuestraReintentar() {
        fincaConfigurada();
        db.close();
        AnimalesActivity pantalla = abrir().get();
        assertEquals(View.VISIBLE, pantalla.findViewById(R.id.estadoError).getVisibility());
        assertEquals(pantalla.getString(R.string.error_carga), textoDe(pantalla, R.id.mensajeError));
    }

    @Test public void tocarUnaTarjetaAbreLaFicha() {
        fincaConfigurada();
        Animal animal = registrar("GH-001", "Lucero");
        AnimalesActivity pantalla = abrir().get();
        fila(pantalla, 0).itemView.performClick();
        Intent siguiente = shadowOf(pantalla).getNextStartedActivity();
        assertEquals(DetalleAnimalActivity.class.getName(), siguiente.getComponent().getClassName());
        assertEquals(animal.idAnimal, siguiente.getStringExtra(DetalleAnimalActivity.EXTRA_ID_ANIMAL));
    }
}
