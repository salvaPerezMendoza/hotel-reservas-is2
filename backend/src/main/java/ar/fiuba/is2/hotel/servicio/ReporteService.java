package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.pago.Pago;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.repositorio.HabitacionRepository;
import ar.fiuba.is2.hotel.repositorio.PagoRepository;
import ar.fiuba.is2.hotel.repositorio.ReservaRepository;
import ar.fiuba.is2.hotel.web.dto.ReporteDtos.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reportes de gestión para el administrador (RF-28, RF-29) y su exportación (RF-30). */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final int MAXIMO_DIAS = 366;

    private final ReservaRepository reservas;
    private final PagoRepository pagos;
    private final HabitacionRepository habitaciones;
    private final Clock reloj;

    public ReporteService(ReservaRepository reservas, PagoRepository pagos, HabitacionRepository habitaciones,
                          Clock reloj) {
        this.reservas = reservas;
        this.pagos = pagos;
        this.habitaciones = habitaciones;
        this.reloj = reloj;
    }

    /** Ocupación entre dos fechas inclusive, por tipo de habitación y por día. */
    public ReporteOcupacionDto ocupacion(LocalDate desde, LocalDate hasta) {
        Estadia periodo = periodo(desde, hasta);
        List<Habitacion> activas = habitaciones.findByActivaTrueOrderByNumeroAsc();
        List<Reserva> ocupando = reservas.ocupandoEntre(periodo.checkIn(), periodo.checkOut());
        long dias = periodo.noches();

        Map<String, Long> habitacionesPorTipo = activas.stream()
                .collect(Collectors.groupingBy(h -> h.getTipo().getNombre(), TreeMap::new, Collectors.counting()));
        Map<String, Long> nochesOcupadasPorTipo = new HashMap<>();
        for (Reserva r : ocupando) {
            nochesOcupadasPorTipo.merge(r.getTipo().getNombre(), r.getEstadia().nochesEnComunCon(periodo), Long::sum);
        }
        List<OcupacionTipoDto> porTipo = habitacionesPorTipo.entrySet().stream().map(e -> {
            long disponibles = e.getValue() * dias;
            long ocupadas = Math.min(nochesOcupadasPorTipo.getOrDefault(e.getKey(), 0L), disponibles);
            return new OcupacionTipoDto(e.getKey(), e.getValue(), disponibles, ocupadas, porcentaje(ocupadas, disponibles));
        }).toList();

        List<OcupacionDiaDto> porDia = periodo.fechasDeNoches().stream().map(fecha -> {
            long ocupadas = ocupando.stream().filter(r -> r.getEstadia().contiene(fecha)).count();
            return new OcupacionDiaDto(fecha, ocupadas, activas.size(), porcentaje(ocupadas, activas.size()));
        }).toList();

        long totalDisponibles = porTipo.stream().mapToLong(OcupacionTipoDto::nochesDisponibles).sum();
        long totalOcupadas = porTipo.stream().mapToLong(OcupacionTipoDto::nochesOcupadas).sum();
        return new ReporteOcupacionDto(desde, hasta, totalDisponibles, totalOcupadas,
                porcentaje(totalOcupadas, totalDisponibles), porTipo, porDia);
    }

    /** Ingresos cobrados entre dos fechas inclusive, por tipo de habitación, medio de pago y día. */
    public ReporteIngresosDto ingresos(LocalDate desde, LocalDate hasta) {
        Estadia periodo = periodo(desde, hasta);
        ZoneId zona = reloj.getZone();
        Instant inicio = periodo.checkIn().atStartOfDay(zona).toInstant();
        Instant fin = periodo.checkOut().atStartOfDay(zona).toInstant();
        List<Pago> aprobados = pagos.aprobadosEntre(inicio, fin);

        BigDecimal cobrado = suma(aprobados.stream().filter(p -> !p.esReintegro()).toList(), Pago::getMonto);
        BigDecimal reintegrado = suma(aprobados.stream().filter(Pago::esReintegro).toList(), Pago::getMonto);

        List<MontoDto> porTipo = agrupar(aprobados, p -> p.getReserva().getTipo().getNombre());
        List<MontoDto> porMedio = agrupar(aprobados, p -> p.getMedio().name());
        Map<LocalDate, BigDecimal> diarios = new TreeMap<>();
        periodo.fechasDeNoches().forEach(f -> diarios.put(f, BigDecimal.ZERO));
        for (Pago p : aprobados) {
            diarios.merge(p.getFecha().atZone(zona).toLocalDate(), p.aporteNeto(), BigDecimal::add);
        }
        List<IngresoDiaDto> porDia = diarios.entrySet().stream()
                .map(e -> new IngresoDiaDto(e.getKey(), e.getValue())).toList();
        return new ReporteIngresosDto(desde, hasta, cobrado, reintegrado, cobrado.subtract(reintegrado),
                porTipo, porMedio, porDia);
    }

    /** Planilla de Excel con los dos reportes (RF-30). */
    public byte[] exportarExcel(LocalDate desde, LocalDate hasta) {
        ReporteOcupacionDto ocupacion = ocupacion(desde, hasta);
        ReporteIngresosDto ingresos = ingresos(desde, hasta);
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle titulo = libro.createCellStyle();
            Font negrita = libro.createFont();
            negrita.setBold(true);
            titulo.setFont(negrita);
            CellStyle plata = libro.createCellStyle();
            plata.setDataFormat(libro.createDataFormat().getFormat("$ #,##0.00"));
            CellStyle porciento = libro.createCellStyle();
            porciento.setDataFormat(libro.createDataFormat().getFormat("0.0\"%\""));

            Sheet hojaOcupacion = libro.createSheet("Ocupación");
            int fila = 0;
            fila(hojaOcupacion, fila++, titulo, "Reporte de ocupación", desde + " al " + hasta);
            fila++;
            fila(hojaOcupacion, fila++, titulo, "Tipo", "Habitaciones", "Noches disponibles", "Noches ocupadas", "Ocupación");
            for (OcupacionTipoDto t : ocupacion.porTipo()) {
                Row row = hojaOcupacion.createRow(fila++);
                row.createCell(0).setCellValue(t.tipo());
                row.createCell(1).setCellValue(t.habitaciones());
                row.createCell(2).setCellValue(t.nochesDisponibles());
                row.createCell(3).setCellValue(t.nochesOcupadas());
                celda(row, 4, t.porcentaje(), porciento);
            }
            Row total = hojaOcupacion.createRow(fila++);
            total.createCell(0).setCellValue("Total");
            total.getCell(0).setCellStyle(titulo);
            total.createCell(2).setCellValue(ocupacion.nochesDisponibles());
            total.createCell(3).setCellValue(ocupacion.nochesOcupadas());
            celda(total, 4, ocupacion.porcentaje(), porciento);
            fila++;
            fila(hojaOcupacion, fila++, titulo, "Fecha", "Ocupadas", "Disponibles", "Ocupación");
            for (OcupacionDiaDto d : ocupacion.porDia()) {
                Row row = hojaOcupacion.createRow(fila++);
                row.createCell(0).setCellValue(d.fecha().toString());
                row.createCell(1).setCellValue(d.ocupadas());
                row.createCell(2).setCellValue(d.disponibles());
                celda(row, 3, d.porcentaje(), porciento);
            }
            for (int i = 0; i < 5; i++) {
                hojaOcupacion.autoSizeColumn(i);
            }

            Sheet hojaIngresos = libro.createSheet("Ingresos");
            fila = 0;
            fila(hojaIngresos, fila++, titulo, "Reporte de ingresos", desde + " al " + hasta);
            fila++;
            Row r1 = hojaIngresos.createRow(fila++);
            r1.createCell(0).setCellValue("Cobrado");
            celda(r1, 1, ingresos.cobrado(), plata);
            Row r2 = hojaIngresos.createRow(fila++);
            r2.createCell(0).setCellValue("Reintegrado");
            celda(r2, 1, ingresos.reintegrado(), plata);
            Row r3 = hojaIngresos.createRow(fila++);
            r3.createCell(0).setCellValue("Neto");
            r3.getCell(0).setCellStyle(titulo);
            celda(r3, 1, ingresos.neto(), plata);
            fila++;
            fila = bloqueMontos(hojaIngresos, fila, titulo, plata, "Por tipo de habitación", ingresos.porTipo());
            fila++;
            fila = bloqueMontos(hojaIngresos, fila, titulo, plata, "Por medio de pago", ingresos.porMedio());
            fila++;
            fila(hojaIngresos, fila++, titulo, "Fecha", "Neto del día");
            for (IngresoDiaDto d : ingresos.porDia()) {
                Row row = hojaIngresos.createRow(fila++);
                row.createCell(0).setCellValue(d.fecha().toString());
                celda(row, 1, d.monto(), plata);
            }
            for (int i = 0; i < 3; i++) {
                hojaIngresos.autoSizeColumn(i);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel", e);
        }
    }

    private int bloqueMontos(Sheet hoja, int fila, CellStyle titulo, CellStyle plata, String nombre, List<MontoDto> montos) {
        fila(hoja, fila++, titulo, nombre, "Neto", "Operaciones");
        for (MontoDto m : montos) {
            Row row = hoja.createRow(fila++);
            row.createCell(0).setCellValue(m.clave());
            celda(row, 1, m.monto(), plata);
            row.createCell(2).setCellValue(m.operaciones());
        }
        return fila;
    }

    private void fila(Sheet hoja, int numero, CellStyle estilo, String... valores) {
        Row row = hoja.createRow(numero);
        for (int i = 0; i < valores.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(valores[i]);
            c.setCellStyle(estilo);
        }
    }

    private void celda(Row row, int columna, BigDecimal valor, CellStyle estilo) {
        Cell c = row.createCell(columna);
        c.setCellValue(valor.doubleValue());
        c.setCellStyle(estilo);
    }

    private Estadia periodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("Elegí un período válido");
        }
        Estadia periodo = new Estadia(desde, hasta.plusDays(1));
        if (periodo.noches() > MAXIMO_DIAS) {
            throw new ReglaDeNegocioException("El período no puede superar un año");
        }
        return periodo;
    }

    private List<MontoDto> agrupar(List<Pago> lista, Function<Pago, String> clave) {
        Map<String, List<Pago>> grupos = lista.stream().collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.toList()));
        return grupos.entrySet().stream()
                .map(e -> new MontoDto(e.getKey(), suma(e.getValue(), Pago::aporteNeto), e.getValue().size()))
                .toList();
    }

    private BigDecimal suma(List<Pago> lista, Function<Pago, BigDecimal> valor) {
        return lista.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal porcentaje(long parte, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(parte * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
