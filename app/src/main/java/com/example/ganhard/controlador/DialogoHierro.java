package com.example.ganhard.controlador;

import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import com.example.ganhard.R;
import com.example.ganhard.databinding.DialogHierroBinding;
import com.example.ganhard.modelo.entidad.Hierro;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Cuadro para crear o editar un hierro del catálogo. Lo usan la pantalla de hierros y el formulario del animal
 * ("+ Nuevo hierro…"). Valida en el Modelo; los errores salen junto a su campo y el cuadro no se cierra hasta guardar.
 */
final class DialogoHierro {
    /** Qué hacer con el hierro ya guardado. */
    interface AlGuardar {
        void con(Hierro hierro);
    }

    private DialogoHierro() {}

    /** Con {@code existente} nulo crea un hierro; si no, edita ese. */
    static void mostrar(BaseActivity actividad, Hierro existente, AlGuardar alGuardar) {
        DialogHierroBinding vista = DialogHierroBinding.inflate(actividad.getLayoutInflater());
        if (existente != null) {
            vista.nombre.setText(existente.nombre);
            vista.descripcion.setText(existente.descripcion);
        }
        AlertDialog dialogo = new MaterialAlertDialogBuilder(actividad)
                .setTitle(existente == null ? R.string.hierro_nuevo_titulo : R.string.hierro_editar_titulo)
                .setView(vista.getRoot())
                .setPositiveButton(R.string.hierro_guardar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        dialogo.show();
        // El botón se conecta después de mostrar el cuadro; si no, pulsarlo lo cerraría antes de validar.
        Button guardar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
        guardar.setOnClickListener(v -> {
            guardar.setEnabled(false);
            vista.mensaje.setVisibility(android.view.View.GONE);
            Callback<Hierro> resultado = new Callback<Hierro>() {
                @Override public void exito(Hierro hierro) {
                    if (actividad.isFinishing() || actividad.isDestroyed()) return;
                    dialogo.dismiss();
                    alGuardar.con(hierro);
                }
                @Override public void error(ErrorModelo error) {
                    if (actividad.isFinishing() || actividad.isDestroyed()) return;
                    guardar.setEnabled(true);
                    vista.campoNombre.setError(textoNombre(actividad, error.campos.get(Campos.NOMBRE)));
                    vista.campoDescripcion.setError(error.campos.get(Campos.DESCRIPCION) == null ? null
                            : MensajesError.texto(actividad, error.campos.get(Campos.DESCRIPCION)));
                    if (!error.campos.isEmpty()) return;
                    vista.mensaje.setText(error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO ? R.string.hierro_no_encontrado : R.string.hierro_error_guardado);
                    vista.mensaje.setVisibility(android.view.View.VISIBLE);
                }
            };
            String nombre = String.valueOf(vista.nombre.getText()), descripcion = String.valueOf(vista.descripcion.getText());
            if (existente == null) actividad.app().hierros().crear(nombre, descripcion, resultado);
            else actividad.app().hierros().actualizar(existente.idHierro, nombre, descripcion, resultado);
        });
    }

    private static String textoNombre(BaseActivity actividad, ErrorCampo error) {
        if (error == null) return null;
        if (error.codigo == CodigoError.OBLIGATORIO) return actividad.getString(R.string.error_hierro_nombre_obligatorio);
        if (error.codigo == CodigoError.DUPLICADO) return actividad.getString(R.string.error_hierro_duplicado);
        return MensajesError.texto(actividad, error);
    }
}
