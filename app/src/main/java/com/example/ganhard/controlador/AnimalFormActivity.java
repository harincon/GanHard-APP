package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador del formulario de animales: registra uno nuevo (RF03) o, con {@link #EXTRA_ID_ANIMAL}, edita
 * el existente (RF06) y su estado (RF07) sin crear otro registro. Las fechas se escriben dd/mm/aaaa y se
 * entregan al Modelo en ISO; valida y pide guardar al Modelo.
 */
public class AnimalFormActivity extends BaseActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    private static final String ESTADO_RAZA = "id_raza";
    private static final String ESTADO_REGISTRO = "id_registro";
    private static final String ESTADO_CARGADO = "formulario_cargado";

    private ActivityAnimalFormBinding vista;
    private final List<Raza> razas = new ArrayList<>();
    private String idAnimal;
    private String idRaza = "";
    /** Id del animal nuevo: se genera al abrir y se conserva al rotar para que guardar dos veces no duplique. */
    private String idRegistro;
    private String ingresoInicial;
    /** Datos y estado del animal al abrir la edición; permiten saber si hay cambios sin guardar. */
    private EntradaAnimal original;
    private String estadoOriginal = "", fechaEstadoOriginal = "";
    private boolean restaurado, razasCargadas, guardando;
    /** En edición, indica que los campos ya recibieron los datos del animal (aunque la pantalla se haya recreado). */
    private boolean formularioCargado;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityAnimalFormBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        restaurado = estado != null;
        formularioCargado = restaurado && estado.getBoolean(ESTADO_CARGADO, false);
        idRegistro = restaurado ? estado.getString(ESTADO_REGISTRO) : null;
        if (idRegistro == null) idRegistro = UUID.randomUUID().toString();
        ingresoInicial = Tiempo.hoy().toString();
        vista.sexo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                Arrays.asList(getString(R.string.sexo_hembra), getString(R.string.sexo_macho))));
        vista.estado.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                Arrays.asList(getString(R.string.estado_activo), getString(R.string.estado_vendido), getString(R.string.estado_fallecido))));
        if (esEdicion()) {
            vista.toolbar.setTitle(R.string.editar_animal);
            vista.guardar.setText(R.string.guardar_cambios);
            vista.guardar.setEnabled(false); // Se habilita al cargar el animal.
            vista.campoEstado.setVisibility(View.VISIBLE);
            vista.campoFechaEstado.setVisibility(View.VISIBLE);
        } else if (!restaurado) {
            vista.ingreso.setText(FormatoFecha.aPantalla(ingresoInicial));
        }
        if (restaurado) idRaza = estado.getString(ESTADO_RAZA, "");
        vista.guardar.setOnClickListener(v -> guardar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        vista.campoNacimiento.setEndIconOnClickListener(v -> elegirFecha(vista.nacimiento));
        vista.campoIngreso.setEndIconOnClickListener(v -> elegirFecha(vista.ingreso));
        vista.campoFechaEstado.setEndIconOnClickListener(v -> elegirFecha(vista.fechaEstado));
        vista.raza.setOnItemClickListener((adaptador, fila, posicion, id) -> {
            idRaza = posicion == 0 ? "" : razas.get(posicion - 1).idRaza;
            if (posicion == 0) vista.raza.setText("", false);
        });
        // Al elegir otro estado la fecha propone hoy; al volver al estado original, su fecha.
        vista.estado.setOnItemClickListener((adaptador, fila, posicion, id) ->
                vista.fechaEstado.setText(estadoCodigo().equals(estadoOriginal) ? FormatoFecha.aPantalla(fechaEstadoOriginal) : FormatoFecha.aPantalla(Tiempo.hoy().toString())));
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
        if (esEdicion()) cargarAnimal();
    }

    private boolean esEdicion() { return idAnimal != null; }

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
                razasCargadas = true;
                mostrarRazaElegida();
            }
            @Override public void error(ErrorModelo error) {
                if (disponible()) mensaje(R.string.error_carga);
            }
        });
    }

    private void cargarAnimal() {
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                original = entradaDe(animal);
                estadoOriginal = animal.estado;
                fechaEstadoOriginal = animal.fechaEstado;
                if (!formularioCargado) {
                    vista.codigo.setText(original.codigo);
                    vista.nombre.setText(original.nombre);
                    vista.sexo.setText(Presentacion.sexo(AnimalFormActivity.this, original.sexo), false);
                    vista.nacimiento.setText(FormatoFecha.aPantalla(original.fechaNacimiento));
                    vista.ingreso.setText(FormatoFecha.aPantalla(original.fechaIngreso));
                    vista.observaciones.setText(original.observaciones);
                    vista.estado.setText(Presentacion.estado(AnimalFormActivity.this, animal.estado), false);
                    vista.fechaEstado.setText(FormatoFecha.aPantalla(animal.fechaEstado));
                    idRaza = original.idRaza;
                    mostrarRazaElegida();
                }
                formularioCargado = true;
                vista.guardar.setEnabled(true);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    mensaje(R.string.error_carga);
                }
            }
        });
    }

    /** Muestra el nombre de la raza elegida cuando ya se conocen tanto la lista como la elección. */
    private void mostrarRazaElegida() {
        if (!razasCargadas) return;
        for (Raza raza : razas) if (raza.idRaza.equals(idRaza)) vista.raza.setText(raza.nombre, false);
    }

    private static EntradaAnimal entradaDe(Animal animal) {
        return new EntradaAnimal(animal.codigo, animal.nombre, animal.sexo, animal.idRaza,
                animal.fechaNacimiento, animal.fechaIngreso, animal.observaciones);
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
        Callback<Animal> resultado = new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                guardando = false;
                int texto = esEdicion() ? R.string.animal_actualizado : R.string.animal_guardado;
                Toast.makeText(getApplicationContext(), getString(texto, animal.codigo), Toast.LENGTH_SHORT).show();
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
        };
        if (esEdicion()) app().animales().actualizar(idAnimal, entrada, estadoCodigo(), FormatoFecha.aIso(texto(vista.fechaEstado)), resultado);
        else app().animales().registrar(idRegistro, entrada, resultado);
    }

    private EntradaAnimal entrada() {
        return new EntradaAnimal(texto(vista.codigo), texto(vista.nombre), sexoCodigo(), idRaza,
                FormatoFecha.aIso(texto(vista.nacimiento)), FormatoFecha.aIso(texto(vista.ingreso)), texto(vista.observaciones));
    }

    private String sexoCodigo() {
        String elegido = texto(vista.sexo).trim();
        if (elegido.equals(getString(R.string.sexo_hembra))) return Animal.HEMBRA;
        if (elegido.equals(getString(R.string.sexo_macho))) return Animal.MACHO;
        return "";
    }

    private String estadoCodigo() {
        String elegido = texto(vista.estado).trim();
        if (elegido.equals(getString(R.string.estado_activo))) return Animal.ACTIVO;
        if (elegido.equals(getString(R.string.estado_vendido))) return Animal.VENDIDO;
        if (elegido.equals(getString(R.string.estado_fallecido))) return Animal.FALLECIDO;
        return "";
    }

    private static String texto(TextView campo) { return String.valueOf(campo.getText()); }

    private Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put("codigo", vista.campoCodigo);
        campos.put("nombre", vista.campoNombre);
        campos.put("sexo", vista.campoSexo);
        campos.put("idRaza", vista.campoRaza);
        campos.put("fechaNacimiento", vista.campoNacimiento);
        campos.put("fechaIngreso", vista.campoIngreso);
        campos.put("estado", vista.campoEstado);
        campos.put("fechaEstado", vista.campoFechaEstado);
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

    private boolean hayCambios() {
        EntradaAnimal actual = entrada();
        if (esEdicion()) {
            return original != null && !(actual.codigo.equals(original.codigo) && actual.nombre.equals(original.nombre)
                    && actual.sexo.equals(original.sexo) && actual.idRaza.equals(original.idRaza)
                    && actual.fechaNacimiento.equals(original.fechaNacimiento) && actual.fechaIngreso.equals(original.fechaIngreso)
                    && actual.observaciones.equals(original.observaciones)
                    && estadoCodigo().equals(estadoOriginal) && FormatoFecha.aIso(texto(vista.fechaEstado)).equals(fechaEstadoOriginal));
        }
        return !actual.codigo.isEmpty() || !actual.nombre.isEmpty() || !actual.sexo.isEmpty() || !actual.idRaza.isEmpty()
                || !actual.fechaNacimiento.isEmpty() || !actual.fechaIngreso.equals(ingresoInicial) || !actual.observaciones.isEmpty();
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

    private void irAConfiguracion() {
        startActivity(new Intent(this, ConfiguracionActivity.class));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_RAZA, idRaza);
        estado.putString(ESTADO_REGISTRO, idRegistro);
        estado.putBoolean(ESTADO_CARGADO, formularioCargado);
        super.onSaveInstanceState(estado);
    }
}
