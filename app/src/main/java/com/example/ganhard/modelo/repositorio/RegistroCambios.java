package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import java.util.UUID;
import org.json.JSONObject;

/**
 * Único punto por donde se escriben los registros que llevan historial (RN06): la escritura y sus entradas van en
 * una misma transacción, así que quedan las dos o ninguna. Corre en el hilo del EjecutorModelo.
 */
public final class RegistroCambios {
    private final GanHardDatabase db;

    public RegistroCambios(GanHardDatabase db) { this.db = db; }

    /**
     * Arma la entrada de un cambio con la finca activa y su responsable. No escribe nada: se entrega después a
     * {@link #guardar}. {@code anteriores} es nulo en una creación.
     */
    public HistorialCambio preparar(String entidad, String idRegistro, String accion, JSONObject anteriores, JSONObject nuevos,
                                    String motivo, String fechaEfectiva, String registradoEn) throws ErrorModelo {
        Finca finca = db.configuracionDao().fincaActiva();
        if (finca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
        HistorialCambio cambio = new HistorialCambio();
        cambio.idHistorialCambio = UUID.randomUUID().toString();
        // El autor es la persona de este teléfono (la dueña o un encargado); sin esa marca, el dueño de la finca.
        Usuario autor = db.configuracionDao().usuarioDeEsteTelefono();
        cambio.idUsuario = autor != null ? autor.idUsuario : finca.idPropietario;
        cambio.idFinca = finca.idFinca;
        cambio.entidad = entidad;
        cambio.idRegistro = idRegistro;
        cambio.accion = accion;
        cambio.registradoEn = registradoEn;
        cambio.fechaEfectiva = fechaEfectiva;
        cambio.motivo = motivo;
        cambio.valoresAnteriores = anteriores == null ? null : anteriores.toString();
        cambio.valoresNuevos = nuevos.toString();
        return cambio;
    }

    /** Ejecuta la escritura y guarda sus entradas de historial en una sola transacción. */
    public void guardar(Runnable escritura, HistorialCambio... cambios) {
        db.runInTransaction(() -> {
            escritura.run();
            for (HistorialCambio cambio : cambios) db.historialDao().insertar(cambio);
        });
    }
}
