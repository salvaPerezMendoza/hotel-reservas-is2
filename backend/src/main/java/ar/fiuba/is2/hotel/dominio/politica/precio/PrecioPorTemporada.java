package ar.fiuba.is2.hotel.dominio.politica.precio;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Estrategia por defecto: cada noche se cobra según la temporada en la que cae.
 * Una estadía del 30/11 al 3/12 puede tener noches de temporada media y alta.
 */
public class PrecioPorTemporada implements PoliticaDePrecio {

    @Override
    public Cotizacion cotizar(TipoHabitacion tipo, Estadia estadia, Tarifario tarifario) {
        List<Cotizacion.NochePrecio> noches = estadia.fechasDeNoches().stream()
                .map(fecha -> new Cotizacion.NochePrecio(
                        fecha, tarifario.categoriaDe(fecha), tarifario.precioNoche(tipo, fecha)))
                .toList();
        BigDecimal subtotal = noches.stream()
                .map(Cotizacion.NochePrecio::precio)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new Cotizacion(noches, subtotal, BigDecimal.ZERO.setScale(2), null, subtotal);
    }

    @Override
    public String descripcion() {
        return "Cada noche se cobra según la temporada (alta, media o baja) en la que cae.";
    }
}
