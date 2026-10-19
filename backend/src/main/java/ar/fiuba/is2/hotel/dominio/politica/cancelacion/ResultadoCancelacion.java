package ar.fiuba.is2.hotel.dominio.politica.cancelacion;

import java.math.BigDecimal;

/** Cuánto se le devuelve al huésped al cancelar y por qué. */
public record ResultadoCancelacion(int porcentajeReintegro, BigDecimal montoReintegro, String explicacion) {
}
