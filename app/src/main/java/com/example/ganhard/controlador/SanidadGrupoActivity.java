package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivitySanidadGrupoBinding;
import com.example.ganhard.databinding.ItemPersonaBinding;
import com.example.ganhard.modelo.datos.EventoSanitarioResumen;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.repositorio.GrupoSanitario;

/**
 * Una actividad sanitaria aplicada a varios animales: sus datos comunes, los animales que la tienen y las acciones de
 * corregirla o anularla para todos. Tocar un animal abre la corrección de su actividad, que también se puede anular sola.
 */
public class SanidadGrupoActivity extends BaseActivity {
    public static final String EXTRA_ID_GRUPO = "id_grupo";

    private ActivitySanidadGrupoBinding vista;
    private String idGrupo;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivitySanidadGrupoBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idGrupo = getIntent().getStringExtra(EXTRA_ID_GRUPO);
        vista.corregirGrupo.setOnClickListener(v -> startActivity(new Intent(this, SanidadFormActivity.class)
                .putExtra(SanidadFormActivity.EXTRA_ID_GRUPO, idGrupo)));
        vista.anularGrupo.setOnClickListener(v -> startActivity(new Intent(this, AnulacionActivity.class)
                .putExtra(AnulacionActivity.EXTRA_ENTIDAD, HistorialCambio.EVENTO_SANITARIO)
                .putExtra(AnulacionActivity.EXTRA_ID_GRUPO, idGrupo)));
        if (idGrupo == null) noEncontrado();
    }

    /** Se recarga al volver: una corrección cambia sus datos y una anulación total deja el grupo sin nada. */
    @Override protected void onResume() {
        super.onResume();
        if (idGrupo != null) cargar();
    }

    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        app().sanidad().grupo(idGrupo, new Callback<GrupoSanitario>() {
            @Override public void exito(GrupoSanitario grupo) {
                if (!disponible()) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.mensaje.setVisibility(View.GONE);
                pintar(grupo);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
                vista.mensaje.setText(R.string.error_carga);
                vista.mensaje.setVisibility(View.VISIBLE);
            }
        });
    }

    private void pintar(GrupoSanitario grupo) {
        EventoSanitario referencia = grupo.referencia;
        int total = grupo.actividades.size();
        vista.grupoCantidad.setText(getResources().getQuantityString(R.plurals.grupo_aplicada_a, total, total));
        vista.grupoTipo.setText(Presentacion.tipoSanidad(this, referencia.tipo));
        vista.grupoDetalle.setText(referencia.producto == null ? FormatoFecha.corta(referencia.fecha)
                : getString(R.string.codigo_nombre, FormatoFecha.corta(referencia.fecha), referencia.producto));
        vista.grupoDescripcion.setText(referencia.descripcion);
        vista.listaAnimales.removeAllViews();
        for (EventoSanitarioResumen actividad : grupo.actividades) {
            String animal = actividad.animalNombre == null ? actividad.animalCodigo
                    : getString(R.string.codigo_nombre, actividad.animalCodigo, actividad.animalNombre);
            ItemPersonaBinding tarjeta = TarjetaPersona.agregar(this, vista.listaAnimales, null, animal, null, null);
            tarjeta.getRoot().setOnClickListener(v -> startActivity(new Intent(this, SanidadFormActivity.class)
                    .putExtra(SanidadFormActivity.EXTRA_ID_EVENTO, actividad.evento.idEventoSanitario)));
        }
    }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.grupo_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
