package com.example.ganhard.modelo.repositorio;

import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Gasto;
import com.example.ganhard.modelo.entidad.HistorialCambio;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import com.example.ganhard.modelo.reglas.Campos;
import com.example.ganhard.modelo.reglas.CodigoError;
import com.example.ganhard.modelo.reglas.EntradaGasto;
import com.example.ganhard.modelo.reglas.ErrorCampo;
import com.example.ganhard.modelo.reglas.ValidadorGasto;
import com.example.ganhard.modelo.reglas.ValidadorMotivo;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.json.JSONObject;

/**
 * Apoyo del Modelo para los gastos: registro y consulta por periodo (RF13), corrección (RF14) y anulación (RF15).
 * Corre en el hilo del {@link EjecutorModelo}; cada escritura pasa por {@link RegistroCambios}. El dinero se
 * maneja siempre como entero en centavos.
 */
public final class GastoRepository {
    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;
    private final RegistroCambios registro;

    public GastoRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
        this.registro = new RegistroCambios(db);
    }

    /**
     * Guarda un gasto de la finca activa. El id lo elige quien llama y la operación es idempotente: si ya existe,
     * devuelve el guardado, de modo que repetir la solicitud (por ejemplo tras rotar) no duplica.
     */
    public void registrar(String idGasto, EntradaGasto entrada, Callback<Gasto> callback) {
        ejecutor.ejecutar(() -> {
            Gasto existente = db.gastoDao().porId(idGasto);
            if (existente != null) return existente;
            String idFinca = idFincaActiva();
            Map<String, ErrorCampo> errores = ValidadorGasto.validar(entrada, Tiempo.hoy());
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            String ahora = Tiempo.ahoraUtc();
            Gasto gasto = new Gasto();
            gasto.idGasto = idGasto;
            gasto.idFinca = idFinca;
            aplicar(gasto, entrada);
            gasto.creadoEn = ahora;
            gasto.actualizadoEn = ahora;
            HistorialCambio creacion = registro.preparar(HistorialCambio.GASTO, idGasto, HistorialCambio.CREAR,
                    null, ValoresCambio.de(gasto), null, null, ahora);
            registro.guardar(() -> db.gastoDao().insertar(gasto), creacion);
            return gasto;
        }, callback);
    }

    /** El gasto vigente con ese id; uno anulado ya no se puede abrir para corregirlo. */
    public void obtener(String idGasto, Callback<Gasto> callback) {
        ejecutor.ejecutar(() -> gastoVigente(idGasto), callback);
    }

    /** Corrige un gasto con las reglas del registro y un motivo obligatorio. Si nada cambió, no se guarda. */
    public void corregir(String idGasto, EntradaGasto entrada, String motivo, Callback<Gasto> callback) {
        ejecutor.ejecutar(() -> {
            Gasto gasto = gastoVigente(idGasto);
            Map<String, ErrorCampo> errores = ValidadorGasto.validar(entrada, Tiempo.hoy());
            ErrorCampo errorMotivo = ValidadorMotivo.validar(motivo);
            if (errorMotivo != null) errores.put(Campos.MOTIVO, errorMotivo);
            if (!errores.isEmpty()) throw ErrorModelo.validacion(errores);
            JSONObject antes = ValoresCambio.de(gasto);
            aplicar(gasto, entrada);
            JSONObject[] diferencia = ValoresCambio.diferencia(antes, ValoresCambio.de(gasto));
            if (diferencia == null) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.VALOR, new ErrorCampo(CodigoError.SIN_CAMBIOS)));
            }
            String ahora = Tiempo.ahoraUtc();
            gasto.actualizadoEn = ahora;
            HistorialCambio edicion = registro.preparar(HistorialCambio.GASTO, idGasto, HistorialCambio.EDITAR,
                    diferencia[0], diferencia[1], motivo.trim(), null, ahora);
            registro.guardar(() -> db.gastoDao().actualizar(gasto), edicion);
            return gasto;
        }, callback);
    }

    /** Anula un gasto con motivo obligatorio: se conserva, pero sale de las listas y del total. No se puede deshacer. */
    public void anular(String idGasto, String motivo, Callback<Gasto> callback) {
        ejecutor.ejecutar(() -> {
            Gasto gasto = db.gastoDao().porId(idGasto);
            if (gasto == null) throw ErrorModelo.noEncontrado(ErrorModelo.GASTO);
            if (gasto.anulado == 1) return gasto;
            ErrorCampo errorMotivo = ValidadorMotivo.validar(motivo);
            if (errorMotivo != null) throw ErrorModelo.validacion(Collections.singletonMap(Campos.MOTIVO, errorMotivo));
            // La entrada guarda qué gasto se anuló, para mostrarlo cuando ya no esté en la lista.
            JSONObject anterior = ValoresCambio.de(gasto);
            JSONObject anulado = new JSONObject();
            ValoresCambio.poner(anulado, "anulado", 1);
            String ahora = Tiempo.ahoraUtc();
            gasto.anulado = 1;
            gasto.anuladoEn = ahora;
            gasto.motivoAnulacion = motivo.trim();
            gasto.actualizadoEn = ahora;
            HistorialCambio anulacion = registro.preparar(HistorialCambio.GASTO, idGasto, HistorialCambio.ANULAR,
                    anterior, anulado, gasto.motivoAnulacion, null, ahora);
            registro.guardar(() -> db.gastoDao().actualizar(gasto), anulacion);
            return gasto;
        }, callback);
    }

    /** Gastos vigentes de un mes ("2026-10") y su total, del más reciente al más antiguo. */
    public void delPeriodo(String periodo, Callback<PeriodoGastos> callback) {
        ejecutor.ejecutar(() -> {
            YearMonth mes;
            try {
                if (periodo == null || !periodo.matches("\\d{4}-\\d{2}")) throw new DateTimeParseException("Periodo inválido", String.valueOf(periodo), 0);
                mes = YearMonth.parse(periodo);
            } catch (DateTimeParseException ex) {
                throw ErrorModelo.validacion(Collections.singletonMap(Campos.FECHA, new ErrorCampo(CodigoError.FORMATO)));
            }
            String idFinca = idFincaActiva();
            String desde = mes.atDay(1).toString(), hasta = mes.plusMonths(1).atDay(1).toString();
            return new PeriodoGastos(db.gastoDao().total(idFinca, desde, hasta), db.gastoDao().delPeriodo(idFinca, desde, hasta));
        }, callback);
    }

    /** Meses que se pueden consultar: el actual y los que tienen gastos, del más reciente al más antiguo. */
    public void periodos(Callback<List<String>> callback) {
        ejecutor.ejecutar(() -> {
            TreeSet<String> meses = new TreeSet<>(Collections.reverseOrder());
            meses.addAll(db.gastoDao().meses(idFincaActiva()));
            meses.add(YearMonth.from(Tiempo.hoy()).toString());
            return new ArrayList<>(meses);
        }, callback);
    }

    private static void aplicar(Gasto gasto, EntradaGasto entrada) {
        gasto.fecha = entrada.fecha;
        gasto.categoria = entrada.categoria;
        gasto.concepto = entrada.concepto;
        gasto.valorCentavos = ValidadorGasto.centavos(entrada.valor);
    }

    private Gasto gastoVigente(String idGasto) throws ErrorModelo {
        Gasto gasto = db.gastoDao().porId(idGasto);
        if (gasto == null || gasto.anulado == 1) throw ErrorModelo.noEncontrado(ErrorModelo.GASTO);
        return gasto;
    }

    private String idFincaActiva() throws ErrorModelo {
        String idFinca = db.configuracionDao().idFincaActiva();
        if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
        return idFinca;
    }
}
