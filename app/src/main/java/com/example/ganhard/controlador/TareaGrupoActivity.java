package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityTareaGrupoBinding;
import com.example.ganhard.databinding.ItemPersonaBinding;
import com.example.ganhard.modelo.datos.TareaResumen;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.repositorio.GrupoTarea;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Una tarea programada para varios animales: sus datos comunes, cuántos la tienen pendiente, completada o cancelada, y la
 * lista de animales. Se puede cancelar lo pendiente de todo el grupo y, si es de sanidad, cumplirla para todos con una sola
 * actividad. Tocar un animal abre su propia tarea, que también se cumple o cancela sola.
 */
public class TareaGrupoActivity extends BaseActivity {
    public static final String EXTRA_ID_GRUPO = "id_grupo";

    private ActivityTareaGrupoBinding vista;
    private String idGrupo;
    private GrupoTarea grupo;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityTareaGrupoBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idGrupo = getIntent().getStringExtra(EXTRA_ID_GRUPO);
        vista.cumplirGrupo.setOnClickListener(v -> startActivity(new Intent(this, SanidadFormActivity.class)
                .putExtra(SanidadFormActivity.EXTRA_ID_GRUPO_TAREAS, idGrupo)));
        vista.cancelarGrupo.setOnClickListener(v -> confirmarCancelacion());
        if (idGrupo == null) noEncontrado();
    }

    /** Se recarga al volver: cumplir o cancelar tareas cambia los conteos y los animales pendientes. */
    @Override protected void onResume() {
        super.onResume();
        if (idGrupo != null) cargar();
    }

    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        app().tareas().grupo(idGrupo, new Callback<GrupoTarea>() {
            @Override public void exito(GrupoTarea cargado) {
                if (!disponible()) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.mensaje.setVisibility(View.GONE);
                grupo = cargado;
                pintar(cargado);
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

    private void pintar(GrupoTarea grupo) {
        Tarea referencia = grupo.referencia;
        vista.grupoTipo.setText(getResources().getQuantityString(R.plurals.tarea_grupo_para, grupo.tareas.size(), grupo.tareas.size()));
        vista.grupoTitulo.setText(referencia.titulo);
        vista.grupoDetalle.setText(getString(R.string.codigo_nombre, Presentacion.tipoTarea(this, referencia),
                getString(R.string.tarea_detalle_programada, FormatoFecha.corta(referencia.fechaProgramada))));
        vista.grupoResumen.setText(getString(R.string.tarea_grupo_resumen, grupo.pendientes, grupo.completadas, grupo.canceladas));
        vista.grupoDescripcion.setText(referencia.descripcion);
        vista.grupoDescripcion.setVisibility(referencia.descripcion == null ? View.GONE : View.VISIBLE);
        // Solo lo pendiente se puede cancelar, y solo la de sanidad se cumple para todos con una actividad.
        vista.cancelarGrupo.setVisibility(grupo.pendientes > 0 ? View.VISIBLE : View.GONE);
        vista.cumplirGrupo.setVisibility(grupo.pendientes > 0 && Tarea.SANIDAD.equals(referencia.tipo) ? View.VISIBLE : View.GONE);
        vista.listaAnimales.removeAllViews();
        for (TareaResumen fila : grupo.tareas) {
            String animal = fila.animalNombre == null ? fila.animalCodigo
                    : getString(R.string.codigo_nombre, fila.animalCodigo, fila.animalNombre);
            String estado = Presentacion.estadoTarea(this, fila.tarea.estado);
            ItemPersonaBinding tarjeta = TarjetaPersona.agregar(this, vista.listaAnimales, estado, animal, null, null);
            tarjeta.getRoot().setOnClickListener(v -> startActivity(new Intent(this, TareaDetalleActivity.class)
                    .putExtra(TareaDetalleActivity.EXTRA_ID_TAREA, fila.tarea.idTarea)));
        }
    }

    private void confirmarCancelacion() {
        if (grupo == null || grupo.pendientes == 0) return;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.tarea_grupo_cancelar_titulo)
                .setMessage(getResources().getQuantityString(R.plurals.tarea_grupo_cancelar_mensaje, grupo.pendientes, grupo.pendientes))
                .setPositiveButton(R.string.tarea_grupo_cancelar_confirmar, (dialogo, boton) -> cancelar())
                .setNegativeButton(R.string.volver, null).show();
    }

    private void cancelar() {
        vista.mensaje.setVisibility(View.GONE);
        vista.cancelarGrupo.setEnabled(false);
        app().tareas().cancelarGrupo(idGrupo, new Callback<Integer>() {
            @Override public void exito(Integer canceladas) {
                if (!disponible()) return;
                vista.cancelarGrupo.setEnabled(true);
                Toast.makeText(getApplicationContext(), getResources().getQuantityString(R.plurals.tarea_grupo_canceladas_ok, canceladas, canceladas),
                        Toast.LENGTH_SHORT).show();
                cargar();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                vista.cancelarGrupo.setEnabled(true);
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
                // Otra pantalla ya cerró lo que faltaba: se avisa y se muestra el estado real.
                vista.mensaje.setText(error.campos.isEmpty() ? R.string.error_carga : R.string.tarea_no_pendiente);
                vista.mensaje.setVisibility(View.VISIBLE);
                cargar();
            }
        });
    }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.tarea_grupo_no_encontrada, Toast.LENGTH_SHORT).show();
        finish();
    }
}
