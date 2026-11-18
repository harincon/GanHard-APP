package com.example.ganhard.controlador;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivitySesionBinding;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.example.ganhard.modelo.remoto.SesionRemota;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Iniciar sesión, crear una cuenta, confirmar el correo con un código o recuperar el acceso: una sola pantalla con cuatro modos. Crear la cuenta no abre
 * sesión; hay que confirmar el correo y entrar. Devuelve RESULT_OK cuando se inició sesión.
 */
public class SesionActivity extends FormularioActivity {
    public static final String EXTRA_MODO = "modo";
    public static final int INICIO = 0, REGISTRO = 1, RECUPERAR = 2, CONFIRMAR = 3;

    private ActivitySesionBinding vista;
    private int modo;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivitySesionBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        modo = estado != null ? estado.getInt(EXTRA_MODO, INICIO) : getIntent().getIntExtra(EXTRA_MODO, INICIO);
        vista.guardar.setOnClickListener(v -> enviar());
        vista.enlaceCambiar.setOnClickListener(v -> cambiarModo(modo == INICIO ? REGISTRO : INICIO));
        vista.enlaceRecuperar.setOnClickListener(v -> cambiarModo(RECUPERAR));
        vista.enlaceCodigo.setOnClickListener(v -> cambiarModo(CONFIRMAR));
        vista.enlaceReenviar.setOnClickListener(v -> reenviarCodigo());
        // Quien confirmó con el enlace del correo no necesita el código: solo entra con su contraseña.
        vista.enlaceYaConfirme.setOnClickListener(v -> { cambiarModo(INICIO); mensaje(R.string.sesion_ya_confirme_ayuda); });
        aplicarModo();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putInt(EXTRA_MODO, modo);
    }

    private void cambiarModo(int nuevo) {
        if (guardando) return;
        modo = nuevo;
        mostrarErrores(Collections.emptyMap());
        ocultarMensaje();
        aplicarModo();
    }

    private void aplicarModo() {
        boolean registro = modo == REGISTRO, recuperar = modo == RECUPERAR, confirmar = modo == CONFIRMAR;
        vista.toolbar.setTitle(registro ? R.string.sesion_titulo_registro : recuperar ? R.string.sesion_titulo_recuperar
                : confirmar ? R.string.sesion_titulo_confirmar : R.string.sesion_titulo_inicio);
        vista.ayuda.setVisibility(modo == INICIO ? View.GONE : View.VISIBLE);
        vista.ayuda.setText(registro ? R.string.sesion_ayuda_registro : recuperar ? R.string.sesion_ayuda_recuperar : R.string.sesion_ayuda_confirmar);
        vista.campoNombre.setVisibility(registro ? View.VISIBLE : View.GONE);
        vista.campoContrasena.setVisibility(recuperar || confirmar ? View.GONE : View.VISIBLE);
        vista.campoContrasena.setHelperText(registro ? getString(R.string.sesion_ayuda_contrasena) : null);
        vista.campoContrasenaRepetida.setVisibility(registro ? View.VISIBLE : View.GONE);
        vista.campoCodigo.setVisibility(confirmar ? View.VISIBLE : View.GONE);
        vista.guardar.setText(registro ? R.string.sesion_accion_registro : recuperar ? R.string.sesion_accion_recuperar
                : confirmar ? R.string.sesion_accion_confirmar : R.string.sesion_accion_inicio);
        vista.enlaceCambiar.setText(modo == INICIO ? R.string.sesion_no_tengo : R.string.sesion_ya_tengo);
        vista.enlaceRecuperar.setVisibility(modo == INICIO ? View.VISIBLE : View.GONE);
        vista.enlaceCodigo.setVisibility(modo == INICIO ? View.VISIBLE : View.GONE);
        vista.enlaceReenviar.setVisibility(confirmar ? View.VISIBLE : View.GONE);
        vista.enlaceYaConfirme.setVisibility(confirmar ? View.VISIBLE : View.GONE);
    }

    /** Pide un código nuevo para el correo escrito; el anterior deja de valer. */
    private void reenviarCodigo() {
        if (guardando) return;
        empezarGuardado();
        cuenta().reenviarConfirmacion(texto(vista.correo), new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) {
                if (!disponible()) return;
                terminarGuardado();
                mostrarErrores(Collections.emptyMap());
                mensaje(R.string.sesion_reenviado);
            }
            @Override public void error(ErrorRemoto error) { fallo(error); }
        });
    }

    private void enviar() {
        if (guardando) return;
        empezarGuardado();
        String correo = texto(vista.correo), contrasena = texto(vista.contrasena);
        if (modo == REGISTRO) {
            cuenta().registrar(texto(vista.nombre), correo, contrasena, texto(vista.contrasenaRepetida), new CallbackRemoto<Void>() {
                @Override public void exito(Void valor) {
                    if (!disponible()) return;
                    terminarGuardado();
                    vista.contrasena.setText("");
                    vista.contrasenaRepetida.setText("");
                    // Crear la cuenta no abre sesión: se pide el código que llegó al correo para confirmarla.
                    cambiarModo(CONFIRMAR);
                    mensaje(R.string.sesion_registro_listo);
                }
                @Override public void error(ErrorRemoto error) { fallo(error); }
            });
        } else if (modo == CONFIRMAR) {
            cuenta().confirmarCorreo(correo, texto(vista.codigo), new CallbackRemoto<SesionRemota>() {
                @Override public void exito(SesionRemota sesion) {
                    if (!disponible()) return;
                    guardando = false;
                    Toast.makeText(getApplicationContext(), R.string.sesion_confirmada, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
                @Override public void error(ErrorRemoto error) { fallo(error); }
            });
        } else if (modo == RECUPERAR) {
            cuenta().recuperarAcceso(correo, new CallbackRemoto<Void>() {
                @Override public void exito(Void valor) {
                    if (!disponible()) return;
                    terminarGuardado();
                    mostrarErrores(Collections.emptyMap());
                    mensaje(R.string.sesion_recuperar_listo);
                }
                @Override public void error(ErrorRemoto error) { fallo(error); }
            });
        } else {
            cuenta().iniciarSesion(correo, contrasena, new CallbackRemoto<SesionRemota>() {
                @Override public void exito(SesionRemota sesion) {
                    if (!disponible()) return;
                    guardando = false;
                    Toast.makeText(getApplicationContext(), R.string.sesion_iniciada, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
                @Override public void error(ErrorRemoto error) { fallo(error); }
            });
        }
    }

    private void fallo(ErrorRemoto error) {
        if (!disponible()) return;
        terminarGuardado();
        mostrarError(error);
    }

    @Override protected Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new LinkedHashMap<>();
        campos.put(Campos.NOMBRE, vista.campoNombre);
        campos.put(Campos.CORREO, vista.campoCorreo);
        campos.put(Campos.CONTRASENA, vista.campoContrasena);
        campos.put(Campos.CONTRASENA_REPETIDA, vista.campoContrasenaRepetida);
        campos.put(Campos.CODIGO_CONFIRMACION, vista.campoCodigo);
        return campos;
    }

    /** Solo crear una cuenta lleva datos que valga la pena proteger; iniciar sesión se reescribe en segundos. */
    @Override protected boolean hayCambios() {
        return modo == REGISTRO && (!texto(vista.nombre).isEmpty() || !texto(vista.correo).isEmpty() || !texto(vista.contrasena).isEmpty());
    }
}
