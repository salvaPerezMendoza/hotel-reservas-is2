package ar.fiuba.is2.hotel.dominio;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.TipoPoliticaCancelacion;
import ar.fiuba.is2.hotel.dominio.politica.precio.Cotizacion;
import ar.fiuba.is2.hotel.dominio.politica.precio.PrecioConDescuentoPorEstadiaLarga;
import ar.fiuba.is2.hotel.dominio.politica.precio.PrecioPorTemporada;
import ar.fiuba.is2.hotel.dominio.tarifa.CategoriaTemporada;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifa;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifario;
import ar.fiuba.is2.hotel.dominio.tarifa.Temporada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests del patrón Strategy: políticas de precio y de cancelación intercambiables. */
class PoliticasTest {

    private final TipoHabitacion standard = Fixtures.tipo(1, "Standard", 2, "100000");
    private final Tarifario tarifario = new Tarifario(
            List.of(new Temporada("Verano", CategoriaTemporada.ALTA, LocalDate.of(2026, 12, 15), LocalDate.of(2027, 2, 28))),
            List.of(new Tarifa(standard, CategoriaTemporada.BAJA, new BigDecimal("100000")),
                    new Tarifa(standard, CategoriaTemporada.ALTA, new BigDecimal("150000"))));

    @Test
    void cadaNocheSeCobraSegunSuTemporada() {
        Estadia cruzaTemporadas = new Estadia(LocalDate.of(2026, 12, 13), LocalDate.of(2026, 12, 17));
        Cotizacion c = new PrecioPorTemporada().cotizar(standard, cruzaTemporadas, tarifario);
        // 13 y 14 en baja (100.000) + 15 y 16 en alta (150.000)
        assertThat(c.total()).isEqualByComparingTo("500000");
        assertThat(c.noches()).extracting(Cotizacion.NochePrecio::categoria)
                .containsExactly(CategoriaTemporada.BAJA, CategoriaTemporada.BAJA, CategoriaTemporada.ALTA, CategoriaTemporada.ALTA);
    }

    @Test
    void laEstrategiaConDescuentoAplica10PorCientoDesde7Noches() {
        Estadia larga = new Estadia(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8));
        Cotizacion c = new PrecioConDescuentoPorEstadiaLarga().cotizar(standard, larga, tarifario);
        assertThat(c.subtotal()).isEqualByComparingTo("700000");
        assertThat(c.descuento()).isEqualByComparingTo("70000");
        assertThat(c.total()).isEqualByComparingTo("630000");

        Estadia corta = new Estadia(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));
        assertThat(new PrecioConDescuentoPorEstadiaLarga().cotizar(standard, corta, tarifario).descuento()).isZero();
    }

    @Test
    void politicaFlexibleDevuelveTodoHastaUnDiaAntes() {
        var flexible = TipoPoliticaCancelacion.FLEXIBLE.crear();
        assertThat(flexible.evaluar(new BigDecimal("1000"), 1).montoReintegro()).isEqualByComparingTo("1000");
        assertThat(flexible.evaluar(new BigDecimal("1000"), 0).montoReintegro()).isEqualByComparingTo("0");
    }

    @Test
    void politicaModeradaDevuelveTodoOMitadSegunAnticipacion() {
        var moderada = TipoPoliticaCancelacion.MODERADA.crear();
        assertThat(moderada.evaluar(new BigDecimal("1000"), 10).porcentajeReintegro()).isEqualTo(100);
        assertThat(moderada.evaluar(new BigDecimal("1000"), 3).montoReintegro()).isEqualByComparingTo("500");
        assertThat(moderada.evaluar(new BigDecimal("1000"), 0).montoReintegro()).isEqualByComparingTo("0");
    }

    @Test
    void politicaEstrictaSoloDevuelveLaMitadConDosSemanas() {
        var estricta = TipoPoliticaCancelacion.ESTRICTA.crear();
        assertThat(estricta.evaluar(new BigDecimal("1000"), 20).montoReintegro()).isEqualByComparingTo("500");
        assertThat(estricta.evaluar(new BigDecimal("1000"), 13).montoReintegro()).isEqualByComparingTo("0");
    }
}
