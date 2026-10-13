package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;
import com.example.ganhard.R;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class FilaNavegacionTest {
    private final Context contexto = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_GanHard);

    private FilaNavegacion fila() { return new FilaNavegacion(contexto, null); }
    private String texto(FilaNavegacion fila, int id) { return ((TextView) fila.findViewById(id)).getText().toString(); }

    @Test public void muestraTituloYSubtitulo() {
        FilaNavegacion fila = fila();
        fila.setTitulo("Animales");
        fila.setSubtitulo("Inventario, fichas y pesajes");
        assertEquals("Animales", texto(fila, R.id.filaTitulo));
        assertEquals("Inventario, fichas y pesajes", texto(fila, R.id.filaSubtitulo));
        assertEquals(View.VISIBLE, fila.findViewById(R.id.filaSubtitulo).getVisibility());
    }

    @Test public void sinSubtituloLoOcultaYConSubtituloVuelveAMostrarlo() {
        FilaNavegacion fila = fila();
        fila.setTitulo("Animales");
        fila.setSubtitulo(null);
        assertEquals(View.GONE, fila.findViewById(R.id.filaSubtitulo).getVisibility());
        fila.setSubtitulo("Otra vez");
        assertEquals(View.VISIBLE, fila.findViewById(R.id.filaSubtitulo).getVisibility());
    }

    @Test public void habilitadaEjecutaElClic() {
        FilaNavegacion fila = fila();
        AtomicInteger clics = new AtomicInteger();
        fila.setOnClickListener(v -> clics.incrementAndGet());
        assertTrue(fila.performClick());
        assertEquals(1, clics.get());
    }

    @Test public void deshabilitadaNoResponderAlClicYQuedaAtenuada() {
        FilaNavegacion fila = fila();
        AtomicInteger clics = new AtomicInteger();
        fila.setOnClickListener(v -> clics.incrementAndGet());
        fila.setHabilitada(false);
        assertFalse(fila.performClick());
        assertEquals(0, clics.get());
        assertFalse(fila.isEnabled());
        assertTrue(fila.getAlpha() < 1f);
        fila.setHabilitada(true);
        assertTrue(fila.performClick());
        assertEquals(1f, fila.getAlpha(), 0.001f);
    }

    @Test public void elTituloYElSubtituloUsanLosTamanosDelDiseno() {
        FilaNavegacion fila = fila();
        android.util.DisplayMetrics metricas = contexto.getResources().getDisplayMetrics();
        assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 14, metricas),
                ((TextView) fila.findViewById(R.id.filaTitulo)).getTextSize(), 0.5f);
        assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 12, metricas),
                ((TextView) fila.findViewById(R.id.filaSubtitulo)).getTextSize(), 0.5f);
    }
}
