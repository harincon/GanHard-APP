package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivitySanidadFormBinding;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.EntradaSanidad;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador del formulario de sanidad. Sin {@link #EXTRA_ID_EVENTO} registra una actividad realizada (RF11), con
 * el animal fijo si llega {@link #EXTRA_ID_ANIMAL}; con él, corrige esa actividad pidiendo un motivo (RF14) y da
 * acceso a su anulación (RF15). Las reglas y los errores por campo los entrega el Modelo.
 */
public class SanidadFormActivity extends FormularioActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    public static final String EXTRA_ID_EVENTO = "id_evento";
    private static final String ESTADO_ANIMAL = "animal_elegido", ESTADO_TIPO = "tipo_elegido";
    private static final String ESTADO_REGISTRO = "id_registro", ESTADO_CARGADO = "formulario_cargado";

    private ActivitySanidadFormBinding vista;
    private final List<AnimalResumen> animales = new ArrayList<>();
    private String idEvento;
    /** Animal y tipo elegidos, como los entiende el Modelo; lo que se ve en pantalla son sus nombres. */
    private String idAnimal = "", tipo = "";
    private boolean animalFijo;
    /** Id de la actividad nueva: se genera al abrir y se conserva al rotar para que guardar dos veces no duplique. */
    private String idRegistro;
    private String fechaInicial = "";
    /** Lo que tenía la actividad al abrir la corrección; permite saber si hay cambios sin guardar. */
    private EntradaSanidad original;
    private boolean formularioCargado;
    /** Si la anulación termina bien, ya no hay nada que corregir y esta pantalla se cierra con ella. */
    private final ActivityResultLauncher<Intent> anulacion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) { setResult(RESULT_OK); finish(); }
            });

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivitySanidadFormBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        idEvento = getIntent().getStringExtra(EXTRA_ID_EVENTO);
        String animalRecibido = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        animalFijo = !esCorreccion() && animalRecibido != null;
        fechaInicial = FormatoFecha.aPantalla(Tiempo.hoy().toString());
        if (estado != null) {
            idAnimal = estado.getString(ESTADO_ANIMAL, "");
            tipo = estado.getString(ESTADO_TIPO, "");
            idRegistro = estado.getString(ESTADO_REGISTRO);
            formularioCargado = estado.getBoolean(ESTADO_CARGADO, false);
        } else {
            if (animalFijo) idAnimal = animalRecibido;
            if (!esCorreccion()) vista.fecha.setText(fechaInicial);
        }
        if (idRegistro == null) idRegistro = UUID.randomUUID().toString();

        List<String> tipos = new ArrayList<>();
        for (String codigo : EventoSanitario.TIPOS) tipos.add(Presentacion.tipoSanidad(this, codigo));
        vista.tipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, tipos));
        vista.tipo.setOnItemClickListener((adaptador, fila, posicion, id) -> tipo = EventoSanitario.TIPOS.get(posicion));
        vista.animal.setOnItemClickListener((adaptador, fila, posicion, id) -> idAnimal = animales.get(posicion).idAnimal);
        vista.fecha.addTextChangedListener(new MascaraFecha());
        vista.campoFecha.setEndIconOnClickListener(v -> elegirFecha(vista.fecha));
        vista.guardar.setOnClickListener(v -> guardar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        vista.guardar.setEnabled(false); // Se habilita cuando están los datos necesarios.

        if (esCorreccion()) {
            vista.toolbar.setTitle(R.string.corregir_actividad);
            vista.guardar.setText(R.string.guardar_correccion);
            vista.campoAnimal.setVisibility(View.GONE);
            vista.tarjetaFutura.setVisibility(View.GONE);
            vista.tarjetaContexto.setVisibility(View.VISIBLE);
            vista.campoMotivo.setVisibility(View.VISIBLE);
            vista.notaCorreccion.setVisibility(View.VISIBLE);
            vista.tarjetaAnular.setVisibility(View.VISIBLE);
            vista.filaAnular.setOnClickListener(v -> anulacion.launch(new Intent(this, AnulacionActivity.class)
                    .putExtra(AnulacionActivity.EXTRA_ENTIDAD, HistorialCambio.EVENTO_SANITARIO).putExtra(AnulacionActivity.EXTRA_ID, idEvento)));
            cargarActividad();
        } else {
            cargarAnimales();
        }
    }

    private boolean esCorreccion() { return idEvento != null; }

    /** Llena el selector con los animales de la finca; sin animales no hay a quién registrarle nada. */
    private void cargarAnimales() {
        app().animales().buscar("", null, new Callback<List<AnimalResumen>>() {
            @Override public void exito(List<AnimalResumen> lista) {
                if (!disponible()) return;
                animales.clear();
                animales.addAll(lista);
                List<String> nombres = new ArrayList<>();
                for (AnimalResumen animal : animales) nombres.add(nombreDe(animal.codigo, animal.nombre));
                vista.animal.setAdapter(new ArrayAdapter<>(SanidadFormActivity.this, android.R.layout.simple_dropdown_item_1line, nombres));
                if (animales.isEmpty()) { mensaje(R.string.sanidad_sin_animales); return; }
                for (AnimalResumen animal : animales) {
                    if (animal.idAnimal.equals(idAnimal)) vista.animal.setText(nombreDe(animal.codigo, animal.nombre), false);
                }
                if (!tipo.isEmpty()) vista.tipo.setText(Presentacion.tipoSanidad(SanidadFormActivity.this, tipo), false);
                vista.guardar.setEnabled(true);
                if (animalFijo) vista.campoAnimal.setEnabled(false);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) irAConfiguracion(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void cargarActividad() {
        app().sanidad().obtener(idEvento, new Callback<EventoSanitario>() {
            @Override public void exito(EventoSanitario evento) {
                if (!disponible()) return;
                original = new EntradaSanidad(evento.tipo, evento.fecha, evento.descripcion, evento.producto, evento.observaciones);
                if (!formularioCargado) {
                    tipo = evento.tipo;
                    vista.fecha.setText(FormatoFecha.aPantalla(evento.fecha));
                    vista.descripcion.setText(evento.descripcion);
                    vista.producto.setText(evento.producto);
                    vista.observaciones.setText(evento.observaciones);
                }
                vista.tipo.setText(Presentacion.tipoSanidad(SanidadFormActivity.this, tipo), false);
                formularioCargado = true;
                vista.guardar.setEnabled(true);
                vista.contextoDetalle.setText(getString(R.string.codigo_nombre,
                        Presentacion.tipoSanidad(SanidadFormActivity.this, evento.tipo), FormatoFecha.corta(evento.fecha)));
                cargarContexto(evento.idAnimal);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrada(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void cargarContexto(String idDelAnimal) {
        app().animales().obtener(idDelAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (disponible()) vista.contextoAnimal.setText(nombreDe(animal.codigo, animal.nombre));
            }
            @Override public void error(ErrorModelo error) { /* El formulario sigue siendo útil sin el nombre del animal. */ }
        });
    }

    private String nombreDe(String codigo, String nombre) {
        return nombre == null ? codigo : getString(R.string.codigo_nombre, codigo, nombre);
    }

    private void guardar() {
        if (guardando) return;
        EntradaSanidad entrada = entrada();
        empezarGuardado();
        Callback<EventoSanitario> resultado = new Callback<EventoSanitario>() {
            @Override public void exito(EventoSanitario evento) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), esCorreccion() ? R.string.actividad_corregida : R.string.actividad_guardada, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    if (ErrorModelo.FINCA.equals(error.faltante)) irAConfiguracion(); else noEncontrada();
                    return;
                }
                mostrarError(error, R.string.error_guardado_actividad);
            }
        };
        if (esCorreccion()) app().sanidad().corregir(idEvento, entrada, texto(vista.motivo), resultado);
        else app().sanidad().registrar(idRegistro, idAnimal, entrada, resultado);
    }

    private EntradaSanidad entrada() {
        return new EntradaSanidad(tipo, FormatoFecha.aIso(texto(vista.fecha)), texto(vista.descripcion), texto(vista.producto), texto(vista.observaciones));
    }

    @Override protected Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put(Campos.ID_ANIMAL, vista.campoAnimal);
        campos.put(Campos.TIPO, vista.campoTipo);
        campos.put(Campos.FECHA, vista.campoFecha);
        campos.put(Campos.DESCRIPCION, vista.campoDescripcion);
        campos.put(Campos.PRODUCTO, vista.campoProducto);
        campos.put(Campos.OBSERVACIONES, vista.campoObservaciones);
        campos.put(Campos.MOTIVO, vista.campoMotivo);
        return campos;
    }

    /** Al desbloquear el formulario, el animal que vino fijo desde la ficha sigue sin poder cambiarse. */
    @Override protected void ocupado(boolean ocupado) {
        super.ocupado(ocupado);
        if (animalFijo) vista.campoAnimal.setEnabled(false);
    }

    @Override protected boolean hayCambios() {
        EntradaSanidad actual = entrada();
        if (esCorreccion()) {
            return original != null && (!actual.tipo.equals(original.tipo) || !actual.fecha.equals(original.fecha)
                    || !actual.descripcion.equals(original.descripcion) || !actual.producto.equals(original.producto)
                    || !actual.observaciones.equals(original.observaciones) || !texto(vista.motivo).trim().isEmpty());
        }
        return !actual.tipo.isEmpty() || !texto(vista.fecha).trim().equals(fechaInicial) || !actual.descripcion.isEmpty()
                || !actual.producto.isEmpty() || !actual.observaciones.isEmpty() || (!animalFijo && !idAnimal.isEmpty());
    }

    private void noEncontrada() {
        Toast.makeText(getApplicationContext(), esCorreccion() ? R.string.actividad_no_encontrada : R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void irAConfiguracion() {
        startActivity(new Intent(this, ConfiguracionActivity.class));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_ANIMAL, idAnimal);
        estado.putString(ESTADO_TIPO, tipo);
        estado.putString(ESTADO_REGISTRO, idRegistro);
        estado.putBoolean(ESTADO_CARGADO, formularioCargado);
        super.onSaveInstanceState(estado);
    }
}
