package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityHistorialBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.datos.HistorialResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador del historial de cambios (RF16). Sin {@link #EXTRA_ID_ANIMAL} muestra todo lo ocurrido en la finca;
 * con él, solo lo del animal y sus pesajes. Es de consulta: no edita ni restaura nada.
 */
public class HistorialActivity extends BaseActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";

    private ActivityHistorialBinding vista;
    private HistorialAdapter adaptador;
    private String idAnimal;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityHistorialBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        adaptador = new HistorialAdapter(this);
        vista.lista.setLayoutManager(new LinearLayoutManager(this));
        vista.lista.setAdapter(adaptador);
        vista.reintentar.setOnClickListener(v -> cargar());
        cargar();
    }

    /** Primero la cabecera (finca o animal), que además confirma que existen; luego las razas y los cambios. */
    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        vista.estadoError.setVisibility(View.GONE);
        if (idAnimal == null) {
            repositorio().cargar(new Callback<ConfiguracionLocal>() {
                @Override public void exito(ConfiguracionLocal datos) {
                    if (!disponible()) return;
                    if (datos == null) { irAConfiguracion(); return; }
                    vista.historialCabecera.setText(datos.finca.nombre);
                    cargarRazas();
                }
                @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(error); }
            });
        } else {
            app().animales().obtener(idAnimal, new Callback<Animal>() {
                @Override public void exito(Animal animal) {
                    if (!disponible()) return;
                    vista.historialCabecera.setText(animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
                    cargarRazas();
                }
                @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(error); }
            });
        }
    }

    private void cargarRazas() {
        app().animales().razas(new Callback<List<Raza>>() {
            @Override public void exito(List<Raza> lista) {
                if (!disponible()) return;
                Map<String, String> razas = new HashMap<>();
                for (Raza raza : lista) razas.put(raza.idRaza, raza.nombre);
                cargarCambios(razas);
            }
            @Override public void error(ErrorModelo error) {
                // Sin el catálogo, un cambio de raza se muestra con su identificador; el resto no depende de él.
                if (disponible()) cargarCambios(new HashMap<>());
            }
        });
    }

    private void cargarCambios(Map<String, String> razas) {
        Callback<List<HistorialResumen>> resultado = new Callback<List<HistorialResumen>>() {
            @Override public void exito(List<HistorialResumen> cambios) {
                if (!disponible()) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                adaptador.mostrar(cambios, razas);
                boolean vacio = cambios.isEmpty();
                vista.estadoError.setVisibility(View.GONE);
                vista.lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
                vista.estadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
            }
            @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(error); }
        };
        if (idAnimal == null) app().historial().deLaFinca(resultado);
        else app().historial().delAnimal(idAnimal, resultado);
    }

    private void mostrarError(ErrorModelo error) {
        if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
            if (ErrorModelo.FINCA.equals(error.faltante)) irAConfiguracion();
            else {
                Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.lista.setVisibility(View.GONE);
        vista.estadoVacio.setVisibility(View.GONE);
        vista.mensajeError.setText(R.string.error_carga);
        vista.estadoError.setVisibility(View.VISIBLE);
    }

    private void irAConfiguracion() {
        startActivity(new Intent(this, ConfiguracionActivity.class));
        finish();
    }
}
