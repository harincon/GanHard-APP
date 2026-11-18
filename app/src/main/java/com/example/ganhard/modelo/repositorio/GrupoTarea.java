package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.TareaResumen;
import com.example.ganhard.modelo.entidad.Tarea;
import java.util.List;

/** Una tarea programada para varios animales: sus datos comunes y la tarea de cada animal, en cualquier estado. */
public final class GrupoTarea {
    public final String idGrupo;
    /** Una de las tareas del grupo, que da el tipo, el título, la fecha y la descripción comunes. */
    public final Tarea referencia;
    /** Las tareas del grupo, por código de animal. */
    public final List<TareaResumen> tareas;
    public final int pendientes, completadas, canceladas;

    GrupoTarea(String idGrupo, Tarea referencia, List<TareaResumen> tareas) {
        this.idGrupo = idGrupo;
        this.referencia = referencia;
        this.tareas = tareas;
        int pendientes = 0, completadas = 0, canceladas = 0;
        for (TareaResumen fila : tareas) {
            if (Tarea.PENDIENTE.equals(fila.tarea.estado)) pendientes++;
            else if (Tarea.COMPLETADA.equals(fila.tarea.estado)) completadas++;
            else canceladas++;
        }
        this.pendientes = pendientes;
        this.completadas = completadas;
        this.canceladas = canceladas;
    }
}
