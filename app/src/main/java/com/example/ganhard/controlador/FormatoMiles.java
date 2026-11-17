package com.example.ganhard.controlador;

/** Puntos de miles para los pesos: "320000" se ve "320.000" y de vuelta a dígitos para el Modelo. */
final class FormatoMiles {
    /** Dígitos máximos que acepta el campo de valor; el Modelo valida el mismo tope. */
    static final int MAXIMO_DIGITOS = 12;

    private FormatoMiles() {}

    /** Deja solo los dígitos de un texto. */
    static String soloDigitos(String texto) { return texto == null ? "" : texto.replaceAll("[^0-9]", ""); }

    /** "1234567" → "1.234.567"; lo que no sean solo dígitos se devuelve tal cual. */
    static String agrupar(String digitos) {
        if (digitos == null || !digitos.matches("[0-9]+")) return digitos == null ? "" : digitos;
        StringBuilder agrupado = new StringBuilder();
        int resto = digitos.length() % 3;
        for (int i = 0; i < digitos.length(); i++) {
            if (i > 0 && (i - resto) % 3 == 0) agrupado.append('.');
            agrupado.append(digitos.charAt(i));
        }
        return agrupado.toString();
    }

    /** Lo que se muestra mientras se escribe: solo dígitos, con tope de longitud y puntos de miles. */
    static String mascara(String texto) {
        String digitos = soloDigitos(texto);
        if (digitos.length() > MAXIMO_DIGITOS) digitos = digitos.substring(0, MAXIMO_DIGITOS);
        return agrupar(digitos);
    }
}
