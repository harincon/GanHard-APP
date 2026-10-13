package com.example.ganhard.modelo.nucleo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.Test;

public class TiempoTest {
    @Test public void fechaDeSelectorUsaUtcSinDesfase() {
        // 2023-11-14T22:13:20Z: en una zona UTC-5 ya sería el 14, y en UTC+5 el 15 si se usara la zona local.
        assertEquals(LocalDate.of(2023, 11, 14), Tiempo.fechaDeSelector(1_700_000_000_000L));
    }

    @Test public void aSelectorEsLaMedianocheUtcYVuelveALaMismaFecha() {
        LocalDate fecha = LocalDate.of(2023, 11, 14);
        assertEquals(1_699_920_000_000L, Tiempo.aSelector(fecha));
        assertEquals(fecha, Tiempo.fechaDeSelector(Tiempo.aSelector(fecha)));
    }

    @Test public void ahoraUtcTieneFormatoIso() {
        assertTrue(Tiempo.ahoraUtc().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$"));
    }

    @Test public void ahoraUtcConRelojFijoEsExacto() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-12T15:30:45.123Z"), ZoneOffset.UTC);
        assertEquals("2026-10-12T15:30:45.123Z", Tiempo.ahoraUtc(reloj));
    }
}
