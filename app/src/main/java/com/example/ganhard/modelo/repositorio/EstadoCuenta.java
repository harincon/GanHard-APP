package com.example.ganhard.modelo.repositorio;

/** Lo que la pantalla de cuenta necesita saber sin llamar al servidor: sesión guardada y vínculo de la finca local. */
public final class EstadoCuenta {
    /** Falso si la app se compiló sin los datos de Supabase. */
    public final boolean configurada;
    public final boolean conSesion;
    public final String correo;
    public final boolean correoConfirmado;
    public final boolean hayFincaLocal;
    public final String nombreFinca;
    public final boolean fincaVinculada;
    /** La finca local está vinculada y a la cuenta con la sesión actual: solo entonces se puede administrar. */
    public final boolean fincaDeEstaCuenta;

    public EstadoCuenta(boolean configurada, boolean conSesion, String correo, boolean correoConfirmado, boolean hayFincaLocal,
                        String nombreFinca, boolean fincaVinculada, boolean fincaDeEstaCuenta) {
        this.configurada = configurada;
        this.conSesion = conSesion;
        this.correo = correo;
        this.correoConfirmado = correoConfirmado;
        this.hayFincaLocal = hayFincaLocal;
        this.nombreFinca = nombreFinca;
        this.fincaVinculada = fincaVinculada;
        this.fincaDeEstaCuenta = fincaDeEstaCuenta;
    }
}
