package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.repositorio.ReservaRepository;
import ar.fiuba.is2.hotel.servicio.notificacion.EventoReserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** Procesos automáticos del sistema. */
@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final ReservaRepository reservas;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public TareasProgramadas(ReservaRepository reservas, ApplicationEventPublisher eventos, Clock reloj) {
        this.reservas = reservas;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** Cada 30 segundos libera las habitaciones cuya retención venció sin pago (RF-07). */
    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    @Transactional
    public void expirarRetenciones() {
        int expiradas = reservas.expirarRetencionesVencidas(reloj.instant());
        if (expiradas > 0) {
            log.info("Se liberaron {} habitaciones con la retención vencida", expiradas);
        }
    }

    /** Cada hora manda el recordatorio a quienes ingresan mañana (RF-27). */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 20_000)
    @Transactional
    public void enviarRecordatorios() {
        LocalDate manana = LocalDate.now(reloj).plusDays(1);
        for (Reserva r : reservas.pendientesDeRecordatorio(manana)) {
            r.marcarRecordatorioEnviado();
            eventos.publishEvent(new EventoReserva(r.getId(), EventoReserva.Tipo.RECORDATORIO));
        }
    }
}
