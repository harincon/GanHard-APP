package com.example.ganhard.controlador;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.EventoSanitarioResumen;
import java.util.ArrayList;
import java.util.List;

/** Presenta las actividades sanitarias; al tocar una se abre su corrección. */
public final class SanidadAdapter extends RecyclerView.Adapter<SanidadAdapter.Fila> {
    public interface Escucha {
        void alSeleccionar(EventoSanitarioResumen actividad);
    }

    public static final class Fila extends RecyclerView.ViewHolder {
        private final TextView animal, titulo, detalle, descripcion;
        Fila(View vista) {
            super(vista);
            animal = vista.findViewById(R.id.itemAnimal);
            titulo = vista.findViewById(R.id.itemTitulo);
            detalle = vista.findViewById(R.id.itemDetalle);
            descripcion = vista.findViewById(R.id.itemDescripcion);
        }
    }

    private final Context contexto;
    private final Escucha escucha;
    private final List<EventoSanitarioResumen> actividades = new ArrayList<>();
    /** Con la lista de todos los animales cada tarjeta dice de cuál es; con uno elegido sobra. */
    private boolean conAnimal;

    public SanidadAdapter(Context contexto, Escucha escucha) {
        this.contexto = contexto;
        this.escucha = escucha;
    }

    public void mostrar(List<EventoSanitarioResumen> nuevas, boolean conAnimal) {
        this.conAnimal = conAnimal;
        actividades.clear();
        actividades.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_sanidad, padre, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        EventoSanitarioResumen actividad = actividades.get(posicion);
        fila.animal.setText(actividad.animalNombre == null ? actividad.animalCodigo
                : contexto.getString(R.string.codigo_nombre, actividad.animalCodigo, actividad.animalNombre));
        fila.animal.setVisibility(conAnimal ? View.VISIBLE : View.GONE);
        fila.titulo.setText(Presentacion.tipoSanidad(contexto, actividad.evento.tipo));
        String fecha = FormatoFecha.corta(actividad.evento.fecha);
        fila.detalle.setText(actividad.autor == null ? fecha : contexto.getString(R.string.codigo_nombre, fecha, actividad.autor));
        fila.descripcion.setText(actividad.evento.descripcion);
        fila.itemView.setOnClickListener(v -> escucha.alSeleccionar(actividad));
    }

    @Override public int getItemCount() { return actividades.size(); }
}
