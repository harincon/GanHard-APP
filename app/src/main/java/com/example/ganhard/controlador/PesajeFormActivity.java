package com.example.ganhard.controlador;

import android.os.Bundle;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityPesajeFormBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.EntradaPesaje;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador del registro de pesajes (RF08). La fecha se escribe dd/mm/aaaa y viaja al Modelo en ISO; las reglas y
 * los errores por campo los entrega el Modelo.
 */
public class PesajeFormActivity extends FormularioActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    private static final String ESTADO_FECHA_INICIAL = "fecha_inicial";
    private static final String ESTADO_HORA_INICIAL = "hora_inicial";
    private static final String ESTADO_REGISTRO = "id_registro";

    private ActivityPesajeFormBinding vista;
    private String idAnimal;
    /** Id del pesaje nuevo: se genera al abrir y se conserva al rotar para que guardar dos veces no duplique. */
    private String idRegistro;
    private String fechaInicial, horaInicial;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityPesajeFormBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        idRegistro = estado == null ? null : estado.getString(ESTADO_REGISTRO);
        if (idRegistro == null) idRegistro = UUID.randomUUID().toString();
        if (estado == null) {
            fechaInicial = FormatoFecha.aPantalla(Tiempo.hoy().toString());
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
        vista.fecha.addTextChangedListener(new MascaraFecha());
        vista.campoFecha.setEndIconOnClickListener(v -> elegirFecha(vista.fecha));
        vista.campoHora.setEndIconOnClickListener(v -> elegirHora());
        if (idAnimal == null) noEncontrado(); else cargarAnimal();
    }

    /** Tarjeta de contexto: el animal al que se le registra el pesaje y la finca. */
    private void cargarAnimal() {
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                vista.contextoAnimal.setText(animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
                String estado = Presentacion.estado(PesajeFormActivity.this, animal.estado).toLowerCase(Locale.ROOT);
                cargarFinca(estado);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrado(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void cargarFinca(String estado) {
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (!disponible()) return;
                String finca = datos == null ? "" : datos.finca.nombre;
                vista.contextoDetalle.setText(getString(R.string.contexto_animal, estado, finca));
            }
            @Override public void error(ErrorModelo error) {
                if (disponible()) vista.contextoDetalle.setText(getString(R.string.contexto_animal, estado, ""));
            }
        });
    }

    private void guardar() {
        if (guardando) return;
        EntradaPesaje entrada = new EntradaPesaje(FormatoFecha.aIso(texto(vista.fecha)), texto(vista.hora), texto(vista.peso), texto(vista.observaciones));
        empezarGuardado();
        app().pesajes().registrar(idRegistro, idAnimal, entrada, new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), getString(R.string.pesaje_guardado, Presentacion.kilos(pesaje.pesoKg)), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
                mostrarError(error, R.string.error_guardado_pesaje);
            }
        });
    }

    @Override protected Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put("fecha", vista.campoFecha);
        campos.put("hora", vista.campoHora);
        campos.put("peso", vista.campoPeso);
        campos.put("observaciones", vista.campoObservaciones);
        return campos;
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

    @Override protected boolean hayCambios() {
        return !texto(vista.peso).trim().isEmpty() || !texto(vista.observaciones).trim().isEmpty()
                || !texto(vista.fecha).trim().equals(fechaInicial) || !texto(vista.hora).trim().equals(horaInicial);
    }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_FECHA_INICIAL, fechaInicial);
        estado.putString(ESTADO_HORA_INICIAL, horaInicial);
        estado.putString(ESTADO_REGISTRO, idRegistro);
        super.onSaveInstanceState(estado);
    }
}
