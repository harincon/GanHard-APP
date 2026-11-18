package com.example.ganhard.controlador;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import com.example.ganhard.R;
import com.example.ganhard.databinding.DialogBorrarTodoBinding;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Cuadro de "Cerrar sesión y borrar datos". Avisa de lo que se pierde y no deja confirmar hasta escribir BORRAR; no se
 * cierra tocando afuera. Al confirmar vuelve a la pantalla de datos de la finca con la pila de pantallas vacía.
 */
final class DialogoBorrarTodo {
    private DialogoBorrarTodo() {}

    static void mostrar(BaseActivity actividad) {
        DialogBorrarTodoBinding vista = DialogBorrarTodoBinding.inflate(actividad.getLayoutInflater());
        String palabra = actividad.getString(R.string.borrar_todo_palabra);
        AlertDialog dialogo = new MaterialAlertDialogBuilder(actividad)
                .setTitle(R.string.borrar_todo_titulo)
                .setView(vista.getRoot())
                .setPositiveButton(R.string.borrar_todo_confirmar, null)
                .setNegativeButton(R.string.cancelar, null)
                .setCancelable(false)
                .create();
        dialogo.show();
        Button borrar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE), cancelar = dialogo.getButton(AlertDialog.BUTTON_NEGATIVE);
        borrar.setEnabled(false);
        vista.confirmacion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int inicio, int cuenta, int despues) {}
            @Override public void onTextChanged(CharSequence s, int inicio, int antes, int cuenta) {}
            @Override public void afterTextChanged(Editable texto) { borrar.setEnabled(palabra.equalsIgnoreCase(texto.toString().trim())); }
        });
        // El botón se conecta después de mostrar el cuadro; si no, pulsarlo lo cerraría antes de terminar.
        borrar.setOnClickListener(v -> {
            borrar.setEnabled(false);
            cancelar.setEnabled(false);
            vista.confirmacion.setEnabled(false);
            vista.mensaje.setVisibility(View.GONE);
            actividad.cuenta().cerrarTodo(new CallbackRemoto<Void>() {
                @Override public void exito(Void valor) {
                    dialogo.dismiss();
                    if (actividad.isFinishing() || actividad.isDestroyed()) return;
                    actividad.startActivity(new Intent(actividad, ConfiguracionActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    actividad.finish();
                }
                @Override public void error(ErrorRemoto error) {
                    if (actividad.isFinishing() || actividad.isDestroyed()) return;
                    cancelar.setEnabled(true);
                    vista.confirmacion.setEnabled(true);
                    borrar.setEnabled(palabra.equalsIgnoreCase(String.valueOf(vista.confirmacion.getText()).trim()));
                    vista.mensaje.setText(R.string.borrar_todo_error);
                    vista.mensaje.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
