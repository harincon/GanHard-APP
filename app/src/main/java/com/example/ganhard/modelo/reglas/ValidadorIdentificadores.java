package com.example.ganhard.modelo.reglas;

import com.example.ganhard.modelo.entidad.AnimalIdentificador;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reglas de la lista de identificadores de un animal. No hay unicidad: los valores pueden repetirse. Lo único
 * obligatorio es al menos un hierro al registrar, un valor en los aretes y números, y un tope de diez en total.
 */
public final class ValidadorIdentificadores {
    public static final int MAXIMO = 10, VALOR_MAXIMO = 50;

    private ValidadorIdentificadores() {}

    /**
     * {@code exigirHierro} pide al menos un hierro elegido (al registrar un animal). Los errores del hierro van en
     * {@link Campos#HIERRO}; los de los demás identificadores, en {@link Campos#IDENTIFICADORES}.
     */
    public static Map<String, ErrorCampo> validar(List<EntradaIdentificador> lista, boolean exigirHierro) {
        Map<String, ErrorCampo> errores = new LinkedHashMap<>();
        boolean hayHierro = false;
        for (EntradaIdentificador entrada : lista) if (entrada.esHierro() && !entrada.idHierro.isEmpty()) hayHierro = true;
        if (exigirHierro && !hayHierro) errores.put(Campos.HIERRO, new ErrorCampo(CodigoError.OBLIGATORIO));
        if (lista.size() > MAXIMO) {
            errores.put(Campos.IDENTIFICADORES, new ErrorCampo(CodigoError.LONGITUD_MAXIMA, MAXIMO));
            return errores;
        }
        for (EntradaIdentificador entrada : lista) {
            ErrorCampo error = errorDe(entrada);
            if (error != null) {
                errores.put(Campos.IDENTIFICADORES, error);
                break;
            }
        }
        return errores;
    }

    private static ErrorCampo errorDe(EntradaIdentificador entrada) {
        switch (entrada.tipo) {
            case AnimalIdentificador.HIERRO: return null; // Un hierro sin elegir no se guarda; ya lo cubre la regla del hierro.
            case AnimalIdentificador.ARETE:
            case AnimalIdentificador.NUMERO:
                if (entrada.valor.isEmpty()) return new ErrorCampo(CodigoError.OBLIGATORIO);
                if (entrada.valor.length() > VALOR_MAXIMO) return new ErrorCampo(CodigoError.LONGITUD_MAXIMA, VALOR_MAXIMO);
                return null;
            default: return new ErrorCampo(CodigoError.VALOR_INVALIDO);
        }
    }
}
