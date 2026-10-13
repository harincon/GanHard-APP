package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityAnimalesBinding;
import com.example.ganhard.modelo.datos.AnimalResumen;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.List;

/** Controlador de la consulta de animales (RF04): lista con búsqueda por código o nombre y filtro por estado. */
public class AnimalesActivity extends BaseActivity {
    private static final long RETARDO_BUSQUEDA_MS = 300;

    private ActivityAnimalesBinding vista;
    private AnimalAdapter adaptador;
    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final Runnable consulta = this::consultar;
    /** Cada consulta lleva un número; una respuesta de una consulta anterior se descarta. */
    private int peticion;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityAnimalesBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        adaptador = new AnimalAdapter(this, animal -> startActivity(new Intent(this, DetalleAnimalActivity.class)
                .putExtra(DetalleAnimalActivity.EXTRA_ID_ANIMAL, animal.idAnimal)));
        vista.lista.setLayoutManager(new LinearLayoutManager(this));
        vista.lista.setAdapter(adaptador);
        vista.buscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int inicio, int cuenta, int despues) {}
            @Override public void onTextChanged(CharSequence s, int inicio, int antes, int cuenta) {}
            @Override public void afterTextChanged(Editable s) { programarConsulta(); }
        });
        vista.filtros.setOnCheckedChangeListener((grupo, id) -> consultar());
        vista.registrarAnimal.setOnClickListener(v -> startActivity(new Intent(this, AnimalFormActivity.class)));
        vista.accionVacio.setOnClickListener(v -> limpiarBusqueda());
        vista.reintentar.setOnClickListener(v -> consultar());
    }

    @Override protected void onResume() {
        super.onResume();
        consultar();
    }

    @Override protected void onDestroy() {
        manejador.removeCallbacks(consulta);
        super.onDestroy();
    }

    private void programarConsulta() {
        manejador.removeCallbacks(consulta);
        manejador.postDelayed(consulta, RETARDO_BUSQUEDA_MS);
    }

    private void consultar() {
        manejador.removeCallbacks(consulta);
        final int mia = ++peticion;
        final String estado = estadoFiltro();
        vista.progreso.setVisibility(View.VISIBLE);
        app().animales().buscar(textoBusqueda(), estado, new Callback<List<AnimalResumen>>() {
            @Override public void exito(List<AnimalResumen> animales) {
                if (!disponible() || mia != peticion) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                adaptador.mostrar(animales);
                vista.contadorFiltro.setText(getResources().getQuantityString(plurales(estado), animales.size(), animales.size()));
                boolean vacia = animales.isEmpty();
                vista.estadoError.setVisibility(View.GONE);
                vista.lista.setVisibility(vacia ? View.GONE : View.VISIBLE);
                vista.estadoVacio.setVisibility(vacia ? View.VISIBLE : View.GONE);
                if (vacia) mostrarVacio(estado);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible() || mia != peticion) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) {
                    startActivity(new Intent(AnimalesActivity.this, ConfiguracionActivity.class));
                    finish();
                    return;
                }
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.lista.setVisibility(View.GONE);
                vista.estadoVacio.setVisibility(View.GONE);
                vista.mensajeError.setText(R.string.error_carga);
                vista.estadoError.setVisibility(View.VISIBLE);
            }
        });
    }

    /** Con texto escrito el mensaje es de búsqueda y ofrece limpiarla; sin texto dice qué grupo está vacío. */
    private void mostrarVacio(String estado) {
        boolean buscando = !textoBusqueda().isEmpty();
        if (buscando) vista.mensajeVacio.setText(R.string.sin_coincidencias);
        else if (Animal.VENDIDO.equals(estado)) vista.mensajeVacio.setText(R.string.sin_animales_vendidos);
        else if (Animal.FALLECIDO.equals(estado)) vista.mensajeVacio.setText(R.string.sin_animales_fallecidos);
        else vista.mensajeVacio.setText(R.string.sin_animales_activos);
        vista.accionVacio.setVisibility(buscando ? View.VISIBLE : View.GONE);
    }

    private static int plurales(String estado) {
        if (Animal.VENDIDO.equals(estado)) return R.plurals.vendidos_en_finca;
        if (Animal.FALLECIDO.equals(estado)) return R.plurals.fallecidos_en_finca;
        return R.plurals.activos_en_finca;
    }

    private String textoBusqueda() { return String.valueOf(vista.buscar.getText()).trim(); }

    /** Estado elegido en el filtro; Activos es la opción con la que abre la pantalla. */
    private String estadoFiltro() {
        int elegido = vista.filtros.getCheckedRadioButtonId();
        if (elegido == R.id.filtroVendidos) return Animal.VENDIDO;
        if (elegido == R.id.filtroFallecidos) return Animal.FALLECIDO;
        return Animal.ACTIVO;
    }

    private void limpiarBusqueda() {
        vista.buscar.setText("");
        vista.filtroActivos.setChecked(true);
        consultar();
    }
}
