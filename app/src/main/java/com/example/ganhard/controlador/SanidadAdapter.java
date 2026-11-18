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
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.repositorio.ElementoSanidad;
import java.util.ArrayList;
import java.util.List;

/**
 * Presenta las actividades sanitarias. Al tocar una individual se abre su corrección; una aplicación a varios
 * animales es una sola tarjeta con su total, y al tocarla se abre el detalle del grupo.
 */
public final class SanidadAdapter extends RecyclerView.Adapter<SanidadAdapter.Fila> {
    public interface Escucha {
        void alSeleccionar(EventoSanitarioResumen actividad);
    }

    public interface EscuchaGrupo {
        void alSeleccionarGrupo(String idGrupo);
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
    private EscuchaGrupo escuchaGrupo;
    private final List<ElementoSanidad> elementos = new ArrayList<>();
    /** Con la lista de todos los animales cada tarjeta dice de cuál es; con uno elegido sobra. */
    private boolean conAnimal;

    public SanidadAdapter(Context contexto, Escucha escucha) {
        this.contexto = contexto;
        this.escucha = escucha;
    }

    public void alSeleccionarGrupo(EscuchaGrupo escuchaGrupo) { this.escuchaGrupo = escuchaGrupo; }

    /** Muestra solo actividades individuales, como la lista de un animal. */
    public void mostrar(List<EventoSanitarioResumen> nuevas, boolean conAnimal) {
        List<ElementoSanidad> individuales = new ArrayList<>();
        for (EventoSanitarioResumen actividad : nuevas) individuales.add(ElementoSanidad.individual(actividad));
        mostrarElementos(individuales, conAnimal);
    }

    /** Muestra actividades individuales y aplicaciones en grupo, cada una en su lugar. */
    public void mostrarElementos(List<ElementoSanidad> nuevos, boolean conAnimal) {
        this.conAnimal = conAnimal;
        elementos.clear();
        elementos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_sanidad, padre, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        ElementoSanidad elemento = elementos.get(posicion);
        if (elemento.esGrupo()) {
            EventoSanitario referencia = elemento.referencia;
            fila.animal.setText(contexto.getResources().getQuantityString(R.plurals.grupo_aplicada_a, elemento.cantidad, elemento.cantidad));
            fila.animal.setVisibility(View.VISIBLE);
            fila.titulo.setText(Presentacion.tipoSanidad(contexto, referencia.tipo));
            fila.detalle.setText(FormatoFecha.corta(referencia.fecha));
            fila.descripcion.setText(referencia.descripcion);
            fila.itemView.setOnClickListener(v -> { if (escuchaGrupo != null) escuchaGrupo.alSeleccionarGrupo(elemento.idGrupo); });
            return;
        }
        EventoSanitarioResumen actividad = elemento.actividad;
        fila.animal.setText(actividad.animalNombre == null ? actividad.animalCodigo
                : contexto.getString(R.string.codigo_nombre, actividad.animalCodigo, actividad.animalNombre));
        fila.animal.setVisibility(conAnimal ? View.VISIBLE : View.GONE);
        fila.titulo.setText(Presentacion.tipoSanidad(contexto, actividad.evento.tipo));
        String fecha = FormatoFecha.corta(actividad.evento.fecha);
        fila.detalle.setText(actividad.autor == null ? fecha : contexto.getString(R.string.codigo_nombre, fecha, actividad.autor));
        fila.descripcion.setText(actividad.evento.descripcion);
        fila.itemView.setOnClickListener(v -> escucha.alSeleccionar(actividad));
    }

    @Override public int getItemCount() { return elementos.size(); }
}
