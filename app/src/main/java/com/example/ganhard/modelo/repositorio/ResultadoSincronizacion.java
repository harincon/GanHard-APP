package com.example.ganhard.modelo.repositorio;

/** Lo que dejó una sincronización completa. */
public final class ResultadoSincronizacion {
    /** Cuántos registros se subieron (las razas, que se envían completas cada vez, no cuentan). */
    public final int subidas;

    ResultadoSincronizacion(int subidas) { this.subidas = subidas; }
}
