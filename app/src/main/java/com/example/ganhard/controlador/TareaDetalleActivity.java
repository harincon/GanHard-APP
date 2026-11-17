package com.example.ganhard.controlador;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityTareaDetalleBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Controlador del detalle de una tarea. Una pendiente se puede realizar (RF17), corregir o cancelar; una cerrada
 * solo se consulta. Realizar una de pesaje o sanidad abre el formulario correspondiente con el animal puesto, y la
 * tarea se completa cuando ese formulario guarda; una general se completa aquí, con su fecha de finalización.
 */
public class TareaDetalleActivity extends BaseActivity {
    public static final String EXTRA_ID_TAREA = "id_tarea";

    private ActivityTareaDetalleBinding vista;
    private String idTarea;
    private Tarea tarea;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityTareaDetalleBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idTarea = getIntent().getStringExtra(EXTRA_ID_TAREA);
        vista.realizar.setOnClickListener(v -> realizar());
        vista.filaCorregir.setOnClickListener(v -> startActivity(new Intent(this, TareaFormActivity.class)
                .putExtra(TareaFormActivity.EXTRA_ID_TAREA, idTarea)));
        vista.filaCancelar.setOnClickListener(v -> confirmarCancelacion());
        if (idTarea == null) noEncontrada();
    }

    /** Se recarga al volver: el formulario de pesaje o sanidad pudo completar la tarea. */
    @Override protected void onResume() {
        super.onResume();
        if (idTarea != null) cargar();
    }

    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        app().tareas().obtener(idTarea, new Callback<Tarea>() {
            @Override public void exito(Tarea cargada) {
                if (!disponible()) return;
                tarea = cargada;
                if (cargada.idAnimal == null) mostrar(null);
                else app().animales().obtener(cargada.idAnimal, new Callback<Animal>() {
                    @Override public void exito(Animal animal) { if (disponible()) mostrar(animal); }
                    @Override public void error(ErrorModelo error) { if (disponible()) mostrar(null); }
                });
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrada();
                else { vista.progreso.setVisibility(View.INVISIBLE); mensaje(R.string.error_carga); }
            }
        });
    }

    private void mostrar(Animal animal) {
        vista.progreso.setVisibility(View.INVISIBLE);
        vista.contenido.setVisibility(View.VISIBLE);
        boolean pendiente = Tarea.PENDIENTE.equals(tarea.estado);
        vista.detalleEtiqueta.setText(getString(R.string.codigo_nombre, Presentacion.tipoTarea(this, tarea.tipo), Presentacion.estadoTarea(this, tarea.estado)));
        vista.detalleTitulo.setText(tarea.titulo);
        vista.detalleAnimal.setText(animal == null ? getString(R.string.tarea_general_finca)
                : animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
        String programada = getString(R.string.tarea_detalle_programada, FormatoFecha.corta(tarea.fechaProgramada));
        if (pendiente && tarea.fechaProgramada.compareTo(Tiempo.hoy().toString()) < 0) {
            programada = getString(R.string.codigo_nombre, programada, getString(R.string.tarea_vencida));
        }
        vista.detalleProgramada.setText(programada);
        boolean completada = Tarea.COMPLETADA.equals(tarea.estado) && tarea.fechaCompletada != null;
        vista.detalleCompletada.setVisibility(completada ? View.VISIBLE : View.GONE);
        if (completada) vista.detalleCompletada.setText(getString(R.string.tarea_detalle_completada, FormatoFecha.corta(tarea.fechaCompletada)));
        vista.detalleDescripcion.setVisibility(tarea.descripcion == null ? View.GONE : View.VISIBLE);
        vista.detalleDescripcion.setText(tarea.descripcion);
        vista.acciones.setVisibility(pendiente ? View.VISIBLE : View.GONE);
        vista.realizar.setText(Tarea.GENERAL.equals(tarea.tipo) ? R.string.tarea_marcar_completada : R.string.tarea_realizar);
    }

    private void realizar() {
        if (tarea == null) return;
        vista.mensaje.setVisibility(View.GONE);
        if (Tarea.PESAJE.equals(tarea.tipo)) {
            startActivity(new Intent(this, PesajeFormActivity.class)
                    .putExtra(PesajeFormActivity.EXTRA_ID_ANIMAL, tarea.idAnimal).putExtra(PesajeFormActivity.EXTRA_ID_TAREA, idTarea));
        } else if (Tarea.SANIDAD.equals(tarea.tipo)) {
            startActivity(new Intent(this, SanidadFormActivity.class)
                    .putExtra(SanidadFormActivity.EXTRA_ID_ANIMAL, tarea.idAnimal).putExtra(SanidadFormActivity.EXTRA_ID_TAREA, idTarea));
        } else {
            pedirFechaDeFinalizacion();
        }
    }

    /** Diálogo con la fecha de finalización (hoy por defecto); no se cierra mientras la fecha no sea válida. */
    private void pedirFechaDeFinalizacion() {
        View contenido = getLayoutInflater().inflate(R.layout.dialog_completar_tarea, null);
        TextInputLayout campo = contenido.findViewById(R.id.campoFechaCompletada);
        TextInputEditText fecha = contenido.findViewById(R.id.fechaCompletada);
        fecha.addTextChangedListener(new MascaraFecha());
        fecha.setText(FormatoFecha.aPantalla(Tiempo.hoy().toString()));
        campo.setEndIconOnClickListener(v -> elegirFecha(fecha));
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this).setTitle(R.string.tarea_completar_titulo).setView(contenido)
                .setPositiveButton(R.string.tarea_completar_confirmar, null).setNegativeButton(R.string.cancelar, null).create();
        dialogo.show();
        // Con el listener en null el botón cerraría el diálogo; se instala aquí para validar antes de cerrar.
        dialogo.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> completar(dialogo, campo, FormatoFecha.aIso(String.valueOf(fecha.getText()))));
    }

    private void completar(AlertDialog dialogo, TextInputLayout campo, String fecha) {
        campo.setError(null);
        app().tareas().completar(idTarea, fecha, new Callback<Tarea>() {
            @Override public void exito(Tarea completada) {
                if (!disponible()) return;
                dialogo.dismiss();
                terminar(R.string.tarea_completada);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.campos.containsKey(Campos.FECHA)) {
                    campo.setError(MensajesError.texto(TareaDetalleActivity.this, Campos.FECHA, error.campos.get(Campos.FECHA)));
                    return;
                }
                dialogo.dismiss();
                falloDeCambio(error);
            }
        });
    }

    private void confirmarCancelacion() {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.tarea_cancelar_titulo).setMessage(R.string.tarea_cancelar_mensaje)
                .setPositiveButton(R.string.tarea_cancelar_confirmar, (dialogo, boton) -> cancelar())
                .setNegativeButton(R.string.volver, null).show();
    }

    private void cancelar() {
        vista.mensaje.setVisibility(View.GONE);
        app().tareas().cancelar(idTarea, new Callback<Tarea>() {
            @Override public void exito(Tarea cancelada) { if (disponible()) terminar(R.string.tarea_cancelada); }
            @Override public void error(ErrorModelo error) { if (disponible()) falloDeCambio(error); }
        });
    }

    /** Si otra persona ya cerró la tarea, se avisa y se recarga para mostrar su estado real. */
    private void falloDeCambio(ErrorModelo error) {
        if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrada(); return; }
        mensaje(error.campos.containsKey(Campos.ESTADO) ? R.string.tarea_no_pendiente : R.string.error_guardado_tarea);
        cargar();
    }

    private void terminar(int aviso) {
        Toast.makeText(getApplicationContext(), aviso, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void mensaje(int recurso) {
        vista.mensaje.setText(recurso);
        vista.mensaje.setVisibility(View.VISIBLE);
    }

    private void noEncontrada() {
        Toast.makeText(getApplicationContext(), R.string.tarea_no_encontrada, Toast.LENGTH_SHORT).show();
        finish();
    }
}
