package com.example.ganhard.controlador;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MensajesErrorTest {
    private final Context contexto = RuntimeEnvironment.getApplication();

    @Test public void cadaCodigoTieneTextoNoVacio() {
        for (CodigoError codigo : CodigoError.values()) {
            String texto = MensajesError.texto(contexto, new ErrorCampo(codigo, 100));
            assertFalse(codigo.name(), texto.trim().isEmpty());
        }
    }

    @Test public void laLongitudMaximaIncluyeElLimite() {
        assertTrue(MensajesError.texto(contexto, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, 150)).contains("150"));
    }
}
