package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityInicioBinding;
import com.example.ganhard.databinding.ItemBarraBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.repositorio.EstadisticasMes;
import java.time.YearMonth;
import java.util.List;

/**
 * Controlador de Inicio (RF10): resumen del hato, los pesos y los gastos de la finca, mes por mes; el menú ☰ lleva a los
 * demás módulos. El mes elegido manda sobre las tres tarjetas y sobrevive a la rotación.
 */
public class InicioActivity extends BaseActivity {
    private static final String ESTADO_MES = "mes";

    private ActivityInicioBinding vista;
    private YearMonth mesActual, mesElegido, primerMes;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityInicioBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        mesActual = YearMonth.from(Tiempo.hoy());
        mesElegido = estado != null && estado.getString(ESTADO_MES) != null ? YearMonth.parse(estado.getString(ESTADO_MES)) : mesActual;
        primerMes = mesElegido.isBefore(mesActual) ? mesElegido : mesActual;
        vista.toolbar.setNavigationIcon(R.drawable.ic_menu);
        vista.toolbar.setNavigationContentDescription(R.string.abrir_menu);
        vista.toolbar.setNavigationOnClickListener(v -> abrir(MenuActivity.class));
        vista.registrarAnimal.setOnClickListener(v -> abrir(AnimalFormActivity.class));
        vista.reintentar.setOnClickListener(v -> cargar());
        vista.mesAnterior.setOnClickListener(v -> cambiarMes(mesElegido.minusMonths(1)));
        vista.mesSiguiente.setOnClickListener(v -> cambiarMes(mesElegido.plusMonths(1)));
    }

    @Override protected void onSaveInstanceState(Bundle salida) {
        super.onSaveInstanceState(salida);
        salida.putString(ESTADO_MES, mesElegido.toString());
    }

    /** Se recarga al volver para reflejar altas y cambios de estado hechos en otras pantallas. */
    @Override protected void onResume() {
        super.onResume();
        cargar();
    }

    private void abrir(Class<?> pantalla) { startActivity(new Intent(this, pantalla)); }

    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        vista.estadoError.setVisibility(View.GONE);
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (!disponible()) return;
                if (datos == null) {
                    abrir(ConfiguracionActivity.class);
                    finish();
                    return;
                }
                vista.nombreFinca.setText(datos.finca.nombre);
                vista.administrador.setText(getString(R.string.responsable, datos.usuario.nombre));
                buscarPrimerMes();
            }
            @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(); }
        });
    }

    /** El primer mes con datos limita hasta dónde se puede retroceder con la flecha. */
    private void buscarPrimerMes() {
        app().estadisticas().primerMes(new Callback<String>() {
            @Override public void exito(String mes) {
                if (!disponible()) return;
                primerMes = YearMonth.parse(mes);
                if (mesElegido.isAfter(mesActual)) mesElegido = mesActual;
                calcular();
            }
            @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(); }
        });
    }

    private void cambiarMes(YearMonth mes) {
        if (mes.isAfter(mesActual) || mes.isBefore(primerMes)) return;
        mesElegido = mes;
        vista.progreso.setVisibility(View.VISIBLE);
        calcular();
    }

    private void calcular() {
        YearMonth pedido = mesElegido;
        app().estadisticas().deMes(pedido.toString(), new Callback<EstadisticasMes>() {
            @Override public void exito(EstadisticasMes cifras) {
                if (!disponible() || !pedido.equals(mesElegido)) return; // llegó tarde: ya se eligió otro mes
                pintar(cifras);
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.contenido.setVisibility(View.VISIBLE);
            }
            @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(); }
        });
    }

    // --- Pintar las tarjetas ---

    private void pintar(EstadisticasMes cifras) {
        vista.mesTitulo.setText(FormatoFecha.mesLargo(cifras.mes));
        vista.mesAnterior.setEnabled(mesElegido.isAfter(primerMes));
        vista.mesAnterior.setAlpha(vista.mesAnterior.isEnabled() ? 1f : 0.3f);
        vista.mesSiguiente.setEnabled(mesElegido.isBefore(mesActual));
        vista.mesSiguiente.setAlpha(vista.mesSiguiente.isEnabled() ? 1f : 0.3f);
        pintarAnimales(cifras.animales);
        pintarPesos(cifras.pesos, cifras.animales.activos);
        pintarGastos(cifras.gastos);
    }

    private void pintarAnimales(EstadisticasMes.Animales a) {
        vista.animalesActivos.setText(getResources().getQuantityString(R.plurals.animales_activos, a.activos, a.activos));
        int cambio = a.activos - a.activosAnterior;
        vista.animalesVariacion.setText(cambio == 0 ? getString(R.string.est_animales_sin_cambio)
                : getString(R.string.est_animales_variacion, conSigno(cambio)));
        vista.animalesMovimientos.setText(getString(R.string.est_movimientos, a.ingresos, a.ventas, a.fallecidos));
        vista.animalesDetalle.setVisibility(a.activos == 0 ? View.GONE : View.VISIBLE);
        vista.animalesSexo.removeAllViews();
        vista.animalesRazas.removeAllViews();
        if (a.activos == 0) return;
        agregarBarra(vista.animalesSexo, getString(R.string.est_hembras), String.valueOf(a.hembras), porcentaje(a.hembras, a.activos));
        agregarBarra(vista.animalesSexo, getString(R.string.est_machos), String.valueOf(a.machos), porcentaje(a.machos, a.activos));
        List<EstadisticasMes.Conteo> razas = a.razas;
        for (EstadisticasMes.Conteo raza : razas) {
            agregarBarra(vista.animalesRazas, raza.nombre == null ? getString(R.string.est_sin_raza) : raza.nombre,
                    String.valueOf(raza.cantidad), porcentaje(raza.cantidad, a.activos));
        }
    }

    private void pintarPesos(EstadisticasMes.Pesos p, int activos) {
        vista.pesosBarras.removeAllViews();
        boolean hayPesos = p.promedioKg != null;
        vista.pesosVacio.setVisibility(hayPesos ? View.GONE : View.VISIBLE);
        vista.pesosPromedio.setVisibility(hayPesos ? View.VISIBLE : View.GONE);
        vista.pesosVariacion.setVisibility(hayPesos && p.variacionKg != null ? View.VISIBLE : View.GONE);
        vista.pesosCalculo.setVisibility(hayPesos ? View.VISIBLE : View.GONE);
        if (!hayPesos) return;
        vista.pesosPromedio.setText(getString(R.string.est_pesos_promedio, kilos(p.promedioKg)));
        if (p.variacionKg != null) vista.pesosVariacion.setText(getString(R.string.est_pesos_variacion, conSigno(p.variacionKg)));
        agregarBarra(vista.pesosBarras, getString(R.string.est_pesos_pesados), p.pesadosEnElMes + " / " + activos,
                porcentaje(p.pesadosEnElMes, activos));
        vista.pesosCalculo.setText(getString(R.string.est_pesos_calculo, p.animalesConPeso, activos) + "\n"
                + getString(R.string.est_pesos_sin_pesar, p.sinPesar));
    }

    private void pintarGastos(EstadisticasMes.Gastos g) {
        vista.gastosCategorias.removeAllViews();
        if (g.totalCentavos == 0) {
            vista.gastosTotal.setText(Presentacion.pesos(0));
            vista.gastosVariacion.setText(R.string.est_gastos_vacio);
            return;
        }
        vista.gastosTotal.setText(Presentacion.pesos(g.totalCentavos));
        long dif = g.totalCentavos - g.anteriorCentavos;
        if (g.anteriorCentavos == 0) {
            vista.gastosVariacion.setText(R.string.est_gastos_sin_anterior);
        } else if (dif == 0) {
            vista.gastosVariacion.setText(R.string.est_gastos_igual);
        } else {
            String monto = (dif > 0 ? "+" : "−") + Presentacion.pesos(Math.abs(dif));
            int porcentaje = (int) Math.round(100.0 * Math.abs(dif) / g.anteriorCentavos);
            vista.gastosVariacion.setText(getString(R.string.est_gastos_variacion, monto, (dif > 0 ? "+" : "−") + porcentaje + " %"));
        }
        for (EstadisticasMes.CategoriaGasto categoria : g.categorias) {
            int parte = porcentaje(categoria.centavos, g.totalCentavos);
            agregarBarra(vista.gastosCategorias, Presentacion.categoriaGasto(this, categoria.categoria),
                    getString(R.string.est_gasto_categoria_valor, Presentacion.pesos(categoria.centavos), parte), parte);
        }
    }

    // --- Utilidades ---

    private void agregarBarra(ViewGroup contenedor, String etiqueta, String valor, int porciento) {
        ItemBarraBinding barra = ItemBarraBinding.inflate(LayoutInflater.from(this), contenedor, false);
        barra.barraEtiqueta.setText(etiqueta);
        barra.barraValor.setText(valor);
        barra.barraIndicador.setProgressCompat(porciento, false);
        contenedor.addView(barra.getRoot());
    }

    private static int porcentaje(long parte, long total) { return total <= 0 ? 0 : (int) Math.round(100.0 * parte / total); }

    private static String conSigno(int numero) { return numero > 0 ? "+" + numero : numero < 0 ? "−" + Math.abs(numero) : "0"; }

    private static String conSigno(double numero) {
        String texto = Presentacion.kilos(Math.abs(numero));
        return numero > 0 ? "+" + texto : numero < 0 ? "−" + texto : "0";
    }

    private static String kilos(double numero) { return Presentacion.kilos(numero); }

    private void mostrarError() {
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.contenido.setVisibility(View.GONE);
        vista.mensajeError.setText(R.string.error_carga);
        vista.estadoError.setVisibility(View.VISIBLE);
    }
}
