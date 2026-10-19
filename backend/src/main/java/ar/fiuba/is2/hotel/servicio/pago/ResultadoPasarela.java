package ar.fiuba.is2.hotel.servicio.pago;

/** Respuesta de la pasarela de pago. */
public record ResultadoPasarela(boolean aprobado, String referencia, String mensaje) {
}
