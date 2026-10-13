package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.PreferenciaTema;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class TemaConfiguracionTest {
    private GanHardDatabase db;
    private final Context contexto = RuntimeEnvironment.getApplication();

    @Before public void preparar() {
        db = Room.inMemoryDatabaseBuilder(contexto, GanHardDatabase.class).allowMainThreadQueries().build();
        ((GanHardApplication) contexto).instalar(db, Runnable::run);
    }
    @After public void limpiar() {
        db.close();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    @Test public void elegirOscuroPersisteYAplica() {
        ConfiguracionActivity pantalla = Robolectric.buildActivity(ConfiguracionActivity.class).create().start().resume().get();
        shadowOf(Looper.getMainLooper()).idle();
        ((RadioGroup) pantalla.findViewById(R.id.temas)).check(R.id.temaOscuro);
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, PreferenciaTema.leer(contexto));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode());
    }

    @Test public void seleccionInicialReflejaLoGuardado() {
        PreferenciaTema.guardar(contexto, AppCompatDelegate.MODE_NIGHT_NO);
        ConfiguracionActivity pantalla = Robolectric.buildActivity(ConfiguracionActivity.class).create().start().resume().get();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(R.id.temaClaro, ((RadioGroup) pantalla.findViewById(R.id.temas)).getCheckedRadioButtonId());
    }

    @Test public void sinPreferenciaSigueElSistema() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, PreferenciaTema.leer(contexto));
    }
}
