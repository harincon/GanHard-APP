package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityAnimalFormBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorAnimal;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Controlador del registro de animales (RF03): lee el formulario, valida y pide guardar al Modelo. */
public class AnimalFormActivity extends BaseActivity {
    /** Reservado para la edición del animal (tarea 14); sin este extra la pantalla registra uno nuevo. */
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    private static final String ESTADO_RAZA = "id_raza";

    private ActivityAnimalFormBinding vista;
    private final List<Raza> razas = new ArrayList<>();
    private String idRaza = "";
    private String ingresoInicial;
    private boolean guardando;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityAnimalFormBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        ingresoInicial = Tiempo.hoy().toString();
        if (estado == null) vista.ingreso.setText(ingresoInicial);
        else idRaza = estado.getString(ESTADO_RAZA, "");
        vista.guardar.setOnClickListener(v -> guardar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        vista.campoNacimiento.setEndIconOnClickListener(v -> elegirFecha(vista.nacimiento));
        vista.campoIngreso.setEndIconOnClickListener(v -> elegirFecha(vista.ingreso));
        vista.raza.setOnItemClickListener((adaptador, fila, posicion, id) -> {
            idRaza = posicion == 0 ? "" : razas.get(posicion - 1).idRaza;
            if (posicion == 0) vista.raza.setText("", false);
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (guardando) return;
                if (!hayCambios()) { finish(); return; }
                new MaterialAlertDialogBuilder(AnimalFormActivity.this)
                        .setTitle(R.string.descartar_titulo).setMessage(R.string.descartar_mensaje)
                        .setPositiveButton(R.string.descartar, (dialogo, boton) -> finish())
                        .setNegativeButton(R.string.seguir_editando, null).show();
            }
        });
        verificarFinca();
        cargarRazas();
    }

    /** Sin finca configurada no hay a dónde asociar el animal: se lleva al usuario a configurarla. */
    private void verificarFinca() {
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (disponible() && datos == null) irAConfiguracion();
            }
            @Override public void error(ErrorModelo error) {
                if (disponible()) mensaje(R.string.error_carga);
            }
        });
    }

    private void cargarRazas() {
        app().animales().razas(new Callback<List<Raza>>() {
            @Override public void exito(List<Raza> lista) {
                if (!disponible()) return;
                razas.clear();
                razas.addAll(lista);
                List<String> nombres = new ArrayList<>();
                nombres.add(getString(R.string.sin_raza));
                for (Raza raza : razas) nombres.add(raza.nombre);
                vista.raza.setAdapter(new ArrayAdapter<>(AnimalFormActivity.this, android.R.layout.simple_dropdown_item_1line, nombres));
                for (Raza raza : razas) if (raza.idRaza.equals(idRaza)) vista.raza.setText(raza.nombre, false);
            }
            @Override public void error(ErrorModelo error) {
                if (disponible()) mensaje(R.string.error_carga);
            }
        });
    }

    private void guardar() {
        if (guardando) return;
        EntradaAnimal entrada = entrada();
        Map<String, ErrorCampo> errores = ValidadorAnimal.validar(entrada, Tiempo.hoy());
        mostrarErrores(errores);
        if (!errores.isEmpty()) { enfocar(errores.keySet().iterator().next()); return; }
        guardando = true;
        ocupado(true);
        vista.mensaje.setVisibility(View.GONE);
        app().animales().registrar(entrada, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), getString(R.string.animal_guardado, animal.codigo), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                guardando = false;
                ocupado(false);
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { irAConfiguracion(); return; }
                if (!error.campos.isEmpty()) {
                    mostrarErrores(error.campos);
                    enfocar(error.campos.keySet().iterator().next());
                } else {
                    mensaje(R.string.error_guardado_animal);
                }
            }
        });
    }

    private EntradaAnimal entrada() {
        String sexo = vista.sexoHembra.isChecked() ? Animal.HEMBRA : vista.sexoMacho.isChecked() ? Animal.MACHO : "";
        return new EntradaAnimal(texto(vista.codigo), texto(vista.nombre), sexo, idRaza,
                texto(vista.nacimiento), texto(vista.ingreso), texto(vista.observaciones));
    }

    private static String texto(TextInputEditText campo) { return String.valueOf(campo.getText()); }

    private Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put("codigo", vista.campoCodigo);
        campos.put("nombre", vista.campoNombre);
        campos.put("idRaza", vista.campoRaza);
        campos.put("fechaNacimiento", vista.campoNacimiento);
        campos.put("fechaIngreso", vista.campoIngreso);
        campos.put("observaciones", vista.campoObservaciones);
        return campos;
    }

    private void mostrarErrores(Map<String, ErrorCampo> errores) {
        for (Map.Entry<String, TextInputLayout> campo : campos().entrySet()) {
            ErrorCampo error = errores.get(campo.getKey());
            campo.getValue().setError(error == null ? null : MensajesError.texto(this, campo.getKey(), error));
        }
        ErrorCampo sexo = errores.get("sexo");
        vista.errorSexo.setText(sexo == null ? "" : MensajesError.texto(this, "sexo", sexo));
        vista.errorSexo.setVisibility(sexo == null ? View.GONE : View.VISIBLE);
    }

    private void enfocar(String campo) {
        if ("sexo".equals(campo)) { vista.sexoHembra.requestFocus(); return; }
        TextInputLayout destino = campos().get(campo);
        if (destino != null && destino.getEditText() != null) destino.getEditText().requestFocus();
    }

    private void elegirFecha(TextInputEditText campo) {
        LocalDate actual;
        try { actual = LocalDate.parse(texto(campo).trim()); }
        catch (DateTimeParseException ex) { actual = Tiempo.hoy(); }
        MaterialDatePicker<Long> selector = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.elegir_fecha).setSelection(Tiempo.aSelector(actual)).build();
        selector.addOnPositiveButtonClickListener(seleccion -> campo.setText(Tiempo.fechaDeSelector(seleccion).toString()));
        selector.show(getSupportFragmentManager(), "fecha");
    }

    private boolean hayCambios() {
        return !texto(vista.codigo).trim().isEmpty() || !texto(vista.nombre).trim().isEmpty()
                || vista.sexoHembra.isChecked() || vista.sexoMacho.isChecked() || !idRaza.isEmpty()
                || !texto(vista.nacimiento).trim().isEmpty() || !texto(vista.ingreso).trim().equals(ingresoInicial)
                || !texto(vista.observaciones).trim().isEmpty();
    }

    private void ocupado(boolean ocupado) {
        vista.progreso.setVisibility(ocupado ? View.VISIBLE : View.INVISIBLE);
        vista.guardar.setEnabled(!ocupado);
        for (TextInputLayout campo : campos().values()) campo.setEnabled(!ocupado);
        vista.sexoHembra.setEnabled(!ocupado);
        vista.sexoMacho.setEnabled(!ocupado);
    }

    private void mensaje(int recurso) {
        vista.mensaje.setText(recurso);
        vista.mensaje.setVisibility(View.VISIBLE);
    }

    private void irAConfiguracion() {
        startActivity(new Intent(this, ConfiguracionActivity.class));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_RAZA, idRaza);
        super.onSaveInstanceState(estado);
    }
}
