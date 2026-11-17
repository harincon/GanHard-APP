package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityIntegrantesBinding;
import com.example.ganhard.databinding.ItemPersonaBinding;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.remoto.IntegranteRemoto;
import com.example.ganhard.modelo.remoto.IntegrantesRemotos;
import com.example.ganhard.modelo.remoto.InvitacionRemota;

/**
 * Lo que ve el dueño de la finca: quién tiene o tuvo acceso (con la fecha y el motivo de los retiros) y qué
 * invitaciones envió. Desde aquí invita, cancela invitaciones y retira accesos.
 */
public class IntegrantesActivity extends BaseActivity {
    private ActivityIntegrantesBinding vista;
    private boolean ocupado;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityIntegrantesBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        vista.botonInvitar.setOnClickListener(v -> startActivity(new Intent(this, InvitarActivity.class)));
    }

    /** Se recarga al volver: una invitación o un retiro acaban de cambiar la lista. */
    @Override protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        ocupar(true);
        cuenta().integrantes(new CallbackRemoto<IntegrantesRemotos>() {
            @Override public void exito(IntegrantesRemotos datos) {
                if (!disponible()) return;
                ocupar(false);
                pintar(datos);
            }
            @Override public void error(ErrorRemoto error) { if (disponible()) fallo(error); }
        });
    }

    private void pintar(IntegrantesRemotos datos) {
        vista.listaIntegrantes.removeAllViews();
        vista.listaInvitaciones.removeAllViews();
        for (IntegranteRemoto integrante : datos.integrantes) {
            boolean retirado = IntegranteRemoto.REVOCADO.equals(integrante.estado);
            String etiqueta = getString(R.string.codigo_nombre, MensajesRemotos.rol(this, integrante.rol), MensajesRemotos.estadoIntegrante(this, integrante.estado));
            String nota = retirado
                    ? getString(R.string.integrante_retiro, MensajesRemotos.fecha(integrante.fechaRetiro), integrante.motivoRetiro == null ? "" : integrante.motivoRetiro)
                    : getString(R.string.integrante_ingreso, MensajesRemotos.fecha(integrante.fechaIngreso));
            ItemPersonaBinding tarjeta =
                    TarjetaPersona.agregar(this, vista.listaIntegrantes, etiqueta, integrante.nombre, integrante.correo, nota);
            if (IntegranteRemoto.ENCARGADO.equals(integrante.rol) && IntegranteRemoto.ACTIVO.equals(integrante.estado)) {
                TarjetaPersona.accion(tarjeta, R.string.integrante_retirar, v -> startActivity(new Intent(this, RetirarAccesoActivity.class)
                        .putExtra(RetirarAccesoActivity.EXTRA_ID, integrante.idUsuarioFinca)
                        .putExtra(RetirarAccesoActivity.EXTRA_NOMBRE, integrante.nombre)
                        .putExtra(RetirarAccesoActivity.EXTRA_CORREO, integrante.correo)));
            }
        }
        vista.sinInvitaciones.setVisibility(datos.invitaciones.isEmpty() ? View.VISIBLE : View.GONE);
        for (InvitacionRemota invitacion : datos.invitaciones) {
            boolean pendiente = InvitacionRemota.PENDIENTE.equals(invitacion.estado);
            ItemPersonaBinding tarjeta = TarjetaPersona.agregar(this, vista.listaInvitaciones,
                    MensajesRemotos.estadoInvitacion(this, invitacion.estado), invitacion.correoInvitado,
                    pendiente ? getString(R.string.invitacion_vence, MensajesRemotos.fecha(invitacion.venceEn)) : null, null);
            if (pendiente) TarjetaPersona.accion(tarjeta, R.string.invitacion_cancelar, v -> cancelar(invitacion.idInvitacion));
        }
    }

    private void cancelar(String idInvitacion) {
        if (ocupado) return;
        ocupar(true);
        cuenta().cancelarInvitacion(idInvitacion, new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) {
                if (!disponible()) return;
                cargar();
                mostrar(getString(R.string.invitacion_cancelada_ok));
            }
            @Override public void error(ErrorRemoto error) {
                if (!disponible()) return;
                fallo(error);
                cargar();
            }
        });
    }

    private void fallo(ErrorRemoto error) {
        ocupar(false);
        mostrar(MensajesRemotos.texto(this, error));
    }

    private void ocupar(boolean valor) {
        ocupado = valor;
        vista.progreso.setVisibility(valor ? View.VISIBLE : View.INVISIBLE);
        if (valor) vista.mensaje.setVisibility(View.GONE);
    }

    private void mostrar(String texto) {
        vista.mensaje.setText(texto);
        vista.mensaje.setVisibility(View.VISIBLE);
    }
}
