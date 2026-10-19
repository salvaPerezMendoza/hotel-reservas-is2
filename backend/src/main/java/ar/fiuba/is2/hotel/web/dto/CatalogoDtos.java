package ar.fiuba.is2.hotel.web.dto;

import ar.fiuba.is2.hotel.dominio.tarifa.CategoriaTemporada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record TipoHabitacionDto(Long id, String nombre, String descripcion, int capacidad, BigDecimal precioBase,
                                    List<String> servicios, String imagenUrl, boolean activo, long cantidadHabitaciones) {
    }

    public record NocheDto(LocalDate fecha, CategoriaTemporada categoria, BigDecimal precio) {
    }

    public record CotizacionDto(long noches, List<NocheDto> detalle, BigDecimal subtotal, BigDecimal descuento,
                                String motivoDescuento, BigDecimal total, BigDecimal sena) {
    }

    public record OpcionDisponibleDto(TipoHabitacionDto tipo, int habitacionesLibres, CotizacionDto cotizacion) {
    }

    public record ResultadoBusquedaDto(LocalDate checkIn, LocalDate checkOut, int huespedes, long noches,
                                       List<OpcionDisponibleDto> opciones) {
    }

    public record HotelDto(String nombre, String horaCheckIn, String horaCheckOut, int porcentajeSena,
                           int minutosRetencion, String politicaCancelacion, String descripcionPoliticaCancelacion,
                           String descripcionPoliticaPrecio) {
    }
}
