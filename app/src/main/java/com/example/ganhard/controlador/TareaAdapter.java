package com.example.ganhard.controlador;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ganhard.R;
import com.example.ganhard.modelo.datos.TareaResumen;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Tiempo;
import java.util.ArrayList;
import java.util.List;

/** Presenta las tareas de un estado; al tocar una se abre su detalle. */
public final class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.Fila> {
    public interface Escucha {
        void alSeleccionar(TareaResumen tarea);
    }

    public static final class Fila extends RecyclerView.ViewHolder {
        private final TextView etiqueta, titulo, detalle, descripcion;
        Fila(View vista) {
            super(vista);
            etiqueta = vista.findViewById(R.id.itemEtiqueta);
            titulo = vista.findViewById(R.id.itemTitulo);
            detalle = vista.findViewById(R.id.itemDetalle);
            descripcion = vista.findViewById(R.id.itemDescripcion);
        }
    }

    private final Context contexto;
    private final Escucha escucha;
    private final List<TareaResumen> tareas = new ArrayList<>();

    public TareaAdapter(Context contexto, Escucha escucha) {
        this.contexto = contexto;
        this.escucha = escucha;
    }

    public void mostrar(List<TareaResumen> nuevas) {
        tareas.clear();
        tareas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_tarea, padre, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        TareaResumen resumen = tareas.get(posicion);
        Tarea tarea = resumen.tarea;
        boolean pendiente = Tarea.PENDIENTE.equals(tarea.estado);
        fila.etiqueta.setText(contexto.getString(R.string.codigo_nombre, Presentacion.tipoTarea(contexto, tarea.tipo), Presentacion.estadoTarea(contexto, tarea.estado)));
        fila.titulo.setText(tarea.titulo);
        String quien = resumen.animalCodigo == null ? contexto.getString(R.string.tarea_general_finca) : resumen.animalCodigo;
        String fecha = Tarea.COMPLETADA.equals(tarea.estado) && tarea.fechaCompletada != null
                ? contexto.getString(R.string.tarea_completada_el, FormatoFecha.corta(tarea.fechaCompletada)) : FormatoFecha.corta(tarea.fechaProgramada);
        String detalle = contexto.getString(R.string.codigo_nombre, quien, fecha);
        // ISO se ordena como texto: una fecha prevista anterior a hoy es una tarea pendiente vencida.
        if (pendiente && tarea.fechaProgramada.compareTo(Tiempo.hoy().toString()) < 0) {
            detalle = contexto.getString(R.string.codigo_nombre, detalle, contexto.getString(R.string.tarea_vencida));
        }
        fila.detalle.setText(detalle);
        String descripcion = tarea.descripcion;
        if (descripcion == null && pendiente) {
            if (Tarea.PESAJE.equals(tarea.tipo)) descripcion = contexto.getString(R.string.tarea_pista_pesaje);
            else if (Tarea.SANIDAD.equals(tarea.tipo)) descripcion = contexto.getString(R.string.tarea_pista_sanidad);
        }
        fila.descripcion.setText(descripcion);
        fila.descripcion.setVisibility(descripcion == null ? View.GONE : View.VISIBLE);
        fila.itemView.setOnClickListener(v -> escucha.alSeleccionar(resumen));
    }

    @Override public int getItemCount() { return tareas.size(); }
}
