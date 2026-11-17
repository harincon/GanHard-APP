package com.example.ganhard.controlador;

import android.os.Bundle;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityAnulacionBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.Campos;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Collections;
import java.util.Map;

/**
 * Controlador de la anulación de un registro (RF15, RN07): muestra qué se va a anular, pide el motivo y confirma.
 * Sirve a pesajes y a actividades sanitarias según {@link #EXTRA_ENTIDAD}. El registro se conserva, pero deja de
 * contar; no se puede deshacer. Devuelve RESULT_OK cuando se anuló.
 */
public class AnulacionActivity extends FormularioActivity {
    /** Qué se anula: {@link HistorialCambio#PESAJE} o {@link HistorialCambio#EVENTO_SANITARIO}. */
    public static final String EXTRA_ENTIDAD = "entidad";
    public static final String EXTRA_ID = "id_registro";

    private ActivityAnulacionBinding vista;
    private String id;
    private boolean esPesaje, esActividad;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityAnulacionBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        String entidad = getIntent().getStringExtra(EXTRA_ENTIDAD);
        id = getIntent().getStringExtra(EXTRA_ID);
        esPesaje = HistorialCambio.PESAJE.equals(entidad);
        esActividad = HistorialCambio.EVENTO_SANITARIO.equals(entidad);
        vista.toolbar.setTitle(esActividad ? R.string.anular_actividad : R.string.anular_pesaje);
        vista.guardar.setEnabled(false); // Se habilita al cargar el registro.
        vista.guardar.setOnClickListener(v -> anular());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (id == null || !(esPesaje || esActividad)) noEncontrado();
        else if (esPesaje) cargarPesaje();
        else cargarActividad();
    }

    private void cargarPesaje() {
        app().pesajes().obtener(id, new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible()) return;
                mostrar(pesaje.idAnimal, getString(R.string.anulacion_detalle, Presentacion.kilos(pesaje.pesoKg),
                        FormatoFecha.fechaYHoraConComa(pesaje.fecha, pesaje.hora)));
            }
            @Override public void error(ErrorModelo error) { if (disponible()) errorDeCarga(error); }
        });
    }

    private void cargarActividad() {
        app().sanidad().obtener(id, new Callback<EventoSanitario>() {
            @Override public void exito(EventoSanitario evento) {
                if (!disponible()) return;
                mostrar(evento.idAnimal, getString(R.string.codigo_nombre, Presentacion.tipoSanidad(AnulacionActivity.this, evento.tipo),
                        FormatoFecha.corta(evento.fecha)));
            }
            @Override public void error(ErrorModelo error) { if (disponible()) errorDeCarga(error); }
        });
    }

    /** Resumen de lo que se va a anular y, encima, el animal al que pertenece. */
    private void mostrar(String idAnimal, String detalle) {
        vista.anulacionDetalle.setText(detalle);
        vista.guardar.setEnabled(true);
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                vista.anulacionTitulo.setText(animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
            }
            @Override public void error(ErrorModelo error) { /* El detalle ya identifica lo que se va a anular. */ }
        });
    }

    private void errorDeCarga(ErrorModelo error) {
        if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrado(); else mensaje(R.string.error_carga);
    }

    private void anular() {
        if (guardando) return;
        empezarGuardado();
        if (esPesaje) app().pesajes().anular(id, texto(vista.motivo), new Resultado<Pesaje>());
        else app().sanidad().anular(id, texto(vista.motivo), new Resultado<EventoSanitario>());
    }

    /** El desenlace es el mismo para cualquier registro anulado. */
    private final class Resultado<T> implements Callback<T> {
        @Override public void exito(T anulado) {
            if (!disponible()) return;
            guardando = false;
            Toast.makeText(getApplicationContext(), esActividad ? R.string.actividad_anulada : R.string.pesaje_anulado, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
        @Override public void error(ErrorModelo error) {
            if (!disponible()) return;
            terminarGuardado();
            if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
            mostrarError(error, R.string.error_anulacion);
        }
    }

    @Override protected Map<String, TextInputLayout> campos() {
        return Collections.singletonMap(Campos.MOTIVO, vista.campoMotivo);
    }

    @Override protected boolean hayCambios() { return !texto(vista.motivo).trim().isEmpty(); }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), esActividad ? R.string.actividad_no_encontrada : R.string.pesaje_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
