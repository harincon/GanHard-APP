package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.GastoResumen;
import java.util.List;

/** Gastos vigentes de un periodo y su total en centavos. */
public final class PeriodoGastos {
    public final long totalCentavos;
    public final List<GastoResumen> gastos;

    public PeriodoGastos(long totalCentavos, List<GastoResumen> gastos) {
        this.totalCentavos = totalCentavos;
        this.gastos = gastos;
    }
}
