package ar.fiuba.is2.hotel.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReporteDtos {

    private ReporteDtos() {
    }

    public record OcupacionTipoDto(String tipo, long habitaciones, long nochesDisponibles, long nochesOcupadas,
                                   BigDecimal porcentaje) {
    }

    public record OcupacionDiaDto(LocalDate fecha, long ocupadas, long disponibles, BigDecimal porcentaje) {
    }

    public record ReporteOcupacionDto(LocalDate desde, LocalDate hasta, long nochesDisponibles, long nochesOcupadas,
                                      BigDecimal porcentaje, List<OcupacionTipoDto> porTipo,
                                      List<OcupacionDiaDto> porDia) {
    }

    public record MontoDto(String clave, BigDecimal monto, long operaciones) {
    }

    public record IngresoDiaDto(LocalDate fecha, BigDecimal monto) {
    }

    public record ReporteIngresosDto(LocalDate desde, LocalDate hasta, BigDecimal cobrado, BigDecimal reintegrado,
                                     BigDecimal neto, List<MontoDto> porTipo, List<MontoDto> porMedio,
                                     List<IngresoDiaDto> porDia) {
    }

    public record TableroDto(long llegadasHoy, long salidasHoy, long enCurso, long habitacionesDisponibles,
                             long habitacionesTotales, long enLimpieza, long enMantenimiento,
                             long pendientesDePago, BigDecimal ocupacionHoy) {
    }
}
