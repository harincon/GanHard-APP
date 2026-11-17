package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityPesajeCorreccionBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.EntradaPesaje;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Controlador de la corrección de un pesaje (RF14): edita peso, fecha, hora y observaciones con un motivo
 * obligatorio, y da acceso a su anulación (RF15). Las reglas y los errores por campo los entrega el Modelo.
 */
public class PesajeCorreccionActivity extends FormularioActivity {
    public static final String EXTRA_ID_PESAJE = "id_pesaje";
    private static final String ESTADO_CARGADO = "formulario_cargado";

    private ActivityPesajeCorreccionBinding vista;
    private String idPesaje;
    /** Lo que tenía el pesaje al abrir; permite saber si hay cambios sin guardar. */
    private String pesoOriginal = "", fechaOriginal = "", horaOriginal = "", observacionesOriginal = "";
    /** Indica que los campos ya recibieron los datos del pesaje, aunque la pantalla se haya recreado. */
    private boolean formularioCargado;
    /** Si la anulación termina bien, ya no hay nada que corregir y esta pantalla se cierra con ella. */
    private final ActivityResultLauncher<Intent> anulacion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) { setResult(RESULT_OK); finish(); }
            });

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityPesajeCorreccionBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        idPesaje = getIntent().getStringExtra(EXTRA_ID_PESAJE);
        formularioCargado = estado != null && estado.getBoolean(ESTADO_CARGADO, false);
        vista.guardar.setEnabled(false); // Se habilita al cargar el pesaje.
        vista.guardar.setOnClickListener(v -> guardar());
        vista.fecha.addTextChangedListener(new MascaraFecha());
        vista.campoFecha.setEndIconOnClickListener(v -> elegirFecha(vista.fecha));
        vista.campoHora.setEndIconOnClickListener(v -> elegirHora());
        vista.filaAnular.setOnClickListener(v -> anulacion.launch(
                new Intent(this, AnulacionActivity.class).putExtra(AnulacionActivity.EXTRA_ENTIDAD, HistorialCambio.PESAJE)
                        .putExtra(AnulacionActivity.EXTRA_ID, idPesaje)));
        if (idPesaje == null) noEncontrado(); else cargar();
    }

    private void cargar() {
        app().pesajes().obtener(idPesaje, new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible()) return;
                pesoOriginal = Presentacion.kilos(pesaje.pesoKg);
                fechaOriginal = FormatoFecha.aPantalla(pesaje.fecha);
                horaOriginal = pesaje.hora == null ? "" : pesaje.hora.substring(0, 5);
                observacionesOriginal = pesaje.observaciones == null ? "" : pesaje.observaciones;
                if (!formularioCargado) {
                    vista.peso.setText(pesoOriginal);
                    vista.fecha.setText(fechaOriginal);
                    vista.hora.setText(horaOriginal);
                    vista.observaciones.setText(observacionesOriginal);
                }
                formularioCargado = true;
                vista.guardar.setEnabled(true);
                vista.contextoDetalle.setText(getString(R.string.medicion_del,
                        FormatoFecha.fechaYHora(pesaje.fecha, pesaje.hora, getString(R.string.sin_hora))));
                cargarAnimal(pesaje.idAnimal);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrado(); else mensaje(R.string.error_carga);
            }
        });
    }

    /** Tarjeta de contexto: a qué animal pertenece la medición. */
    private void cargarAnimal(String idAnimal) {
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                vista.contextoAnimal.setText(animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
            }
            @Override public void error(ErrorModelo error) { /* El formulario sigue siendo útil sin el nombre del animal. */ }
        });
    }

    private void guardar() {
        if (guardando) return;
        EntradaPesaje entrada = new EntradaPesaje(FormatoFecha.aIso(texto(vista.fecha)), texto(vista.hora), texto(vista.peso), texto(vista.observaciones));
        empezarGuardado();
        app().pesajes().corregir(idPesaje, entrada, texto(vista.motivo), new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), R.string.pesaje_corregido, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
                mostrarError(error, R.string.error_correccion);
            }
        });
    }

    @Override protected Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put(Campos.PESO, vista.campoPeso);
        campos.put(Campos.FECHA, vista.campoFecha);
        campos.put(Campos.HORA, vista.campoHora);
        campos.put(Campos.OBSERVACIONES, vista.campoObservaciones);
        campos.put(Campos.MOTIVO, vista.campoMotivo);
        return campos;
    }

    @Override protected boolean hayCambios() {
        return !texto(vista.peso).trim().equals(pesoOriginal) || !texto(vista.fecha).trim().equals(fechaOriginal)
                || !texto(vista.hora).trim().equals(horaOriginal) || !texto(vista.observaciones).trim().equals(observacionesOriginal)
                || !texto(vista.motivo).trim().isEmpty();
    }

    private void elegirHora() {
        LocalTime actual;
        try { actual = LocalTime.parse(texto(vista.hora).trim()); }
        catch (DateTimeParseException ex) { actual = Tiempo.ahora().toLocalTime(); }
        MaterialTimePicker selector = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(actual.getHour()).setMinute(actual.getMinute()).setTitleText(R.string.elegir_hora).build();
        selector.addOnPositiveButtonClickListener(v -> vista.hora.setText(String.format(Locale.US, "%02d:%02d", selector.getHour(), selector.getMinute())));
        selector.show(getSupportFragmentManager(), "hora");
    }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.pesaje_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putBoolean(ESTADO_CARGADO, formularioCargado);
        super.onSaveInstanceState(estado);
    }
}
