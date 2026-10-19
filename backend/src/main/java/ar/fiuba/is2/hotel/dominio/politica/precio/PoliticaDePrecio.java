package ar.fiuba.is2.hotel.dominio.politica.precio;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifario;

/**
 * <b>Patrón Strategy</b> para el cálculo del precio de una estadía.
 *
 * <p>Las reservas no saben cómo se calcula el precio: le piden a la política
 * vigente una {@link Cotizacion}. El administrador elige la política en la
 * configuración del hotel y se puede sumar una nueva implementación sin tocar
 * el código de reservas (principio abierto/cerrado).</p>
 */
public interface PoliticaDePrecio {

    Cotizacion cotizar(TipoHabitacion tipo, Estadia estadia, Tarifario tarifario);

    /** Texto para mostrarle al administrador y al huésped. */
    String descripcion();
}
