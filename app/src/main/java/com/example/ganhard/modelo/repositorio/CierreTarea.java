package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.entidad.Tarea;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import java.util.Collections;

/**
 * Cierre de una tarea pendiente porque se realizó su pesaje o su actividad sanitaria (RF17). Se prepara antes de
 * escribir y se guarda en la misma transacción que el registro realizado: o quedan los dos o ninguno.
 */
final class CierreTarea {
    final Tarea tarea;
    final HistorialCambio cambio;

    private CierreTarea(Tarea tarea, HistorialCambio cambio) {
        this.tarea = tarea;
        this.cambio = cambio;
    }

    /**
     * Comprueba que la tarea exista, siga pendiente y sea del tipo y del animal del registro que se va a guardar,
     * y la deja completada en la fecha del registro.
     */
    static CierreTarea preparar(GanHardDatabase db, RegistroCambios registro, String idTarea, String tipo,
                                String idAnimal, String fecha, String ahora) throws ErrorModelo {
        Tarea tarea = db.tareaDao().porId(idTarea);
        if (tarea == null) throw ErrorModelo.noEncontrado(ErrorModelo.TAREA);
        if (!Tarea.PENDIENTE.equals(tarea.estado)) throw rechazo(Campos.ESTADO, CodigoError.NO_PENDIENTE);
        if (!tipo.equals(tarea.tipo)) throw rechazo(Campos.TIPO, CodigoError.VALOR_INVALIDO);
        if (!idAnimal.equals(tarea.idAnimal)) throw rechazo(Campos.ID_ANIMAL, CodigoError.VALOR_INVALIDO);
        org.json.JSONObject antes = ValoresCambio.estadoTarea(tarea);
        tarea.estado = Tarea.COMPLETADA;
        tarea.fechaCompletada = fecha;
        tarea.actualizadoEn = ahora;
        HistorialCambio cambio = registro.preparar(HistorialCambio.TAREA, idTarea, HistorialCambio.CAMBIAR_ESTADO,
                antes, ValoresCambio.estadoTarea(tarea), null, fecha, ahora);
        return new CierreTarea(tarea, cambio);
    }

    private static ErrorModelo rechazo(String campo, CodigoError codigo) {
        return ErrorModelo.validacion(Collections.singletonMap(campo, new ErrorCampo(codigo)));
    }
}
