package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityInicioBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.text.DecimalFormat;

/** Controlador de Inicio: solicita datos al Modelo y los presenta en la Vista XML. */
public class InicioActivity extends BaseActivity {
    private ActivityInicioBinding vista;
    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityInicioBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        vista.editar.setOnClickListener(v -> startActivity(new Intent(this, ConfiguracionActivity.class)));
        vista.reintentar.setOnClickListener(v -> cargar());
        int tema = AppCompatDelegate.getDefaultNightMode();
        vista.temas.check(tema == AppCompatDelegate.MODE_NIGHT_YES ? R.id.temaOscuro
                : tema == AppCompatDelegate.MODE_NIGHT_NO ? R.id.temaClaro : R.id.temaSistema);
        vista.temas.setOnCheckedChangeListener((grupo, id) -> {
            int modo = id == R.id.temaOscuro ? AppCompatDelegate.MODE_NIGHT_YES
                    : id == R.id.temaClaro ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            getSharedPreferences("preferencias", MODE_PRIVATE).edit().putInt("tema", modo).apply();
            AppCompatDelegate.setDefaultNightMode(modo);
        });
    }
    @Override protected void onResume() { super.onResume(); cargar(); }
    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        vista.editar.setEnabled(false);
        vista.reintentar.setVisibility(View.GONE);
        vista.mensaje.setText(R.string.cargando);
        vista.mensaje.setVisibility(View.VISIBLE);
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (!disponible()) return;
                if (datos == null) {
                    startActivity(new Intent(InicioActivity.this, ConfiguracionActivity.class));
                    finish();
                    return;
                }
                vista.nombreFinca.setText(datos.finca.nombre);
                vista.administrador.setText(getString(R.string.responsable, datos.usuario.nombre));
                vista.correo.setText(datos.usuario.correo);
                vista.ubicacion.setText(datos.finca.ubicacion == null ? getString(R.string.ubicacion_sin_registrar) : datos.finca.ubicacion);
                vista.area.setText(datos.finca.areaHectareas == null ? getString(R.string.area_sin_registrar)
                        : getString(R.string.area_formato, new DecimalFormat("0.##").format(datos.finca.areaHectareas)));
                vista.progreso.setVisibility(View.GONE);
                vista.mensaje.setVisibility(View.GONE);
                vista.editar.setEnabled(true);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                vista.progreso.setVisibility(View.GONE);
                vista.mensaje.setText(R.string.error_carga);
                vista.reintentar.setVisibility(View.VISIBLE);
            }
        });
    }
}
