package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityDetalleAnimalBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

/**
 * Controlador de la ficha del animal (RF05): muestra sus datos, su raza, su estado y su último pesaje, y
 * lleva a editarlo (RF06), cambiar su estado (RF07) y a sus pesajes (RF08, RF09).
 */
public class DetalleAnimalActivity extends BaseActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";

    private ActivityDetalleAnimalBinding vista;
    private String idAnimal;
    private Animal actual;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityDetalleAnimalBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        vista.reintentar.setOnClickListener(v -> cargar());
        vista.registrarPesaje.setOnClickListener(v -> abrir(PesajeFormActivity.class));
        vista.verPesos.setOnClickListener(v -> abrir(PesajesActivity.class));
        vista.editar.setOnClickListener(v -> abrir(AnimalFormActivity.class));
        vista.cambiarEstado.setOnClickListener(v -> cambiarEstado());
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
        if (animal.idRaza == null) { mostrar(animal, null); return; }
        app().animales().razas(new Callback<List<Raza>>() {
            @Override public void exito(List<Raza> razas) {
                if (!disponible()) return;
                String nombre = null;
                for (Raza raza : razas) if (raza.idRaza.equals(animal.idRaza)) nombre = raza.nombre;
                mostrar(animal, nombre);
            }
            @Override public void error(ErrorModelo error) {
                if (disponible()) mostrarError();
            }
        });
    }

    private void mostrar(Animal animal, String razaNombre) {
        actual = animal;
        String sinRegistrar = getString(R.string.sin_registrar);
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.estadoError.setVisibility(View.GONE);
        vista.contenido.setVisibility(View.VISIBLE);
        vista.toolbar.setTitle(animal.codigo);
        vista.fichaCodigo.setText(animal.codigo);
        vista.fichaNombre.setText(animal.nombre == null ? getString(R.string.sin_nombre) : animal.nombre);
        vista.datoSexo.setText(Presentacion.sexo(this, animal.sexo));
        vista.datoRaza.setText(razaNombre == null ? sinRegistrar : razaNombre);
        vista.datoNacimiento.setText(animal.fechaNacimiento == null ? sinRegistrar : animal.fechaNacimiento);
        vista.datoIngreso.setText(animal.fechaIngreso);
        vista.datoEstado.setText(getString(R.string.estado_desde, Presentacion.estado(this, animal.estado), animal.fechaEstado));
        vista.datoObservaciones.setText(animal.observaciones == null ? sinRegistrar : animal.observaciones);
        vista.datoUltimoPeso.setText(R.string.sin_pesajes);
        cargarUltimoPeso();
    }

    private void abrir(Class<?> pantalla) {
        startActivity(new Intent(this, pantalla).putExtra(EXTRA_ID_ANIMAL, idAnimal));
    }

    /** Sustituye "Sin pesajes" por el último pesaje vigente; si falla la consulta se conserva la indicación. */
    private void cargarUltimoPeso() {
        app().pesajes().ultimo(idAnimal, new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible() || pesaje == null) return;
                vista.datoUltimoPeso.setText(getString(R.string.ultimo_peso_ficha, Presentacion.kilos(pesaje.pesoKg), pesaje.fecha));
            }
            @Override public void error(ErrorModelo error) { /* Se mantiene "Sin pesajes". */ }
        });
    }

    /** Diálogo para elegir el nuevo estado y su fecha efectiva; solo se cierra si el Modelo acepta el cambio. */
    private void cambiarEstado() {
        if (actual == null) return;
        View contenido = getLayoutInflater().inflate(R.layout.dialog_estado, null);
        RadioGroup grupo = contenido.findViewById(R.id.grupoEstado);
        TextInputLayout campoFecha = contenido.findViewById(R.id.campoEstadoFecha);
        TextInputEditText fecha = contenido.findViewById(R.id.estadoFecha);
        grupo.check(Animal.VENDIDO.equals(actual.estado) ? R.id.estadoVendido
                : Animal.FALLECIDO.equals(actual.estado) ? R.id.estadoFallecido : R.id.estadoActivo);
        fecha.setText(Tiempo.hoy().toString());
        campoFecha.setEndIconOnClickListener(v -> elegirFecha(fecha));
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this).setTitle(R.string.cambiar_estado).setView(contenido)
                .setPositiveButton(R.string.aplicar, null).setNegativeButton(R.string.cancelar, null).create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String estado = grupo.getCheckedRadioButtonId() == R.id.estadoVendido ? Animal.VENDIDO
                    : grupo.getCheckedRadioButtonId() == R.id.estadoFallecido ? Animal.FALLECIDO : Animal.ACTIVO;
            app().animales().cambiarEstado(idAnimal, estado, String.valueOf(fecha.getText()), new Callback<Animal>() {
                @Override public void exito(Animal animal) {
                    if (!disponible()) return;
                    dialogo.dismiss();
                    cargar();
                }
                @Override public void error(ErrorModelo error) {
                    if (!disponible()) return;
                    ErrorCampo errorFecha = error.campos.get("fechaEstado");
                    if (errorFecha != null) campoFecha.setError(MensajesError.texto(DetalleAnimalActivity.this, "fechaEstado", errorFecha));
                    else Toast.makeText(getApplicationContext(), R.string.error_guardado_animal, Toast.LENGTH_SHORT).show();
                }
            });
        });
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
