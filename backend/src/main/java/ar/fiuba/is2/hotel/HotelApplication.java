package ar.fiuba.is2.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * Punto de entrada del sistema de reservas de hotel.
 * TP Ingeniería de Software II (FIUBA) - Grupo 11.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class HotelApplication {

    static {
        // El servidor trabaja en UTC; la zona del hotel se aplica explícitamente con el Clock.
        // Además evita que el driver le mande a PostgreSQL alias de zona que no reconoce.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(HotelApplication.class, args);
    }
}
