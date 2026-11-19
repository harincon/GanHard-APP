package com.example.ganhard.modelo.repositorio;

/** Lo que muestra la pantalla Sincronización, leído solo del teléfono. */
public final class EstadoSincronizacion {
    /** Registros nuevos o cambiados que todavía no se han subido (las razas no cuentan). */
    public final int pendientes;
    /** Cuándo terminó la última sincronización completa (instante UTC); nulo si nunca se hizo. */
    public final String ultimaSubida;

    EstadoSincronizacion(int pendientes, String ultimaSubida) {
        this.pendientes = pendientes;
        this.ultimaSubida = ultimaSubida;
    }
}
