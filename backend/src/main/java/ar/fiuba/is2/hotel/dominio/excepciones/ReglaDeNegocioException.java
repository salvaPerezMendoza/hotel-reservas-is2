package ar.fiuba.is2.hotel.dominio.excepciones;

/**
 * Se lanza cuando una operación viola una regla del negocio
 * (por ejemplo, cancelar una reserva que ya finalizó).
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
