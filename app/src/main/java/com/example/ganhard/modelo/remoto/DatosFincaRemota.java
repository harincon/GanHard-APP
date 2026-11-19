package com.example.ganhard.modelo.remoto;

import java.util.List;

/** La finca registrada en el servidor y las personas que la usan; con esto se arma la finca local en un teléfono vacío. */
public final class DatosFincaRemota {
    public final FincaRemota finca;
    /** Id de la cuenta de la dueña o del dueño. */
    public final String idPropietario;
    public final String creadoEn;
    public final List<PersonaRemota> personas;

    public DatosFincaRemota(FincaRemota finca, String idPropietario, String creadoEn, List<PersonaRemota> personas) {
        this.finca = finca;
        this.idPropietario = idPropietario;
        this.creadoEn = creadoEn;
        this.personas = personas;
    }
}
