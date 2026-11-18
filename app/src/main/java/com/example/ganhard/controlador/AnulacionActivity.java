package com.example.ganhard.controlador;

import android.os.Bundle;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityAnulacionBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.Gasto;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.repositorio.GrupoSanitario;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Collections;
import java.util.Map;

/**
 * Controlador de la anulación de un registro (RF15, RN07): muestra qué se va a anular, pide el motivo y confirma.
 * Sirve a pesajes, actividades sanitarias y gastos según {@link #EXTRA_ENTIDAD}. El registro se conserva, pero deja
 * de contar; no se puede deshacer. Devuelve RESULT_OK cuando se anuló.
 */
public class AnulacionActivity extends FormularioActivity {
    /** Qué se anula: {@link HistorialCambio#PESAJE}, {@link HistorialCambio#EVENTO_SANITARIO} o {@link HistorialCambio#GASTO}. */
    public static final String EXTRA_ENTIDAD = "entidad";
    public static final String EXTRA_ID = "id_registro";
    /** Con él (y la entidad de sanidad) se anulan a la vez todas las actividades de ese grupo de animales. */
    public static final String EXTRA_ID_GRUPO = "id_grupo";

    private ActivityAnulacionBinding vista;
    private String id, idGrupo, entidad = "";

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityAnulacionBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        String recibida = getIntent().getStringExtra(EXTRA_ENTIDAD);
        entidad = recibida == null ? "" : recibida;
        id = getIntent().getStringExtra(EXTRA_ID);
        vista.toolbar.setTitle(textos(R.string.anular_pesaje, R.string.anular_actividad, R.string.anular_gasto));
        vista.guardar.setEnabled(false); // Se habilita al cargar el registro.
        vista.guardar.setOnClickListener(v -> anular());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        idGrupo = getIntent().getStringExtra(EXTRA_ID_GRUPO);
        if (idGrupo != null) {
            vista.toolbar.setTitle(R.string.grupo_anular_toolbar);
            cargarGrupo();
        } else if (id == null) noEncontrado();
        else switch (entidad) {
            case HistorialCambio.PESAJE: cargarPesaje(); break;
            case HistorialCambio.EVENTO_SANITARIO: cargarActividad(); break;
            case HistorialCambio.GASTO: cargarGasto(); break;
            default: noEncontrado(); break;
        }
    }

    /** Elige el texto según lo que se anula: pesaje, actividad sanitaria o gasto. */
    private int textos(int pesaje, int actividad, int gasto) {
        if (HistorialCambio.EVENTO_SANITARIO.equals(entidad)) return actividad;
        if (HistorialCambio.GASTO.equals(entidad)) return gasto;
        return pesaje;
    }

    private void cargarPesaje() {
        app().pesajes().obtener(id, new Callback<Pesaje>() {
            @Override public void exito(Pesaje pesaje) {
                if (!disponible()) return;
                mostrarDeAnimal(pesaje.idAnimal, getString(R.string.anulacion_detalle, Presentacion.kilos(pesaje.pesoKg),
                        FormatoFecha.fechaYHoraConComa(pesaje.fecha, pesaje.hora)));
            }
            @Override public void error(ErrorModelo error) { if (disponible()) errorDeCarga(error); }
        });
    }

    private void cargarActividad() {
        app().sanidad().obtener(id, new Callback<EventoSanitario>() {
            @Override public void exito(EventoSanitario evento) {
                if (!disponible()) return;
                mostrarDeAnimal(evento.idAnimal, getString(R.string.codigo_nombre, Presentacion.tipoSanidad(AnulacionActivity.this, evento.tipo),
                        FormatoFecha.corta(evento.fecha)));
            }
            @Override public void error(ErrorModelo error) { if (disponible()) errorDeCarga(error); }
        });
    }

    /** Un gasto no es de un animal: el título es su concepto y el detalle, su valor y su fecha. */
    private void cargarGasto() {
        app().gastos().obtener(id, new Callback<Gasto>() {
            @Override public void exito(Gasto gasto) {
                if (!disponible()) return;
                vista.anulacionTitulo.setText(gasto.concepto);
                vista.anulacionDetalle.setText(getString(R.string.gasto_contexto_detalle, Presentacion.pesos(gasto.valorCentavos), FormatoFecha.corta(gasto.fecha)));
                vista.guardar.setEnabled(true);
            }
            @Override public void error(ErrorModelo error) { if (disponible()) errorDeCarga(error); }
        });
    }

    /** Resumen de lo que se va a anular y, encima, el animal al que pertenece. */
    private void mostrarDeAnimal(String idAnimal, String detalle) {
        vista.anulacionDetalle.setText(detalle);
        vista.guardar.setEnabled(true);
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (!disponible()) return;
                vista.anulacionTitulo.setText(animal.nombre == null ? animal.codigo : getString(R.string.codigo_nombre, animal.codigo, animal.nombre));
            }
            @Override public void error(ErrorModelo error) { /* El detalle ya identifica lo que se va a anular. */ }
        });
    }

    private void errorDeCarga(ErrorModelo error) {
        if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) noEncontrado(); else mensaje(R.string.error_carga);
    }

    /** Una aplicación a varios animales: el título dice qué actividad es y el detalle, a cuántos animales alcanza. */
    private void cargarGrupo() {
        app().sanidad().grupo(idGrupo, new Callback<GrupoSanitario>() {
            @Override public void exito(GrupoSanitario grupo) {
                if (!disponible()) return;
                int total = grupo.actividades.size();
                vista.anulacionTitulo.setText(Presentacion.tipoSanidad(AnulacionActivity.this, grupo.referencia.tipo));
                vista.anulacionDetalle.setText(getString(R.string.codigo_nombre,
                        getResources().getQuantityString(R.plurals.grupo_aplicada_a, total, total), FormatoFecha.corta(grupo.referencia.fecha)));
                vista.guardar.setEnabled(true);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) grupoNoEncontrado(); else mensaje(R.string.error_carga);
            }
        });
    }

    private void anularGrupo() {
        app().sanidad().anularGrupo(idGrupo, texto(vista.motivo), new Callback<Integer>() {
            @Override public void exito(Integer total) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), getResources().getQuantityString(R.plurals.grupo_anulada, total, total), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { grupoNoEncontrado(); return; }
                mostrarError(error, R.string.error_anulacion);
            }
        });
    }

    private void grupoNoEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.grupo_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void anular() {
        if (guardando) return;
        empezarGuardado();
        if (idGrupo != null) { anularGrupo(); return; }
        switch (entidad) {
            case HistorialCambio.EVENTO_SANITARIO: app().sanidad().anular(id, texto(vista.motivo), new Resultado<EventoSanitario>()); break;
            case HistorialCambio.GASTO: app().gastos().anular(id, texto(vista.motivo), new Resultado<Gasto>()); break;
            default: app().pesajes().anular(id, texto(vista.motivo), new Resultado<Pesaje>()); break;
        }
    }

    /** El desenlace es el mismo para cualquier registro anulado. */
    private final class Resultado<T> implements Callback<T> {
        @Override public void exito(T anulado) {
            if (!disponible()) return;
            guardando = false;
            Toast.makeText(getApplicationContext(), textos(R.string.pesaje_anulado, R.string.actividad_anulada, R.string.gasto_anulado), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
        @Override public void error(ErrorModelo error) {
            if (!disponible()) return;
            terminarGuardado();
            if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
            mostrarError(error, R.string.error_anulacion);
        }
    }

    @Override protected Map<String, TextInputLayout> campos() {
        return Collections.singletonMap(Campos.MOTIVO, vista.campoMotivo);
    }

    @Override protected boolean hayCambios() { return !texto(vista.motivo).trim().isEmpty(); }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), textos(R.string.pesaje_no_encontrado, R.string.actividad_no_encontrada, R.string.gasto_no_encontrado), Toast.LENGTH_SHORT).show();
        finish();
    }
}
