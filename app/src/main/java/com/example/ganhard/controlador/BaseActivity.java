package com.example.ganhard.controlador;

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.ganhard.GanHardApplication;
import com.example.ganhard.R;
import com.example.ganhard.modelo.repositorio.ConfiguracionRepository;
import com.google.android.material.appbar.MaterialToolbar;

/** Aplica márgenes de las barras Android para API 35+ y comparte los repositorios. */
public abstract class BaseActivity extends AppCompatActivity {
    protected GanHardApplication app() { return (GanHardApplication) getApplication(); }
    protected ConfiguracionRepository repositorio() { return app().configuracion(); }
    protected boolean disponible() { return !isFinishing() && !isDestroyed(); }

    /** Con flecha de regreso, la barra superior vuelve a la pantalla de origen como el botón Atrás. */
    protected void configurarToolbar(MaterialToolbar toolbar, boolean conAtras) {
        if (!conAtras) return;
        toolbar.setNavigationIcon(R.drawable.ic_atras);
        toolbar.setNavigationContentDescription(R.string.volver);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

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
