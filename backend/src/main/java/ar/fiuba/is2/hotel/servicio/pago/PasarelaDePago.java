package ar.fiuba.is2.hotel.servicio.pago;

import java.math.BigDecimal;

/**
 * Contrato con la pasarela de pago externa (RNF-06: el hotel nunca guarda datos de
 * tarjetas). En este proyecto se usa una implementación simulada; conectar una
 * pasarela real implica solamente escribir otra implementación de esta interfaz.
 */
public interface PasarelaDePago {

    ResultadoPasarela cobrar(BigDecimal monto, String descripcion, boolean simularRechazo);
}
