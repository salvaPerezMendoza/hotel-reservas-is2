package ar.fiuba.is2.hotel.servicio.notificacion;

import ar.fiuba.is2.hotel.config.AppProperties;
import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.pago.Pago;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.repositorio.ConfiguracionHotelRepository;
import ar.fiuba.is2.hotel.repositorio.ReservaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Comparator;

/**
 * Escucha los eventos de reservas y manda el mail correspondiente (RF-09, RF-16, RF-27).
 * Corre después de que la transacción se confirma y en otro hilo, así el huésped
 * no espera al servidor de correo.
 */
@Component
public class NotificadorDeReservas {

    private final ReservaRepository reservas;
    private final ConfiguracionHotelRepository configuraciones;
    private final ServicioDeCorreo correo;
    private final PlantillasDeCorreo plantillas;
    private final AppProperties props;

    public NotificadorDeReservas(ReservaRepository reservas, ConfiguracionHotelRepository configuraciones,
                                 ServicioDeCorreo correo, PlantillasDeCorreo plantillas, AppProperties props) {
        this.reservas = reservas;
        this.configuraciones = configuraciones;
        this.correo = correo;
        this.plantillas = plantillas;
        this.props = props;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alOcurrir(EventoReserva evento) {
        Reserva r = reservas.findById(evento.reservaId()).orElse(null);
        if (r == null) {
            return;
        }
        ConfiguracionHotel hotel = configuraciones.findById(ConfiguracionHotel.ID_UNICO).orElseThrow();
        String destino = r.getHuesped().getEmail();
        String url = props.urlFrontend() + "/mis-reservas/" + r.getCodigo();
        switch (evento.tipo()) {
            case CONFIRMADA -> correo.enviar(destino, "Reserva confirmada " + r.getCodigo() + " - " + hotel.getNombreHotel(),
                    plantillas.reserva(hotel, r, "¡Tu reserva está confirmada!",
                            "Hola " + r.getHuesped().getNombre() + ", te esperamos. Guardá tu código de reserva.", url));
            case MODIFICADA -> correo.enviar(destino, "Modificamos tu reserva " + r.getCodigo(),
                    plantillas.reserva(hotel, r, "Reserva modificada",
                            "Hola " + r.getHuesped().getNombre() + ", estos son los nuevos datos de tu reserva.", url));
            case CANCELADA -> correo.enviar(destino, "Reserva cancelada " + r.getCodigo(), plantillas.cancelacion(hotel, r));
            case RECORDATORIO -> correo.enviar(destino, "Mañana te esperamos - " + hotel.getNombreHotel(),
                    plantillas.reserva(hotel, r, "¡Mañana es tu ingreso!",
                            "Hola " + r.getHuesped().getNombre() + ", te recordamos los datos de tu estadía.", url));
            case PAGO_REGISTRADO -> r.getPagos().stream()
                    .filter(Pago::estaAprobado)
                    .max(Comparator.comparing(Pago::getFecha))
                    .ifPresent(p -> correo.enviar(destino, "Comprobante de pago " + p.getReferencia(),
                            plantillas.comprobante(hotel, r, p)));
            case FINALIZADA -> correo.enviar(destino, "Gracias por tu visita - " + hotel.getNombreHotel(),
                    plantillas.reserva(hotel, r, "¡Gracias por elegirnos!",
                            "Hola " + r.getHuesped().getNombre() + ", este es el resumen de tu estadía.", url));
        }
    }
}
