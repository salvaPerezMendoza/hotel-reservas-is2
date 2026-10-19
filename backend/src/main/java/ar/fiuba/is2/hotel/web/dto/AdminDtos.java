package ar.fiuba.is2.hotel.web.dto;

import ar.fiuba.is2.hotel.dominio.habitacion.EstadoHabitacion;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.TipoPoliticaCancelacion;
import ar.fiuba.is2.hotel.dominio.politica.precio.TipoPoliticaPrecio;
import ar.fiuba.is2.hotel.dominio.tarifa.CategoriaTemporada;
import ar.fiuba.is2.hotel.dominio.usuario.Rol;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record TipoHabitacionRequest(
            @NotBlank @Size(max = 80) String nombre,
            @Size(max = 2000) String descripcion,
            @Min(1) @Max(10) int capacidad,
            @NotNull @DecimalMin("1") BigDecimal precioBase,
            List<String> servicios,
            @Size(max = 500) String imagenUrl) {
    }

    public record HabitacionRequest(
            @NotBlank @Size(max = 10) String numero,
            @Min(0) @Max(100) int piso,
            @NotNull Long tipoId) {
    }

    public record HabitacionDto(Long id, String numero, int piso, Long tipoId, String tipoNombre,
                                EstadoHabitacion estado, boolean activa) {
    }

    public record EstadoHabitacionRequest(@NotNull EstadoHabitacion estado) {
    }

    public record TemporadaRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotNull CategoriaTemporada categoria,
            @NotNull LocalDate fechaDesde,
            @NotNull LocalDate fechaHasta) {
    }

    public record TemporadaDto(Long id, String nombre, CategoriaTemporada categoria, LocalDate fechaDesde,
                               LocalDate fechaHasta) {
    }

    public record TarifaItem(@NotNull CategoriaTemporada categoria, @NotNull @DecimalMin("1") BigDecimal precioNoche) {
    }

    public record TarifasTipoRequest(@NotNull List<TarifaItem> tarifas) {
    }

    public record TarifasTipoDto(Long tipoId, String tipoNombre, BigDecimal precioBase, BigDecimal alta,
                                 BigDecimal media, BigDecimal baja) {
    }

    public record ConfiguracionRequest(
            @NotBlank @Size(max = 120) String nombreHotel,
            @NotNull TipoPoliticaCancelacion politicaCancelacion,
            @Min(0) @Max(100) int porcentajeSena,
            @Min(1) @Max(120) int minutosRetencion,
            @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String horaCheckIn,
            @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String horaCheckOut,
            @NotNull TipoPoliticaPrecio estrategiaPrecio) {
    }

    public record OpcionDto(String valor, String nombre, String descripcion) {
    }

    public record ConfiguracionDto(String nombreHotel, TipoPoliticaCancelacion politicaCancelacion, int porcentajeSena,
                                   int minutosRetencion, String horaCheckIn, String horaCheckOut,
                                   TipoPoliticaPrecio estrategiaPrecio,
                                   List<OpcionDto> politicasCancelacion, List<OpcionDto> estrategiasPrecio) {
    }

    public record BloqueoRequest(
            @NotNull Long habitacionId,
            @NotNull LocalDate desde,
            @NotNull LocalDate hasta,
            @NotBlank @Size(max = 200) String motivo) {
    }

    public record BloqueoDto(Long id, Long habitacionId, String habitacionNumero, LocalDate desde, LocalDate hasta,
                             String motivo) {
    }

    public record NuevoUsuarioRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Rol rol) {
    }

    public record CambioRolRequest(@NotNull Rol rol) {
    }

    public record AuditoriaDto(Long id, Instant fecha, String usuario, String accion, String entidad,
                               String entidadId, String detalle) {
    }
}
