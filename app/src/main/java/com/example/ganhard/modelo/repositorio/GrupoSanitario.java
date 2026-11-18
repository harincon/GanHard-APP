package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.EventoSanitarioResumen;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import java.util.List;

/** Una actividad sanitaria aplicada a varios animales: sus datos comunes y la actividad vigente de cada animal. */
public final class GrupoSanitario {
    public final String idGrupo;
    /** Una de las actividades vigentes, que da el tipo, la fecha, el producto y la descripción del grupo. */
    public final EventoSanitario referencia;
    /** Las actividades vigentes del grupo, por código de animal. */
    public final List<EventoSanitarioResumen> actividades;

    GrupoSanitario(String idGrupo, EventoSanitario referencia, List<EventoSanitarioResumen> actividades) {
        this.idGrupo = idGrupo;
        this.referencia = referencia;
        this.actividades = actividades;
    }
}
