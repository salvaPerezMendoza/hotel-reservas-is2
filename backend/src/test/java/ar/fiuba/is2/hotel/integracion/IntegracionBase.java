package ar.fiuba.is2.hotel.integracion;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de los tests de integración: corren contra un PostgreSQL real porque la
 * garantía anti-overbooking es una restricción propia de PostgreSQL.
 *
 * <p>Si hay Docker disponible, levanta un contenedor descartable (Testcontainers).
 * Si no, usa la base indicada en TEST_DB_URL (por defecto localhost:5432/hotel_test).</p>
 */
@SpringBootTest(properties = {"app.datos-demo=false", "app.pagos.demora-segundos=0"})
public abstract class IntegracionBase {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (dockerDisponible()) {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    private static boolean dockerDisponible() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url",
                    () -> System.getenv().getOrDefault("TEST_DB_URL", "jdbc:postgresql://localhost:5432/hotel_test"));
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USER", "postgres"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", "postgres"));
        }
    }
}
