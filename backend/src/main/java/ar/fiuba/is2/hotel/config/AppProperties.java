package ar.fiuba.is2.hotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Propiedades propias de la aplicación (sección "app" del application.yml). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String zonaHoraria,
        String prefijoCodigo,
        String urlFrontend,
        List<String> corsOrigenes,
        Jwt jwt,
        Mail mail,
        Pagos pagos,
        boolean datosDemo) {

    public record Jwt(String secreto, int horasValidez) {
    }

    public record Mail(boolean habilitado, String remitente) {
    }

    public record Pagos(int demoraSegundos) {
    }
}
