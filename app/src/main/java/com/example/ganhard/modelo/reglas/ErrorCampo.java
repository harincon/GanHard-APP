package com.example.ganhard.modelo.reglas;

/** Error de un campo: el código y, cuando aplica, el límite que se superó (por ejemplo, la longitud máxima). */
public final class ErrorCampo {
    public final CodigoError codigo;
    public final int limite;
    public ErrorCampo(CodigoError codigo, int limite) { this.codigo = codigo; this.limite = limite; }
    public ErrorCampo(CodigoError codigo) { this(codigo, 0); }
}
