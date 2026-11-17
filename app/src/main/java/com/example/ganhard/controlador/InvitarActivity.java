package com.example.ganhard.controlador;

import android.os.Bundle;
import android.widget.Toast;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityInvitarBinding;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.ValidadorCuenta;
import com.example.ganhard.modelo.remoto.CallbackRemoto;
import com.example.ganhard.modelo.remoto.ErrorRemoto;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Collections;
import java.util.Map;

/** Invita por correo a un encargado de la finca. La invitación vale siete días y la persona debe aceptarla con su cuenta. */
public class InvitarActivity extends FormularioActivity {
    private ActivityInvitarBinding vista;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityInvitarBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        prepararFormulario(vista.progreso, vista.guardar, vista.mensaje);
        vista.guardar.setOnClickListener(v -> invitar());
        vista.cancelar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void invitar() {
        if (guardando) return;
        empezarGuardado();
        String correo = ValidadorCuenta.normalizarCorreo(texto(vista.correo));
        cuenta().invitar(texto(vista.correo), new CallbackRemoto<Void>() {
            @Override public void exito(Void valor) {
                if (!disponible()) return;
                guardando = false;
                Toast.makeText(getApplicationContext(), getString(R.string.invitar_ok, correo), Toast.LENGTH_SHORT).show();
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

    @Override protected Map<String, TextInputLayout> campos() { return Collections.singletonMap(Campos.CORREO, vista.campoCorreo); }

    @Override protected boolean hayCambios() { return !texto(vista.correo).trim().isEmpty(); }
}
