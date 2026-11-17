package com.example.ganhard.modelo.remoto;

import java.util.List;

/** Lo que el dueño ve de su finca: quiénes tienen o tuvieron acceso y qué invitaciones hizo. */
public final class IntegrantesRemotos {
    public final List<IntegranteRemoto> integrantes;
    public final List<InvitacionRemota> invitaciones;

    public IntegrantesRemotos(List<IntegranteRemoto> integrantes, List<InvitacionRemota> invitaciones) {
        this.integrantes = integrantes;
        this.invitaciones = invitaciones;
    }
}
