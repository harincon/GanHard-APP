package com.example.ganhard.controlador;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.HistorialResumen;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Presenta el historial de cambios; las tarjetas son de consulta y no se pueden pulsar. */
public final class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.Fila> {
    public static final class Fila extends RecyclerView.ViewHolder {
        private final TextView etiqueta, titulo, autor, fecha, lineas;
        Fila(View vista) {
            super(vista);
            etiqueta = vista.findViewById(R.id.itemEtiqueta);
            titulo = vista.findViewById(R.id.itemTitulo);
            autor = vista.findViewById(R.id.itemAutor);
            fecha = vista.findViewById(R.id.itemFecha);
            lineas = vista.findViewById(R.id.itemLineas);
        }
    }

    private final Context contexto;
    private final List<HistorialResumen> cambios = new ArrayList<>();
    /** Nombre de cada raza por su id, para mostrar un cambio de raza con palabras. */
    private Map<String, String> razas = Collections.emptyMap();

    public HistorialAdapter(Context contexto) { this.contexto = contexto; }

    public void mostrar(List<HistorialResumen> nuevos, Map<String, String> razas) {
        this.razas = razas;
        cambios.clear();
        cambios.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_historial, padre, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        HistorialResumen resumen = cambios.get(posicion);
        fila.etiqueta.setText(TextoHistorial.etiqueta(contexto, resumen.cambio));
        fila.titulo.setText(TextoHistorial.titulo(contexto, resumen));
        fila.autor.setText(contexto.getString(R.string.responsable, resumen.autor));
        fila.fecha.setText(TextoHistorial.registro(contexto, resumen.cambio.registradoEn));
        List<String> lineas = TextoHistorial.lineas(contexto, resumen.cambio, razas);
        fila.lineas.setText(TextUtils.join("\n", lineas));
        fila.lineas.setVisibility(lineas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override public int getItemCount() { return cambios.size(); }
}
