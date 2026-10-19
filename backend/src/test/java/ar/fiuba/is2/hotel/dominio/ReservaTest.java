package ar.fiuba.is2.hotel.dominio;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.habitacion.EstadoHabitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.pago.ConceptoPago;
import ar.fiuba.is2.hotel.dominio.pago.EstadoPago;
import ar.fiuba.is2.hotel.dominio.pago.MedioPago;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.TipoPoliticaCancelacion;
import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ciclo de vida de la reserva: las reglas viven en el dominio (encapsulamiento). */
class ReservaTest {

    private final TipoHabitacion standard = Fixtures.tipo(1, "Standard", 2, "100000");
    private final Habitacion h101 = Fixtures.habitacion(1, "101", standard);
    private final Instant ahora = Instant.parse("2026-11-01T12:00:00Z");
    private final LocalDate hoy = LocalDate.of(2026, 11, 1);
    private final Estadia estadia = new Estadia(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13));
    private final BigDecimal total = new BigDecimal("300000");
    private final BigDecimal sena = new BigDecimal("90000");

    private Reserva nueva() {
        return Reserva.online("AUR-ABC123", Fixtures.huesped(5), h101, estadia, 2, total, ahora, 10);
    }

    @Test
    void reservaOnlineQuedaPendienteConLaHabitacionRetenida() {
        Reserva r = nueva();
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(r.retencionVigente(ahora.plus(9, ChronoUnit.MINUTES))).isTrue();
        assertThat(r.retencionVigente(ahora.plus(10, ChronoUnit.MINUTES))).isFalse();
    }

    @Test
    void pagarLaSenaConfirmaLaReserva() {
        Reserva r = nueva();
        r.registrarPago(sena, MedioPago.ONLINE, ConceptoPago.SENA, EstadoPago.APROBADO, "PAY-1", null, sena, ahora);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(r.saldoPendiente()).isEqualByComparingTo("210000");
    }

    @Test
    void unPagoRechazadoNoConfirma() {
        Reserva r = nueva();
        r.registrarPago(sena, MedioPago.ONLINE, ConceptoPago.SENA, EstadoPago.RECHAZADO, "PAY-1", null, sena, ahora);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(r.totalPagado()).isZero();
    }

    @Test
    void noSePuedePagarConLaRetencionVencida() {
        Reserva r = nueva();
        assertThatThrownBy(() -> r.registrarPago(sena, MedioPago.ONLINE, ConceptoPago.SENA, EstadoPago.APROBADO,
                "PAY-1", null, sena, ahora.plus(15, ChronoUnit.MINUTES)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("venció");
    }

    @Test
    void cancelarConAnticipacionReintegraSegunLaPolitica() {
        Reserva r = nueva();
        r.registrarPago(sena, MedioPago.ONLINE, ConceptoPago.SENA, EstadoPago.APROBADO, "PAY-1", null, sena, ahora);
        var resultado = r.cancelar(TipoPoliticaCancelacion.MODERADA.crear(), hoy, ahora, null);
        assertThat(resultado.porcentajeReintegro()).isEqualTo(100);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(r.totalPagado()).isZero();
        assertThat(r.getPagos()).anyMatch(p -> p.getConcepto() == ConceptoPago.REINTEGRO);
    }

    @Test
    void checkInYCheckOutCambianElEstadoDeLaHabitacion() {
        Reserva r = nueva();
        r.registrarPago(total, MedioPago.ONLINE, ConceptoPago.TOTAL, EstadoPago.APROBADO, "PAY-1", null, sena, ahora);
        assertThatThrownBy(() -> r.hacerCheckIn(h101, hoy, ahora))
                .hasMessageContaining("Todavía no es la fecha de ingreso");

        r.hacerCheckIn(h101, estadia.checkIn(), ahora);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.EN_CURSO);
        assertThat(h101.getEstado()).isEqualTo(EstadoHabitacion.OCUPADA);

        r.hacerCheckOut(ahora);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
        assertThat(h101.getEstado()).isEqualTo(EstadoHabitacion.LIMPIEZA);
    }

    @Test
    void noSePuedeHacerCheckOutConSaldoPendiente() {
        Reserva r = nueva();
        r.registrarPago(sena, MedioPago.ONLINE, ConceptoPago.SENA, EstadoPago.APROBADO, "PAY-1", null, sena, ahora);
        r.hacerCheckIn(h101, estadia.checkIn(), ahora);
        r.agregarConsumo("Frigobar", new BigDecimal("5000"), null, ahora);
        assertThat(r.saldoPendiente()).isEqualByComparingTo("215000");
        assertThatThrownBy(() -> r.hacerCheckOut(ahora)).hasMessageContaining("saldo pendiente");
    }

    @Test
    void noAdmiteMasHuespedesQueLaCapacidad() {
        assertThatThrownBy(() -> Reserva.online("AUR-X", Fixtures.huesped(5), h101, estadia, 3, total, ahora, 10))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("admite hasta 2");
    }
}
