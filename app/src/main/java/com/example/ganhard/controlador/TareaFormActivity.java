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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controlador del formulario de tareas. Sin {@link #EXTRA_ID_TAREA} programa una tarea pendiente (RF12, RF17), con
 * el tipo y el animal ya puestos si llegan {@link #EXTRA_TIPO} y {@link #EXTRA_ID_ANIMAL}; con él, corrige esa tarea
 * pidiendo un motivo (RF14): solo cambian el título, la descripción y la fecha. El tipo se elige de la lista o se
 * escribe (un tipo propio funciona como una tarea general), y en el animal se puede elegir "Todos los animales
 * activos" para programar la misma tarea a varios a la vez. Las reglas y los errores por campo los entrega el Modelo.
 */
public class TareaFormActivity extends FormularioActivity {
    public static final String EXTRA_ID_TAREA = "id_tarea";
    public static final String EXTRA_TIPO = "tipo";
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    private static final String ESTADO_ANIMAL = "animal_elegido";
    private static final String ESTADO_REGISTRO = "id_registro", ESTADO_CARGADO = "formulario_cargado";
    private static final String ESTADO_TODOS = "aplicar_a_todos", ESTADO_ELEGIDOS = "animales_elegidos", ESTADO_ELEGIDOS_DEFINIDOS = "elegidos_definidos";
    /** Posiciones fijas del menú de animales: "Sin animal", "Todos los animales activos" y después cada animal. */
    private static final int SIN_ANIMAL = 0, TODOS = 1, PRIMER_ANIMAL = 2;

    private ActivityTareaFormBinding vista;
    private final List<AnimalResumen> animales = new ArrayList<>();
    private String idTarea;
    /** Animal elegido, como lo entiende el Modelo; lo que se ve en pantalla es su nombre. */
    private String idAnimal = "";
    /** Lo que traía el formulario al abrir, para saber si hay cambios sin guardar. */
    private String tipoInicial = "", animalInicial = "";
    private boolean animalFijo;
    /** Con "Todos los animales activos" la tarea se programa para los animales elegidos, no para uno. */
    private boolean todos, elegidosDefinidos;
    private final Set<String> elegidos = new LinkedHashSet<>();
    /** El texto del botón de guardar de una tarea individual, para devolverlo al dejar de elegir "Todos". */
    private CharSequence textoGuardar;
    /** Id de la tarea nueva (o del grupo): se genera al abrir y se conserva al rotar para que guardar dos veces no duplique. */
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
            if (tipoRecibido != null) tipoInicial = Presentacion.tipoTarea(this, tipoRecibido);
            if (animalRecibido != null) animalInicial = animalRecibido;
        }
        if (estado != null) {
            idAnimal = estado.getString(ESTADO_ANIMAL, "");
            idRegistro = estado.getString(ESTADO_REGISTRO);
            formularioCargado = estado.getBoolean(ESTADO_CARGADO, false);
            todos = estado.getBoolean(ESTADO_TODOS, false);
            elegidosDefinidos = estado.getBoolean(ESTADO_ELEGIDOS_DEFINIDOS, false);
            List<String> guardados = estado.getStringArrayList(ESTADO_ELEGIDOS);
            if (guardados != null) elegidos.addAll(guardados);
        } else {
            idAnimal = animalInicial;
            if (!tipoInicial.isEmpty()) vista.tipo.setText(tipoInicial, false);
        }
        if (idRegistro == null) idRegistro = UUID.randomUUID().toString();

        mostrarTipos(new ArrayList<>());
        // Lo que se elige de la lista queda escrito en el campo; el Modelo entiende el texto, sea de la lista o propio.
        vista.tipo.setOnItemClickListener((adaptador, fila, posicion, id) -> { });
        vista.animal.setOnItemClickListener((adaptador, fila, posicion, id) -> elegirDeLaLista(posicion));
        vista.filaAnimales.setOnClickListener(v -> elegirAnimales());
        vista.fecha.addTextChangedListener(new MascaraFecha());
        vista.campoFecha.setEndIconOnClickListener(v -> elegirFecha(vista.fecha));
        vista.guardar.setOnClickListener(v -> guardar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        textoGuardar = vista.guardar.getText();

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
            mostrarModo();
            cargarTiposPropios();
            cargarAnimales();
        }
    }

    private boolean esCorreccion() { return idTarea != null; }

    // --- Tipo ---

    /** El menú del tipo: los tres normales y, después, los tipos propios que la finca ya usó. */
    private void mostrarTipos(List<String> propios) {
        List<String> opciones = new ArrayList<>();
        for (String codigo : Tarea.TIPOS) opciones.add(Presentacion.tipoTarea(this, codigo));
        opciones.addAll(propios);
        vista.tipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, opciones));
    }

    private void cargarTiposPropios() {
        app().tareas().tiposPropios(new Callback<List<String>>() {
            @Override public void exito(List<String> propios) { if (disponible()) mostrarTipos(propios); }
            @Override public void error(ErrorModelo error) { /* Sin los tipos propios el menú sigue sirviendo con los tres normales. */ }
        });
    }

    // --- Animal: uno, ninguno o todos ---

    private void elegirDeLaLista(int posicion) {
        if (posicion == SIN_ANIMAL) { idAnimal = ""; todos = false; }
        else if (posicion == TODOS) { idAnimal = ""; todos = true; }
        else { idAnimal = animales.get(posicion - PRIMER_ANIMAL).idAnimal; todos = false; }
        mostrarModo();
    }

    /** Con "Todos" aparece la fila con el total de animales elegidos y el botón dice a cuántos se guarda. */
    private void mostrarModo() {
        if (esCorreccion()) return;
        vista.filaAnimales.setVisibility(todos ? View.VISIBLE : View.GONE);
        if (!todos) { vista.guardar.setText(textoGuardar); return; }
        vista.filaAnimales.setText(getString(R.string.sanidad_animales_elegidos, elegidos.size(), animales.size()));
        vista.guardar.setText(getResources().getQuantityString(R.plurals.tarea_guardar_grupo, elegidos.size(), elegidos.size()));
    }

    private void elegirAnimales() {
        if (guardando) return;
        SeleccionAnimales.mostrar(this, R.string.tarea_animales_dialogo_titulo, animales, elegidos, () -> { elegidosDefinidos = true; mostrarModo(); });
    }

    /** Los animales que se ofrecen son los activos (RN08); sin finca no hay a quién programarle nada. */
    private void cargarAnimales() {
        app().animales().buscar("", Animal.ACTIVO, new Callback<List<AnimalResumen>>() {
            @Override public void exito(List<AnimalResumen> lista) {
                if (!disponible()) return;
                animales.clear();
                animales.addAll(lista);
                List<String> nombres = new ArrayList<>();
                nombres.add(getString(R.string.sin_animal));
                nombres.add(getString(R.string.tarea_todos_animales));
                for (AnimalResumen animal : animales) nombres.add(nombreDe(animal.codigo, animal.nombre));
                vista.animal.setAdapter(new ArrayAdapter<>(TareaFormActivity.this, android.R.layout.simple_dropdown_item_1line, nombres));
                boolean mostrado = false;
                for (AnimalResumen animal : animales) {
                    if (animal.idAnimal.equals(idAnimal)) { vista.animal.setText(nombreDe(animal.codigo, animal.nombre), false); mostrado = true; }
                }
                if (todos) vista.animal.setText(getString(R.string.tarea_todos_animales), false);
                // Un animal fijo que ya no está activo no está en la lista, pero se muestra para explicar el error.
                else if (!mostrado && !idAnimal.isEmpty()) mostrarAnimal(idAnimal);
                // Por defecto reciben la tarea todos los animales activos; después solo quedan los que sigan siéndolo.
                Set<String> activos = new LinkedHashSet<>();
                for (AnimalResumen animal : animales) activos.add(animal.idAnimal);
                if (!elegidosDefinidos) { elegidos.clear(); elegidos.addAll(activos); elegidosDefinidos = true; }
                else elegidos.retainAll(activos);
                mostrarModo();
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
                idAnimal = tarea.idAnimal == null ? "" : tarea.idAnimal;
                if (!formularioCargado) {
                    vista.titulo.setText(tarea.titulo);
                    vista.fecha.setText(FormatoFecha.aPantalla(tarea.fechaProgramada));
                    vista.descripcion.setText(tarea.descripcion);
                }
                vista.tipo.setText(Presentacion.tipoTarea(TareaFormActivity.this, tarea), false);
                if (idAnimal.isEmpty()) vista.animal.setText(R.string.sin_animal); else mostrarAnimal(idAnimal);
                formularioCargado = true;
                vista.contextoTitulo.setText(tarea.titulo);
                vista.contextoDetalle.setText(getString(R.string.codigo_nombre, Presentacion.tipoTarea(TareaFormActivity.this, tarea),
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
        if (!esCorreccion() && todos) { programarEnGrupo(entrada); return; }
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

    /** Programa la tarea para todos los animales elegidos; si alguno ya no es válido, no se guarda nada. */
    private void programarEnGrupo(EntradaTarea entrada) {
        if (elegidos.isEmpty()) { mensaje(R.string.grupo_sin_animales); return; }
        empezarGuardado();
        app().tareas().programarGrupo(idRegistro, new ArrayList<>(elegidos), entrada, new Callback<List<Tarea>>() {
            @Override public void exito(List<Tarea> creadas) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), getResources().getQuantityString(R.plurals.tarea_grupo_guardada, creadas.size(), creadas.size()),
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO && ErrorModelo.FINCA.equals(error.faltante)) { irAConfiguracion(); return; }
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO || error.campos.containsKey(Campos.ID_ANIMAL)) {
                    // Un animal se vendió o ya no existe mientras se llenaba el formulario: se actualiza la lista.
                    mensaje(R.string.grupo_animal_inactivo);
                    cargarAnimales();
                    return;
                }
                mostrarError(error, R.string.error_guardado_tarea);
            }
        });
    }

    private EntradaTarea entrada() {
        return new EntradaTarea(texto(vista.tipo), idAnimal, texto(vista.titulo), texto(vista.descripcion), FormatoFecha.aIso(texto(vista.fecha)));
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
        vista.filaAnimales.setEnabled(!ocupado);
    }

    @Override protected boolean hayCambios() {
        EntradaTarea actual = entrada();
        if (esCorreccion()) {
            return original != null && (!actual.titulo.equals(original.titulo) || !actual.descripcion.equals(original.descripcion)
                    || !actual.fechaProgramada.equals(original.fechaProgramada) || !texto(vista.motivo).trim().isEmpty());
        }
        return !actual.tipo.equals(tipoInicial) || !idAnimal.equals(animalInicial) || todos || !actual.titulo.isEmpty()
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
        estado.putString(ESTADO_ANIMAL, idAnimal);
        estado.putString(ESTADO_REGISTRO, idRegistro);
        estado.putBoolean(ESTADO_CARGADO, formularioCargado);
        estado.putBoolean(ESTADO_TODOS, todos);
        estado.putBoolean(ESTADO_ELEGIDOS_DEFINIDOS, elegidosDefinidos);
        estado.putStringArrayList(ESTADO_ELEGIDOS, new ArrayList<>(elegidos));
        super.onSaveInstanceState(estado);
    }
}
