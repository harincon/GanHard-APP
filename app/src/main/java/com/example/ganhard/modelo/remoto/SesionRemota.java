package com.example.ganhard.modelo.remoto;

/** Sesión de una cuenta: los tokens que entrega el servidor y cuándo vence el de acceso. */
public final class SesionRemota {
    /** Se renueva un poco antes de que venza para no enviar una petición con un token a punto de caducar. */
    private static final long MARGEN_SEGUNDOS = 30;

    public final String accesoToken, renovacionToken, idCuenta, correo;
    public final boolean correoConfirmado;
    /** Instante de vencimiento del token de acceso, en segundos desde 1970. */
    public final long expiraEnSegundos;

    public SesionRemota(String accesoToken, String renovacionToken, String idCuenta, String correo, boolean correoConfirmado, long expiraEnSegundos) {
        this.accesoToken = accesoToken;
        this.renovacionToken = renovacionToken;
        this.idCuenta = idCuenta;
        this.correo = correo;
        this.correoConfirmado = correoConfirmado;
        this.expiraEnSegundos = expiraEnSegundos;
    }

    public boolean caducada(long ahoraSegundos) { return ahoraSegundos + MARGEN_SEGUNDOS >= expiraEnSegundos; }

    public SesionRemota conExpiracion(long expiraEnSegundos) {
        return new SesionRemota(accesoToken, renovacionToken, idCuenta, correo, correoConfirmado, expiraEnSegundos);
    }
}
