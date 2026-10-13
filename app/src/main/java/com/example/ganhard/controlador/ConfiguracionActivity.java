package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityConfiguracionBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.EntradaConfiguracion;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorConfiguracion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

/** Controlador del formulario. Las reglas y consultas permanecen en el Modelo. */
public class ConfiguracionActivity extends BaseActivity {
    private ActivityConfiguracionBinding vista;
    private EntradaConfiguracion original = new EntradaConfiguracion("", "", "", "", "");
    private boolean restaurado, guardando;
    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityConfiguracionBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        // Un Bundle también existe si se rotó antes de que terminara la carga inicial.
        restaurado = estado != null && estado.getBoolean("formulario_cargado", false);
        if (restaurado) mostrar(new EntradaConfiguracion(estado.getString("nombre"), estado.getString("correo"),
                estado.getString("finca"), estado.getString("ubicacion"), estado.getString("area")));
        vista.guardar.setOnClickListener(v -> guardar());
        vista.reintentar.setOnClickListener(v -> cargar());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (guardando) return;
                EntradaConfiguracion actual = entrada();
                boolean cambios = !actual.nombre.equals(original.nombre) || !actual.correo.equals(original.correo)
                        || !actual.finca.equals(original.finca) || !actual.ubicacion.equals(original.ubicacion) || !actual.area.equals(original.area);
                if (!cambios) { finish(); return; }
                new MaterialAlertDialogBuilder(ConfiguracionActivity.this)
                        .setTitle(R.string.descartar_titulo).setMessage(R.string.descartar_mensaje)
                        .setPositiveButton(R.string.descartar, (dialogo, boton) -> finish())
                        .setNegativeButton(R.string.seguir_editando, null).show();
            }
        });
        cargar();
    }
    private void cargar() {
        ocupado(true);
        vista.mensaje.setVisibility(View.GONE);
        vista.reintentar.setVisibility(View.GONE);
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (!disponible()) return;
                if (datos != null) {
                    original = new EntradaConfiguracion(datos.usuario.nombre, datos.usuario.correo, datos.finca.nombre,
                            datos.finca.ubicacion, datos.finca.areaHectareas == null ? "" : new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(datos.finca.areaHectareas));
                    vista.titulo.setText(R.string.editar_configuracion);
                    vista.toolbar.setTitle(R.string.editar_configuracion);
                    vista.guardar.setText(R.string.guardar_cambios);
                    if (!restaurado) mostrar(original);
                }
                restaurado = true;
                ocupado(false);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                ocupado(true);
                vista.progreso.setVisibility(View.GONE);
                mensaje(R.string.error_carga);
                vista.reintentar.setVisibility(View.VISIBLE);
            }
        });
    }
    private void guardar() {
        EntradaConfiguracion datos = entrada();
        Map<String, ErrorCampo> errores = ValidadorConfiguracion.validar(datos);
        Map<String, TextInputLayout> campos = campos();
        for (Map.Entry<String, TextInputLayout> campo : campos.entrySet()) {
            ErrorCampo error = errores.get(campo.getKey());
            campo.getValue().setError(error == null ? null : MensajesError.texto(this, campo.getKey(), error));
        }
        if (!errores.isEmpty()) {
            TextInputLayout primero = campos.get(errores.keySet().iterator().next());
            if (primero != null && primero.getEditText() != null) primero.getEditText().requestFocus();
            return;
        }
        guardando = true;
        ocupado(true);
        vista.mensaje.setVisibility(View.GONE);
        repositorio().guardar(datos, new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal configuracion) {
                if (!disponible()) return;
                guardando = false;
                startActivity(new Intent(ConfiguracionActivity.this, InicioActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                guardando = false;
                ocupado(false);
                mensaje(R.string.error_guardado);
            }
        });
    }
    private Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new java.util.LinkedHashMap<>();
        campos.put("nombre", vista.campoNombre); campos.put("correo", vista.campoCorreo); campos.put("finca", vista.campoFinca);
        campos.put("ubicacion", vista.campoUbicacion); campos.put("area", vista.campoArea);
        return campos;
    }
    private EntradaConfiguracion entrada() {
        return new EntradaConfiguracion(String.valueOf(vista.nombreAdministrador.getText()), String.valueOf(vista.correoAdministrador.getText()),
                String.valueOf(vista.nombreFinca.getText()), String.valueOf(vista.ubicacion.getText()), String.valueOf(vista.area.getText()));
    }
    private void mostrar(EntradaConfiguracion datos) {
        vista.nombreAdministrador.setText(datos.nombre); vista.correoAdministrador.setText(datos.correo);
        vista.nombreFinca.setText(datos.finca); vista.ubicacion.setText(datos.ubicacion); vista.area.setText(datos.area);
    }
    private void ocupado(boolean ocupado) {
        vista.progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
        vista.guardar.setEnabled(!ocupado);
        for (TextInputLayout campo : campos().values()) campo.setEnabled(!ocupado);
    }
    private void mensaje(int recurso) { vista.mensaje.setText(recurso); vista.mensaje.setVisibility(View.VISIBLE); }
    @Override protected void onSaveInstanceState(Bundle estado) {
        EntradaConfiguracion datos = entrada();
        estado.putBoolean("formulario_cargado", restaurado);
        estado.putString("nombre", datos.nombre); estado.putString("correo", datos.correo); estado.putString("finca", datos.finca);
        estado.putString("ubicacion", datos.ubicacion); estado.putString("area", datos.area);
        super.onSaveInstanceState(estado);
    }
}
