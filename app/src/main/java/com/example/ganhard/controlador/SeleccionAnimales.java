package com.example.ganhard.controlador;

import android.app.Activity;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.Set;

/**
 * Lista con casillas de los animales que reciben una actividad o una tarea; el botón del medio marca o quita a todos de
 * una vez. La usan los formularios de sanidad y de tareas cuando se aplican a varios animales.
 */
final class SeleccionAnimales {
    private SeleccionAnimales() {}

    /**
     * Muestra la lista; al aceptar, deja en {@code elegidos} los ids marcados y avisa. Cancelar no cambia nada.
     * {@code titulo} es el texto del encabezado del cuadro.
     */
    static void mostrar(Activity actividad, int titulo, List<AnimalResumen> animales, Set<String> elegidos, Runnable alAceptar) {
        String[] nombres = new String[animales.size()];
        boolean[] marcados = new boolean[animales.size()];
        for (int i = 0; i < nombres.length; i++) {
            AnimalResumen animal = animales.get(i);
            nombres[i] = animal.nombre == null ? animal.codigo : actividad.getString(R.string.codigo_nombre, animal.codigo, animal.nombre);
            marcados[i] = elegidos.contains(animal.idAnimal);
        }
        AlertDialog dialogo = new MaterialAlertDialogBuilder(actividad)
                .setTitle(titulo)
                .setMultiChoiceItems(nombres, marcados, (d, posicion, marcado) -> marcados[posicion] = marcado)
                .setPositiveButton(R.string.grupo_aceptar, (d, boton) -> {
                    elegidos.clear();
                    for (int i = 0; i < marcados.length; i++) if (marcados[i]) elegidos.add(animales.get(i).idAnimal);
                    alAceptar.run();
                })
                .setNegativeButton(R.string.cancelar, null)
                .setNeutralButton(R.string.grupo_quitar_todos, null)
                .create();
        dialogo.show();
        // El botón de en medio se conecta después de mostrar el cuadro; si no, pulsarlo lo cerraría.
        Button marcarOQuitar = dialogo.getButton(AlertDialog.BUTTON_NEUTRAL);
        Runnable etiquetar = () -> marcarOQuitar.setText(todosMarcados(marcados) ? R.string.grupo_quitar_todos : R.string.grupo_marcar_todos);
        etiquetar.run();
        dialogo.getListView().setOnItemClickListener((lista, fila, posicion, id) -> {
            marcados[posicion] = dialogo.getListView().isItemChecked(posicion);
            etiquetar.run();
        });
        marcarOQuitar.setOnClickListener(v -> {
            boolean todos = todosMarcados(marcados);
            for (int i = 0; i < marcados.length; i++) {
                marcados[i] = !todos;
                dialogo.getListView().setItemChecked(i, !todos);
            }
            etiquetar.run();
        });
    }

    private static boolean todosMarcados(boolean[] marcados) {
        for (boolean marcado : marcados) if (!marcado) return false;
        return true;
    }
}
