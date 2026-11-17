package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.EventoSanitario;
import com.example.ganhard.modelo.entidad.Gasto;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.entidad.Tarea;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Valores de un registro como JSON para el historial, con los nombres de columna como claves. Solo lleva lo que
 * la persona escribe: los campos derivados (antecedente, fechas de auditoría, versión) no se registran.
 */
public final class ValoresCambio {
    private ValoresCambio() {}

    /** Datos editables del animal, sin su estado; lo que no tiene valor se omite. */
    public static JSONObject de(Animal animal) {
        JSONObject datos = new JSONObject();
        poner(datos, "codigo", animal.codigo);
        poner(datos, "nombre", animal.nombre);
        poner(datos, "sexo", animal.sexo);
        poner(datos, "id_raza", animal.idRaza);
        poner(datos, "fecha_nacimiento", animal.fechaNacimiento);
        poner(datos, "fecha_ingreso", animal.fechaIngreso);
        poner(datos, "observaciones", animal.observaciones);
        return datos;
    }

    /** Estado del animal y la fecha desde la que rige. */
    public static JSONObject estado(Animal animal) {
        JSONObject datos = new JSONObject();
        poner(datos, "estado", animal.estado);
        poner(datos, "fecha_estado", animal.fechaEstado);
        return datos;
    }

    public static JSONObject de(Pesaje pesaje) {
        JSONObject datos = new JSONObject();
        poner(datos, "fecha", pesaje.fecha);
        poner(datos, "hora", pesaje.hora);
        poner(datos, "peso_kg", pesaje.pesoKg);
        poner(datos, "observaciones", pesaje.observaciones);
        return datos;
    }

    /** Datos de la tarea; el tipo y el animal van aunque una corrección no los cambie. */
    public static JSONObject de(Tarea tarea) {
        JSONObject datos = new JSONObject();
        poner(datos, "tipo", tarea.tipo);
        poner(datos, "id_animal", tarea.idAnimal);
        poner(datos, "titulo", tarea.titulo);
        poner(datos, "descripcion", tarea.descripcion);
        poner(datos, "fecha_programada", tarea.fechaProgramada);
        return datos;
    }

    /** Estado de la tarea y su fecha de finalización, si la tiene. */
    public static JSONObject estadoTarea(Tarea tarea) {
        JSONObject datos = new JSONObject();
        poner(datos, "estado", tarea.estado);
        poner(datos, "fecha_completada", tarea.fechaCompletada);
        return datos;
    }

    public static JSONObject de(Gasto gasto) {
        JSONObject datos = new JSONObject();
        poner(datos, "fecha", gasto.fecha);
        poner(datos, "categoria", gasto.categoria);
        poner(datos, "concepto", gasto.concepto);
        poner(datos, "valor_centavos", gasto.valorCentavos);
        return datos;
    }

    public static JSONObject de(EventoSanitario evento) {
        JSONObject datos = new JSONObject();
        poner(datos, "tipo", evento.tipo);
        poner(datos, "fecha", evento.fecha);
        poner(datos, "descripcion", evento.descripcion);
        poner(datos, "producto", evento.producto);
        poner(datos, "observaciones", evento.observaciones);
        return datos;
    }

    /**
     * Compara dos versiones de un registro y devuelve {anteriores, nuevos} solo con las claves cuyo valor difiere,
     * o null si son iguales. Una clave que existe en un solo lado se anota como nula en el otro.
     */
    public static JSONObject[] diferencia(JSONObject antes, JSONObject despues) {
        Set<String> claves = new LinkedHashSet<>();
        for (Iterator<String> it = antes.keys(); it.hasNext(); ) claves.add(it.next());
        for (Iterator<String> it = despues.keys(); it.hasNext(); ) claves.add(it.next());
        JSONObject anteriores = new JSONObject(), nuevos = new JSONObject();
        for (String clave : claves) {
            Object previo = antes.opt(clave), actual = despues.opt(clave);
            if (Objects.equals(previo, actual)) continue;
            poner(anteriores, clave, previo == null ? JSONObject.NULL : previo);
            poner(nuevos, clave, actual == null ? JSONObject.NULL : actual);
        }
        return nuevos.length() == 0 ? null : new JSONObject[]{anteriores, nuevos};
    }

    static void poner(JSONObject datos, String clave, Object valor) {
        if (valor == null) return;
        try {
            datos.put(clave, valor);
        } catch (JSONException ex) {
            throw new IllegalStateException("Valor no representable en el historial: " + clave, ex);
        }
    }
}
