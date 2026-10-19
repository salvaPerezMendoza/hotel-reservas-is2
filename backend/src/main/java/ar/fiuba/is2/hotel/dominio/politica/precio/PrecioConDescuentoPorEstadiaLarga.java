package ar.fiuba.is2.hotel.dominio.politica.precio;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifario;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estrategia alternativa: precio por temporada con un descuento para estadías
 * largas. Reutiliza la estrategia por temporada y aplica el descuento encima.
 */
public class PrecioConDescuentoPorEstadiaLarga implements PoliticaDePrecio {

    public static final int NOCHES_MINIMAS = 7;
    public static final BigDecimal PORCENTAJE_DESCUENTO = new BigDecimal("10");

    private final PoliticaDePrecio base = new PrecioPorTemporada();

    @Override
    public Cotizacion cotizar(TipoHabitacion tipo, Estadia estadia, Tarifario tarifario) {
        Cotizacion cotizacionBase = base.cotizar(tipo, estadia, tarifario);
        if (estadia.noches() < NOCHES_MINIMAS) {
            return cotizacionBase;
        }
        BigDecimal descuento = cotizacionBase.subtotal()
                .multiply(PORCENTAJE_DESCUENTO)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return new Cotizacion(
                cotizacionBase.noches(),
                cotizacionBase.subtotal(),
                descuento,
                PORCENTAJE_DESCUENTO.stripTrailingZeros().toPlainString() + "% por estadía de " + NOCHES_MINIMAS + " noches o más",
                cotizacionBase.subtotal().subtract(descuento));
    }

    @Override
    public String descripcion() {
        return "Precio por temporada, con 10% de descuento en estadías de 7 noches o más.";
    }
}
