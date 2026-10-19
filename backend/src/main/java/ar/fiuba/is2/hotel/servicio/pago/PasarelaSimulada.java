package ar.fiuba.is2.hotel.servicio.pago;

import ar.fiuba.is2.hotel.config.AppProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;

/** Pasarela de mentira: espera unos segundos y aprueba (o rechaza si se pide, para probar ese flujo). */
@Component
public class PasarelaSimulada implements PasarelaDePago {

    private final SecureRandom random = new SecureRandom();
    private final int demoraSegundos;

    public PasarelaSimulada(AppProperties props) {
        this.demoraSegundos = props.pagos().demoraSegundos();
    }

    @Override
    public ResultadoPasarela cobrar(BigDecimal monto, String descripcion, boolean simularRechazo) {
        try {
            Thread.sleep(demoraSegundos * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String referencia = "PAY-" + (100000 + random.nextInt(900000));
        if (simularRechazo) {
            return new ResultadoPasarela(false, referencia, "El pago fue rechazado por la entidad emisora. Probá con otro medio.");
        }
        return new ResultadoPasarela(true, referencia, "Pago aprobado");
    }
}
