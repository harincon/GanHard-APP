package com.example.ganhard.controlador;

import android.os.Bundle;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityPesajeAnulacionBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.Campos;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Collections;
import java.util.Map;

/**
 * Controlador de la anulación de un pesaje (RF15, RN07): muestra la medición, pide el motivo y confirma.
 * El pesaje se conserva, pero deja de contar; no se puede deshacer. Devuelve RESULT_OK cuando se anuló.
 */
public class PesajeAnulacionActivity extends FormularioActivity {
    public static final String EXTRA_ID_PESAJE = "id_pesaje";

    private ActivityPesajeAnulacionBinding vista;
    private String idPesaje;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityPesajeAnulacionBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        idPesaje = getIntent().getStringExtra(EXTRA_ID_PESAJE);
        vista.guardar.setEnabled(false); // Se habilita al cargar el pesaje.
        vista.guardar.setOnClickListener(v -> anular());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (idPesaje == null) noEncontrado(); else cargar();
    }

    private void cargar() {
        app().pesajes().obtener(idPesaje, new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible()) return;
                vista.anulacionDetalle.setText(getString(R.string.anulacion_detalle, Presentacion.kilos(pesaje.pesoKg),
                        FormatoFecha.fechaYHoraConComa(pesaje.fecha, pesaje.hora)));
                vista.guardar.setEnabled(true);
                cargarAnimal(pesaje.idAnimal);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrado(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void cargarAnimal(String idAnimal) {
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                vista.anulacionTitulo.setText(animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
            }
            @Override public void error(ErrorModelo error) { /* La medición ya identifica lo que se va a anular. */ }
        });
    }

    private void anular() {
        if (guardando) return;
        empezarGuardado();
        app().pesajes().anular(idPesaje, texto(vista.motivo), new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), R.string.pesaje_anulado, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
                mostrarError(error, R.string.error_anulacion);
            }
        });
    }

    @Override protected Map<String, TextInputLayout> campos() {
        return Collections.singletonMap(Campos.MOTIVO, vista.campoMotivo);
    }

    @Override protected boolean hayCambios() { return !texto(vista.motivo).trim().isEmpty(); }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.pesaje_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
