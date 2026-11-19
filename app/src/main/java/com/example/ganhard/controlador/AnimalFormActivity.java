package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityAnimalFormBinding;
import com.example.ganhard.databinding.DialogIdentificadorBinding;
import com.example.ganhard.databinding.ItemIdentificadorFormBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.datos.IdentificadorResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.AnimalIdentificador;
import com.example.ganhard.modelo.entidad.Hierro;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.EntradaAnimal;
import com.example.ganhard.modelo.reglas.EntradaIdentificador;
import com.example.ganhard.modelo.reglas.ValidadorIdentificadores;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador del formulario de animales: registra uno nuevo (RF03) o, con {@link #EXTRA_ID_ANIMAL}, edita
 * el existente (RF06) y su estado (RF07) sin crear otro registro. Las fechas se escriben dd/mm/aaaa y se
 * entregan al Modelo en ISO; valida y pide guardar al Modelo.
 *
 * <p>El código no se escribe: el Modelo asigna el siguiente al registrar y después solo se muestra. Los
 * identificadores son un hierro del catálogo (obligatorio al registrar) y, con "+", los demás hierros, aretes y números.
 */
public class AnimalFormActivity extends FormularioActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";
    private static final String ESTADO_REGISTRO = "id_registro";
    private static final String ESTADO_CARGADO = "formulario_cargado";
    private static final String ESTADO_IDENTIFICADORES_CARGADOS = "identificadores_cargados";
    private static final String ESTADO_HIERRO = "id_hierro";
    private static final String ESTADO_HIERRO_NOMBRE = "nombre_hierro";
    private static final String ESTADO_EXTRAS = "identificadores_extra";
    private static final String ESTADO_EXIGIR_HIERRO = "exigir_hierro";
    private static final String SEPARADOR = "\u0001";

    /** Un identificador adicional de la lista; los hierros llevan además el nombre para mostrarlo. */
    private static final class Extra {
        final String tipo, idHierro, valor, nombre;
        Extra(String tipo, String idHierro, String valor, String nombre) {
            this.tipo = tipo;
            this.idHierro = idHierro;
            this.valor = valor;
            this.nombre = nombre;
        }
        String clave() { return tipo + SEPARADOR + idHierro + SEPARADOR + valor; }
        String texto() { return AnimalIdentificador.HIERRO.equals(tipo) ? nombre : valor; }
    }

    private ActivityAnimalFormBinding vista;
    private final List<Raza> razas = new ArrayList<>();
    private final List<Hierro> catalogo = new ArrayList<>();
    private final List<Extra> extras = new ArrayList<>();
    private String idAnimal;
    /**
     * Raza que trae el animal en edición y que aún no se ha puesto en el campo; nulo después. La raza se lee del
     * texto del campo, que se puede escribir o elegir de la lista.
     */
    private String razaInicial;
    /** Hierro principal elegido (id y nombre); vacío mientras no se elija. */
    private String idHierro = "", nombreHierro = "";
    /** Id del animal nuevo: se genera al abrir y se conserva al rotar para que guardar dos veces no duplique. */
    private String idRegistro;
    private String ingresoInicial;
    /** Datos y estado del animal al abrir la edición; permiten saber si hay cambios sin guardar. */
    private EntradaAnimal original;
    private String estadoOriginal = "", fechaEstadoOriginal = "";
    /** Identificadores al abrir la edición, como claves comparables. */
    private String hierroOriginal = "";
    private List<String> extrasOriginales = new ArrayList<>();
    /** Falso en el teléfono de una persona encargada: solo el dueño crea hierros nuevos. */
    private boolean puedeCrearHierros = true;
    private boolean restaurado, razasCargadas;
    /** En edición, indica que los campos ya recibieron los datos del animal (aunque la pantalla se haya recreado). */
    private boolean formularioCargado, identificadoresCargados;
    /** Al registrar siempre se exige hierro; al editar, solo si el animal ya tenía alguno. */
    private boolean exigirHierro = true;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityAnimalFormBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        restaurado = estado != null;
        formularioCargado = restaurado && estado.getBoolean(ESTADO_CARGADO, false);
        identificadoresCargados = restaurado && estado.getBoolean(ESTADO_IDENTIFICADORES_CARGADOS, false);
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
            vista.guardar.setEnabled(false); // Se habilita al cargar el animal y sus identificadores.
            vista.campoEstado.setVisibility(View.VISIBLE);
            vista.campoFechaEstado.setVisibility(View.VISIBLE);
            vista.notaEstado.setVisibility(View.VISIBLE);
        } else if (!restaurado) {
            vista.ingreso.setText(FormatoFecha.aPantalla(ingresoInicial));
        }
        if (restaurado) {
            idHierro =estado.getString(ESTADO_HIERRO, "");
            nombreHierro = estado.getString(ESTADO_HIERRO_NOMBRE, "");
            exigirHierro = estado.getBoolean(ESTADO_EXIGIR_HIERRO, true);
            List<String> guardados = estado.getStringArrayList(ESTADO_EXTRAS);
            if (guardados != null) {
                for (String texto : guardados) {
                    String[] partes = texto.split(SEPARADOR, -1);
                    extras.add(new Extra(partes[0], partes[1], partes[2], partes[3]));
                }
            }
        }
        vista.guardar.setOnClickListener(v -> guardar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        vista.nacimiento.addTextChangedListener(new MascaraFecha());
        vista.ingreso.addTextChangedListener(new MascaraFecha());
        vista.fechaEstado.addTextChangedListener(new MascaraFecha());
        vista.campoNacimiento.setEndIconOnClickListener(v -> elegirFecha(vista.nacimiento));
        vista.campoIngreso.setEndIconOnClickListener(v -> elegirFecha(vista.ingreso));
        vista.campoFechaEstado.setEndIconOnClickListener(v -> elegirFecha(vista.fechaEstado));
        // La raza se escribe o se elige; elegir "Sin raza" deja el campo vacío.
        vista.raza.setOnItemClickListener((adaptador, fila, posicion, id) -> {
            Object elegido = adaptador == null ? null : adaptador.getItemAtPosition(posicion);
            if (getString(R.string.sin_raza).equals(String.valueOf(elegido))) vista.raza.setText("", false);
        });
        vista.hierro.setOnItemClickListener((adaptador, fila, posicion, id) -> elegirHierro(posicion));
        vista.agregarIdentificador.setOnClickListener(v -> agregarIdentificador());
        // Al elegir otro estado la fecha propone hoy; al volver al estado original, su fecha.
        vista.estado.setOnItemClickListener((adaptador, fila, posicion, id) ->
                vista.fechaEstado.setText(estadoCodigo().equals(estadoOriginal) ? FormatoFecha.aPantalla(fechaEstadoOriginal) : FormatoFecha.aPantalla(Tiempo.hoy().toString())));
        vista.campoHierro.setHint(exigirHierro ? R.string.hierro_hint_obligatorio : R.string.hierro_hint_opcional);
        mostrarHierro();
        mostrarExtras();
        verificarFinca();
        cargarRazas();
        cargarHierros();
        if (esEdicion()) {
            cargarAnimal();
            cargarIdentificadores();
        }
    }

    private boolean esEdicion() { return idAnimal != null; }

    /** Sin finca configurada no hay a dónde asociar el animal: se lleva al usuario a configurarla. */
    private void verificarFinca() {
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (!disponible()) return;
                if (datos == null) irAConfiguracion();
                else if (!datos.esAdministrador()) { puedeCrearHierros = false; actualizarMenuHierros(); } // solo el dueño crea hierros
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

    /** Los hierros activos del catálogo son los que se ofrecen; el último renglón del menú crea uno nuevo. */
    private void cargarHierros() {
        app().hierros().listar(true, new Callback<List<Hierro>>() {
            @Override public void exito(List<Hierro> lista) {
                if (!disponible()) return;
                catalogo.clear();
                catalogo.addAll(lista);
                actualizarMenuHierros();
            }
            @Override public void error(ErrorModelo error) {
                if (disponible() && !(error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO)) mensaje(R.string.error_carga);
            }
        });
    }

    private void actualizarMenuHierros() {
        vista.hierro.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nombresConNuevo()));
        mostrarHierro();
    }

    private List<String> nombresConNuevo() {
        List<String> nombres = new ArrayList<>();
        for (Hierro hierro : catalogo) nombres.add(hierro.nombre);
        if (puedeCrearHierros) nombres.add(getString(R.string.hierro_nuevo_opcion));
        return nombres;
    }

    private void elegirHierro(int posicion) {
        if (posicion >= catalogo.size()) {
            mostrarHierro(); // Devuelve el texto anterior mientras se crea el nuevo.
            DialogoHierro.mostrar(this, null, nuevo -> {
                catalogo.add(nuevo);
                idHierro = nuevo.idHierro;
                nombreHierro = nuevo.nombre;
                actualizarMenuHierros();
            });
            return;
        }
        idHierro = catalogo.get(posicion).idHierro;
        nombreHierro = catalogo.get(posicion).nombre;
        vista.campoHierro.setError(null);
    }

    private void mostrarHierro() { vista.hierro.setText(nombreHierro, false); }

    private void cargarAnimal() {
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                original = entradaDe(animal);
                estadoOriginal = animal.estado;
                fechaEstadoOriginal = animal.fechaEstado;
                vista.notaCodigo.setText(getString(R.string.codigo_automatico_existente, animal.codigo));
                vista.notaCodigo.setVisibility(View.VISIBLE);
                if (!formularioCargado) {
                    vista.nombre.setText(original.nombre);
                    vista.sexo.setText(Presentacion.sexo(AnimalFormActivity.this, original.sexo), false);
                    vista.nacimiento.setText(FormatoFecha.aPantalla(original.fechaNacimiento));
                    vista.ingreso.setText(FormatoFecha.aPantalla(original.fechaIngreso));
                    vista.observaciones.setText(original.observaciones);
                    vista.estado.setText(Presentacion.estado(AnimalFormActivity.this, animal.estado), false);
                    vista.fechaEstado.setText(FormatoFecha.aPantalla(animal.fechaEstado));
                    razaInicial = original.idRaza;
                    mostrarRazaElegida();
                }
                formularioCargado = true;
                habilitarGuardado();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    animalNoEncontrado();
                } else {
                    mensaje(R.string.error_carga);
                }
            }
        });
    }

    /** Los identificadores guardados: el primer hierro es el principal y los demás van en la lista adicional. */
    private void cargarIdentificadores() {
        app().animales().identificadores(idAnimal, new Callback<List<IdentificadorResumen>>() {
            @Override public void exito(List<IdentificadorResumen> guardados) {
                if (!disponible()) return;
                boolean tieneHierro = false;
                String principalId = "", principalNombre = "";
                List<Extra> resto = new ArrayList<>();
                for (IdentificadorResumen identificador : guardados) {
                    boolean esHierro = AnimalIdentificador.HIERRO.equals(identificador.tipo);
                    if (esHierro && !tieneHierro) {
                        tieneHierro = true;
                        principalId = identificador.idHierro;
                        principalNombre = identificador.hierroNombre;
                    } else {
                        resto.add(new Extra(identificador.tipo, esHierro ? identificador.idHierro : "", esHierro ? "" : identificador.valor,
                                esHierro ? identificador.hierroNombre : ""));
                    }
                }
                exigirHierro = tieneHierro;
                vista.campoHierro.setHint(exigirHierro ? R.string.hierro_hint_obligatorio : R.string.hierro_hint_opcional);
                hierroOriginal = principalId;
                extrasOriginales = new ArrayList<>();
                for (Extra extra : resto) extrasOriginales.add(extra.clave());
                if (!identificadoresCargados) {
                    idHierro = principalId;
                    nombreHierro = principalNombre;
                    extras.clear();
                    extras.addAll(resto);
                    mostrarHierro();
                    mostrarExtras();
                }
                identificadoresCargados = true;
                habilitarGuardado();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) animalNoEncontrado(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void habilitarGuardado() {
        if (!esEdicion() || (formularioCargado && identificadoresCargados)) vista.guardar.setEnabled(!guardando);
    }

    // --- Identificadores adicionales ---

    private void mostrarExtras() {
        vista.listaIdentificadores.removeAllViews();
        for (Extra extra : new ArrayList<>(extras)) {
            ItemIdentificadorFormBinding fila = ItemIdentificadorFormBinding.inflate(getLayoutInflater(), vista.listaIdentificadores, false);
            String texto = getString(R.string.identificador_linea, tipoEtiqueta(extra.tipo), extra.texto());
            fila.identificadorTexto.setText(texto);
            fila.identificadorQuitar.setContentDescription(getString(R.string.identificador_quitar_descripcion, texto));
            fila.identificadorQuitar.setOnClickListener(v -> {
                extras.remove(extra);
                mostrarExtras();
            });
            vista.listaIdentificadores.addView(fila.getRoot());
        }
    }

    private String tipoEtiqueta(String tipo) {
        if (AnimalIdentificador.HIERRO.equals(tipo)) return getString(R.string.identificador_tipo_hierro);
        if (AnimalIdentificador.ARETE.equals(tipo)) return getString(R.string.identificador_tipo_arete);
        return getString(R.string.identificador_tipo_numero);
    }

    private int totalIdentificadores() { return extras.size() + (idHierro.isEmpty() ? 0 : 1); }

    /** Cuadro para agregar otro hierro, un arete o un número. El botón "+" no se bloquea: avisa si ya está el máximo. */
    private void agregarIdentificador() {
        if (guardando) return;
        if (totalIdentificadores() >= ValidadorIdentificadores.MAXIMO) {
            mensaje(getString(R.string.error_identificadores_maximo, ValidadorIdentificadores.MAXIMO));
            return;
        }
        ocultarMensaje();
        DialogIdentificadorBinding dialogo = DialogIdentificadorBinding.inflate(getLayoutInflater());
        String[] codigos = {AnimalIdentificador.ARETE, AnimalIdentificador.NUMERO, AnimalIdentificador.HIERRO};
        List<String> etiquetas = new ArrayList<>();
        for (String codigo : codigos) etiquetas.add(tipoEtiqueta(codigo));
        String[] tipo = {AnimalIdentificador.ARETE};
        Hierro[] hierro = {null};
        dialogo.tipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, etiquetas));
        dialogo.tipo.setText(etiquetas.get(0), false);
        Runnable alternar = () -> {
            boolean esHierro = AnimalIdentificador.HIERRO.equals(tipo[0]);
            dialogo.campoHierro.setVisibility(esHierro ? View.VISIBLE : View.GONE);
            dialogo.campoValor.setVisibility(esHierro ? View.GONE : View.VISIBLE);
        };
        alternar.run();
        dialogo.tipo.setOnItemClickListener((adaptador, fila, posicion, id) -> {
            tipo[0] = codigos[posicion];
            alternar.run();
        });
        dialogo.hierro.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nombresConNuevo()));
        dialogo.hierro.setOnItemClickListener((adaptador, fila, posicion, id) -> {
            if (posicion < catalogo.size()) {
                hierro[0] = catalogo.get(posicion);
                dialogo.campoHierro.setError(null);
                return;
            }
            dialogo.hierro.setText(hierro[0] == null ? "" : hierro[0].nombre, false);
            DialogoHierro.mostrar(this, null, nuevo -> {
                catalogo.add(nuevo);
                hierro[0] = nuevo;
                dialogo.hierro.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nombresConNuevo()));
                dialogo.hierro.setText(nuevo.nombre, false);
                actualizarMenuHierros();
            });
        });
        AlertDialog cuadro = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.identificador_dialogo_titulo)
                .setView(dialogo.getRoot())
                .setPositiveButton(R.string.identificador_dialogo_agregar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        cuadro.show();
        // El botón se conecta después de mostrar el cuadro; si no, pulsarlo lo cerraría antes de validar.
        Button agregar = cuadro.getButton(AlertDialog.BUTTON_POSITIVE);
        agregar.setOnClickListener(v -> {
            if (AnimalIdentificador.HIERRO.equals(tipo[0])) {
                if (hierro[0] == null) { dialogo.campoHierro.setError(getString(R.string.error_hierro_obligatorio)); return; }
                extras.add(new Extra(AnimalIdentificador.HIERRO, hierro[0].idHierro, "", hierro[0].nombre));
            } else {
                String valor = String.valueOf(dialogo.valor.getText()).trim();
                if (valor.isEmpty()) { dialogo.campoValor.setError(getString(R.string.error_identificador_valor)); return; }
                if (valor.length() > ValidadorIdentificadores.VALOR_MAXIMO) {
                    dialogo.campoValor.setError(getString(R.string.error_identificador_largo, ValidadorIdentificadores.VALOR_MAXIMO));
                    return;
                }
                extras.add(new Extra(tipo[0], "", valor, ""));
            }
            cuadro.dismiss();
            mostrarExtras();
        });
    }

    /** Pone en el campo la raza que trae el animal, cuando ya se conocen tanto la lista como la raza. */
    private void mostrarRazaElegida() {
        if (!razasCargadas || razaInicial == null) return;
        vista.raza.setText(nombreDeRaza(razaInicial), false);
        razaInicial = null;
    }

    private String nombreDeRaza(String idRaza) {
        for (Raza raza : razas) if (raza.idRaza.equals(idRaza)) return raza.nombre;
        return "";
    }

    /** Minúsculas y sin tildes: "cebu" y "Cebú" son la misma raza. */
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private static EntradaAnimal entradaDe(Animal animal) {
        return new EntradaAnimal(animal.codigo, animal.nombre, animal.sexo, animal.idRaza,
                animal.fechaNacimiento, animal.fechaIngreso, animal.observaciones);
    }

    private void guardar() {
        if (guardando) return;
        EntradaAnimal entrada = entrada();
        empezarGuardado();
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
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    // Sin finca se configura primero; si lo que falta es el animal, ya no hay qué editar.
                    if (ErrorModelo.FINCA.equals(error.faltante)) irAConfiguracion(); else animalNoEncontrado();
                    return;
                }
                mostrarErroresDelAnimal(error);
            }
        };
        if (esEdicion()) app().animales().actualizar(idAnimal, entrada, estadoCodigo(), FormatoFecha.aIso(texto(vista.fechaEstado)), resultado);
        else app().animales().registrar(idRegistro, entrada, resultado);
    }

    /**
     * Cada error va junto a su campo. Los de la lista de identificadores y los de un campo que esta pantalla ya
     * no tiene (como el código, que es automático) salen como mensaje general, para que nunca se guarde en silencio.
     */
    private void mostrarErroresDelAnimal(ErrorModelo error) {
        mostrarErrores(error.campos);
        if (error.campos.containsKey(Campos.IDENTIFICADORES)) {
            mensaje(MensajesError.identificadores(this, error.campos.get(Campos.IDENTIFICADORES)));
            return;
        }
        boolean algunoVisible = false;
        for (String campo : error.campos.keySet()) if (campos().containsKey(campo)) algunoVisible = true;
        if (!algunoVisible) mensaje(R.string.error_guardado_animal);
    }

    private EntradaAnimal entrada() {
        List<EntradaIdentificador> identificadores = new ArrayList<>();
        if (!idHierro.isEmpty()) identificadores.add(new EntradaIdentificador(AnimalIdentificador.HIERRO, idHierro, ""));
        for (Extra extra : extras) identificadores.add(new EntradaIdentificador(extra.tipo, extra.idHierro, extra.valor));
        // Si la raza del animal aún no llegó al campo, se conserva la que ya tenía en lugar de leer un campo vacío.
        EntradaAnimal entrada = new EntradaAnimal(original == null ? "" : original.codigo, texto(vista.nombre), sexoCodigo(),
                razaInicial == null ? "" : razaInicial, FormatoFecha.aIso(texto(vista.nacimiento)), FormatoFecha.aIso(texto(vista.ingreso)),
                texto(vista.observaciones), identificadores);
        return razaInicial == null ? entrada.conRazaEscrita(texto(vista.raza)) : entrada;
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

    @Override protected Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put(Campos.NOMBRE, vista.campoNombre);
        campos.put(Campos.HIERRO, vista.campoHierro);
        campos.put(Campos.SEXO, vista.campoSexo);
        campos.put(Campos.ID_RAZA, vista.campoRaza);
        campos.put(Campos.FECHA_NACIMIENTO, vista.campoNacimiento);
        campos.put(Campos.FECHA_INGRESO, vista.campoIngreso);
        campos.put(Campos.ESTADO, vista.campoEstado);
        campos.put(Campos.FECHA_ESTADO, vista.campoFechaEstado);
        campos.put(Campos.OBSERVACIONES, vista.campoObservaciones);
        return campos;
    }

    @Override protected boolean hayCambios() {
        EntradaAnimal actual = entrada();
        if (esEdicion()) {
            return original != null && !(actual.nombre.equals(original.nombre)
                    && actual.sexo.equals(original.sexo) && razaSinCambios(actual, original)
                    && actual.fechaNacimiento.equals(original.fechaNacimiento) && actual.fechaIngreso.equals(original.fechaIngreso)
                    && actual.observaciones.equals(original.observaciones)
                    && estadoCodigo().equals(estadoOriginal) && FormatoFecha.aIso(texto(vista.fechaEstado)).equals(fechaEstadoOriginal)
                    && idHierro.equals(hierroOriginal) && clavesExtras().equals(extrasOriginales));
        }
        return !actual.nombre.isEmpty() || !actual.sexo.isEmpty() || !texto(vista.raza).trim().isEmpty() || !idHierro.isEmpty() || !extras.isEmpty()
                || !actual.fechaNacimiento.isEmpty() || !actual.fechaIngreso.equals(ingresoInicial) || !actual.observaciones.isEmpty();
    }

    /** La raza es la misma si el texto del campo equivale al nombre de la raza original (o ambos están vacíos). */
    private boolean razaSinCambios(EntradaAnimal actual, EntradaAnimal original) {
        if (razaInicial != null) return true; // todavía no se mostró: no hay nada escrito por la persona
        String antes = original.idRaza.isEmpty() ? "" : nombreDeRaza(original.idRaza);
        return normalizar(texto(vista.raza)).equals(normalizar(antes));
    }

    private List<String> clavesExtras() {
        List<String> claves = new ArrayList<>();
        for (Extra extra : extras) claves.add(extra.clave());
        return claves;
    }

    private void animalNoEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void irAConfiguracion() {
        startActivity(new Intent(this, ConfiguracionActivity.class));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_REGISTRO, idRegistro);
        estado.putBoolean(ESTADO_CARGADO, formularioCargado);
        estado.putBoolean(ESTADO_IDENTIFICADORES_CARGADOS, identificadoresCargados);
        estado.putString(ESTADO_HIERRO, idHierro);
        estado.putString(ESTADO_HIERRO_NOMBRE, nombreHierro);
        estado.putBoolean(ESTADO_EXIGIR_HIERRO, exigirHierro);
        ArrayList<String> guardados = new ArrayList<>();
        for (Extra extra : extras) guardados.add(extra.tipo + SEPARADOR + extra.idHierro + SEPARADOR + extra.valor + SEPARADOR + extra.nombre);
        estado.putStringArrayList(ESTADO_EXTRAS, guardados);
        super.onSaveInstanceState(estado);
    }
}
