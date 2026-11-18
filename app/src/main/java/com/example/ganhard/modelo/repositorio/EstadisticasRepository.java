package com.example.ganhard.modelo.repositorio;

import androidx.annotation.VisibleForTesting;
import com.example.ganhard.modelo.datos.CategoriaTotal;
import com.example.ganhard.modelo.datos.GanHardDatabase;
import com.example.ganhard.modelo.entidad.Animal;
import com.example.ganhard.modelo.entidad.Pesaje;
import com.example.ganhard.modelo.entidad.Raza;
import com.example.ganhard.modelo.nucleo.Callback;
import com.example.ganhard.modelo.nucleo.EjecutorModelo;
import com.example.ganhard.modelo.nucleo.ErrorModelo;
import com.example.ganhard.modelo.nucleo.Tiempo;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cifras del Inicio por mes (animales, pesos y gastos). Todo se calcula con lo ya guardado y no se escribe nada.
 *
 * <p>Los meses pasados se reconstruyen con la fecha de ingreso y con el estado actual del animal (vendido o
 * fallecido, con su fecha): si un animal cambió de estado varias veces, solo cuenta el último.
 */
public final class EstadisticasRepository {
    private static final int RAZAS_MAXIMO = 5, DIAS_SIN_PESAR = 60;

    private final GanHardDatabase db;
    private final EjecutorModelo ejecutor;

    public EstadisticasRepository(GanHardDatabase db, EjecutorModelo ejecutor) {
        this.db = db;
        this.ejecutor = ejecutor;
    }

    /** Las cifras del mes (AAAA-MM). Un mes futuro se calcula con hoy como corte, así que no inventa datos. */
    public void deMes(String mes, Callback<EstadisticasMes> callback) { deMes(mes, Tiempo.hoy(), callback); }

    @VisibleForTesting public void deMes(String mes, LocalDate hoy, Callback<EstadisticasMes> callback) {
        ejecutor.ejecutar(() -> calcular(mes, hoy), callback);
    }

    /** El primer mes (AAAA-MM) con algún dato; sin datos, el mes actual. Limita hasta dónde se puede retroceder. */
    public void primerMes(Callback<String> callback) { primerMes(Tiempo.hoy(), callback); }

    @VisibleForTesting public void primerMes(LocalDate hoy, Callback<String> callback) {
        ejecutor.ejecutar(() -> {
            String idFinca = idFincaActiva();
            String primera = null;
            for (String fecha : new String[]{db.animalDao().primerIngreso(idFinca), db.pesajeDao().primeraFecha(idFinca), db.gastoDao().primeraFecha(idFinca)}) {
                if (fecha != null && !fecha.isEmpty() && (primera == null || fecha.compareTo(primera) < 0)) primera = fecha;
            }
            return primera == null ? YearMonth.from(hoy).toString() : primera.substring(0, 7);
        }, callback);
    }

    private EstadisticasMes calcular(String mes, LocalDate hoy) throws ErrorModelo {
        String idFinca = idFincaActiva();
        YearMonth periodo = YearMonth.parse(mes);
        LocalDate finMes = periodo.atEndOfMonth();
        String inicio = periodo.atDay(1).toString(), siguiente = periodo.plusMonths(1).atDay(1).toString();
        String corte = (finMes.isAfter(hoy) ? hoy : finMes).toString();
        String corteAnterior = periodo.minusMonths(1).atEndOfMonth().toString();
        String finDelMes = corte; // el último día que cuenta de este mes

        List<Animal> animales = db.animalDao().todos(idFinca);
        Map<String, String> nombresDeRaza = new HashMap<>();
        for (Raza raza : db.razaDao().todas()) nombresDeRaza.put(raza.idRaza, raza.nombre);
        Map<String, List<Pesaje>> pesajes = new HashMap<>();
        for (Pesaje pesaje : db.pesajeDao().deLaFinca(idFinca)) pesajes.computeIfAbsent(pesaje.idAnimal, id -> new ArrayList<>()).add(pesaje);

        return new EstadisticasMes(mes,
                animales(animales, nombresDeRaza, inicio, finDelMes, corte, corteAnterior),
                pesos(animales, pesajes, inicio, corte, corteAnterior),
                gastos(idFinca, inicio, siguiente, periodo));
    }

    // --- Animales ---

    private static boolean activoEn(Animal animal, String fecha) {
        if (animal.fechaIngreso.compareTo(fecha) > 0) return false;
        return Animal.ACTIVO.equals(animal.estado) || animal.fechaEstado.compareTo(fecha) > 0;
    }

    private static boolean enElMes(String fecha, String inicio, String fin) { return fecha.compareTo(inicio) >= 0 && fecha.compareTo(fin) <= 0; }

    private static EstadisticasMes.Animales animales(List<Animal> animales, Map<String, String> nombresDeRaza, String inicio, String fin,
                                                    String corte, String corteAnterior) {
        int activos = 0, anterior = 0, hembras = 0, machos = 0, ingresos = 0, ventas = 0, fallecidos = 0;
        Map<String, Integer> porRaza = new LinkedHashMap<>();
        for (Animal animal : animales) {
            if (activoEn(animal, corte)) {
                activos++;
                if (Animal.HEMBRA.equals(animal.sexo)) hembras++; else machos++;
                String clave = animal.idRaza == null ? "" : animal.idRaza;
                porRaza.merge(clave, 1, Integer::sum);
            }
            if (activoEn(animal, corteAnterior)) anterior++;
            if (enElMes(animal.fechaIngreso, inicio, fin)) ingresos++;
            if (!Animal.ACTIVO.equals(animal.estado) && enElMes(animal.fechaEstado, inicio, fin)) {
                if (Animal.VENDIDO.equals(animal.estado)) ventas++; else fallecidos++;
            }
        }
        List<EstadisticasMes.Conteo> razas = new ArrayList<>();
        for (Map.Entry<String, Integer> fila : porRaza.entrySet()) {
            razas.add(new EstadisticasMes.Conteo(fila.getKey().isEmpty() ? null : nombresDeRaza.get(fila.getKey()), fila.getValue()));
        }
        razas.sort(Comparator.comparingInt((EstadisticasMes.Conteo c) -> -c.cantidad)
                .thenComparing(c -> c.nombre == null ? "" : c.nombre));
        if (razas.size() > RAZAS_MAXIMO) razas = new ArrayList<>(razas.subList(0, RAZAS_MAXIMO));
        return new EstadisticasMes.Animales(activos, anterior, hembras, machos, ingresos, ventas, fallecidos, razas);
    }

    // --- Pesos ---

    /** El último pesaje vigente hasta {@code fecha}: el de fecha más reciente y, a igual fecha, el de hora más tardía. */
    private static Pesaje ultimoHasta(List<Pesaje> pesajes, String fecha) {
        Pesaje ultimo = null;
        if (pesajes == null) return null;
        for (Pesaje pesaje : pesajes) {
            if (pesaje.fecha.compareTo(fecha) > 0) continue;
            if (ultimo == null || comparar(pesaje, ultimo) > 0) ultimo = pesaje;
        }
        return ultimo;
    }

    private static int comparar(Pesaje a, Pesaje b) {
        int porFecha = a.fecha.compareTo(b.fecha);
        if (porFecha != 0) return porFecha;
        int porHora = (a.hora == null ? "" : a.hora).compareTo(b.hora == null ? "" : b.hora);
        return porHora != 0 ? porHora : a.creadoEn.compareTo(b.creadoEn);
    }

    private static Double promedio(List<Animal> animales, Map<String, List<Pesaje>> pesajes, String corte) {
        double suma = 0;
        int cuenta = 0;
        for (Animal animal : animales) {
            if (!activoEn(animal, corte)) continue;
            Pesaje ultimo = ultimoHasta(pesajes.get(animal.idAnimal), corte);
            if (ultimo != null) { suma += ultimo.pesoKg; cuenta++; }
        }
        return cuenta == 0 ? null : suma / cuenta;
    }

    private static EstadisticasMes.Pesos pesos(List<Animal> animales, Map<String, List<Pesaje>> pesajes, String inicio, String corte, String corteAnterior) {
        String limite = LocalDate.parse(corte).minusDays(DIAS_SIN_PESAR).toString();
        int conPeso = 0, pesadosEnElMes = 0, sinPesar = 0;
        for (Animal animal : animales) {
            if (!activoEn(animal, corte)) continue;
            List<Pesaje> propios = pesajes.get(animal.idAnimal);
            Pesaje ultimo = ultimoHasta(propios, corte);
            if (ultimo == null) { sinPesar++; continue; }
            conPeso++;
            if (ultimo.fecha.compareTo(limite) < 0) sinPesar++;
            if (ultimo.fecha.compareTo(inicio) >= 0) pesadosEnElMes++;
        }
        return new EstadisticasMes.Pesos(promedio(animales, pesajes, corte), promedio(animales, pesajes, corteAnterior), conPeso, pesadosEnElMes, sinPesar);
    }

    // --- Gastos ---

    private EstadisticasMes.Gastos gastos(String idFinca, String inicio, String siguiente, YearMonth periodo) {
        long total = db.gastoDao().total(idFinca, inicio, siguiente);
        long anterior = db.gastoDao().total(idFinca, periodo.minusMonths(1).atDay(1).toString(), inicio);
        List<EstadisticasMes.CategoriaGasto> categorias = new ArrayList<>();
        for (CategoriaTotal fila : db.gastoDao().porCategoria(idFinca, inicio, siguiente)) {
            categorias.add(new EstadisticasMes.CategoriaGasto(fila.categoria, fila.total));
        }
        return new EstadisticasMes.Gastos(total, anterior, categorias);
    }

    private String idFincaActiva() throws ErrorModelo {
        String idFinca = db.configuracionDao().idFincaActiva();
        if (idFinca == null) throw ErrorModelo.noEncontrado(ErrorModelo.FINCA);
        return idFinca;
    }
}
