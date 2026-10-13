package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityInicioBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Controlador de Inicio (RF10): resumen real del inventario y acceso a los módulos disponibles. */
public class InicioActivity extends BaseActivity {
    private ActivityInicioBinding vista;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityInicioBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        vista.registrarAnimal.setOnClickListener(v -> abrir(AnimalFormActivity.class));
        vista.moduloAnimales.setOnClickListener(v -> abrir(AnimalesActivity.class));
        vista.moduloConfiguracion.setOnClickListener(v -> abrir(ConfiguracionActivity.class));
        vista.reintentar.setOnClickListener(v -> cargar());
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
                mostrarFinca(datos);
                contarActivos();
            }
            @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(); }
        });
    }

    private void contarActivos() {
        app().animales().contarActivos(new Callback<Integer>() {
            @Override public void exito(Integer activos) {
                if (!disponible()) return;
                vista.inventarioActivos.setText(getResources().getQuantityString(R.plurals.animales_activos, activos, activos));
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.contenido.setVisibility(View.VISIBLE);
            }
            @Override public void error(ErrorModelo error) { if (disponible()) mostrarError(); }
        });
    }

    private void mostrarFinca(ConfiguracionLocal datos) {
        vista.nombreFinca.setText(datos.finca.nombre);
        vista.administrador.setText(getString(R.string.responsable, datos.usuario.nombre));
        vista.correo.setText(datos.usuario.correo);
        vista.ubicacion.setText(datos.finca.ubicacion == null ? getString(R.string.ubicacion_sin_registrar) : datos.finca.ubicacion);
        vista.area.setText(datos.finca.areaHectareas == null ? getString(R.string.area_sin_registrar)
                : getString(R.string.area_formato, new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(datos.finca.areaHectareas)));
    }

    private void mostrarError() {
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.contenido.setVisibility(View.GONE);
        vista.mensajeError.setText(R.string.error_carga);
        vista.estadoError.setVisibility(View.VISIBLE);
    }
}
