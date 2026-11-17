package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityTareasBinding;
import com.example.ganhard.modelo.datos.TareaResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.List;

/**
 * Controlador de la consulta de tareas (RF12): pendientes, completadas o canceladas, de la finca o, con
 * {@link #EXTRA_ID_ANIMAL}, solo las de un animal, como cuando se llega desde su ficha.
 */
public class TareasActivity extends BaseActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";

    private ActivityTareasBinding vista;
    private TareaAdapter adaptador;
    private String idAnimal;
    /** Cada consulta lleva un número; la respuesta de una consulta anterior se descarta. */
    private int peticion;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityTareasBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        adaptador = new TareaAdapter(this, tarea -> startActivity(new Intent(this, TareaDetalleActivity.class)
                .putExtra(TareaDetalleActivity.EXTRA_ID_TAREA, tarea.tarea.idTarea)));
        vista.lista.setLayoutManager(new LinearLayoutManager(this));
        vista.lista.setAdapter(adaptador);
        vista.filtros.setOnCheckedChangeListener((grupo, id) -> cargar());
        vista.programarTarea.setOnClickListener(v -> {
            Intent programar = new Intent(this, TareaFormActivity.class);
            if (idAnimal != null) programar.putExtra(TareaFormActivity.EXTRA_ID_ANIMAL, idAnimal);
            startActivity(programar);
        });
        vista.reintentar.setOnClickListener(v -> cargar());
        if (idAnimal != null) cargarCabecera();
    }

    /** Se recarga al volver para reflejar lo programado, realizado o cancelado en otras pantallas. */
    @Override protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargarCabecera() {
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                vista.tareasCabecera.setText(animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
                vista.tareasCabecera.setVisibility(View.VISIBLE);
            }
            @Override public void error(ErrorModelo error) { /* La lista informa el error; la cabecera es solo contexto. */ }
        });
    }

    private void cargar() {
        final int mia = ++peticion;
        final String estado = estadoFiltro();
        vista.progreso.setVisibility(View.VISIBLE);
        app().tareas().listar(estado, idAnimal, new Callback<List<TareaResumen>>() {
            @Override public void exito(List<TareaResumen> tareas) {
                if (!disponible() || mia != peticion) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                adaptador.mostrar(tareas);
                boolean vacia = tareas.isEmpty();
                vista.estadoError.setVisibility(View.GONE);
                vista.lista.setVisibility(vacia ? View.GONE : View.VISIBLE);
                vista.estadoVacio.setVisibility(vacia ? View.VISIBLE : View.GONE);
                if (vacia) vista.mensajeVacio.setText(mensajeVacio(estado));
            }
            @Override public void error(ErrorModelo error) { if (disponible() && mia == peticion) mostrarError(error); }
        });
    }

    private static int mensajeVacio(String estado) {
        if (Tarea.COMPLETADA.equals(estado)) return R.string.tareas_vacias_completadas;
        if (Tarea.CANCELADA.equals(estado)) return R.string.tareas_vacias_canceladas;
        return R.string.tareas_vacias_pendientes;
    }

    /** Estado elegido en el filtro; Pendientes es la opción con la que abre la pantalla. */
    private String estadoFiltro() {
        int elegido = vista.filtros.getCheckedRadioButtonId();
        if (elegido == R.id.filtroCompletadas) return Tarea.COMPLETADA;
        if (elegido == R.id.filtroCanceladas) return Tarea.CANCELADA;
        return Tarea.PENDIENTE;
    }

    private void mostrarError(ErrorModelo error) {
        if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
            if (ErrorModelo.FINCA.equals(error.faltante)) {
                startActivity(new Intent(this, ConfiguracionActivity.class));
            } else {
                Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
            }
            finish();
            return;
        }
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.lista.setVisibility(View.GONE);
        vista.estadoVacio.setVisibility(View.GONE);
        vista.mensajeError.setText(R.string.error_carga);
        vista.estadoError.setVisibility(View.VISIBLE);
    }
}
