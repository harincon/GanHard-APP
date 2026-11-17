package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityGastosBinding;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.repositorio.PeriodoGastos;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la consulta de gastos (RF13): los gastos vigentes de un mes y su total. Abre en el mes más
 * reciente, que es el actual; el selector ofrece además los meses que tienen gastos.
 */
public class GastosActivity extends BaseActivity {
    private static final String ESTADO_PERIODO = "periodo_elegido";

    private ActivityGastosBinding vista;
    private GastoAdapter adaptador;
    private final List<String> periodos = new ArrayList<>();
    /** Mes elegido ("2026-10"); null hasta conocer los meses disponibles. */
    private String periodo;
    /** Cada consulta lleva un número; la respuesta de una consulta anterior se descarta. */
    private int peticion;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityGastosBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        if (estado != null) periodo = estado.getString(ESTADO_PERIODO);
        adaptador = new GastoAdapter(this, gasto -> startActivity(new Intent(this, GastoFormActivity.class)
                .putExtra(GastoFormActivity.EXTRA_ID_GASTO, gasto.gasto.idGasto)));
        vista.lista.setLayoutManager(new LinearLayoutManager(this));
        vista.lista.setAdapter(adaptador);
        vista.filtroPeriodo.setOnItemClickListener((lista, fila, posicion, id) -> {
            periodo = periodos.get(posicion);
            cargarGastos();
        });
        vista.registrarGasto.setOnClickListener(v -> startActivity(new Intent(this, GastoFormActivity.class)));
        vista.reintentar.setOnClickListener(v -> cargar());
    }

    /** Se recarga al volver para reflejar lo registrado, corregido o anulado en otras pantallas. */
    @Override protected void onResume() {
        super.onResume();
        cargar();
    }

    /** Primero los meses del selector (que además confirman que hay finca) y luego los gastos del elegido. */
    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        app().gastos().periodos(new Callback<List<String>>() {
            @Override public void exito(List<String> meses) {
                if (!disponible()) return;
                periodos.clear();
                periodos.addAll(meses);
                // Los meses vienen del más reciente al más antiguo; si el elegido ya no existe, vuelve al más reciente.
                if (periodo == null || !periodos.contains(periodo)) periodo = periodos.get(0);
                List<String> nombres = new ArrayList<>();
                for (String mes : periodos) nombres.add(FormatoFecha.mesLargo(mes));
                vista.filtroPeriodo.setAdapter(new ArrayAdapter<>(GastosActivity.this, android.R.layout.simple_dropdown_item_1line, nombres));
                vista.filtroPeriodo.setText(FormatoFecha.mesLargo(periodo), false);
                cargarGastos();
            }
            @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(error); }
        });
    }

    private void cargarGastos() {
        final int mia = ++peticion;
        vista.progreso.setVisibility(View.VISIBLE);
        app().gastos().delPeriodo(periodo, new Callback<PeriodoGastos>() {
            @Override public void exito(PeriodoGastos resultado) {
                if (!disponible() || mia != peticion) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.totalPesos.setText(Presentacion.pesos(resultado.totalCentavos));
                adaptador.mostrar(resultado.gastos);
                boolean vacio = resultado.gastos.isEmpty();
                vista.estadoError.setVisibility(View.GONE);
                vista.lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
                vista.estadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
            }
            @Override public void error(ErrorModelo error) { if (disponible() && mia == peticion) mostrarError(error); }
        });
    }

    private void mostrarError(ErrorModelo error) {
        if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO && ErrorModelo.FINCA.equals(error.faltante)) {
            startActivity(new Intent(this, ConfiguracionActivity.class));
            finish();
            return;
        }
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.lista.setVisibility(View.GONE);
        vista.estadoVacio.setVisibility(View.GONE);
        vista.mensajeError.setText(R.string.error_carga);
        vista.estadoError.setVisibility(View.VISIBLE);
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString(ESTADO_PERIODO, periodo);
        super.onSaveInstanceState(estado);
    }
}
