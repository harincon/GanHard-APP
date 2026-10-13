package com.example.ganhard.controlador;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ganhard.R;
import com.example.ganhard.modelo.entidad.Pesaje;
import java.util.ArrayList;
import java.util.List;

/** Presenta el historial de pesajes; solo el más reciente se marca como vigente. */
public final class PesajeAdapter extends RecyclerView.Adapter<PesajeAdapter.Fila> {
    public static final class Fila extends RecyclerView.ViewHolder {
        private final TextView vigente, peso, fechaHora, observaciones, antecedente;
        Fila(View vista) {
            super(vista);
            vigente = vista.findViewById(R.id.itemVigente);
            peso = vista.findViewById(R.id.itemPeso);
            fechaHora = vista.findViewById(R.id.itemFechaHora);
            observaciones = vista.findViewById(R.id.itemObservaciones);
            antecedente = vista.findViewById(R.id.itemAntecedente);
        }
    }

    private final Context contexto;
    private final List<Pesaje> pesajes = new ArrayList<>();

    public PesajeAdapter(Context contexto) { this.contexto = contexto; }

    public void mostrar(List<Pesaje> nuevos) {
        pesajes.clear();
        pesajes.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_pesaje, padre, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        Pesaje pesaje = pesajes.get(posicion);
        fila.vigente.setVisibility(posicion == 0 ? View.VISIBLE : View.GONE);
        fila.peso.setText(contexto.getString(R.string.peso_formato, Presentacion.kilos(pesaje.pesoKg)));
        fila.fechaHora.setText(FormatoFecha.fechaYHora(pesaje.fecha, pesaje.hora, contexto.getString(R.string.sin_hora)));
        fila.observaciones.setText(pesaje.observaciones);
        fila.observaciones.setVisibility(pesaje.observaciones == null ? View.GONE : View.VISIBLE);
        fila.antecedente.setVisibility(pesaje.esAntecedente == 1 ? View.VISIBLE : View.GONE);
    }

    @Override public int getItemCount() { return pesajes.size(); }
}
