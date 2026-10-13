package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityAnimalesBinding;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.List;

/** Controlador de la consulta de animales (RF04): lista con búsqueda por código o nombre y filtro por estado. */
public class AnimalesActivity extends BaseActivity {
    private static final long RETARDO_BUSQUEDA_MS = 300;

    private ActivityAnimalesBinding vista;
    private AnimalAdapter adaptador;
    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final Runnable consulta = this::consultar;
    /** Cada consulta lleva un número; una respuesta de una consulta anterior se descarta. */
    private int peticion;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityAnimalesBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        adaptador = new AnimalAdapter(this, animal -> { /* La ficha se conecta en la tarea 10. */ });
        vista.lista.setLayoutManager(new LinearLayoutManager(this));
        vista.lista.setAdapter(adaptador);
        vista.buscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int inicio, int cuenta, int despues) {}
            @Override public void onTextChanged(CharSequence s, int inicio, int antes, int cuenta) {}
            @Override public void afterTextChanged(Editable s) { programarConsulta(); }
        });
        vista.filtros.setOnCheckedStateChangeListener((grupo, ids) -> consultar());
        vista.agregar.setOnClickListener(v -> registrarAnimal());
        vista.accionVacio.setOnClickListener(v -> { if (hayFiltros()) limpiarFiltros(); else registrarAnimal(); });
        vista.reintentar.setOnClickListener(v -> consultar());
    }

    @Override protected void onResume() {
        super.onResume();
        consultar();
    }

    @Override protected void onDestroy() {
        manejador.removeCallbacks(consulta);
        super.onDestroy();
    }

    private void programarConsulta() {
        manejador.removeCallbacks(consulta);
        manejador.postDelayed(consulta, RETARDO_BUSQUEDA_MS);
    }

    private void consultar() {
        manejador.removeCallbacks(consulta);
        final int mia = ++peticion;
        vista.progreso.setVisibility(View.VISIBLE);
        app().animales().buscar(textoBusqueda(), estadoFiltro(), new Callback<List<AnimalResumen>>() {
            @Override public void exito(List<AnimalResumen> animales) {
                if (!disponible() || mia != peticion) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                adaptador.mostrar(animales);
                boolean vacia = animales.isEmpty();
                vista.estadoError.setVisibility(View.GONE);
                vista.lista.setVisibility(vacia ? View.GONE : View.VISIBLE);
                vista.estadoVacio.setVisibility(vacia ? View.VISIBLE : View.GONE);
                if (vacia) {
                    boolean filtrando = hayFiltros();
                    vista.mensajeVacio.setText(filtrando ? R.string.sin_coincidencias : R.string.sin_animales);
                    vista.accionVacio.setText(filtrando ? R.string.limpiar_busqueda : R.string.registrar_animal);
                }
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible() || mia != peticion) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    startActivity(new Intent(AnimalesActivity.this, ConfiguracionActivity.class));
                    finish();
                    return;
                }
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.lista.setVisibility(View.GONE);
                vista.estadoVacio.setVisibility(View.GONE);
                vista.mensajeError.setText(R.string.error_carga);
                vista.estadoError.setVisibility(View.VISIBLE);
            }
        });
    }

    private String textoBusqueda() { return String.valueOf(vista.buscar.getText()).trim(); }

    /** Estado elegido en los filtros; null significa todos. */
    private String estadoFiltro() {
        int elegido = vista.filtros.getCheckedChipId();
        if (elegido == R.id.chipActivos) return Animal.ACTIVO;
        if (elegido == R.id.chipVendidos) return Animal.VENDIDO;
        if (elegido == R.id.chipFallecidos) return Animal.FALLECIDO;
        return null;
    }

    private boolean hayFiltros() { return !textoBusqueda().isEmpty() || estadoFiltro() != null; }

    private void limpiarFiltros() {
        vista.buscar.setText("");
        vista.chipTodos.setChecked(true);
        consultar();
    }

    private void registrarAnimal() { startActivity(new Intent(this, AnimalFormActivity.class)); }
}
