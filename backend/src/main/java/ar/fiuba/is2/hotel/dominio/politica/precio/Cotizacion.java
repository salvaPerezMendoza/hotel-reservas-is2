package ar.fiuba.is2.hotel.dominio.politica.precio;

import ar.fiuba.is2.hotel.dominio.tarifa.CategoriaTemporada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de cotizar una estadía: detalle noche por noche, descuento y total.
 */
public record Cotizacion(List<NochePrecio> noches, BigDecimal subtotal, BigDecimal descuento,
                         String motivoDescuento, BigDecimal total) {

    public record NochePrecio(LocalDate fecha, CategoriaTemporada categoria, BigDecimal precio) {
    }
}
