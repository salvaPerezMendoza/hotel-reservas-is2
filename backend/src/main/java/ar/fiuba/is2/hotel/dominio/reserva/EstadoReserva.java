package ar.fiuba.is2.hotel.dominio.reserva;

/**
 * Ciclo de vida de una reserva.
 *
 * <pre>
 * PENDIENTE_PAGO --pago--> CONFIRMADA --check-in--> EN_CURSO --check-out--> FINALIZADA
 *       |                      |
 *       +--vence retención--> EXPIRADA
 *       +----------cancelar--------> CANCELADA
 * </pre>
 */
public enum EstadoReserva {
    PENDIENTE_PAGO,
    CONFIRMADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA,
    EXPIRADA;

    /** Estados en los que la reserva bloquea la habitación (igual que la restricción de la base). */
    public boolean bloqueaHabitacion() {
        return this == PENDIENTE_PAGO || this == CONFIRMADA || this == EN_CURSO;
    }

    public boolean esModificable() {
        return this == PENDIENTE_PAGO || this == CONFIRMADA;
    }
}
