package com.example.ganhard.controlador;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityPesajeFormBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.EntradaPesaje;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Controlador del registro de pesajes (RF08). Las reglas y los errores por campo los entrega el Modelo. */
public class PesajeFormActivity extends BaseActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    private static final String ESTADO_FECHA_INICIAL = "fecha_inicial";
    private static final String ESTADO_HORA_INICIAL = "hora_inicial";

    private ActivityPesajeFormBinding vista;
    private String idAnimal;
    private String fechaInicial, horaInicial;
    private boolean guardando;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityPesajeFormBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        if (estado == null) {
            fechaInicial = Tiempo.hoy().toString();
            LocalTime ahora = Tiempo.ahora().toLocalTime();
            horaInicial = String.format(Locale.US, "%02d:%02d", ahora.getHour(), ahora.getMinute());
            vista.fecha.setText(fechaInicial);
            vista.hora.setText(horaInicial);
        } else {
            fechaInicial = estado.getString(ESTADO_FECHA_INICIAL, "");
            horaInicial = estado.getString(ESTADO_HORA_INICIAL, "");
        }
        vista.guardar.setOnClickListener(v -> guardar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        vista.campoFecha.setEndIconOnClickListener(v -> elegirFecha());
        vista.campoHora.setEndIconOnClickListener(v -> elegirHora());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (guardando) return;
                if (!hayCambios()) { finish(); return; }
                new MaterialAlertDialogBuilder(PesajeFormActivity.this)
                        .setTitle(R.string.descartar_titulo).setMessage(R.string.descartar_mensaje)
                        .setPositiveButton(R.string.descartar, (dialogo, boton) -> finish())
                        .setNegativeButton(R.string.seguir_editando, null).show();
            }
        });
        if (idAnimal == null) noEncontrado(); else cargarAnimal();
    }

    private void cargarAnimal() {
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (disponible()) vista.toolbar.setSubtitle(animal.codigo);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrado(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void guardar() {
        if (guardando) return;
        EntradaPesaje entrada = new EntradaPesaje(texto(vista.fecha), texto(vista.hora), texto(vista.peso), texto(vista.observaciones));
        guardando = true;
        ocupado(true);
        vista.mensaje.setVisibility(View.GONE);
        app().pesajes().registrar(idAnimal, entrada, new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), getString(R.string.pesaje_guardado, Presentacion.kilos(pesaje.pesoKg)), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                guardando = false;
                ocupado(false);
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
                if (!error.campos.isEmpty()) {
                    mostrarErrores(error.campos);
                    enfocar(error.campos.keySet().iterator().next());
                } else {
                    mensaje(R.string.error_guardado_pesaje);
                }
            }
        });
    }

    private static String texto(TextInputEditText campo) { return String.valueOf(campo.getText()); }

    private Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put("fecha", vista.campoFecha);
        campos.put("hora", vista.campoHora);
        campos.put("peso", vista.campoPeso);
        campos.put("observaciones", vista.campoObservaciones);
        return campos;
    }

    private void mostrarErrores(Map<String, ErrorCampo> errores) {
        for (Map.Entry<String, TextInputLayout> campo : campos().entrySet()) {
            ErrorCampo error = errores.get(campo.getKey());
            campo.getValue().setError(error == null ? null : MensajesError.texto(this, campo.getKey(), error));
        }
    }

    private void enfocar(String campo) {
        TextInputLayout destino = campos().get(campo);
        if (destino != null && destino.getEditText() != null) destino.getEditText().requestFocus();
    }

    private void elegirFecha() {
        LocalDate actual;
        try { actual = LocalDate.parse(texto(vista.fecha).trim()); }
        catch (DateTimeParseException ex) { actual = Tiempo.hoy(); }
        MaterialDatePicker<Long> selector = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.elegir_fecha).setSelection(Tiempo.aSelector(actual)).build();
        selector.addOnPositiveButtonClickListener(seleccion -> vista.fecha.setText(Tiempo.fechaDeSelector(seleccion).toString()));
        selector.show(getSupportFragmentManager(), "fecha");
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

    private boolean hayCambios() {
        return !texto(vista.peso).trim().isEmpty() || !texto(vista.observaciones).trim().isEmpty()
                || !texto(vista.fecha).trim().equals(fechaInicial) || !texto(vista.hora).trim().equals(horaInicial);
    }

    private void ocupado(boolean ocupado) {
        vista.progreso.setVisibility(ocupado ? View.VISIBLE : View.INVISIBLE);
        vista.guardar.setEnabled(!ocupado);
        for (TextInputLayout campo : campos().values()) campo.setEnabled(!ocupado);
    }

    private void mensaje(int recurso) {
        vista.mensaje.setText(recurso);
        vista.mensaje.setVisibility(View.VISIBLE);
    }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_FECHA_INICIAL, fechaInicial);
        estado.putString(ESTADO_HORA_INICIAL, horaInicial);
        super.onSaveInstanceState(estado);
    }
}
