package ar.fiuba.is2.hotel.servicio.notificacion;

/** Algo relevante le pasó a una reserva y hay que avisarle al huésped (RF-09 / RF-27). */
public record EventoReserva(Long reservaId, Tipo tipo) {

    public enum Tipo {
        CONFIRMADA,
        MODIFICADA,
        CANCELADA,
        PAGO_REGISTRADO,
        RECORDATORIO,
        FINALIZADA
    }
}
