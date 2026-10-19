package ar.fiuba.is2.hotel.dominio.excepciones;

/** Se lanza cuando no hay habitaciones libres para la estadía pedida. */
public class SinDisponibilidadException extends RuntimeException {

    public SinDisponibilidadException(String mensaje) {
        super(mensaje);
    }
}
