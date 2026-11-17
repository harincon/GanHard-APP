package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityGastoFormBinding;
import com.example.ganhard.modelo.entidad.Gasto;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.EntradaGasto;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador del formulario de gastos. Sin {@link #EXTRA_ID_GASTO} registra un gasto (RF13); con él, corrige ese
 * gasto pidiendo un motivo (RF14) y da acceso a su anulación (RF15). El valor se escribe en pesos enteros con puntos
 * de miles y viaja al Modelo solo con dígitos; las reglas y los errores por campo los entrega el Modelo.
 */
public class GastoFormActivity extends FormularioActivity {
    public static final String EXTRA_ID_GASTO = "id_gasto";
    private static final String ESTADO_CATEGORIA = "categoria_elegida", ESTADO_REGISTRO = "id_registro", ESTADO_CARGADO = "formulario_cargado";

    private ActivityGastoFormBinding vista;
    private String idGasto;
    /** Categoría elegida, como la entiende el Modelo; lo que se ve en pantalla es su nombre. */
    private String categoria = "";
    /** Id del gasto nuevo: se genera al abrir y se conserva al rotar para que guardar dos veces no duplique. */
    private String idRegistro;
    private String fechaInicial = "";
    /** Lo que tenía el gasto al abrir la corrección; permite saber si hay cambios sin guardar. */
    private EntradaGasto original;
    private boolean formularioCargado;
    /** Si la anulación termina bien, ya no hay nada que corregir y esta pantalla se cierra con ella. */
    private final ActivityResultLauncher<Intent> anulacion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) { setResult(RESULT_OK); finish(); }
            });

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityGastoFormBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        idGasto = getIntent().getStringExtra(EXTRA_ID_GASTO);
        fechaInicial = FormatoFecha.aPantalla(Tiempo.hoy().toString());
        if (estado != null) {
            categoria = estado.getString(ESTADO_CATEGORIA, "");
            idRegistro = estado.getString(ESTADO_REGISTRO);
            formularioCargado = estado.getBoolean(ESTADO_CARGADO, false);
        } else if (!esCorreccion()) {
            vista.fecha.setText(fechaInicial);
        }
        if (idRegistro == null) idRegistro = UUID.randomUUID().toString();

        List<String> categorias = new ArrayList<>();
        for (String codigo : Gasto.CATEGORIAS) categorias.add(Presentacion.categoriaGasto(this, codigo));
        vista.categoria.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categorias));
        vista.categoria.setOnItemClickListener((adaptador, fila, posicion, id) -> categoria = Gasto.CATEGORIAS.get(posicion));
        if (!categoria.isEmpty()) vista.categoria.setText(Presentacion.categoriaGasto(this, categoria), false);
        vista.fecha.addTextChangedListener(new MascaraFecha());
        vista.valor.addTextChangedListener(new MascaraMiles());
        vista.campoFecha.setEndIconOnClickListener(v -> elegirFecha(vista.fecha));
        vista.guardar.setOnClickListener(v -> guardar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if (esCorreccion()) {
            vista.toolbar.setTitle(R.string.corregir_gasto);
            vista.guardar.setText(R.string.guardar_correccion);
            vista.guardar.setEnabled(false); // Se habilita al cargar el gasto.
            vista.tarjetaInfo.setVisibility(View.GONE);
            vista.tarjetaContexto.setVisibility(View.VISIBLE);
            vista.campoMotivo.setVisibility(View.VISIBLE);
            vista.notaCorreccion.setVisibility(View.VISIBLE);
            vista.tarjetaAnular.setVisibility(View.VISIBLE);
            vista.filaAnular.setOnClickListener(v -> anulacion.launch(new Intent(this, AnulacionActivity.class)
                    .putExtra(AnulacionActivity.EXTRA_ENTIDAD, HistorialCambio.GASTO).putExtra(AnulacionActivity.EXTRA_ID, idGasto)));
            cargarGasto();
        }
    }

    private boolean esCorreccion() { return idGasto != null; }

    private void cargarGasto() {
        app().gastos().obtener(idGasto, new Callback<Gasto>() {
            @Override public void exito(Gasto gasto) {
                if (!disponible()) return;
                String pesos = String.valueOf(gasto.valorCentavos / 100);
                original = new EntradaGasto(gasto.fecha, gasto.categoria, gasto.concepto, pesos);
                if (!formularioCargado) {
                    categoria = gasto.categoria;
                    vista.fecha.setText(FormatoFecha.aPantalla(gasto.fecha));
                    vista.concepto.setText(gasto.concepto);
                    vista.valor.setText(FormatoMiles.agrupar(pesos));
                }
                vista.categoria.setText(Presentacion.categoriaGasto(GastoFormActivity.this, categoria), false);
                formularioCargado = true;
                vista.guardar.setEnabled(true);
                vista.contextoTitulo.setText(gasto.concepto);
                vista.contextoDetalle.setText(getString(R.string.gasto_contexto_detalle,
                        Presentacion.pesos(gasto.valorCentavos), FormatoFecha.corta(gasto.fecha)));
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrado(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void guardar() {
        if (guardando) return;
        EntradaGasto entrada = entrada();
        empezarGuardado();
        Callback<Gasto> resultado = new Callback<Gasto>() {
            @Override public void exito(Gasto gasto) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), esCorreccion() ? R.string.gasto_corregido : R.string.gasto_guardado, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    if (ErrorModelo.FINCA.equals(error.faltante)) irAConfiguracion(); else noEncontrado();
                    return;
                }
                mostrarError(error, R.string.error_guardado_gasto);
            }
        };
        if (esCorreccion()) app().gastos().corregir(idGasto, entrada, texto(vista.motivo), resultado);
        else app().gastos().registrar(idRegistro, entrada, resultado);
    }

    private EntradaGasto entrada() {
        return new EntradaGasto(FormatoFecha.aIso(texto(vista.fecha)), categoria, texto(vista.concepto), FormatoMiles.soloDigitos(texto(vista.valor)));
    }

    @Override protected Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put(Campos.FECHA, vista.campoFecha);
        campos.put(Campos.CATEGORIA, vista.campoCategoria);
        campos.put(Campos.CONCEPTO, vista.campoConcepto);
        campos.put(Campos.VALOR, vista.campoValor);
        campos.put(Campos.MOTIVO, vista.campoMotivo);
        return campos;
    }

    @Override protected boolean hayCambios() {
        EntradaGasto actual = entrada();
        if (esCorreccion()) {
            return original != null && (!actual.fecha.equals(original.fecha) || !actual.categoria.equals(original.categoria)
                    || !actual.concepto.equals(original.concepto) || !actual.valor.equals(original.valor)
                    || !texto(vista.motivo).trim().isEmpty());
        }
        return !actual.categoria.isEmpty() || !texto(vista.fecha).trim().equals(fechaInicial)
                || !actual.concepto.isEmpty() || !actual.valor.isEmpty();
    }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.gasto_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void irAConfiguracion() {
        startActivity(new Intent(this, ConfiguracionActivity.class));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_CATEGORIA, categoria);
        estado.putString(ESTADO_REGISTRO, idRegistro);
        estado.putBoolean(ESTADO_CARGADO, formularioCargado);
        super.onSaveInstanceState(estado);
    }
}
