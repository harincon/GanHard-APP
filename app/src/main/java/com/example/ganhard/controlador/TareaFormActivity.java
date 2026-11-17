package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityTareaFormBinding;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.EntradaTarea;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador del formulario de tareas. Sin {@link #EXTRA_ID_TAREA} programa una tarea pendiente (RF12, RF17), con
 * el tipo y el animal ya puestos si llegan {@link #EXTRA_TIPO} y {@link #EXTRA_ID_ANIMAL}; con él, corrige esa tarea
 * pidiendo un motivo (RF14): solo cambian el título, la descripción y la fecha. Las reglas y los errores por campo
 * los entrega el Modelo.
 */
public class TareaFormActivity extends FormularioActivity {
    public static final String EXTRA_ID_TAREA = "id_tarea";
    public static final String EXTRA_TIPO = "tipo";
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    private static final String ESTADO_TIPO = "tipo_elegido", ESTADO_ANIMAL = "animal_elegido";
    private static final String ESTADO_REGISTRO = "id_registro", ESTADO_CARGADO = "formulario_cargado";

    private ActivityTareaFormBinding vista;
    private final List<AnimalResumen> animales = new ArrayList<>();
    private String idTarea;
    /** Tipo y animal elegidos, como los entiende el Modelo; lo que se ve en pantalla son sus nombres. */
    private String tipo = "", idAnimal = "";
    /** Lo que traía el formulario al abrir, para saber si hay cambios sin guardar. */
    private String tipoInicial = "", animalInicial = "";
    private boolean animalFijo;
    /** Id de la tarea nueva: se genera al abrir y se conserva al rotar para que guardar dos veces no duplique. */
    private String idRegistro;
    private EntradaTarea original;
    private boolean formularioCargado;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityTareaFormBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        idTarea = getIntent().getStringExtra(EXTRA_ID_TAREA);
        String tipoRecibido = getIntent().getStringExtra(EXTRA_TIPO), animalRecibido = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        animalFijo = !esCorreccion() && animalRecibido != null;
        if (!esCorreccion()) {
            if (tipoRecibido != null) tipoInicial = tipoRecibido;
            if (animalRecibido != null) animalInicial = animalRecibido;
        }
        if (estado != null) {
            tipo = estado.getString(ESTADO_TIPO, "");
            idAnimal = estado.getString(ESTADO_ANIMAL, "");
            idRegistro = estado.getString(ESTADO_REGISTRO);
            formularioCargado = estado.getBoolean(ESTADO_CARGADO, false);
        } else {
            tipo = tipoInicial;
            idAnimal = animalInicial;
        }
        if (idRegistro == null) idRegistro = UUID.randomUUID().toString();

        List<String> tipos = new ArrayList<>();
        for (String codigo : Tarea.TIPOS) tipos.add(Presentacion.tipoTarea(this, codigo));
        vista.tipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, tipos));
        vista.tipo.setOnItemClickListener((adaptador, fila, posicion, id) -> tipo = Tarea.TIPOS.get(posicion));
        if (!tipo.isEmpty()) vista.tipo.setText(Presentacion.tipoTarea(this, tipo), false);
        // La posición 0 de la lista es "Sin animal"; los animales empiezan en la 1.
        vista.animal.setOnItemClickListener((adaptador, fila, posicion, id) -> idAnimal = posicion == 0 ? "" : animales.get(posicion - 1).idAnimal);
        vista.fecha.addTextChangedListener(new MascaraFecha());
        vista.campoFecha.setEndIconOnClickListener(v -> elegirFecha(vista.fecha));
        vista.guardar.setOnClickListener(v -> guardar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if (esCorreccion()) {
            vista.toolbar.setTitle(R.string.corregir_tarea);
            vista.guardar.setText(R.string.guardar_correccion);
            vista.guardar.setEnabled(false); // Se habilita al cargar la tarea.
            vista.campoTipo.setEnabled(false);
            vista.campoAnimal.setEnabled(false);
            vista.tarjetaInfo.setVisibility(View.GONE);
            vista.tarjetaContexto.setVisibility(View.VISIBLE);
            vista.campoMotivo.setVisibility(View.VISIBLE);
            vista.notaCorreccion.setVisibility(View.VISIBLE);
            cargarTarea();
        } else {
            cargarAnimales();
        }
    }

    private boolean esCorreccion() { return idTarea != null; }

    /** Los animales que se ofrecen son los activos (RN08); sin finca no hay a quién programarle nada. */
    private void cargarAnimales() {
        app().animales().buscar("", Animal.ACTIVO, new Callback<List<AnimalResumen>>() {
            @Override public void exito(List<AnimalResumen> lista) {
                if (!disponible()) return;
                animales.clear();
                animales.addAll(lista);
                List<String> nombres = new ArrayList<>();
                nombres.add(getString(R.string.sin_animal));
                for (AnimalResumen animal : animales) nombres.add(nombreDe(animal.codigo, animal.nombre));
                vista.animal.setAdapter(new ArrayAdapter<>(TareaFormActivity.this, android.R.layout.simple_dropdown_item_1line, nombres));
                boolean mostrado = false;
                for (AnimalResumen animal : animales) {
                    if (animal.idAnimal.equals(idAnimal)) { vista.animal.setText(nombreDe(animal.codigo, animal.nombre), false); mostrado = true; }
                }
                // Un animal fijo que ya no está activo no está en la lista, pero se muestra para explicar el error.
                if (!mostrado && !idAnimal.isEmpty()) mostrarAnimal(idAnimal);
                if (animalFijo) vista.campoAnimal.setEnabled(false);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) irAConfiguracion(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void cargarTarea() {
        app().tareas().obtener(idTarea, new Callback<Tarea>() {
            @Override public void exito(Tarea tarea) {
                if (!disponible()) return;
                original = new EntradaTarea(tarea.tipo, tarea.idAnimal, tarea.titulo, tarea.descripcion, tarea.fechaProgramada);
                tipo = tarea.tipo;
                idAnimal = tarea.idAnimal == null ? "" : tarea.idAnimal;
                if (!formularioCargado) {
                    vista.titulo.setText(tarea.titulo);
                    vista.fecha.setText(FormatoFecha.aPantalla(tarea.fechaProgramada));
                    vista.descripcion.setText(tarea.descripcion);
                }
                vista.tipo.setText(Presentacion.tipoTarea(TareaFormActivity.this, tipo), false);
                if (idAnimal.isEmpty()) vista.animal.setText(R.string.sin_animal); else mostrarAnimal(idAnimal);
                formularioCargado = true;
                vista.contextoTitulo.setText(tarea.titulo);
                vista.contextoDetalle.setText(getString(R.string.codigo_nombre, Presentacion.tipoTarea(TareaFormActivity.this, tarea.tipo),
                        FormatoFecha.corta(tarea.fechaProgramada)));
                if (Tarea.PENDIENTE.equals(tarea.estado)) vista.guardar.setEnabled(true); else mensaje(R.string.tarea_no_pendiente);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) tareaNoEncontrada(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void mostrarAnimal(String id) {
        app().animales().obtener(id, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (disponible()) vista.animal.setText(nombreDe(animal.codigo, animal.nombre), false);
            }
            @Override public void error(ErrorModelo error) { /* El campo queda vacío; el Modelo avisará si el animal no sirve. */ }
        });
    }

    private String nombreDe(String codigo, String nombre) {
        return nombre == null ? codigo : getString(R.string.codigo_nombre, codigo, nombre);
    }

    private void guardar() {
        if (guardando) return;
        EntradaTarea entrada = entrada();
        empezarGuardado();
        Callback<Tarea> resultado = new Callback<Tarea>() {
            @Override public void exito(Tarea tarea) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), esCorreccion() ? R.string.tarea_corregida : R.string.tarea_guardada, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    if (ErrorModelo.FINCA.equals(error.faltante)) irAConfiguracion();
                    else if (ErrorModelo.TAREA.equals(error.faltante)) tareaNoEncontrada();
                    else {
                        Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    return;
                }
                if (error.campos.containsKey(Campos.ESTADO)) { mensaje(R.string.tarea_no_pendiente); return; }
                mostrarError(error, R.string.error_guardado_tarea);
            }
        };
        if (esCorreccion()) app().tareas().corregir(idTarea, entrada, texto(vista.motivo), resultado);
        else app().tareas().programar(idRegistro, entrada, resultado);
    }

    private EntradaTarea entrada() {
        return new EntradaTarea(tipo, idAnimal, texto(vista.titulo), texto(vista.descripcion), FormatoFecha.aIso(texto(vista.fecha)));
    }

    @Override protected Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put(Campos.TIPO, vista.campoTipo);
        campos.put(Campos.ID_ANIMAL, vista.campoAnimal);
        campos.put(Campos.TITULO, vista.campoTitulo);
        campos.put(Campos.FECHA_PROGRAMADA, vista.campoFecha);
        campos.put(Campos.DESCRIPCION, vista.campoDescripcion);
        campos.put(Campos.MOTIVO, vista.campoMotivo);
        return campos;
    }

    /** Al desbloquear el formulario, el tipo y el animal que no se pueden cambiar siguen bloqueados. */
    @Override protected void ocupado(boolean ocupado) {
        super.ocupado(ocupado);
        if (esCorreccion()) vista.campoTipo.setEnabled(false);
        if (esCorreccion() || animalFijo) vista.campoAnimal.setEnabled(false);
    }

    @Override protected boolean hayCambios() {
        EntradaTarea actual = entrada();
        if (esCorreccion()) {
            return original != null && (!actual.titulo.equals(original.titulo) || !actual.descripcion.equals(original.descripcion)
                    || !actual.fechaProgramada.equals(original.fechaProgramada) || !texto(vista.motivo).trim().isEmpty());
        }
        return !tipo.equals(tipoInicial) || !idAnimal.equals(animalInicial) || !actual.titulo.isEmpty()
                || !texto(vista.fecha).trim().isEmpty() || !actual.descripcion.isEmpty();
    }

    private void tareaNoEncontrada() {
        Toast.makeText(getApplicationContext(), R.string.tarea_no_encontrada, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void irAConfiguracion() {
        startActivity(new Intent(this, ConfiguracionActivity.class));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_TIPO, tipo);
        estado.putString(ESTADO_ANIMAL, idAnimal);
        estado.putString(ESTADO_REGISTRO, idRegistro);
        estado.putBoolean(ESTADO_CARGADO, formularioCargado);
        super.onSaveInstanceState(estado);
    }
}
