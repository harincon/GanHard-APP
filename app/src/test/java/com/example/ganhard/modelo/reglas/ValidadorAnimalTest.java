package com.example.ganhard.modelo.reglas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.ganhard.modelo.entidad.Animal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.Test;

public class ValidadorAnimalTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 12);

    private EntradaAnimal entrada(String codigo, String sexo, String nacimiento, String ingreso) {
        return new EntradaAnimal(codigo, "Lucero", sexo, "", nacimiento, ingreso, "");
    }
    private Map<String, ErrorCampo> validar(EntradaAnimal entrada) { return ValidadorAnimal.validar(entrada, HOY); }
    private ErrorCampo error(EntradaAnimal entrada, String campo) { return validar(entrada).get(campo); }

    @Test public void animalValidoNoTieneErrores() {
        assertTrue(validar(entrada("GH-001", "H", "2025-03-01", "2026-01-10")).isEmpty());
    }
    @Test public void soloCodigoSexoEIngresoSonObligatorios() {
        EntradaAnimal minima = new EntradaAnimal("GH-001", "", "M", "", "", "2026-10-12", "");
        assertTrue(validar(minima).isEmpty());
    }
    @Test public void codigoVacioEsObligatorio() {
        assertEquals(CodigoError.OBLIGATORIO, error(entrada("   ", "H", "", "2026-01-10"), "codigo").codigo);
    }
    @Test public void codigoDe31EsLongitudMaximaConLimite30() {
        ErrorCampo largo = error(entrada("c".repeat(31), "H", "", "2026-01-10"), "codigo");
        assertEquals(CodigoError.LONGITUD_MAXIMA, largo.codigo);
        assertEquals(30, largo.limite);
        assertNull(error(entrada("c".repeat(30), "H", "", "2026-01-10"), "codigo"));
    }
    @Test public void normalizarCodigoQuitaEspaciosDeLosExtremos() {
        assertEquals("GH-001", ValidadorAnimal.normalizarCodigo("  GH-001 "));
    }
    @Test public void nombreDe101EsLongitudMaximaYObservacionesDe501Tambien() {
        EntradaAnimal largo = new EntradaAnimal("GH-001", "n".repeat(101), "H", "", "", "2026-01-10", "o".repeat(501));
        assertEquals(100, validar(largo).get("nombre").limite);
        assertEquals(500, validar(largo).get("observaciones").limite);
    }
    @Test public void sexoVacioEsObligatorioYOtroValorEsInvalido() {
        assertEquals(CodigoError.OBLIGATORIO, error(entrada("GH-001", "", "", "2026-01-10"), "sexo").codigo);
        assertEquals(CodigoError.VALOR_INVALIDO, error(entrada("GH-001", "X", "", "2026-01-10"), "sexo").codigo);
    }
    @Test public void ingresoVacioEsObligatorio() {
        assertEquals(CodigoError.OBLIGATORIO, error(entrada("GH-001", "H", "", ""), "fechaIngreso").codigo);
    }
    @Test public void ingresoFuturoEsFechaFutura() {
        assertEquals(CodigoError.FECHA_FUTURA, error(entrada("GH-001", "H", "", "2026-10-13"), "fechaIngreso").codigo);
        assertNull(error(entrada("GH-001", "H", "", "2026-10-12"), "fechaIngreso"));
    }
    @Test public void nacimientoFuturoEsFechaFutura() {
        assertEquals(CodigoError.FECHA_FUTURA, error(entrada("GH-001", "H", "2026-12-01", "2026-01-10"), "fechaNacimiento").codigo);
    }
    @Test public void ingresoAnteriorAlNacimientoEsFechaAnterior() {
        assertEquals(CodigoError.FECHA_ANTERIOR, error(entrada("GH-001", "H", "2026-03-01", "2026-01-10"), "fechaIngreso").codigo);
        assertNull(error(entrada("GH-001", "H", "2026-01-10", "2026-01-10"), "fechaIngreso"));
    }
    @Test public void fechaConFormatoInvalidoEsFormato() {
        assertEquals(CodigoError.FORMATO, error(entrada("GH-001", "H", "12/10/2025", "2026-01-10"), "fechaNacimiento").codigo);
        assertEquals(CodigoError.FORMATO, error(entrada("GH-001", "H", "", "2026-1-5"), "fechaIngreso").codigo);
        assertEquals(CodigoError.FORMATO, error(entrada("GH-001", "H", "", "2026-02-30"), "fechaIngreso").codigo);
    }

    private Animal animalIngresadoEl(String fecha) {
        Animal animal = new Animal();
        animal.fechaIngreso = fecha;
        return animal;
    }
    @Test public void fechaDeEstadoValidaNoTieneError() {
        assertNull(ValidadorAnimal.fechaEstado("2026-10-12", animalIngresadoEl("2026-01-10"), HOY));
        assertNull(ValidadorAnimal.fechaEstado("2026-01-10", animalIngresadoEl("2026-01-10"), HOY));
    }
    @Test public void fechaDeEstadoFuturaEsFechaFutura() {
        ErrorCampo error = ValidadorAnimal.fechaEstado("2026-10-13", animalIngresadoEl("2026-01-10"), HOY);
        assertNotNull(error);
        assertEquals(CodigoError.FECHA_FUTURA, error.codigo);
    }
    @Test public void fechaDeEstadoAnteriorAlIngresoEsFechaAnterior() {
        assertEquals(CodigoError.FECHA_ANTERIOR, ValidadorAnimal.fechaEstado("2026-01-09", animalIngresadoEl("2026-01-10"), HOY).codigo);
    }
    @Test public void fechaDeEstadoVaciaOMalEscritaFalla() {
        assertEquals(CodigoError.OBLIGATORIO, ValidadorAnimal.fechaEstado("", animalIngresadoEl("2026-01-10"), HOY).codigo);
        assertEquals(CodigoError.FORMATO, ValidadorAnimal.fechaEstado("hoy", animalIngresadoEl("2026-01-10"), HOY).codigo);
    }
}
