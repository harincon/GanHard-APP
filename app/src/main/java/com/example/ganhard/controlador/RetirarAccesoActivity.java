package com.example.ganhard.controlador;

import android.os.Bundle;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityRetirarAccesoBinding;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Collections;
import java.util.Map;

/**
 * Retira el acceso de un encargado. El motivo es obligatorio y la fila de la persona se conserva en la lista con su
 * fecha de retiro; el servidor no borra nada. Devuelve RESULT_OK cuando se retiró.
 */
public class RetirarAccesoActivity extends FormularioActivity {
    public static final String EXTRA_ID = "id_usuario_finca", EXTRA_NOMBRE = "nombre", EXTRA_CORREO = "correo";

    private ActivityRetirarAccesoBinding vista;
    private String id;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityRetirarAccesoBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        id = getIntent().getStringExtra(EXTRA_ID);
        vista.retirarNombre.setText(getIntent().getStringExtra(EXTRA_NOMBRE));
        vista.retirarCorreo.setText(getIntent().getStringExtra(EXTRA_CORREO));
        vista.guardar.setOnClickListener(v -> retirar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (id == null) finish();
    }

    private void retirar() {
        if (guardando) return;
        empezarGuardado();
        cuenta().retirarAcceso(id, texto(vista.motivo), new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), R.string.retirar_ok, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void error(ErrorRemoto error) {
                if (!disponible()) return;
                terminarGuardado();
                mostrarError(error);
            }
        });
    }

    @Override protected Map<String, TextInputLayout> campos() { return Collections.singletonMap(Campos.MOTIVO, vista.campoMotivo); }

    @Override protected boolean hayCambios() { return !texto(vista.motivo).trim().isEmpty(); }
}
