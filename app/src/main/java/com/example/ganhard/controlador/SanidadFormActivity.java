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
import com.example.ganhard.modelo.datos.TareaResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.EntradaSanidad;
import com.example.ganhard.modelo.repositorio.GrupoSanitario;
import com.example.ganhard.modelo.repositorio.GrupoTarea;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controlador del formulario de sanidad. Sin {@link #EXTRA_ID_EVENTO} registra una actividad realizada (RF11), con
 * el animal fijo si llega {@link #EXTRA_ID_ANIMAL}; con él, corrige esa actividad pidiendo un motivo (RF14) y da
 * acceso a su anulación (RF15). Las reglas y los errores por campo los entrega el Modelo.
 */
public class SanidadFormActivity extends FormularioActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    public static final String EXTRA_ID_EVENTO = "id_evento";
    /** Con él, el formulario corrige a la vez todas las actividades de ese grupo (aplicación a varios animales). */
    public static final String EXTRA_ID_GRUPO = "id_grupo";
    /** Tarea de sanidad que este registro cumple (RF17); sin ella, la actividad se registra sin programar. */
    public static final String EXTRA_ID_TAREA = "id_tarea";
    /** Con él, la actividad se registra en los animales de ese grupo de tareas y completa la tarea pendiente de cada uno. */
    public static final String EXTRA_ID_GRUPO_TAREAS = "id_grupo_tareas";
    private static final String ESTADO_ANIMAL = "animal_elegido", ESTADO_TIPO = "tipo_elegido";
    private static final String ESTADO_REGISTRO = "id_registro", ESTADO_CARGADO = "formulario_cargado";
    private static final String ESTADO_TODOS = "aplicar_a_todos", ESTADO_ELEGIDOS = "animales_elegidos", ESTADO_ELEGIDOS_DEFINIDOS = "elegidos_definidos";

    private ActivitySanidadFormBinding vista;
    private final List<AnimalResumen> animales = new ArrayList<>();
    private String idEvento, idGrupo;
    /** Con el interruptor activo la actividad se registra en todos los animales activos elegidos, no en uno. */
    private boolean todos, elegidosDefinidos;
    private final Set<String> elegidos = new LinkedHashSet<>();
    private String idTarea;
    private String idGrupoTareas;
    /** Al cumplir un grupo de tareas: la tarea pendiente de cada animal. */
    private final Map<String, String> tareasPorAnimal = new java.util.LinkedHashMap<>();
    /** Animal y tipo elegidos, como los entiende el Modelo; lo que se ve en pantalla son sus nombres. */
    private String idAnimal = "", tipo = "";
    private boolean animalFijo;
    /** Id de la actividad nueva: se genera al abrir y se conserva al rotar para que guardar dos veces no duplique. */
    private String idRegistro;
    private String fechaInicial = "";
    /** El texto del botón de guardar de un registro individual, para devolverlo al apagar el interruptor. */
    private CharSequence textoGuardar;
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
        idGrupo = getIntent().getStringExtra(EXTRA_ID_GRUPO);
        idTarea = getIntent().getStringExtra(EXTRA_ID_TAREA);
        idGrupoTareas = getIntent().getStringExtra(EXTRA_ID_GRUPO_TAREAS);
        String animalRecibido = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        animalFijo = !esCorreccion() && animalRecibido != null;
        fechaInicial = FormatoFecha.aPantalla(Tiempo.hoy().toString());
        if (estado != null) {
            idAnimal = estado.getString(ESTADO_ANIMAL, "");
            tipo = estado.getString(ESTADO_TIPO, "");
            idRegistro = estado.getString(ESTADO_REGISTRO);
            formularioCargado = estado.getBoolean(ESTADO_CARGADO, false);
            todos = estado.getBoolean(ESTADO_TODOS, false);
            elegidosDefinidos = estado.getBoolean(ESTADO_ELEGIDOS_DEFINIDOS, false);
            List<String> guardados = estado.getStringArrayList(ESTADO_ELEGIDOS);
            if (guardados != null) elegidos.addAll(guardados);
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

        vista.aplicarATodos.setOnCheckedChangeListener((boton, marcado) -> { todos = marcado; mostrarModo(); });
        vista.filaAnimales.setOnClickListener(v -> elegirAnimales());
        if (esCorreccion()) {
            vista.toolbar.setTitle(idGrupo != null ? R.string.grupo_corregir_toolbar : R.string.corregir_actividad);
            vista.guardar.setText(R.string.guardar_correccion);
            vista.campoAnimal.setVisibility(View.GONE);
            vista.tarjetaFutura.setVisibility(View.GONE);
            vista.tarjetaContexto.setVisibility(View.VISIBLE);
            vista.campoMotivo.setVisibility(View.VISIBLE);
            vista.notaCorreccion.setVisibility(View.VISIBLE);
            // Anular un grupo entero se hace desde su detalle; aquí solo se corrige.
            vista.tarjetaAnular.setVisibility(idGrupo != null ? View.GONE : View.VISIBLE);
            vista.filaAnular.setOnClickListener(v -> anulacion.launch(new Intent(this, AnulacionActivity.class)
                    .putExtra(AnulacionActivity.EXTRA_ENTIDAD, HistorialCambio.EVENTO_SANITARIO).putExtra(AnulacionActivity.EXTRA_ID, idEvento)));
            if (idGrupo != null) cargarGrupo(); else cargarActividad();
        } else {
            // Con un animal fijo (desde su ficha) no se ofrece aplicar a todos. Al cumplir un grupo de tareas siempre es en grupo.
            boolean cumplirGrupo = idGrupoTareas != null;
            if (cumplirGrupo) { todos = true; vista.toolbar.setTitle(R.string.grupo_cumplir_toolbar); }
            else if (!animalFijo) vista.aplicarATodos.setVisibility(View.VISIBLE);
            vista.aplicarATodos.setChecked(todos);
            textoGuardar = vista.guardar.getText();
            mostrarModo();
            if (cumplirGrupo) cargarTareasDelGrupo(); else cargarAnimales();
        }
    }

    private boolean esCorreccion() { return idEvento != null || idGrupo != null; }

    /** Con el interruptor activo se oculta el selector de un animal y aparece la lista de los que reciben la actividad. */
    private void mostrarModo() {
        if (esCorreccion()) return;
        vista.campoAnimal.setVisibility(todos ? View.GONE : View.VISIBLE);
        vista.filaAnimales.setVisibility(todos ? View.VISIBLE : View.GONE);
        refrescarGrupo();
    }

    /** Pone el total elegido en la fila de animales y en el botón de guardar. */
    private void refrescarGrupo() {
        if (esCorreccion()) return;
        if (!todos) { vista.guardar.setText(textoGuardar); return; }
        int total = animalesActivos().size();
        vista.filaAnimales.setText(getString(R.string.sanidad_animales_elegidos, elegidos.size(), total));
        vista.guardar.setText(getResources().getQuantityString(R.plurals.grupo_guardar, elegidos.size(), elegidos.size()));
    }

    /** Los animales activos; al cumplir tareas de un grupo, solo los que aún tienen su tarea pendiente. */
    private List<AnimalResumen> animalesActivos() {
        List<AnimalResumen> activos = new ArrayList<>();
        for (AnimalResumen animal : animales) {
            if (!Animal.ACTIVO.equals(animal.estado)) continue;
            if (idGrupoTareas != null && !tareasPorAnimal.containsKey(animal.idAnimal)) continue;
            activos.add(animal);
        }
        return activos;
    }

    /** Cumplir para todos: trae las tareas pendientes del grupo (una por animal) antes de ofrecer los animales. */
    private void cargarTareasDelGrupo() {
        app().tareas().grupo(idGrupoTareas, new Callback<GrupoTarea>() {
            @Override public void exito(GrupoTarea grupo) {
                if (!disponible()) return;
                tareasPorAnimal.clear();
                for (TareaResumen fila : grupo.tareas) {
                    if (Tarea.PENDIENTE.equals(fila.tarea.estado) && Tarea.SANIDAD.equals(fila.tarea.tipo) && fila.tarea.idAnimal != null) {
                        tareasPorAnimal.put(fila.tarea.idAnimal, fila.tarea.idTarea);
                    }
                }
                if (tareasPorAnimal.isEmpty()) {
                    Toast.makeText(getApplicationContext(), R.string.tarea_no_pendiente, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (!formularioCargado && texto(vista.descripcion).isEmpty()) vista.descripcion.setText(grupo.referencia.titulo);
                formularioCargado = true;
                cargarAnimales();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    Toast.makeText(getApplicationContext(), R.string.tarea_grupo_no_encontrada, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    mensaje(R.string.error_carga);
                }
            }
        });
    }

    /** Lista con casillas de los animales activos; el botón del medio marca o quita a todos de una vez. */
    private void elegirAnimales() {
        if (guardando) return;
        SeleccionAnimales.mostrar(this, idGrupoTareas != null ? R.string.grupo_cumplir_dialogo_titulo : R.string.grupo_dialogo_titulo,
                animalesActivos(), elegidos, () -> { elegidosDefinidos = true; refrescarGrupo(); });
    }

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
                // Por defecto reciben la actividad todos los animales activos; después solo quedan los que sigan siéndolo.
                Set<String> activos = new LinkedHashSet<>();
                for (AnimalResumen animal : animalesActivos()) activos.add(animal.idAnimal);
                if (!elegidosDefinidos) { elegidos.clear(); elegidos.addAll(activos); elegidosDefinidos = true; }
                else elegidos.retainAll(activos);
                refrescarGrupo();
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

    /** Corrección de un grupo: trae sus datos comunes y dice a cuántos animales se aplicará. */
    private void cargarGrupo() {
        app().sanidad().grupo(idGrupo, new Callback<GrupoSanitario>() {
            @Override public void exito(GrupoSanitario grupo) {
                if (!disponible()) return;
                EventoSanitario evento = grupo.referencia;
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
                int total = grupo.actividades.size();
                vista.contextoAnimal.setText(getResources().getQuantityString(R.plurals.grupo_contexto_correccion, total, total));
                vista.contextoDetalle.setText(getString(R.string.codigo_nombre,
                        Presentacion.tipoSanidad(SanidadFormActivity.this, evento.tipo), FormatoFecha.corta(evento.fecha)));
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    Toast.makeText(getApplicationContext(), R.string.grupo_no_encontrado, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    mensaje(R.string.error_carga);
                }
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
        if (idGrupo != null) { corregirGrupo(entrada); return; }
        if (!esCorreccion() && todos) { registrarEnGrupo(entrada); return; }
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
                if (ErrorModelo.TAREA.equals(error.faltante)) {
                    Toast.makeText(getApplicationContext(), R.string.tarea_no_encontrada, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (error.campos.containsKey(Campos.ESTADO)) { mensaje(R.string.tarea_no_pendiente); return; }
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    if (ErrorModelo.FINCA.equals(error.faltante)) irAConfiguracion(); else noEncontrada();
                    return;
                }
                mostrarError(error, R.string.error_guardado_actividad);
            }
        };
        if (esCorreccion()) app().sanidad().corregir(idEvento, entrada, texto(vista.motivo), resultado);
        else app().sanidad().registrar(idRegistro, idAnimal, entrada, idTarea, resultado);
    }

    /** Registra la actividad en todos los animales elegidos; si alguno ya no es válido, no se guarda nada. */
    private void registrarEnGrupo(EntradaSanidad entrada) {
        if (elegidos.isEmpty()) { mensaje(R.string.grupo_sin_animales); return; }
        empezarGuardado();
        boolean cumplirGrupo = idGrupoTareas != null;
        app().sanidad().registrarGrupo(idRegistro, new ArrayList<>(elegidos), entrada, cumplirGrupo ? tareasPorAnimal : null, new Callback<List<EventoSanitario>>() {
            @Override public void exito(List<EventoSanitario> creadas) {
                if (!disponible()) return;
                guardando = false;
                int mensaje = cumplirGrupo ? R.plurals.grupo_cumplida : R.plurals.grupo_registrada;
                Toast.makeText(getApplicationContext(), getResources().getQuantityString(mensaje, creadas.size(), creadas.size()),
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO && ErrorModelo.FINCA.equals(error.faltante)) { irAConfiguracion(); return; }
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO && ErrorModelo.TAREA.equals(error.faltante)) {
                    Toast.makeText(getApplicationContext(), R.string.tarea_no_encontrada, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (cumplirGrupo && error.campos.containsKey(Campos.ESTADO)) {
                    // Otra pantalla ya completó o canceló alguna de las tareas: se vuelven a leer y se reintenta.
                    mensaje(R.string.tarea_no_pendiente);
                    cargarTareasDelGrupo();
                    return;
                }
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO || error.campos.containsKey(Campos.ID_ANIMAL)) {
                    // Un animal se vendió o ya no existe mientras se llenaba el formulario: se actualiza la lista.
                    mensaje(R.string.grupo_animal_inactivo);
                    cargarAnimales();
                    return;
                }
                mostrarError(error, R.string.error_guardado_actividad);
            }
        });
    }

    /** Corrige a la vez todas las actividades del grupo, con un motivo. */
    private void corregirGrupo(EntradaSanidad entrada) {
        empezarGuardado();
        app().sanidad().corregirGrupo(idGrupo, entrada, texto(vista.motivo), new Callback<GrupoSanitario>() {
            @Override public void exito(GrupoSanitario grupo) {
                if (!disponible()) return;
                guardando = false;
                int total = grupo.actividades.size();
                Toast.makeText(getApplicationContext(), getResources().getQuantityString(R.plurals.grupo_corregida, total, total), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    Toast.makeText(getApplicationContext(), R.string.grupo_no_encontrado, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                mostrarError(error, R.string.error_guardado_actividad);
            }
        });
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
        vista.aplicarATodos.setEnabled(!ocupado);
        vista.filaAnimales.setEnabled(!ocupado);
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
        estado.putBoolean(ESTADO_TODOS, todos);
        estado.putBoolean(ESTADO_ELEGIDOS_DEFINIDOS, elegidosDefinidos);
        estado.putStringArrayList(ESTADO_ELEGIDOS, new ArrayList<>(elegidos));
        super.onSaveInstanceState(estado);
    }
}
