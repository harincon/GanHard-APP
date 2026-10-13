package com.example.ganhard.controlador;

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.modelo.repositorio.ConfiguracionRepository;

/** Aplica márgenes de las barras Android para API 35+ y comparte el repositorio. */
public abstract class BaseActivity extends AppCompatActivity {
    protected ConfiguracionRepository repositorio() {
        return ((GanHardApplication) getApplication()).configuracion();
    }
    protected boolean disponible() { return !isFinishing() && !isDestroyed(); }
    protected void ajustarBarras(View vista) {
        int izquierda = vista.getPaddingLeft(), arriba = vista.getPaddingTop();
        int derecha = vista.getPaddingRight(), abajo = vista.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(vista, (v, insets) -> {
            androidx.core.graphics.Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(izquierda + barras.left, arriba + barras.top, derecha + barras.right, abajo + barras.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(vista);
    }
}
