package ar.fiuba.is2.hotel.dominio.politica.cancelacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <b>Patrón Strategy</b> para la política de cancelación (RF-12 / RF-24).
 *
 * <p>Cada política concreta decide qué porcentaje de lo pagado se reintegra
 * según la anticipación con que se cancela. La reserva le delega esa decisión
 * a la política vigente sin conocer sus reglas.</p>
 */
public abstract class PoliticaDeCancelacion {

    /**
     * @param montoPagado          lo que el huésped pagó hasta el momento
     * @param diasDeAnticipacion   días que faltan para el check-in al cancelar
     */
    public final ResultadoCancelacion evaluar(BigDecimal montoPagado, long diasDeAnticipacion) {
        int porcentaje = porcentajeDeReintegro(diasDeAnticipacion);
        BigDecimal monto = montoPagado
                .multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new ResultadoCancelacion(porcentaje, monto, explicar(porcentaje, diasDeAnticipacion));
    }

    /** Paso que cada política concreta define (método plantilla). */
    protected abstract int porcentajeDeReintegro(long diasDeAnticipacion);

    public abstract String nombre();

    public abstract String descripcion();

    private String explicar(int porcentaje, long dias) {
        String anticipacion = dias <= 0 ? "el mismo día del ingreso o después"
                : dias == 1 ? "con 1 día de anticipación" : "con " + dias + " días de anticipación";
        return "Política " + nombre() + ": al cancelar " + anticipacion + " se reintegra el " + porcentaje + "% de lo pagado.";
    }
}
