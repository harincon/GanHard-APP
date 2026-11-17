package com.example.ganhard.controlador;

import android.os.Bundle;
import android.view.View;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityHierrosBinding;
import com.example.ganhard.databinding.ItemPersonaBinding;
import com.example.ganhard.modelo.entidad.Hierro;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.List;

/**
 * Catálogo de hierros de la finca: crear, editar, desactivar y reactivar. Un hierro no se borra; desactivado deja de
 * ofrecerse para animales nuevos pero se sigue viendo en los que ya lo tienen. Tocar un hierro lo edita.
 */
public class HierrosActivity extends BaseActivity {
    private ActivityHierrosBinding vista;
    private boolean ocupado;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityHierrosBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        vista.agregarHierro.setOnClickListener(v -> DialogoHierro.mostrar(this, null, hierro -> {
            mostrar(getString(R.string.hierro_guardado));
            cargar();
        }));
    }

    @Override protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        app().hierros().listar(false, new Callback<List<Hierro>>() {
            @Override public void exito(List<Hierro> hierros) {
                if (!disponible()) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                pintar(hierros);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                mostrar(getString(R.string.error_carga));
            }
        });
    }

    private void pintar(List<Hierro> hierros) {
        vista.listaHierros.removeAllViews();
        vista.sinHierros.setVisibility(hierros.isEmpty() ? View.VISIBLE : View.GONE);
        for (Hierro hierro : hierros) {
            boolean activo = hierro.activo == 1;
            ItemPersonaBinding tarjeta = TarjetaPersona.agregar(this, vista.listaHierros,
                    getString(activo ? R.string.hierro_activo_etiqueta : R.string.hierro_desactivado_etiqueta), hierro.nombre, hierro.descripcion, null);
            tarjeta.getRoot().setOnClickListener(v -> DialogoHierro.mostrar(this, hierro, guardado -> {
                mostrar(getString(R.string.hierro_guardado));
                cargar();
            }));
            TarjetaPersona.accion(tarjeta, activo ? R.string.hierro_desactivar : R.string.hierro_reactivar, v -> cambiarActivo(hierro, !activo));
        }
    }

    private void cambiarActivo(Hierro hierro, boolean activo) {
        if (ocupado) return;
        ocupado = true;
        app().hierros().cambiarActivo(hierro.idHierro, activo, new Callback<Hierro>() {
            @Override public void exito(Hierro cambiado) {
                ocupado = false;
                if (!disponible()) return;
                mostrar(getString(activo ? R.string.hierro_reactivado : R.string.hierro_desactivado));
                cargar();
            }
            @Override public void error(ErrorModelo error) {
                ocupado = false;
                if (!disponible()) return;
                mostrar(getString(error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO ? R.string.hierro_no_encontrado : R.string.error_carga));
                cargar();
            }
        });
    }

    private void mostrar(String texto) {
        vista.mensaje.setText(texto);
        vista.mensaje.setVisibility(View.VISIBLE);
    }
}
