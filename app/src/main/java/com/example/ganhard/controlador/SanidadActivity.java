package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivitySanidadBinding;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.datos.EventoSanitarioResumen;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la consulta de sanidad (RF11): actividades realizadas de todos los animales o de uno elegido en el
 * selector. Con {@link #EXTRA_ID_ANIMAL} abre con ese animal elegido, como cuando se llega desde su ficha.
 */
public class SanidadActivity extends BaseActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    private static final String ESTADO_ANIMAL = "animal_elegido";

    private ActivitySanidadBinding vista;
    private SanidadAdapter adaptador;
    private final List<AnimalResumen> animales = new ArrayList<>();
    /** Animal elegido en el selector; null significa todos los animales. */
    private String idAnimal;
    /** Cada consulta lleva un número; la respuesta de una consulta anterior se descarta. */
    private int peticion;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivitySanidadBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idAnimal = estado != null ? estado.getString(ESTADO_ANIMAL) : getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        adaptador = new SanidadAdapter(this, actividad -> startActivity(new Intent(this, SanidadFormActivity.class)
                .putExtra(SanidadFormActivity.EXTRA_ID_EVENTO, actividad.evento.idEventoSanitario)));
        vista.lista.setLayoutManager(new LinearLayoutManager(this));
        vista.lista.setAdapter(adaptador);
        vista.filtroAnimal.setOnItemClickListener((lista, fila, posicion, id) -> {
            idAnimal = posicion == 0 ? null : animales.get(posicion - 1).idAnimal;
            cargarActividades();
        });
        vista.registrarActividad.setOnClickListener(v -> {
            Intent registro = new Intent(this, SanidadFormActivity.class);
            if (idAnimal != null) registro.putExtra(SanidadFormActivity.EXTRA_ID_ANIMAL, idAnimal);
            startActivity(registro);
        });
        vista.reintentar.setOnClickListener(v -> cargar());
        vista.filaProgramar.setHabilitada(false);
    }

    /** Se recarga al volver para reflejar lo registrado, corregido o anulado en otras pantallas. */
    @Override protected void onResume() {
        super.onResume();
        cargar();
    }

    /** Primero los animales del selector (que además confirman que hay finca) y luego las actividades. */
    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        app().animales().buscar("", null, new Callback<List<AnimalResumen>>() {
            @Override public void exito(List<AnimalResumen> lista) {
                if (!disponible()) return;
                animales.clear();
                animales.addAll(lista);
                List<String> opciones = new ArrayList<>();
                opciones.add(getString(R.string.sanidad_todos));
                String elegido = opciones.get(0);
                boolean existe = false;
                for (AnimalResumen animal : animales) {
                    String nombre = animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre);
                    opciones.add(nombre);
                    if (animal.idAnimal.equals(idAnimal)) { elegido = nombre; existe = true; }
                }
                if (!existe) idAnimal = null;
                vista.filtroAnimal.setAdapter(new ArrayAdapter<>(SanidadActivity.this, android.R.layout.simple_dropdown_item_1line, opciones));
                vista.filtroAnimal.setText(elegido, false);
                cargarActividades();
            }
            @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(error); }
        });
    }

    private void cargarActividades() {
        final int mia = ++peticion;
        final boolean todos = idAnimal == null;
        vista.progreso.setVisibility(View.VISIBLE);
        Callback<List<EventoSanitarioResumen>> resultado = new Callback<List<EventoSanitarioResumen>>() {
            @Override public void exito(List<EventoSanitarioResumen> actividades) {
                if (!disponible() || mia != peticion) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                adaptador.mostrar(actividades, todos);
                boolean vacia = actividades.isEmpty();
                vista.estadoError.setVisibility(View.GONE);
                vista.lista.setVisibility(vacia ? View.GONE : View.VISIBLE);
                vista.estadoVacio.setVisibility(vacia ? View.VISIBLE : View.GONE);
            }
            @Override public void error(ErrorModelo error) { if (disponible() && mia == peticion) mostrarError(error); }
        };
        if (todos) app().sanidad().deLaFinca(resultado);
        else app().sanidad().delAnimal(idAnimal, resultado);
    }

    private void mostrarError(ErrorModelo error) {
        if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO && ErrorModelo.FINCA.equals(error.faltante)) {
            startActivity(new Intent(this, ConfiguracionActivity.class));
            finish();
            return;
        }
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.lista.setVisibility(View.GONE);
        vista.estadoVacio.setVisibility(View.GONE);
        vista.mensajeError.setText(R.string.error_carga);
        vista.estadoError.setVisibility(View.VISIBLE);
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_ANIMAL, idAnimal);
        super.onSaveInstanceState(estado);
    }
}
