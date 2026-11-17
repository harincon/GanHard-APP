package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityCuentaBinding;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.repositorio.EstadoCuenta;

/**
 * Controlador de la cuenta (opcional): muestra si hay sesión, permite entrar, salir y vincular la finca, y lleva a
 * los integrantes y a las invitaciones recibidas. Sin cuenta la app funciona igual con los datos del teléfono.
 */
public class CuentaActivity extends BaseActivity {
    private ActivityCuentaBinding vista;
    private boolean ocupado;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityCuentaBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        vista.botonIniciar.setOnClickListener(v -> abrirSesion(SesionActivity.INICIO));
        vista.botonCrear.setOnClickListener(v -> abrirSesion(SesionActivity.REGISTRO));
        vista.botonVincular.setOnClickListener(v -> vincular());
        vista.botonCerrarSesion.setOnClickListener(v -> cerrarSesion());
        vista.filaIntegrantes.setOnClickListener(v -> startActivity(new Intent(this, IntegrantesActivity.class)));
        vista.filaInvitaciones.setOnClickListener(v -> startActivity(new Intent(this, InvitacionesActivity.class)));
    }

    /** Se recarga al volver: la sesión o el vínculo pueden haber cambiado en otra pantalla. */
    @Override protected void onResume() {
        super.onResume();
        cargarEstado();
    }

    private void abrirSesion(int modo) {
        startActivity(new Intent(this, SesionActivity.class).putExtra(SesionActivity.EXTRA_MODO, modo));
    }

    private void cargarEstado() {
        cuenta().estado(new CallbackRemoto<EstadoCuenta>() {
            @Override public void exito(EstadoCuenta estado) { if (disponible()) pintar(estado); }
            @Override public void error(ErrorRemoto error) { if (disponible()) mostrar(MensajesRemotos.texto(CuentaActivity.this, error)); }
        });
    }

    private void pintar(EstadoCuenta estado) {
        if (!estado.configurada) {
            vista.cuentaTitulo.setText(R.string.cuenta_sin_configurar_titulo);
            vista.cuentaDetalle.setText(R.string.cuenta_sin_configurar_texto);
            vista.bloqueSinSesion.setVisibility(View.GONE);
            vista.bloqueConSesion.setVisibility(View.GONE);
            return;
        }
        if (!estado.conSesion) {
            vista.cuentaTitulo.setText(R.string.cuenta_sin_sesion_titulo);
            vista.cuentaDetalle.setText(R.string.cuenta_sin_sesion_texto);
            vista.bloqueSinSesion.setVisibility(View.VISIBLE);
            vista.bloqueConSesion.setVisibility(View.GONE);
            return;
        }
        vista.cuentaTitulo.setText(R.string.cuenta_con_sesion_titulo);
        vista.cuentaDetalle.setText(estado.correoConfirmado ? estado.correo : getString(R.string.codigo_nombre, estado.correo, getString(R.string.cuenta_correo_sin_confirmar)));
        vista.bloqueSinSesion.setVisibility(View.GONE);
        vista.bloqueConSesion.setVisibility(View.VISIBLE);
        if (!estado.hayFincaLocal) vista.fincaEstado.setText(R.string.cuenta_sin_finca);
        else if (!estado.fincaVinculada) vista.fincaEstado.setText(getString(R.string.cuenta_finca_sin_vincular, estado.nombreFinca));
        else if (estado.fincaDeEstaCuenta) vista.fincaEstado.setText(getString(R.string.cuenta_finca_vinculada, estado.nombreFinca));
        else vista.fincaEstado.setText(getString(R.string.cuenta_finca_de_otra, estado.nombreFinca));
        vista.botonVincular.setVisibility(estado.hayFincaLocal && !estado.fincaVinculada ? View.VISIBLE : View.GONE);
        vista.filaIntegrantes.setVisibility(estado.fincaDeEstaCuenta ? View.VISIBLE : View.GONE);
    }

    private void vincular() {
        if (ocupado) return;
        ocupar(true);
        cuenta().vincularFinca(new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) {
                if (!disponible()) return;
                ocupar(false);
                mostrar(getString(R.string.cuenta_finca_vinculada_ok));
                cargarEstado();
            }
            @Override public void error(ErrorRemoto error) { if (disponible()) fallo(error); }
        });
    }

    private void cerrarSesion() {
        if (ocupado) return;
        ocupar(true);
        cuenta().cerrarSesion(new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) {
                if (!disponible()) return;
                ocupar(false);
                mostrar(getString(R.string.cuenta_sesion_cerrada));
                cargarEstado();
            }
            @Override public void error(ErrorRemoto error) { if (disponible()) fallo(error); }
        });
    }

    private void fallo(ErrorRemoto error) {
        ocupar(false);
        mostrar(MensajesRemotos.texto(this, error));
        if (error.tipo == ErrorRemoto.Tipo.SIN_SESION) cargarEstado();
    }

    private void ocupar(boolean valor) {
        ocupado = valor;
        vista.progreso.setVisibility(valor ? View.VISIBLE : View.INVISIBLE);
        vista.botonVincular.setEnabled(!valor);
        vista.botonCerrarSesion.setEnabled(!valor);
        if (valor) vista.mensaje.setVisibility(View.GONE);
    }

    private void mostrar(String texto) {
        vista.mensaje.setText(texto);
        vista.mensaje.setVisibility(View.VISIBLE);
    }
}
