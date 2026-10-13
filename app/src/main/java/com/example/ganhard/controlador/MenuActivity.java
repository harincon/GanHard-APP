package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityMenuBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;

/** Menú de módulos (icono ☰ de Inicio). Los módulos que aún no existen aparecen deshabilitados con "Próximamente". */
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
        for (FilaNavegacion pendiente : new FilaNavegacion[]{vista.filaSanidad, vista.filaTareas, vista.filaGastos,
                vista.filaHistorial, vista.filaSincronizacion}) {
            pendiente.setHabilitada(false);
        }
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
                vista.menuAdministrador.setText(getString(R.string.responsable, datos.usuario.nombre));
            }
            @Override public void error(ErrorModelo error) { /* El menú sigue siendo útil sin el nombre de la finca. */ }
        });
    }
}
