package com.example.ganhard.controlador;

import android.content.Context;
import com.example.ganhard.R;
import com.example.ganhard.modelo.entidad.Animal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Textos de presentación compartidos por las pantallas de animales; todos salen de recursos. */
final class Presentacion {
    private Presentacion() {}

    static String estado(Context contexto, String estado) {
        if (Animal.VENDIDO.equals(estado)) return contexto.getString(R.string.estado_vendido);
        if (Animal.FALLECIDO.equals(estado)) return contexto.getString(R.string.estado_fallecido);
        return contexto.getString(R.string.estado_activo);
    }

    static String sexo(Context contexto, String sexo) {
        return contexto.getString(Animal.MACHO.equals(sexo) ? R.string.sexo_macho : R.string.sexo_hembra);
    }

    /** Número sin ceros sobrantes y con punto decimal, el mismo formato que acepta el formulario. */
    static String kilos(Double peso) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(peso);
    }

    static String ultimoPesaje(Context contexto, Double peso, String fecha) {
        return peso == null ? contexto.getString(R.string.sin_pesajes)
                : contexto.getString(R.string.ultimo_pesaje_lista, kilos(peso), FormatoFecha.corta(fecha));
    }

    static String ultimoPeso(Context contexto, Double peso) {
        return peso == null ? contexto.getString(R.string.sin_pesajes) : contexto.getString(R.string.ultimo_peso_formato, kilos(peso));
    }
}
