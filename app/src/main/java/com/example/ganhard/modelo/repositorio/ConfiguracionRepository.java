package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.ConfiguracionLocal;
import com.example.ganhard.modelo.datos.ConfiguracionDao;
import com.example.ganhard.modelo.entidad.Finca;
import com.example.ganhard.modelo.entidad.Usuario;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.EntradaConfiguracion;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorConfiguracion;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Apoyo del Modelo: coordina persistencia; nunca realiza consultas en el hilo de UI. */
public final class ConfiguracionRepository {
    private final ConfiguracionDao dao;
    private final EjecutorModelo ejecutor;

    public ConfiguracionRepository(ConfiguracionDao dao, EjecutorModelo ejecutor) {
        this.dao = dao;
        this.ejecutor = ejecutor;
    }

    public void cargar(Callback<ConfiguracionLocal> callback) {
        ejecutor.ejecutar(dao::cargar, callback);
    }

    public void guardar(EntradaConfiguracion entrada, Callback<ConfiguracionLocal> callback) {
        ejecutor.ejecutar(() -> {
            Map<String, String> errores = ValidadorConfiguracion.validar(entrada);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(comoCampos(errores));
            ConfiguracionLocal existente = dao.cargar();
            Usuario usuario = existente == null ? new Usuario() : existente.usuario;
            Finca finca = existente == null ? new Finca() : existente.finca;
            String fecha = Tiempo.ahoraUtc();
            if (existente == null) {
                usuario.idUsuario = UUID.randomUUID().toString();
                finca.idFinca = UUID.randomUUID().toString();
                usuario.creadoEn = fecha;
                finca.creadoEn = fecha;
                finca.idPropietario = usuario.idUsuario;
            }
            usuario.nombre = entrada.nombre;
            usuario.correo = entrada.correo;
            usuario.actualizadoEn = fecha;
            finca.nombre = entrada.finca;
            finca.ubicacion = entrada.ubicacion.isEmpty() ? null : entrada.ubicacion;
            finca.areaHectareas = ValidadorConfiguracion.area(entrada.area);
            finca.actualizadoEn = fecha;
            dao.guardar(usuario, finca);
            return new ConfiguracionLocal(usuario, finca);
        }, callback);
    }

    // Provisional: el validador aún devuelve textos; la tarea 3 lo cambia a códigos y elimina este adaptador.
    private static Map<String, ErrorCampo> comoCampos(Map<String, String> errores) {
        Map<String, ErrorCampo> campos = new LinkedHashMap<>();
        for (String campo : errores.keySet()) campos.put(campo, new ErrorCampo(CodigoError.VALOR_INVALIDO));
        return campos;
    }
}
