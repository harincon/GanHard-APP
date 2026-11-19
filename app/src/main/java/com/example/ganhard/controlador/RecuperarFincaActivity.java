package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityRecuperarFincaBinding;
import com.example.ganhard.databinding.ItemPersonaBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.remoto.IntegranteRemoto;
import com.example.ganhard.modelo.remoto.PertenenciaRemota;
import com.example.ganhard.modelo.repositorio.EstadoCuenta;
import com.example.ganhard.modelo.repositorio.ResultadoSincronizacion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

/**
 * Recuperar una finca de la cuenta en un teléfono vacío (etapas 6.2 y 6.3): sirve a la dueña que cambió de teléfono o
 * borró los datos, y a una persona encargada que entra a una finca ajena. Pide iniciar sesión, muestra las fincas de la
 * cuenta y baja la que se elija.
 */
public class RecuperarFincaActivity extends BaseActivity {
    private ActivityRecuperarFincaBinding vista;
    private boolean ocupado;
    /** Cada lectura lleva un número; la respuesta de una anterior se descarta. */
    private int lectura;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityRecuperarFincaBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        vista.iniciarSesion.setOnClickListener(v -> startActivity(new Intent(this, SesionActivity.class)
                .putExtra(SesionActivity.EXTRA_MODO, SesionActivity.INICIO)));
    }

    /** Se recarga al volver del inicio de sesión. */
    @Override protected void onResume() {
        super.onResume();
        if (!ocupado) cargar();
    }

    private void cargar() {
        final int mia = ++lectura;
        cuenta().estado(new CallbackRemoto<EstadoCuenta>() {
            @Override public void exito(EstadoCuenta estado) {
                if (!disponible() || mia != lectura) return;
                if (!estado.configurada) { sinLista(getString(R.string.sinc_sin_configurar), false); return; }
                if (!estado.conSesion) { sinLista(getString(R.string.recuperar_sin_sesion), true); return; }
                cargarFincas(mia);
            }
            @Override public void error(ErrorRemoto error) {
                if (disponible() && mia == lectura) sinLista(MensajesRemotos.texto(RecuperarFincaActivity.this, error), false);
            }
        });
    }

    private void cargarFincas(int mia) {
        vista.progreso.setVisibility(View.VISIBLE);
        cuenta().misFincas(new CallbackRemoto<List<PertenenciaRemota>>() {
            @Override public void exito(List<PertenenciaRemota> fincas) {
                if (!disponible() || mia != lectura) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.iniciarSesion.setVisibility(View.GONE);
                vista.listaFincas.removeAllViews();
                boolean alguna = false;
                for (PertenenciaRemota finca : fincas) {
                    if (!IntegranteRemoto.ACTIVO.equals(finca.estado)) continue; // una finca que perdió el acceso no se ofrece
                    alguna = true;
                    ItemPersonaBinding tarjeta = TarjetaPersona.agregar(RecuperarFincaActivity.this, vista.listaFincas,
                            MensajesRemotos.rol(RecuperarFincaActivity.this, finca.rol), finca.nombreFinca, null, null);
                    tarjeta.getRoot().setOnClickListener(v -> confirmar(finca));
                }
                if (alguna) {
                    vista.mensaje.setText(R.string.recuperar_elige);
                    vista.mensaje.setVisibility(View.VISIBLE);
                } else {
                    sinLista(getString(R.string.recuperar_sin_fincas), false);
                }
            }
            @Override public void error(ErrorRemoto error) {
                if (!disponible() || mia != lectura) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                sinLista(MensajesRemotos.texto(RecuperarFincaActivity.this, error), error.tipo == ErrorRemoto.Tipo.SIN_SESION);
            }
        });
    }

    private void sinLista(String texto, boolean conBotonDeSesion) {
        vista.listaFincas.removeAllViews();
        vista.mensaje.setText(texto);
        vista.mensaje.setVisibility(View.VISIBLE);
        vista.iniciarSesion.setVisibility(conBotonDeSesion ? View.VISIBLE : View.GONE);
    }

    private void confirmar(PertenenciaRemota finca) {
        if (ocupado) return;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.recuperar_confirmar_titulo)
                .setMessage(getString(R.string.recuperar_confirmar_texto, finca.nombreFinca))
                .setPositiveButton(R.string.recuperar_confirmar, (dialogo, boton) -> recuperar(finca))
                .setNegativeButton(R.string.volver, null).show();
    }

    private void recuperar(PertenenciaRemota finca) {
        ocupado = true;
        vista.progreso.setVisibility(View.VISIBLE);
        vista.mensaje.setText(R.string.recuperar_bajando);
        vista.mensaje.setVisibility(View.VISIBLE);
        app().sincronizacion().recuperarFinca(finca.idFinca, registros -> {
            if (disponible()) vista.mensaje.setText(getResources().getQuantityString(R.plurals.recuperar_bajando_n, registros, registros));
        }, new CallbackRemoto<ResultadoSincronizacion>() {
            @Override public void exito(ResultadoSincronizacion resultado) {
                if (!disponible()) return;
                ocupado = false;
                abrirInicio();
            }
            @Override public void error(ErrorRemoto error) {
                if (!disponible()) return;
                ocupado = false;
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.mensaje.setText(MensajesRemotos.texto(RecuperarFincaActivity.this, error));
                // Si la finca alcanzó a crearse, lo recuperado hasta ahí sirve: se continúa desde Sincronización.
                repositorio().cargar(new Callback<ConfiguracionLocal>() {
                    @Override public void exito(ConfiguracionLocal datos) {
                        if (!disponible() || datos == null) return;
                        Toast.makeText(getApplicationContext(), R.string.recuperar_a_medias, Toast.LENGTH_LONG).show();
                        abrirInicio();
                    }
                    @Override public void error(ErrorModelo e) { /* Queda el mensaje del error. */ }
                });
            }
        });
    }

    private void abrirInicio() {
        startActivity(new Intent(this, InicioActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
