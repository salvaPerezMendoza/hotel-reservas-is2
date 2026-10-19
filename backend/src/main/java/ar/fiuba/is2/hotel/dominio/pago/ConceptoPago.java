package ar.fiuba.is2.hotel.dominio.pago;

public enum ConceptoPago {
    /** Pago parcial para confirmar la reserva. */
    SENA,
    /** Pago del total de la estadía. */
    TOTAL,
    /** Pago de lo que falta (saldo de la estadía y consumos). */
    SALDO,
    /** Devolución al huésped por una cancelación. */
    REINTEGRO
}
