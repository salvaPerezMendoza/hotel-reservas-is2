package ar.fiuba.is2.hotel.config;

import ar.fiuba.is2.hotel.dominio.disponibilidad.VerificadorDeDisponibilidad;
import ar.fiuba.is2.hotel.dominio.reserva.GeneradorDeCodigo;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ConfiguracionGeneral {

    /** Reloj del sistema en la zona horaria del hotel. Se reemplaza en los tests. */
    @Bean
    public Clock reloj(AppProperties props) {
        return Clock.system(ZoneId.of(props.zonaHoraria()));
    }

    @Bean
    public VerificadorDeDisponibilidad verificadorDeDisponibilidad() {
        return new VerificadorDeDisponibilidad();
    }

    @Bean
    public GeneradorDeCodigo generadorDeCodigo(AppProperties props) {
        return new GeneradorDeCodigo(props.prefijoCodigo());
    }
}
