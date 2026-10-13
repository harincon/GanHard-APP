package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityDetalleAnimalBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.List;

/**
 * Controlador de la ficha del animal (RF05): cabecera con sus datos y estado, tarjeta del último peso y accesos
 * a registrar un pesaje (RF08), a su historial (RF09) y a editar sus datos y estado (RF06, RF07).
 */
public class DetalleAnimalActivity extends BaseActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";

    private ActivityDetalleAnimalBinding vista;
    private String idAnimal;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityDetalleAnimalBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        vista.reintentar.setOnClickListener(v -> cargar());
        vista.registrarPesaje.setOnClickListener(v -> abrir(PesajeFormActivity.class));
        vista.filaHistorial.setOnClickListener(v -> abrir(PesajesActivity.class));
        vista.filaEditar.setOnClickListener(v -> abrir(AnimalFormActivity.class));
        vista.filaSanidad.setHabilitada(false);
        vista.filaTareas.setHabilitada(false);
        if (idAnimal == null) noEncontrado();
    }

    /** Se recarga al volver para reflejar lo que se haya cambiado desde otras pantallas. */
    @Override protected void onResume() {
        super.onResume();
        if (idAnimal != null) cargar();
    }

    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        vista.estadoError.setVisibility(View.GONE);
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (disponible()) cargarRaza(animal);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
                mostrarError();
            }
        });
    }

    private void cargarRaza(Animal animal) {
        if (animal.idRaza == null) { cargarPesajes(animal, null); return; }
        app().animales().razas(new Callback<List<Raza>>() {
            @Override public void exito(List<Raza> razas) {
                if (!disponible()) return;
                String nombre = null;
                for (Raza raza : razas) if (raza.idRaza.equals(animal.idRaza)) nombre = raza.nombre;
                cargarPesajes(animal, nombre);
            }
            @Override public void error(ErrorModelo error) {
                if (disponible()) mostrarError();
            }
        });
    }

    /** El historial vigente da, con una sola consulta, el último peso y la cantidad de mediciones. */
    private void cargarPesajes(Animal animal, String razaNombre) {
        app().pesajes().historial(idAnimal, new Callback<List<Pesaje>>() {
            @Override public void exito(List<Pesaje> pesajes) {
                if (disponible()) mostrar(animal, razaNombre, pesajes);
            }
            @Override public void error(ErrorModelo error) {
                if (disponible()) mostrarError();
            }
        });
    }

    private void mostrar(Animal animal, String razaNombre, List<Pesaje> pesajes) {
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.estadoError.setVisibility(View.GONE);
        vista.contenido.setVisibility(View.VISIBLE);
        String sinRegistrar = getString(R.string.sin_registrar);
        vista.fichaEstado.setText(Presentacion.estado(this, animal.estado));
        vista.fichaTitulo.setText(animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
        vista.fichaRaza.setText(getString(R.string.sexo_y_raza, Presentacion.sexo(this, animal.sexo),
                razaNombre == null ? getString(R.string.raza_sin_registrar) : razaNombre));
        vista.fichaIngreso.setText(getString(R.string.ficha_ingreso, FormatoFecha.corta(animal.fechaIngreso)));
        vista.fichaNacimiento.setText(getString(R.string.ficha_nacimiento,
                animal.fechaNacimiento == null ? sinRegistrar : FormatoFecha.corta(animal.fechaNacimiento)));
        boolean activo = Animal.ACTIVO.equals(animal.estado);
        vista.fichaEstadoDesde.setVisibility(activo ? View.GONE : View.VISIBLE);
        if (!activo) vista.fichaEstadoDesde.setText(getString(R.string.estado_desde, Presentacion.estado(this, animal.estado), FormatoFecha.corta(animal.fechaEstado)));
        vista.fichaObservaciones.setVisibility(animal.observaciones == null ? View.GONE : View.VISIBLE);
        if (animal.observaciones != null) vista.fichaObservaciones.setText(getString(R.string.ficha_observaciones, animal.observaciones));

        if (pesajes.isEmpty()) {
            vista.pesoKilos.setText(R.string.sin_pesajes);
            vista.pesoDetalle.setVisibility(View.GONE);
            vista.filaHistorial.setSubtitulo(getString(R.string.sin_mediciones));
        } else {
            Pesaje ultimo = pesajes.get(0);
            vista.pesoKilos.setText(getString(R.string.peso_formato, Presentacion.kilos(ultimo.pesoKg)));
            vista.pesoDetalle.setText(getString(R.string.ultimo_pesaje_ficha, FormatoFecha.fechaYHoraConComa(ultimo.fecha, ultimo.hora)));
            vista.pesoDetalle.setVisibility(View.VISIBLE);
            vista.filaHistorial.setSubtitulo(getResources().getQuantityString(R.plurals.mediciones_vigentes, pesajes.size(), pesajes.size()));
        }
        vista.toolbar.setTitle(R.string.ficha_animal);
    }

    private void abrir(Class<?> pantalla) {
        startActivity(new Intent(this, pantalla).putExtra(EXTRA_ID_ANIMAL, idAnimal));
    }

    private void mostrarError() {
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.contenido.setVisibility(View.GONE);
        vista.mensajeError.setText(R.string.error_carga);
        vista.estadoError.setVisibility(View.VISIBLE);
    }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
