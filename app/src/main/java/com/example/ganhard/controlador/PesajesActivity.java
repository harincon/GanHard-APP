package com.example.ganhard.controlador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ganhard.R;
import com.example.ganhard.databinding.ActivityPesajesBinding;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.List;

/** Controlador del historial de pesos de un animal (RF09), del más reciente al más antiguo. */
public class PesajesActivity extends BaseActivity {
    public static final String EXTRA_ID_ANIMAL = "id_animal";

    private ActivityPesajesBinding vista;
    private PesajeAdapter adaptador;
    private String idAnimal;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        vista = ActivityPesajesBinding.inflate(getLayoutInflater());
        setContentView(vista.getRoot());
        ajustarBarras(vista.getRoot());
        configurarToolbar(vista.toolbar, true);
        idAnimal = getIntent().getStringExtra(EXTRA_ID_ANIMAL);
        adaptador = new PesajeAdapter(this);
        vista.lista.setLayoutManager(new LinearLayoutManager(this));
        vista.lista.setAdapter(adaptador);
        vista.agregar.setOnClickListener(v -> registrar());
        vista.accionVacio.setOnClickListener(v -> registrar());
        vista.reintentar.setOnClickListener(v -> cargar());
        if (idAnimal == null) noEncontrado();
        else cargarAnimal();
    }

    /** Se recarga al volver del formulario para mostrar el pesaje recién guardado. */
    @Override protected void onResume() {
        super.onResume();
        if (idAnimal != null) cargar();
    }

    private void cargarAnimal() {
        app().animales().obtener(idAnimal, new Callback<Animal>() {
            @Override public void exito(Animal animal) {
                if (disponible()) vista.toolbar.setSubtitle(animal.codigo);
            }
            @Override public void error(ErrorModelo error) { /* El historial informa el error; el código es solo contexto. */ }
        });
    }

    private void cargar() {
        vista.progreso.setVisibility(View.VISIBLE);
        app().pesajes().historial(idAnimal, new Callback<List<Pesaje>>() {
            @Override public void exito(List<Pesaje> pesajes) {
                if (!disponible()) return;
                vista.progreso.setVisibility(View.INVISIBLE);
                adaptador.mostrar(pesajes);
                boolean vacio = pesajes.isEmpty();
                vista.estadoError.setVisibility(View.GONE);
                vista.lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
                vista.estadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
            }
            @Override public void error(ErrorModelo error) {
                if (!disponible()) return;
                if (error.tipo == ErrorModelo.Tipo.NO_ENCONTRADO) { noEncontrado(); return; }
                vista.progreso.setVisibility(View.INVISIBLE);
                vista.lista.setVisibility(View.GONE);
                vista.estadoVacio.setVisibility(View.GONE);
                vista.mensajeError.setText(R.string.error_carga);
                vista.estadoError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void registrar() {
        startActivity(new Intent(this, PesajeFormActivity.class).putExtra(PesajeFormActivity.EXTRA_ID_ANIMAL, idAnimal));
    }

    private void noEncontrado() {
        Toast.makeText(getApplicationContext(), R.string.animal_no_encontrado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
