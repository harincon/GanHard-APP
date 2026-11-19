package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivitySincronizacionBinding;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.repositorio.EstadoCuenta;
import com.example.ganhard.modelo.repositorio.EstadoSincronizacion;
import com.example.ganhard.modelo.repositorio.ResultadoSincronizacion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Respaldo en la nube: dice cuántos cambios faltan por subir y cuándo se subió por última vez, y los sube con un
 * botón. Si todavía no se puede (sin cuenta, sin sesión o con la finca sin vincular), explica qué falta y lleva a la
 * cuenta. Por ahora solo sube; no baja datos.
 */
public class SincronizacionActivity extends BaseActivity {
    private ActivitySincronizacionBinding vista;
    private boolean ocupado;
    /** Cada lectura del estado lleva un número; la respuesta de una anterior se descarta. */
    private int lectura;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivitySincronizacionBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        vista.sincronizar.setOnClickListener(v -> sincronizar());
        vista.irCuenta.setOnClickListener(v -> startActivity(new Intent(this, CuentaActivity.class)));
        vista.reenviar.setOnClickListener(v -> confirmarReenvio());
    }

    /** Se recarga al volver: la cuenta o los datos pueden haber cambiado en otra pantalla. */
    @Override protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        final int mia = ++lectura;
        cuenta().estado(new CallbackRemoto<EstadoCuenta>() {
            @Override public void exito(EstadoCuenta cuenta) {
                if (!disponible() || mia != lectura) return;
                leerSincronizacion(cuenta, mia);
            }
            @Override public void error(ErrorRemoto error) { if (disponible() && mia == lectura) mostrarMensaje(MensajesRemotos.texto(SincronizacionActivity.this, error)); }
        });
    }

    private void leerSincronizacion(EstadoCuenta cuenta, int mia) {
        app().sincronizacion().estado(new CallbackRemoto<EstadoSincronizacion>() {
            @Override public void exito(EstadoSincronizacion estado) {
                if (!disponible() || mia != lectura) return;
                pintar(cuenta, estado);
            }
            @Override public void error(ErrorRemoto error) { if (disponible() && mia == lectura) mostrarMensaje(MensajesRemotos.texto(SincronizacionActivity.this, error)); }
        });
    }

    private void pintar(EstadoCuenta cuenta, EstadoSincronizacion estado) {
        vista.pendientes.setText(estado.pendientes == 0 ? getString(R.string.sinc_todo_respaldado)
                : getResources().getQuantityString(R.plurals.sinc_pendientes, estado.pendientes, estado.pendientes));
        vista.ultimaVez.setText(estado.ultimaSubida == null ? getString(R.string.sinc_nunca)
                : getString(R.string.sinc_ultima_vez, MensajesRemotos.fecha(estado.ultimaSubida)));
        int falta = loQueFalta(cuenta);
        vista.aviso.setVisibility(falta == 0 ? View.GONE : View.VISIBLE);
        if (falta != 0) vista.aviso.setText(falta);
        vista.irCuenta.setVisibility(falta == 0 || falta == R.string.sinc_sin_configurar ? View.GONE : View.VISIBLE);
        vista.sincronizar.setVisibility(falta == 0 ? View.VISIBLE : View.GONE);
        vista.sincronizar.setEnabled(!ocupado);
        vista.reenviar.setEnabled(!ocupado);
    }

    /** El texto de lo que falta para poder respaldar, o 0 si ya se puede. */
    private static int loQueFalta(EstadoCuenta cuenta) {
        if (!cuenta.configurada) return R.string.sinc_sin_configurar;
        if (!cuenta.conSesion) return R.string.sinc_sin_sesion;
        if (!cuenta.fincaVinculada) return R.string.sinc_sin_vincular;
        if (!cuenta.fincaDeEstaCuenta) return R.string.sinc_otra_cuenta;
        return 0;
    }

    // --- Subir ---

    private void sincronizar() {
        if (ocupado) return;
        ocupar(true);
        mostrarMensaje(getString(R.string.sinc_subiendo));
        app().sincronizacion().sincronizar(subidas -> {
            if (disponible()) vista.mensaje.setText(getResources().getQuantityString(R.plurals.sinc_subiendo_n, subidas, subidas));
        }, new CallbackRemoto<ResultadoSincronizacion>() {
            @Override public void exito(ResultadoSincronizacion resultado) {
                if (!disponible()) return;
                ocupar(false);
                mostrarMensaje(resultado.subidas == 0 ? getString(R.string.sinc_exito_nada)
                        : getResources().getQuantityString(R.plurals.sinc_exito, resultado.subidas, resultado.subidas));
                cargar();
            }
            @Override public void error(ErrorRemoto error) {
                if (!disponible()) return;
                ocupar(false);
                mostrarMensaje(MensajesRemotos.texto(SincronizacionActivity.this, error));
                cargar();
            }
        });
    }

    private void confirmarReenvio() {
        if (ocupado) return;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.sinc_reenviar_titulo).setMessage(R.string.sinc_reenviar_texto)
                .setPositiveButton(R.string.sinc_reenviar_confirmar, (dialogo, boton) -> reenviar())
                .setNegativeButton(R.string.volver, null).show();
    }

    private void reenviar() {
        app().sincronizacion().volverAEnviarTodo(new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) {
                if (!disponible()) return;
                mostrarMensaje(getString(R.string.sinc_reenviar_listo));
                cargar();
            }
            @Override public void error(ErrorRemoto error) { if (disponible()) mostrarMensaje(MensajesRemotos.texto(SincronizacionActivity.this, error)); }
        });
    }

    private void ocupar(boolean valor) {
        ocupado = valor;
        vista.progreso.setVisibility(valor ? View.VISIBLE : View.INVISIBLE);
        vista.sincronizar.setEnabled(!valor);
        vista.reenviar.setEnabled(!valor);
    }

    private void mostrarMensaje(String texto) {
        vista.mensaje.setText(texto);
        vista.mensaje.setVisibility(View.VISIBLE);
    }
}
