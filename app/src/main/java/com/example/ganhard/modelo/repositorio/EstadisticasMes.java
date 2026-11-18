package com.example.ganhard.modelo.repositorio;

import java.util.List;

/** Las cifras del Inicio para un mes: animales, pesos y gastos, ya calculadas para que la pantalla solo las pinte. */
public final class EstadisticasMes {
    /** Mes de las cifras, AAAA-MM. */
    public final String mes;
    public final Animales animales;
    public final Pesos pesos;
    public final Gastos gastos;

    EstadisticasMes(String mes, Animales animales, Pesos pesos, Gastos gastos) {
        this.mes = mes;
        this.animales = animales;
        this.pesos = pesos;
        this.gastos = gastos;
    }

    /** Una raza (nulo en el nombre significa "sin raza") y cuántos animales activos la tienen. */
    public static final class Conteo {
        public final String nombre;
        public final int cantidad;
        Conteo(String nombre, int cantidad) { this.nombre = nombre; this.cantidad = cantidad; }
    }

    /** Animales activos al cierre del mes (o hoy, si es el mes en curso) y lo que pasó durante el mes. */
    public static final class Animales {
        public final int activos, activosAnterior, hembras, machos, ingresos, ventas, fallecidos;
        /** Las razas más comunes entre los activos, de mayor a menor (hasta cinco). */
        public final List<Conteo> razas;
        Animales(int activos, int activosAnterior, int hembras, int machos, int ingresos, int ventas, int fallecidos, List<Conteo> razas) {
            this.activos = activos;
            this.activosAnterior = activosAnterior;
            this.hembras = hembras;
            this.machos = machos;
            this.ingresos = ingresos;
            this.ventas = ventas;
            this.fallecidos = fallecidos;
            this.razas = razas;
        }
    }

    /** Pesos de los animales activos: promedio con el último pesaje de cada uno y cuántos hace falta pesar. */
    public static final class Pesos {
        /** Nulo si ningún animal activo tiene pesaje. */
        public final Double promedioKg;
        /** Promedio al cierre del mes anterior; nulo si no hay datos. */
        public final Double promedioAnteriorKg;
        /** Promedio menos promedio anterior; nulo si falta alguno de los dos. */
        public final Double variacionKg;
        public final int animalesConPeso, pesadosEnElMes;
        /** Activos que nunca se pesaron o que llevan más de 60 días sin pesarse. */
        public final int sinPesar;
        Pesos(Double promedioKg, Double promedioAnteriorKg, int animalesConPeso, int pesadosEnElMes, int sinPesar) {
            this.promedioKg = promedioKg;
            this.promedioAnteriorKg = promedioAnteriorKg;
            this.variacionKg = promedioKg == null || promedioAnteriorKg == null ? null : promedioKg - promedioAnteriorKg;
            this.animalesConPeso = animalesConPeso;
            this.pesadosEnElMes = pesadosEnElMes;
            this.sinPesar = sinPesar;
        }
    }

    public static final class CategoriaGasto {
        public final String categoria;
        public final long centavos;
        CategoriaGasto(String categoria, long centavos) { this.categoria = categoria; this.centavos = centavos; }
    }

    /** Gastos vigentes del mes, en centavos, y lo gastado en el mes anterior para compararlo. */
    public static final class Gastos {
        public final long totalCentavos, anteriorCentavos;
        /** Por categoría, de mayor a menor. */
        public final List<CategoriaGasto> categorias;
        Gastos(long totalCentavos, long anteriorCentavos, List<CategoriaGasto> categorias) {
            this.totalCentavos = totalCentavos;
            this.anteriorCentavos = anteriorCentavos;
            this.categorias = categorias;
        }
    }
}
