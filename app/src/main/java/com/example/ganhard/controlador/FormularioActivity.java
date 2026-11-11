package com.example.ganhard.controlador;

import android.view.View;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import com.example.ganhard.R;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Map;

/**
 * Base de los formularios: muestra los errores junto a su campo, bloquea la pantalla mientras el Modelo guarda
 * y pide confirmación al salir con cambios sin guardar. Cada formulario dice cuáles son sus campos y si cambió algo.
 */
public abstract class FormularioActivity extends BaseActivity {
    /** Verdadero mientras el Modelo guarda: impide un segundo guardado y salir a medias. */
    protected boolean guardando;
    private View progreso, guardar;
    private TextView mensaje;
    private int progresoOculto;

    /** Campos del formulario por la clave con la que el Modelo nombra sus errores, en el orden de la pantalla. */
    protected abstract Map<String, TextInputLayout> campos();

    /** Indica si hay algo escrito que se perdería al salir. */
    protected abstract boolean hayCambios();

    /** Conecta las vistas comunes; la barra de progreso se oculta sin ceder su espacio. */
    protected void prepararFormulario(View progreso, View guardar, TextView mensaje) {
        prepararFormulario(progreso, guardar, mensaje, View.INVISIBLE);
    }

    /** {@code progresoOculto} es la visibilidad de la barra cuando no hay trabajo: INVISIBLE o GONE. */
    protected void prepararFormulario(View progreso, View guardar, TextView mensaje, int progresoOculto) {
        this.progreso = progreso;
        this.guardar = guardar;
        this.mensaje = mensaje;
        this.progresoOculto = progresoOculto;
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (guardando) return;
                if (!hayCambios()) { finish(); return; }
                new MaterialAlertDialogBuilder(FormularioActivity.this)
                        .setTitle(R.string.descartar_titulo).setMessage(R.string.descartar_mensaje)
                        .setPositiveButton(R.string.descartar, (dialogo, boton) -> finish())
                        .setNegativeButton(R.string.seguir_editando, null).show();
            }
        });
    }

    /** Bloquea el formulario y oculta el mensaje anterior antes de pedir el guardado al Modelo. */
    protected void empezarGuardado() {
        guardando = true;
        ocupado(true);
        ocultarMensaje();
    }

    /** Devuelve el formulario al usuario después de un guardado que falló. */
    protected void terminarGuardado() {
        guardando = false;
        ocupado(false);
    }

    /**
     * Pone cada error junto a su campo, limpia los que ya no aplican y enfoca el primero.
     * Devuelve verdadero si hay errores.
     */
    protected boolean mostrarErrores(Map<String, ErrorCampo> errores) {
        Map<String, TextInputLayout> campos = campos();
        for (Map.Entry<String, TextInputLayout> campo : campos.entrySet()) {
            ErrorCampo error = errores.get(campo.getKey());
            campo.getValue().setError(error == null ? null : MensajesError.texto(this, campo.getKey(), error));
        }
        if (errores.isEmpty()) return false;
        TextInputLayout primero = campos.get(errores.keySet().iterator().next());
        if (primero != null && primero.getEditText() != null) primero.getEditText().requestFocus();
        return true;
    }

    /** Un error del Modelo va junto a sus campos; si no es de ningún campo, se muestra el mensaje general. */
    protected void mostrarError(ErrorModelo error, int mensajeGeneral) {
        if (!mostrarErrores(error.campos)) mensaje(mensajeGeneral);
    }

    protected void ocupado(boolean ocupado) {
        progreso.setVisibility(ocupado ? View.VISIBLE : progresoOculto);
        guardar.setEnabled(!ocupado);
        for (TextInputLayout campo : campos().values()) campo.setEnabled(!ocupado);
    }

    protected void mensaje(int recurso) {
        mensaje.setText(recurso);
        mensaje.setVisibility(View.VISIBLE);
    }

    protected void ocultarMensaje() { mensaje.setVisibility(View.GONE); }

    protected static String texto(TextView campo) { return String.valueOf(campo.getText()); }
}
