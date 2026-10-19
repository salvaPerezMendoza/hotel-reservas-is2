package ar.fiuba.is2.hotel.dominio.excepciones;

/** Se lanza cuando se busca una entidad que no existe. */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
