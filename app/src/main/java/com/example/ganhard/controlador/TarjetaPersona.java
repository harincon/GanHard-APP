package com.example.ganhard.controlador;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.ganhard.databinding.ItemPersonaBinding;

/** Tarjeta de una persona, una invitación o una finca de la cuenta; los textos vacíos no ocupan espacio. */
final class TarjetaPersona {
    private TarjetaPersona() {}

    /** Agrega la tarjeta al final de la lista y la devuelve para que quien la pide ponga su botón de acción. */
    static ItemPersonaBinding agregar(Activity actividad, LinearLayout lista, String etiqueta, String titulo, String detalle, String nota) {
        ItemPersonaBinding tarjeta = ItemPersonaBinding.inflate(actividad.getLayoutInflater(), lista, false);
        poner(tarjeta.itemEtiqueta, etiqueta);
        poner(tarjeta.itemTitulo, titulo);
        poner(tarjeta.itemDetalle, detalle);
        poner(tarjeta.itemNota, nota);
        lista.addView(tarjeta.getRoot());
        return tarjeta;
    }

    static void accion(ItemPersonaBinding tarjeta, int texto, View.OnClickListener alPulsar) {
        tarjeta.itemAccion.setText(texto);
        tarjeta.itemAccion.setVisibility(View.VISIBLE);
        tarjeta.itemAccion.setOnClickListener(alPulsar);
    }

    private static void poner(TextView vista, String texto) {
        vista.setText(texto);
        vista.setVisibility(texto == null || texto.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
