package com.example.ganhard.modelo.reglas;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidadorConfiguracionTest {
    private EntradaConfiguracion entrada(String nombre, String correo, String finca, String area) {
        return new EntradaConfiguracion(nombre, correo, finca, "San Gil", area);
    }
    @Test public void exigeNombreCorreoYFinca() {
        assertEquals(3, ValidadorConfiguracion.validar(entrada("  ", "", "", "")).size());
    }
    @Test public void rechazaCorreoInvalido() {
        assertTrue(ValidadorConfiguracion.validar(entrada("Helver", "correo sin arroba", "La Esperanza", "")).containsKey("correo"));
    }
    @Test public void permiteAreaOpcionalYDatosValidos() {
        assertTrue(ValidadorConfiguracion.validar(entrada("Helver", "helver@ejemplo.com", "La Esperanza", "")).isEmpty());
        assertNull(ValidadorConfiguracion.area( ""));
    }
    @Test public void admiteComaOPuntoSinSeparadoresDeMiles() {
        assertEquals(24.5, ValidadorConfiguracion.area("24,50"), 0.0001);
        assertEquals(24.5, ValidadorConfiguracion.area("24.50"), 0.0001);
        for (String area : new String[]{"-1", "0", "abc", "1.000,50", "NaN", "1e3", "12.345", "100000000"}) {
            assertTrue(area, ValidadorConfiguracion.validar(entrada("Helver", "helver@ejemplo.com", "La Esperanza", area)).containsKey("area"));
        }
    }
    @Test public void respetaLongitudesDelModeloYNormalizaEspacios() {
        assertEquals("Helver", entrada(" Helver ", "h@ejemplo.com", "Finca", "").nombre);
        assertTrue(ValidadorConfiguracion.validar(entrada("a".repeat(101), "h@ejemplo.com", "Finca", "")).containsKey("nombre"));
        assertTrue(ValidadorConfiguracion.validar(entrada("Helver", "h@ejemplo.com", "a".repeat(101), "")).containsKey("finca"));
    }
}
