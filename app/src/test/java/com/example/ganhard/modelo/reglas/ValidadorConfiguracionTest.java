package com.example.ganhard.modelo.reglas;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidadorConfiguracionTest {
    private EntradaConfiguracion entrada(String nombre, String correo, String finca, String area) {
        return new EntradaConfiguracion(nombre, correo, finca, "San Gil", area);
    }
    private ErrorCampo error(EntradaConfiguracion entrada, String campo) {
        return ValidadorConfiguracion.validar(entrada).get(campo);
    }
    @Test public void exigeNombreCorreoYFinca() {
        java.util.Map<String, ErrorCampo> errores = ValidadorConfiguracion.validar(entrada("  ", "", "", ""));
        assertEquals(3, errores.size());
        for (ErrorCampo error : errores.values()) assertEquals(CodigoError.OBLIGATORIO, error.codigo);
    }
    @Test public void rechazaCorreoInvalido() {
        assertEquals(CodigoError.FORMATO, error(entrada("Helver", "correo sin arroba", "La Esperanza", ""), "correo").codigo);
    }
    @Test public void correoSinDominioEsFormato() {
        assertEquals(CodigoError.FORMATO, error(entrada("Helver", "helver@ejemplo", "La Esperanza", ""), "correo").codigo);
    }
    @Test public void correoConEspaciosInternosEsFormato() {
        assertEquals(CodigoError.FORMATO, error(entrada("Helver", "hel ver@ejemplo.com", "La Esperanza", ""), "correo").codigo);
    }
    @Test public void correoDe151CaracteresEsLongitudMaximaConLimite150() {
        String correo = "a".repeat(139) + "@ejemplo.com";
        ErrorCampo error = error(entrada("Helver", correo, "La Esperanza", ""), "correo");
        assertEquals(CodigoError.LONGITUD_MAXIMA, error.codigo);
        assertEquals(150, error.limite);
    }
    @Test public void permiteAreaOpcionalYDatosValidos() {
        assertTrue(ValidadorConfiguracion.validar(entrada("Helver", "helver@ejemplo.com", "La Esperanza", "")).isEmpty());
        assertNull(ValidadorConfiguracion.area( ""));
    }
    @Test public void admiteComaOPuntoSinSeparadoresDeMiles() {
        assertEquals(24.5, ValidadorConfiguracion.area("24,50"), 0.0001);
        assertEquals(24.5, ValidadorConfiguracion.area("24.50"), 0.0001);
        for (String area : new String[]{"-1", "0", "abc", "1.000,50", "NaN", "1e3", "12.345", "100000000"}) {
            ErrorCampo error = error(entrada("Helver", "helver@ejemplo.com", "La Esperanza", area), "area");
            assertNotNull(area, error);
            assertEquals(area, CodigoError.VALOR_INVALIDO, error.codigo);
        }
    }
    @Test public void respetaLongitudesDelModeloYNormalizaEspacios() {
        assertEquals("Helver", entrada(" Helver ", "h@ejemplo.com", "Finca", "").nombre);
        ErrorCampo nombre = error(entrada("a".repeat(101), "h@ejemplo.com", "Finca", ""), "nombre");
        assertEquals(CodigoError.LONGITUD_MAXIMA, nombre.codigo);
        assertEquals(100, nombre.limite);
        assertEquals(CodigoError.LONGITUD_MAXIMA, error(entrada("Helver", "h@ejemplo.com", "a".repeat(101), ""), "finca").codigo);
    }
    @Test public void ubicacionDe201CaracteresEsLongitudMaximaConLimite200() {
        EntradaConfiguracion entrada = new EntradaConfiguracion("Helver", "h@ejemplo.com", "Finca", "u".repeat(201), "");
        ErrorCampo error = error(entrada, "ubicacion");
        assertEquals(CodigoError.LONGITUD_MAXIMA, error.codigo);
        assertEquals(200, error.limite);
        assertNull(error(new EntradaConfiguracion("Helver", "h@ejemplo.com", "Finca", "u".repeat(200), ""), "ubicacion"));
    }
}
