package com.example.ganhard.controlador;

import android.text.Editable;
import android.text.TextWatcher;

/** Inserta las barras de dd/mm/aaaa mientras se escriben solo dígitos; ver {@link FormatoFecha#mascara}. */
final class MascaraFecha implements TextWatcher {
    @Override public void beforeTextChanged(CharSequence s, int inicio, int cuenta, int despues) {}
    @Override public void onTextChanged(CharSequence s, int inicio, int antes, int cuenta) {}

    @Override public void afterTextChanged(Editable texto) {
        String formateado = FormatoFecha.mascara(texto.toString());
        if (!formateado.equals(texto.toString())) texto.replace(0, texto.length(), formateado);
    }
}
