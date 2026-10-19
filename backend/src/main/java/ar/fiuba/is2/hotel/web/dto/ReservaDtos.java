package ar.fiuba.is2.hotel.web.dto;

import ar.fiuba.is2.hotel.dominio.pago.ConceptoPago;
import ar.fiuba.is2.hotel.dominio.pago.EstadoPago;
import ar.fiuba.is2.hotel.dominio.pago.MedioPago;
import ar.fiuba.is2.hotel.dominio.reserva.CanalReserva;
import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ReservaDtos {

    private ReservaDtos() {
    }

    public record CrearReservaRequest(
            @NotNull(message = "Elegí un tipo de habitación") Long tipoId,
            @NotNull(message = "La fecha de ingreso es obligatoria") LocalDate checkIn,
            @NotNull(message = "La fecha de egreso es obligatoria") LocalDate checkOut,
            @Min(value = 1, message = "Tiene que haber al menos un huésped") @Max(10) int huespedes,
            @Size(max = 500) String observaciones) {
    }

    public record ModificarReservaRequest(
            @NotNull Long tipoId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) @Max(10) int huespedes) {
    }

    public record PagoOnlineRequest(
            @NotNull(message = "Indicá si pagás la seña o el total") ConceptoPago concepto,
            @Size(max = 80) String titular,
            /** Solo para demostrar el flujo alternativo de pago rechazado. */
            boolean simularRechazo) {
    }

    public record PagoPresencialRequest(
            @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
            @NotNull MedioPago medio,
            @NotNull ConceptoPago concepto) {
    }

    public record DatosHuespedRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @NotBlank @Email String email,
            @Size(max = 40) String telefono,
            @Size(max = 30) String documento) {
    }

    public record ReservaRecepcionRequest(
            @NotNull @Valid DatosHuespedRequest huesped,
            @NotNull Long tipoId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) @Max(10) int huespedes,
            @Size(max = 500) String observaciones) {
    }

    public record CheckInRequest(Long habitacionId) {
    }

    public record ConsumoRequest(
            @NotBlank @Size(max = 200) String descripcion,
            @NotNull @DecimalMin(value = "0.01") BigDecimal monto) {
    }

    public record PagoDto(Long id, BigDecimal monto, MedioPago medio, ConceptoPago concepto, EstadoPago estado,
                          String referencia, Instant fecha) {
    }

    public record ConsumoDto(Long id, String descripcion, BigDecimal monto, Instant fecha) {
    }

    public record HuespedResumenDto(Long id, String nombre, String apellido, String email, String telefono,
                                    String documento) {
    }

    public record ReservaDto(
            Long id, String codigo, EstadoReserva estado, CanalReserva canal,
            HuespedResumenDto huesped,
            Long tipoId, String tipoNombre, String tipoImagenUrl,
            Long habitacionId, String habitacionNumero,
            LocalDate checkIn, LocalDate checkOut, long noches, int cantidadHuespedes,
            BigDecimal precioTotal, BigDecimal totalConsumos, BigDecimal totalACobrar,
            BigDecimal totalPagado, BigDecimal saldoPendiente, BigDecimal senaMinima,
            Instant retencionHasta, Instant creadaEn, Instant canceladaEn, BigDecimal reembolso,
            Instant checkInReal, Instant checkOutReal, String observaciones,
            List<PagoDto> pagos, List<ConsumoDto> consumos,
            boolean puedeModificarse, boolean puedeCancelarse) {
    }

    public record SimulacionCancelacionDto(int porcentajeReintegro, BigDecimal montoReintegro, BigDecimal totalPagado,
                                           String explicacion) {
    }

    public record ResultadoPagoDto(boolean aprobado, String mensaje, PagoDto pago, ReservaDto reserva) {
    }
}
