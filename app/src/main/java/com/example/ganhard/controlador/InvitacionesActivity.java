package com.example.ganhard.controlador;

import android.os.Bundle;
import android.view.View;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityInvitacionesBinding;
import com.example.ganhard.databinding.ItemPersonaBinding;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.remoto.IntegranteRemoto;
import com.example.ganhard.modelo.remoto.InvitacionRemota;
import com.example.ganhard.modelo.remoto.PertenenciaRemota;
import java.util.List;

/**
 * Lo que ve cualquier cuenta: las invitaciones que recibió (puede aceptarlas) y las fincas a las que pertenece, con
 * su rol y con el aviso si el administrador le retiró el acceso.
 */
public class InvitacionesActivity extends BaseActivity {
    private ActivityInvitacionesBinding vista;
    private boolean ocupado;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityInvitacionesBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
    }

    @Override protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        ocupar(true);
        cuenta().misInvitaciones(new CallbackRemoto<List<InvitacionRemota>>() {
            @Override public void exito(List<InvitacionRemota> invitaciones) {
                if (!disponible()) return;
                pintarInvitaciones(invitaciones);
                cargarFincas();
            }
            @Override public void error(ErrorRemoto error) { if (disponible()) fallo(error); }
        });
    }

    private void cargarFincas() {
        cuenta().misFincas(new CallbackRemoto<List<PertenenciaRemota>>() {
            @Override public void exito(List<PertenenciaRemota> fincas) {
                if (!disponible()) return;
                ocupar(false);
                pintarFincas(fincas);
            }
            @Override public void error(ErrorRemoto error) { if (disponible()) fallo(error); }
        });
    }

    private void pintarInvitaciones(List<InvitacionRemota> invitaciones) {
        vista.listaInvitaciones.removeAllViews();
        vista.sinInvitaciones.setVisibility(invitaciones.isEmpty() ? View.VISIBLE : View.GONE);
        for (InvitacionRemota invitacion : invitaciones) {
            ItemPersonaBinding tarjeta = TarjetaPersona.agregar(this, vista.listaInvitaciones,
                    getString(R.string.recibidas_de, invitacion.invitadoPor), invitacion.nombreFinca,
                    getString(R.string.invitacion_vence, MensajesRemotos.fecha(invitacion.venceEn)), null);
            TarjetaPersona.accion(tarjeta, R.string.recibidas_aceptar, v -> aceptar(invitacion.idInvitacion));
        }
    }

    private void pintarFincas(List<PertenenciaRemota> fincas) {
        vista.listaFincas.removeAllViews();
        vista.sinFincas.setVisibility(fincas.isEmpty() ? View.VISIBLE : View.GONE);
        for (PertenenciaRemota finca : fincas) {
            String etiqueta = getString(R.string.codigo_nombre, MensajesRemotos.rol(this, finca.rol), MensajesRemotos.estadoIntegrante(this, finca.estado));
            String nota = IntegranteRemoto.REVOCADO.equals(finca.estado) ? getString(R.string.mis_fincas_retirado) : null;
            TarjetaPersona.agregar(this, vista.listaFincas, etiqueta, finca.nombreFinca, null, nota);
        }
    }

    private void aceptar(String idInvitacion) {
        if (ocupado) return;
        ocupar(true);
        cuenta().aceptarInvitacion(idInvitacion, new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) {
                if (!disponible()) return;
                cargar();
                mostrar(getString(R.string.recibidas_aceptada));
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
