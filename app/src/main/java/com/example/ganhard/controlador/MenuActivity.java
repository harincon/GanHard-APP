package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityMenuBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;

/** Menú de módulos (icono ☰ de Inicio). Un módulo que aún no exista aparece deshabilitado con "Próximamente". */
public class MenuActivity extends BaseActivity {
    private ActivityMenuBinding vista;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityMenuBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        vista.filaInicio.setOnClickListener(v -> {
            startActivity(new Intent(this, InicioActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        vista.filaAnimales.setOnClickListener(v -> startActivity(new Intent(this, AnimalesActivity.class)));
        vista.filaConfiguracion.setOnClickListener(v -> startActivity(new Intent(this, AjustesActivity.class)));
        vista.filaHistorial.setOnClickListener(v -> startActivity(new Intent(this, HistorialActivity.class)));
        vista.filaSanidad.setOnClickListener(v -> startActivity(new Intent(this, SanidadActivity.class)));
        vista.filaGastos.setOnClickListener(v -> startActivity(new Intent(this, GastosActivity.class)));
        vista.filaTareas.setOnClickListener(v -> startActivity(new Intent(this, TareasActivity.class)));
        vista.filaCerrarTodo.setOnClickListener(v -> DialogoBorrarTodo.mostrar(this));
        vista.filaSincronizacion.setOnClickListener(v -> startActivity(new Intent(this, SincronizacionActivity.class)));
    }

    @Override protected void onResume() {
        super.onResume();
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (!disponible()) return;
                if (datos == null) {
                    startActivity(new Intent(MenuActivity.this, ConfiguracionActivity.class));
                    finish();
                    return;
                }
                vista.menuFinca.setText(datos.finca.nombre);
                vista.menuAdministrador.setText(getString(datos.esAdministrador() ? R.string.responsable : R.string.responsable_encargado, datos.usuario.nombre));
            }
            @Override public void error(ErrorModelo error) { /* El menú sigue siendo útil sin el nombre de la finca. */ }
        });
    }
}
