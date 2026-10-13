package com.example.ganhard.controlador;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FormatoFechaTest {
    @Test public void cortaUsaMesesAbreviadosEnEspanol() {
        String[] meses = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};
        for (int mes = 1; mes <= 12; mes++) {
            assertEquals("15 " + meses[mes - 1] + " 2026", FormatoFecha.corta(String.format("2026-%02d-15", mes)));
        }
        assertEquals("5 mar 2025", FormatoFecha.corta("2025-03-05"));
    }

    @Test public void cortaDevuelveVacioParaNuloOVacioYElTextoSiNoEsUnaFecha() {
        assertEquals("", FormatoFecha.corta(null));
        assertEquals("", FormatoFecha.corta(""));
        assertEquals("basura", FormatoFecha.corta("basura"));
    }

    @Test public void hora12CubreMedianocheMediodiaYTarde() {
        assertEquals("8:15 a. m.", FormatoFecha.hora12("08:15:00"));
        assertEquals("12:05 a. m.", FormatoFecha.hora12("00:05"));
        assertEquals("12:00 p. m.", FormatoFecha.hora12("12:00"));
        assertEquals("1:30 p. m.", FormatoFecha.hora12("13:30:00"));
        assertEquals("11:59 p. m.", FormatoFecha.hora12("23:59:59"));
        assertEquals("9:05 a. m.", FormatoFecha.hora12("9:05"));
    }

    @Test public void fechaYHoraUneConPuntoMedioYSinHoraUsaElTextoRecibido() {
        assertEquals("10 oct 2026 · 8:15 a. m.", FormatoFecha.fechaYHora("2026-10-10", "08:15:00", "Sin hora"));
        assertEquals("10 oct 2026 · Sin hora", FormatoFecha.fechaYHora("2026-10-10", null, "Sin hora"));
    }

    @Test public void aPantallaYAIsoSonInversos() {
        assertEquals("05/03/2025", FormatoFecha.aPantalla("2025-03-05"));
        assertEquals("2025-03-05", FormatoFecha.aIso("05/03/2025"));
        assertEquals("2025-03-05", FormatoFecha.aIso(FormatoFecha.aPantalla("2025-03-05")));
    }

    @Test public void aIsoAceptaDiaYMesDeUnDigito() {
        assertEquals("2025-03-05", FormatoFecha.aIso("5/3/2025"));
        assertEquals("2026-12-01", FormatoFecha.aIso("1/12/2026"));
    }

    @Test public void aIsoDevuelveIgualLoInvalidoParaQueElValidadorLoMarque() {
        for (String texto : new String[]{"31/02/2026", "05-03-2025", "2025-03-05", "", "abc", "32/01/2026", "1/13/2026", "5/3/25"}) {
            assertEquals(texto, FormatoFecha.aIso(texto));
        }
    }

    @Test public void aPantallaDevuelveIgualLoQueNoEsIso() {
        assertEquals("", FormatoFecha.aPantalla(""));
        assertEquals("05/03/2025", FormatoFecha.aPantalla("05/03/2025"));
    }

    @Test public void fechaYHoraConComaParaLaFichaYSinHoraSoloLaFecha() {
        assertEquals("10 oct 2026, 8:15 a. m.", FormatoFecha.fechaYHoraConComa("2026-10-10", "08:15:00"));
        assertEquals("10 oct 2026", FormatoFecha.fechaYHoraConComa("2026-10-10", null));
        assertEquals("10 oct 2026", FormatoFecha.fechaYHoraConComa("2026-10-10", ""));
    }

    @Test public void laMascaraInsertaLasBarrasMientrasSeEscribenSoloDigitos() {
        assertEquals("", FormatoFecha.mascara(""));
        assertEquals("0", FormatoFecha.mascara("0"));
        assertEquals("05", FormatoFecha.mascara("05"));
        assertEquals("05/0", FormatoFecha.mascara("050"));
        assertEquals("05/03", FormatoFecha.mascara("0503"));
        assertEquals("05/03/2", FormatoFecha.mascara("05032"));
        assertEquals("05/03/2025", FormatoFecha.mascara("05032025"));
    }

    @Test public void laMascaraNoTocaLoQueYaTieneBarrasOOtrosSimbolosNiRecortaMas() {
        assertEquals("5/3/2025", FormatoFecha.mascara("5/3/2025"));
        assertEquals("05/03", FormatoFecha.mascara("05/03"));
        assertEquals("5-3-2025", FormatoFecha.mascara("5-3-2025"));
        assertEquals("2026-02-01", FormatoFecha.mascara("2026-02-01"));
        assertEquals("05/03/2025", FormatoFecha.mascara("050320259999"));
    }
}
