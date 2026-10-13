package com.example.ganhard.controlador;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.AnimalResumen;
import java.util.ArrayList;
import java.util.List;

/** Presenta la lista de animales; la lista es corta y se reemplaza completa en cada consulta. */
public final class AnimalAdapter extends RecyclerView.Adapter<AnimalAdapter.Fila> {
    public interface Escucha {
        void alSeleccionar(AnimalResumen animal);
    }

    public static final class Fila extends RecyclerView.ViewHolder {
        private final TextView estado, titulo, detalle, peso;
        Fila(View vista) {
            super(vista);
            estado = vista.findViewById(R.id.itemEstado);
            titulo = vista.findViewById(R.id.itemTitulo);
            detalle = vista.findViewById(R.id.itemDetalle);
            peso = vista.findViewById(R.id.itemPeso);
        }
    }

    private final Context contexto;
    private final Escucha escucha;
    private final List<AnimalResumen> animales = new ArrayList<>();

    public AnimalAdapter(Context contexto, Escucha escucha) {
        this.contexto = contexto;
        this.escucha = escucha;
    }

    public void mostrar(List<AnimalResumen> nuevos) {
        animales.clear();
        animales.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_animal, padre, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        AnimalResumen animal = animales.get(posicion);
        fila.estado.setText(Presentacion.estado(contexto, animal.estado));
        fila.titulo.setText(animal.nombre == null ? animal.codigo : contexto.getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
        String raza = animal.razaNombre == null ? contexto.getString(R.string.raza_sin_registrar) : animal.razaNombre;
        fila.detalle.setText(contexto.getString(R.string.sexo_y_raza, Presentacion.sexo(contexto, animal.sexo), raza));
        fila.peso.setText(Presentacion.ultimoPesaje(contexto, animal.ultimoPesoKg, animal.ultimoPesoFecha));
        fila.itemView.setOnClickListener(v -> escucha.alSeleccionar(animal));
    }

    @Override public int getItemCount() { return animales.size(); }
}
