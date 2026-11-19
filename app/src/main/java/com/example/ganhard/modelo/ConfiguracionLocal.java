package com.example.ganhard.modelo;

import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;

public final class ConfiguracionLocal {
    public final Usuario usuario;
    public final Finca finca;
    public ConfiguracionLocal(Usuario usuario, Finca finca) { this.usuario = usuario; this.finca = finca; }
    /** Quien usa este teléfono es el dueño de la finca (administrador); si no, es un encargado. */
    public boolean esAdministrador() { return usuario.idUsuario.equals(finca.idPropietario); }
}
