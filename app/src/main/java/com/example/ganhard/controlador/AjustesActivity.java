package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityAjustesBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.PreferenciaTema;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;

/** Configuración (RF18): perfil, acceso a los datos de la finca y apariencia claro/oscuro/del sistema. */
public class AjustesActivity extends BaseActivity {
    private ActivityAjustesBinding vista;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityAjustesBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        vista.filaDatosFinca.setOnClickListener(v -> startActivity(new Intent(this, ConfiguracionActivity.class)));
        vista.filaTemaClaro.setOnClickListener(v -> elegirTema(AppCompatDelegate.MODE_NIGHT_NO));
        vista.filaTemaOscuro.setOnClickListener(v -> elegirTema(AppCompatDelegate.MODE_NIGHT_YES));
        vista.filaTemaSistema.setOnClickListener(v -> elegirTema(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        mostrarTema(PreferenciaTema.leer(this));
    }

    /** Se recarga al volver para reflejar lo que se haya editado en los datos de la finca. */
    @Override protected void onResume() {
        super.onResume();
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (!disponible()) return;
                if (datos == null) {
                    startActivity(new Intent(AjustesActivity.this, ConfiguracionActivity.class));
                    finish();
                    return;
                }
                vista.ajustesNombre.setText(datos.usuario.nombre);
                vista.ajustesCorreo.setText(datos.usuario.correo);
                String ubicacion = datos.finca.ubicacion;
                vista.filaDatosFinca.setSubtitulo(ubicacion == null ? datos.finca.nombre : datos.finca.nombre + " · " + ubicacion);
            }
            @Override public void error(ErrorModelo error) { /* La pantalla sigue siendo útil: el tema no depende de la base. */ }
        });
    }

    private void elegirTema(int modo) {
        PreferenciaTema.guardar(this, modo);
        mostrarTema(modo);
    }

    /** La opción elegida lleva "●" y dice "Seleccionado"; las demás llevan "○" y un texto de ayuda. */
    private void mostrarTema(int modo) {
        marcar(vista.filaTemaClaro, R.string.tema_claro, R.string.tema_ejemplo_claro, modo == AppCompatDelegate.MODE_NIGHT_NO);
        marcar(vista.filaTemaOscuro, R.string.tema_oscuro, R.string.tema_ejemplo_oscuro, modo == AppCompatDelegate.MODE_NIGHT_YES);
        boolean sistema = modo != AppCompatDelegate.MODE_NIGHT_NO && modo != AppCompatDelegate.MODE_NIGHT_YES;
        marcar(vista.filaTemaSistema, R.string.tema_sistema, R.string.tema_ejemplo_sistema, sistema);
    }

    private void marcar(FilaNavegacion fila, int nombre, int ayuda, boolean elegida) {
        fila.setTitulo(getString(elegida ? R.string.opcion_marcada : R.string.opcion_sin_marcar, getString(nombre)));
        fila.setSubtitulo(getString(elegida ? R.string.tema_seleccionado : ayuda));
    }
}
