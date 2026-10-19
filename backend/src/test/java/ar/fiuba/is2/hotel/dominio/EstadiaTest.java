package ar.fiuba.is2.hotel.dominio;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadiaTest {

    private static Estadia del(int desde, int hasta) {
        return new Estadia(LocalDate.of(2026, 11, desde), LocalDate.of(2026, 11, hasta));
    }

    @Test
    void cuentaLasNochesSinIncluirElDiaDeSalida() {
        assertThat(del(10, 13).noches()).isEqualTo(3);
    }

    @Test
    void noAdmiteEgresoAnteriorOIgualAlIngreso() {
        assertThatThrownBy(() -> del(10, 10)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> del(10, 9)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void estadiasQueSeCrucenSeSuperponen() {
        assertThat(del(10, 15).seSuperponeCon(del(12, 18))).isTrue();
        assertThat(del(12, 18).seSuperponeCon(del(10, 15))).isTrue();
        assertThat(del(10, 20).seSuperponeCon(del(12, 14))).isTrue();
    }

    @Test
    void quienSaleUnDiaNoChocaConQuienEntraEseMismoDia() {
        assertThat(del(10, 15).seSuperponeCon(del(15, 18))).isFalse();
        assertThat(del(15, 18).seSuperponeCon(del(10, 15))).isFalse();
    }

    @Test
    void calculaNochesEnComun() {
        assertThat(del(10, 15).nochesEnComunCon(del(13, 20))).isEqualTo(2);
        assertThat(del(10, 15).nochesEnComunCon(del(15, 20))).isZero();
    }
}
