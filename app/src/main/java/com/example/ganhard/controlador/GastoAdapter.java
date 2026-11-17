package com.example.ganhard.controlador;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.GastoResumen;
import java.util.ArrayList;
import java.util.List;

/** Presenta los gastos de un periodo; al tocar uno se abre su corrección. */
public final class GastoAdapter extends RecyclerView.Adapter<GastoAdapter.Fila> {
    public interface Escucha {
        void alSeleccionar(GastoResumen gasto);
    }

    public static final class Fila extends RecyclerView.ViewHolder {
        private final TextView titulo, detalle, valor;
        Fila(View vista) {
            super(vista);
            titulo = vista.findViewById(R.id.itemTitulo);
            detalle = vista.findViewById(R.id.itemDetalle);
            valor = vista.findViewById(R.id.itemValor);
        }
    }

    private final Context contexto;
    private final Escucha escucha;
    private final List<GastoResumen> gastos = new ArrayList<>();

    public GastoAdapter(Context contexto, Escucha escucha) {
        this.contexto = contexto;
        this.escucha = escucha;
    }

    public void mostrar(List<GastoResumen> nuevos) {
        gastos.clear();
        gastos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_gasto, padre, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        GastoResumen resumen = gastos.get(posicion);
        fila.titulo.setText(resumen.gasto.concepto);
        String detalle = contexto.getString(R.string.codigo_nombre, FormatoFecha.diaMes(resumen.gasto.fecha),
                Presentacion.categoriaGasto(contexto, resumen.gasto.categoria));
        fila.detalle.setText(resumen.autor == null ? detalle : contexto.getString(R.string.codigo_nombre, detalle, resumen.autor));
        fila.valor.setText(Presentacion.pesos(resumen.gasto.valorCentavos));
        fila.itemView.setOnClickListener(v -> escucha.alSeleccionar(resumen));
    }

    @Override public int getItemCount() { return gastos.size(); }
}
