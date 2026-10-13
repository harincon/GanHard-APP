package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.ContextThemeWrapper;
import com.example.ganhard.R;
import com.google.android.material.color.MaterialColors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Los menús desplegables y diálogos de Material usan las superficies del tema: deben ser de la paleta GanHard. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TemaMaterialTest {
    private final Context contexto = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_GanHard);

    private int color(int atributo) { return MaterialColors.getColor(contexto, atributo, 0); }

    @Test public void lasSuperficiesDeMaterialSonDeLaPaletaGanHard() {
        int superficie = contexto.getColor(R.color.gh_surface);
        assertEquals(superficie, color(com.google.android.material.R.attr.colorSurfaceContainer));
        assertEquals(superficie, color(com.google.android.material.R.attr.colorSurfaceContainerHigh));
        assertEquals(superficie, color(com.google.android.material.R.attr.colorSurfaceContainerLow));
        assertEquals(contexto.getColor(R.color.gh_soft), color(com.google.android.material.R.attr.colorSurfaceVariant));
    }
}
