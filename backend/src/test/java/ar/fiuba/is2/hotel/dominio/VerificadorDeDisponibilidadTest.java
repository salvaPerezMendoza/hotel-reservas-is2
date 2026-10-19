package ar.fiuba.is2.hotel.dominio;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.disponibilidad.VerificadorDeDisponibilidad;
import ar.fiuba.is2.hotel.dominio.habitacion.Bloqueo;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.pago.ConceptoPago;
import ar.fiuba.is2.hotel.dominio.pago.EstadoPago;
import ar.fiuba.is2.hotel.dominio.pago.MedioPago;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests del algoritmo de disponibilidad (punto 6 del TP). */
class VerificadorDeDisponibilidadTest {

    private final VerificadorDeDisponibilidad verificador = new VerificadorDeDisponibilidad();
    private final TipoHabitacion standard = Fixtures.tipo(1, "Standard", 2, "100000");
    private final Habitacion h101 = Fixtures.habitacion(1, "101", standard);
    private final Habitacion h102 = Fixtures.habitacion(2, "102", standard);
    private final Instant ahora = Instant.parse("2026-11-01T12:00:00Z");

    private Estadia del(int desde, int hasta) {
        return new Estadia(LocalDate.of(2026, 11, desde), LocalDate.of(2026, 11, hasta));
    }

    private Reserva confirmada(Habitacion h, Estadia e) {
        Reserva r = Reserva.online("AUR-TEST01", Fixtures.huesped(9), h, e, 2, new BigDecimal("300000"), ahora, 10);
        r.registrarPago(new BigDecimal("300000"), MedioPago.ONLINE, ConceptoPago.TOTAL, EstadoPago.APROBADO, "PAY-1",
                null, new BigDecimal("90000"), ahora);
        return r;
    }

    @Test
    void habitacionSinReservasNiBloqueosEstaDisponible() {
        assertThat(verificador.estaDisponible(h101, del(10, 12), List.of(), List.of(), ahora)).isTrue();
    }

    @Test
    void unaReservaConfirmadaSuperpuestaLaOcupa() {
        Reserva existente = confirmada(h101, del(10, 14));
        assertThat(verificador.estaDisponible(h101, del(12, 16), List.of(existente), List.of(), ahora)).isFalse();
    }

    @Test
    void sePuedeEntrarElDiaQueSaleElHuespedAnterior() {
        Reserva existente = confirmada(h101, del(10, 14));
        assertThat(verificador.estaDisponible(h101, del(14, 16), List.of(existente), List.of(), ahora)).isTrue();
    }

    @Test
    void unaRetencionVigenteBloqueaYUnaVencidaNo() {
        Reserva pendiente = Reserva.online("AUR-TEST02", Fixtures.huesped(9), h101, del(10, 14), 2,
                new BigDecimal("300000"), ahora, 10);
        assertThat(verificador.estaDisponible(h101, del(11, 12), List.of(pendiente), List.of(), ahora.plus(5, ChronoUnit.MINUTES)))
                .isFalse();
        assertThat(verificador.estaDisponible(h101, del(11, 12), List.of(pendiente), List.of(), ahora.plus(11, ChronoUnit.MINUTES)))
                .isTrue();
    }

    @Test
    void unBloqueoPorMantenimientoLaSacaDeDisponibilidad() {
        Bloqueo bloqueo = new Bloqueo(h101, del(5, 11), "Pintura");
        assertThat(verificador.estaDisponible(h101, del(10, 12), List.of(), List.of(bloqueo), ahora)).isFalse();
        assertThat(verificador.estaDisponible(h101, del(11, 12), List.of(), List.of(bloqueo), ahora)).isTrue();
    }

    @Test
    void unaHabitacionDadaDeBajaNoSeOfrece() {
        h101.desactivar();
        assertThat(verificador.estaDisponible(h101, del(10, 12), List.of(), List.of(), ahora)).isFalse();
    }

    @Test
    void filtraSoloLasHabitacionesLibres() {
        Reserva ocupa101 = confirmada(h101, del(10, 14));
        List<Habitacion> libres = verificador.habitacionesLibres(List.of(h101, h102), del(11, 13),
                Map.of(1L, List.of(ocupa101)), Map.of(), ahora);
        assertThat(libres).containsExactly(h102);
    }
}
