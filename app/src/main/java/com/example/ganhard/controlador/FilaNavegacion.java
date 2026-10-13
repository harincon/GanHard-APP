package com.example.ganhard.controlador;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.ganhard.R;
import com.google.android.material.card.MaterialCardView;

/** Fila del diseño: tarjeta con título, subtítulo opcional y una flecha ›. Deshabilitada se ve atenuada y no responde. */
public class FilaNavegacion extends MaterialCardView {
    private static final float ALFA_DESHABILITADA = 0.5f;
    private final TextView titulo, subtitulo;
    private boolean habilitada = true;

    public FilaNavegacion(Context contexto, @Nullable AttributeSet atributos) {
        super(contexto, atributos);
        setRadius(contexto.getResources().getDimension(R.dimen.gh_radio_fila));
        setCardElevation(0f);
        setStrokeWidth(0);
        setCardBackgroundColor(ContextCompat.getColor(contexto, R.color.gh_surface));
        setClickable(true);
        setFocusable(true);
        LayoutInflater.from(contexto).inflate(R.layout.view_fila_navegacion, this, true);
        titulo = findViewById(R.id.filaTitulo);
        subtitulo = findViewById(R.id.filaSubtitulo);
        if (atributos != null) {
            TypedArray datos = contexto.obtainStyledAttributes(atributos, R.styleable.FilaNavegacion);
            try {
                setTitulo(datos.getText(R.styleable.FilaNavegacion_titulo));
                setSubtitulo(datos.getText(R.styleable.FilaNavegacion_subtitulo));
            } finally {
                datos.recycle();
            }
        } else {
            setSubtitulo(null);
        }
    }

    public void setTitulo(CharSequence texto) { titulo.setText(texto); }

    public void setSubtitulo(CharSequence texto) {
        subtitulo.setText(texto);
        subtitulo.setVisibility(texto == null || texto.length() == 0 ? View.GONE : View.VISIBLE);
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
        setEnabled(habilitada);
        setClickable(habilitada);
        setAlpha(habilitada ? 1f : ALFA_DESHABILITADA);
    }

    @Override public boolean performClick() { return habilitada && super.performClick(); }
}
