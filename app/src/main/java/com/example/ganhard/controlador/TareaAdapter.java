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
import com.example.ganhard.modelo.repositorio.ElementoTarea;
import java.util.ArrayList;
import java.util.List;

/**
 * Presenta las tareas de un estado. Al tocar una individual se abre su detalle; una tarea programada para varios animales
 * es una sola tarjeta con su total, y al tocarla se abre el detalle del grupo.
 */
public final class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.Fila> {
    public interface Escucha {
        void alSeleccionar(TareaResumen tarea);
    }

    public interface EscuchaGrupo {
        void alSeleccionarGrupo(String idGrupo);
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
    private EscuchaGrupo escuchaGrupo;
    private final List<ElementoTarea> elementos = new ArrayList<>();

    public TareaAdapter(Context contexto, Escucha escucha) {
        this.contexto = contexto;
        this.escucha = escucha;
    }

    public void alSeleccionarGrupo(EscuchaGrupo escuchaGrupo) { this.escuchaGrupo = escuchaGrupo; }

    /** Muestra solo tareas individuales, como la lista de un animal. */
    public void mostrar(List<TareaResumen> nuevas) {
        List<ElementoTarea> individuales = new ArrayList<>();
        for (TareaResumen tarea : nuevas) individuales.add(ElementoTarea.individual(tarea));
        mostrarElementos(individuales);
    }

    /** Muestra tareas individuales y tareas de grupo, cada una en su lugar. */
    public void mostrarElementos(List<ElementoTarea> nuevos) {
        elementos.clear();
        elementos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_tarea, padre, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        ElementoTarea elemento = elementos.get(posicion);
        if (elemento.esGrupo()) {
            ligarGrupo(fila, elemento);
            return;
        }
        TareaResumen resumen = elemento.tarea;
        Tarea tarea = resumen.tarea;
        boolean pendiente = Tarea.PENDIENTE.equals(tarea.estado);
        fila.etiqueta.setText(contexto.getString(R.string.codigo_nombre, Presentacion.tipoTarea(contexto, tarea), Presentacion.estadoTarea(contexto, tarea.estado)));
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

    /** "Desparasitar · 118 animales pendientes": el tipo y el estado arriba, el título y el total del grupo. */
    private void ligarGrupo(Fila fila, ElementoTarea elemento) {
        Tarea referencia = elemento.referencia;
        int plural = Tarea.COMPLETADA.equals(referencia.estado) ? R.plurals.tarea_grupo_completadas
                : Tarea.CANCELADA.equals(referencia.estado) ? R.plurals.tarea_grupo_canceladas : R.plurals.tarea_grupo_pendientes;
        fila.etiqueta.setText(contexto.getString(R.string.codigo_nombre, Presentacion.tipoTarea(contexto, referencia), Presentacion.estadoTarea(contexto, referencia.estado)));
        fila.titulo.setText(referencia.titulo);
        String total = contexto.getResources().getQuantityString(plural, elemento.cantidad, elemento.cantidad);
        String detalle = contexto.getString(R.string.codigo_nombre, total, FormatoFecha.corta(referencia.fechaProgramada));
        if (Tarea.PENDIENTE.equals(referencia.estado) && referencia.fechaProgramada.compareTo(Tiempo.hoy().toString()) < 0) {
            detalle = contexto.getString(R.string.codigo_nombre, detalle, contexto.getString(R.string.tarea_vencida));
        }
        fila.detalle.setText(detalle);
        fila.descripcion.setText(referencia.descripcion);
        fila.descripcion.setVisibility(referencia.descripcion == null ? View.GONE : View.VISIBLE);
        fila.itemView.setOnClickListener(v -> { if (escuchaGrupo != null) escuchaGrupo.alSeleccionarGrupo(elemento.idGrupo); });
    }

    @Override public int getItemCount() { return elementos.size(); }
}
