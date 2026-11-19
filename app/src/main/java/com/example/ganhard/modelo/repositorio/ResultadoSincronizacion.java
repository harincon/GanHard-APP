package com.example.ganhard.modelo.repositorio;

/** Lo que dejó una sincronización completa. */
public final class ResultadoSincronizacion {
    /** Cuántos registros se subieron (las razas, que se envían completas cada vez, no cuentan). */
    public final int subidas;
    /** Cuántos registros se bajaron del servidor (cambios hechos en otros teléfonos o la finca recuperada). */
    public final int bajadas;

    ResultadoSincronizacion(int subidas, int bajadas) {
        this.subidas = subidas;
        this.bajadas = bajadas;
    }
}
