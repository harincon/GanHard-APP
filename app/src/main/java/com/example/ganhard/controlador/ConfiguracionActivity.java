package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityConfiguracionBinding;
import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.EntradaConfiguracion;
import com.example.ganhard.modelo.reglas.ValidadorConfiguracion;
import com.google.android.material.textfield.TextInputLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

/** Controlador del formulario. Las reglas y consultas permanecen en el Modelo. */
public class ConfiguracionActivity extends FormularioActivity {
    private ActivityConfiguracionBinding vista;
    private EntradaConfiguracion original = new EntradaConfiguracion("", "", "", "", "");
    private boolean restaurado, edicion;
    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityConfiguracionBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje, View.GONE);
        // Un Bundle también existe si se rotó antes de que terminara la carga inicial.
        restaurado = estado != null && estado.getBoolean("formulario_cargado", false);
        if (restaurado) mostrar(new EntradaConfiguracion(estado.getString("nombre"), estado.getString("correo"),
                estado.getString("finca"), estado.getString("ubicacion"), estado.getString("area")));
        vista.guardar.setOnClickListener(v -> guardar());
        vista.reintentar.setOnClickListener(v -> cargar());
        cargar();
    }
    private void cargar() {
        ocupado(true);
        ocultarMensaje();
        vista.reintentar.setVisibility(View.GONE);
        repositorio().cargar(new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal datos) {
                if (!disponible()) return;
                if (datos != null) {
                    original = new EntradaConfiguracion(datos.usuario.nombre, datos.usuario.correo, datos.finca.nombre,
                            datos.finca.ubicacion, datos.finca.areaHectareas == null ? "" : new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(datos.finca.areaHectareas));
                    edicion = true;
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
        if (mostrarErrores(ValidadorConfiguracion.validar(datos))) return;
        empezarGuardado();
        repositorio().guardar(datos, new Callback<ConfiguracionLocal>() {
            @Override public void exito(ConfiguracionLocal configuracion) {
                if (!disponible()) return;
                guardando = false;
                if (edicion) setResult(RESULT_OK);
                else startActivity(new Intent(ConfiguracionActivity.this, InicioActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                terminarGuardado();
                mostrarError(error, R.string.error_guardado);
            }
        });
    }
    @Override protected Map<String, TextInputLayout> campos() {
        Map<String, TextInputLayout> campos = new java.util.LinkedHashMap<>();
        campos.put("nombre", vista.campoNombre); campos.put("correo", vista.campoCorreo); campos.put("finca", vista.campoFinca);
        campos.put("ubicacion", vista.campoUbicacion); campos.put("area", vista.campoArea);
        return campos;
    }
    @Override protected boolean hayCambios() {
        EntradaConfiguracion actual = entrada();
        return !actual.nombre.equals(original.nombre) || !actual.correo.equals(original.correo)
                || !actual.finca.equals(original.finca) || !actual.ubicacion.equals(original.ubicacion) || !actual.area.equals(original.area);
    }
    private EntradaConfiguracion entrada() {
        return new EntradaConfiguracion(String.valueOf(vista.nombreAdministrador.getText()), String.valueOf(vista.correoAdministrador.getText()),
                String.valueOf(vista.nombreFinca.getText()), String.valueOf(vista.ubicacion.getText()), String.valueOf(vista.area.getText()));
    }
    private void mostrar(EntradaConfiguracion datos) {
        vista.nombreAdministrador.setText(datos.nombre); vista.correoAdministrador.setText(datos.correo);
        vista.nombreFinca.setText(datos.finca); vista.ubicacion.setText(datos.ubicacion); vista.area.setText(datos.area);
    }
    @Override protected void onSaveInstanceState(Bundle estado) {
        EntradaConfiguracion datos = entrada();
        estado.putBoolean("formulario_cargado", restaurado);
        estado.putString("nombre", datos.nombre); estado.putString("correo", datos.correo); estado.putString("finca", datos.finca);
        estado.putString("ubicacion", datos.ubicacion); estado.putString("area", datos.area);
        super.onSaveInstanceState(estado);
    }
}
