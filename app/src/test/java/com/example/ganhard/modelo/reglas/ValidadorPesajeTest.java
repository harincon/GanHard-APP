package com.example.ganhard.modelo.reglas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.ganhard.modelo.entidad.Animal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.Test;

public class ValidadorPesajeTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 12, 15, 30, 0);

    private Animal animal() {
        Animal animal = new Animal();
        animal.fechaNacimiento = "2025-03-01";
        animal.fechaIngreso = "2026-01-10";
        return animal;
    }
    private EntradaPesaje entrada(String fecha, String hora, String peso) { return new EntradaPesaje(fecha, hora, peso, ""); }
    private Map<String, ErrorCampo> validar(EntradaPesaje entrada) { return ValidadorPesaje.validar(entrada, animal(), AHORA); }
    private ErrorCampo error(EntradaPesaje entrada, String campo) { return validar(entrada).get(campo); }

    @Test public void pesajeValidoNoTieneErrores() {
        assertTrue(validar(entrada("2026-10-12", "09:15:00", "450,5")).isEmpty());
    }
    @Test public void pesoVacioEsObligatorio() {
        assertEquals(CodigoError.OBLIGATORIO, error(entrada("2026-10-12", "09:15", ""), "peso").codigo);
    }
    @Test public void pesoCeroNegativoYTextoSonInvalidos() {
        for (String peso : new String[]{"0", "0,00", "-5", "abc", "1e3", "NaN", "4 50"}) {
            ErrorCampo error = error(entrada("2026-10-12", "09:15", peso), "peso");
            assertNotNull(peso, error);
            assertEquals(peso, CodigoError.VALOR_INVALIDO, error.codigo);
        }
    }
    @Test public void pesoConComaYPuntoSeInterpretanIgual() {
        assertEquals(450.5, ValidadorPesaje.peso("450,5"), 0.0001);
        assertEquals(450.5, ValidadorPesaje.peso("450.5"), 0.0001);
        assertEquals(450.0, ValidadorPesaje.peso("450"), 0.0001);
    }
    @Test public void masDeDosDecimalesEsFormato() {
        assertEquals(CodigoError.FORMATO, error(entrada("2026-10-12", "09:15", "450.555"), "peso").codigo);
        assertNull(error(entrada("2026-10-12", "09:15", "450.55"), "peso"));
    }
    @Test public void pesoSobre2000EsFueraDeRango() {
        assertEquals(CodigoError.FUERA_DE_RANGO, error(entrada("2026-10-12", "09:15", "2000.01"), "peso").codigo);
        assertEquals(CodigoError.FUERA_DE_RANGO, error(entrada("2026-10-12", "09:15", "99999999"), "peso").codigo);
        assertNull(error(entrada("2026-10-12", "09:15", "2000"), "peso"));
    }
    @Test public void horaObligatoriaSiNoEsAntecedente() {
        assertEquals(CodigoError.OBLIGATORIO, error(entrada("2026-10-12", "", "450"), "hora").codigo);
    }
    @Test public void horaOpcionalSiEsAntecedente() {
        assertTrue(ValidadorPesaje.esAntecedente("2025-12-31", animal()));
        assertFalse(ValidadorPesaje.esAntecedente("2026-01-10", animal()));
        assertNull(error(entrada("2025-12-31", "", "450"), "hora"));
    }
    @Test public void horaConFormatoInvalidoEsFormato() {
        assertEquals(CodigoError.FORMATO, error(entrada("2026-10-12", "9:15", "450"), "hora").codigo);
        assertEquals(CodigoError.FORMATO, error(entrada("2026-10-12", "25:00", "450"), "hora").codigo);
    }
    @Test public void fechaFuturaEsFechaFutura() {
        assertEquals(CodigoError.FECHA_FUTURA, error(entrada("2026-10-13", "09:15", "450"), "fecha").codigo);
    }
    @Test public void hoyConHoraFuturaEsFechaFuturaEnLaHora() {
        assertEquals(CodigoError.FECHA_FUTURA, error(entrada("2026-10-12", "15:31", "450"), "hora").codigo);
        assertNull(error(entrada("2026-10-12", "15:30", "450"), "hora"));
    }
    @Test public void fechaAnteriorAlNacimientoEsFechaAnterior() {
        assertEquals(CodigoError.FECHA_ANTERIOR, error(entrada("2025-02-28", "", "40"), "fecha").codigo);
        assertNull(error(entrada("2025-03-01", "", "40"), "fecha"));
    }
    @Test public void fechaVaciaOMalEscritaFalla() {
        assertEquals(CodigoError.OBLIGATORIO, error(entrada("", "09:15", "450"), "fecha").codigo);
        assertEquals(CodigoError.FORMATO, error(entrada("12-10-2026", "09:15", "450"), "fecha").codigo);
    }
    @Test public void observacionesDe501EsLongitudMaxima() {
        EntradaPesaje largo = new EntradaPesaje("2026-10-12", "09:15", "450", "o".repeat(501));
        assertEquals(500, validar(largo).get("observaciones").limite);
    }

    @Test public void unPuntoDeMilesNoSeLeeComoDecimal() {
        for (String peso : new String[]{"1.050", "1.000", "450,500"}) {
            assertEquals(peso, CodigoError.FORMATO, error(entrada("2026-10-12", "09:15", peso), "peso").codigo);
        }
    }
}
