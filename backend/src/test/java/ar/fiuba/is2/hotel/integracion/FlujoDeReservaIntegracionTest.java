package ar.fiuba.is2.hotel.integracion;

import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.usuario.Recepcionista;
import ar.fiuba.is2.hotel.repositorio.HabitacionRepository;
import ar.fiuba.is2.hotel.repositorio.TipoHabitacionRepository;
import ar.fiuba.is2.hotel.repositorio.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujos completos contra la API y una base PostgreSQL real. */
@AutoConfigureMockMvc
class FlujoDeReservaIntegracionTest extends IntegracionBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    TipoHabitacionRepository tipos;
    @Autowired
    HabitacionRepository habitaciones;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder passwordEncoder;

    private Long tipoSuite;
    private final LocalDate checkIn = LocalDate.now().plusDays(20);
    private final LocalDate checkOut = checkIn.plusDays(3);

    @BeforeEach
    void prepararCatalogo() {
        jdbc.execute("TRUNCATE auditoria, consumos, pagos, reservas, bloqueos, tarifas, temporadas, habitaciones, "
                + "tipos_habitacion, tokens_recuperacion, usuarios RESTART IDENTITY CASCADE");
        TipoHabitacion suite = tipos.save(new TipoHabitacion("Suite", "Test", 2, new BigDecimal("200000"), List.of("Wi-Fi"), null));
        tipoSuite = suite.getId();
        habitaciones.save(new Habitacion("401", 4, suite));
        Recepcionista r = new Recepcionista("Martín", "Gómez", "recepcion@test.com");
        r.cambiarPassword(passwordEncoder.encode("Recepcion123!"));
        usuarios.save(r);
    }

    private String registrar(String email) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Prueba","email":"%s","password":"Secreta123"}
                                """.formatted(email)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String login(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String cuerpoReserva() {
        return """
                {"tipoId":%d,"checkIn":"%s","checkOut":"%s","huespedes":2}
                """.formatted(tipoSuite, checkIn, checkOut);
    }

    @Test
    void flujoCompletoBuscarReservarPagarYCancelar() throws Exception {
        // CU-01: buscar sin iniciar sesión
        mvc.perform(get("/api/disponibilidad").param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString()).param("huespedes", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opciones[0].tipo.nombre").value("Suite"))
                .andExpect(jsonPath("$.opciones[0].cotizacion.total").value(600000.0));

        String token = registrar("ana@test.com");

        // CU-02: reservar → queda pendiente con retención
        MvcResult creada = mvc.perform(post("/api/reservas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoReserva()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andReturn();
        String codigo = json.readTree(creada.getResponse().getContentAsString()).get("codigo").asText();

        // La única Suite queda retenida: nadie más la ve disponible
        mvc.perform(get("/api/disponibilidad").param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString()).param("huespedes", "2"))
                .andExpect(jsonPath("$.opciones").isEmpty());

        // CU-03: pagar la seña (30%) → confirmada
        mvc.perform(post("/api/reservas/" + codigo + "/pagos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"concepto\":\"SENA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprobado").value(true))
                .andExpect(jsonPath("$.reserva.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.reserva.saldoPendiente").value(420000.0));

        // Cancelar con 20 días de anticipación: política moderada → 100% de reintegro
        mvc.perform(get("/api/reservas/" + codigo + "/cancelacion").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.porcentajeReintegro").value(100));
        mvc.perform(post("/api/reservas/" + codigo + "/cancelar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.reembolso").value(180000.0));

        // La habitación vuelve a estar disponible
        mvc.perform(get("/api/disponibilidad").param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString()).param("huespedes", "2"))
                .andExpect(jsonPath("$.opciones[0].habitacionesLibres").value(1));
    }

    @Test
    void unHuespedNoVeReservasAjenasNiEntraARecepcion() throws Exception {
        String ana = registrar("ana@test.com");
        String otro = registrar("otro@test.com");
        MvcResult creada = mvc.perform(post("/api/reservas").header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoReserva()))
                .andExpect(status().isCreated()).andReturn();
        String codigo = json.readTree(creada.getResponse().getContentAsString()).get("codigo").asText();

        mvc.perform(get("/api/reservas/" + codigo).header("Authorization", "Bearer " + otro))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/recepcion/tablero").header("Authorization", "Bearer " + ana))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas/mias")).andExpect(status().isUnauthorized());

        String recepcion = login("recepcion@test.com", "Recepcion123!");
        mvc.perform(get("/api/reservas/" + codigo).header("Authorization", "Bearer " + recepcion))
                .andExpect(status().isOk());
    }

    /**
     * RF-08 / RNF-04: muchas personas intentan reservar la última habitación al mismo
     * tiempo. Exactamente una lo logra; el resto recibe "sin disponibilidad".
     */
    @Test
    void reservasSimultaneasNoGeneranOverbooking() throws Exception {
        int intentos = 8;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < intentos; i++) {
            tokens.add(registrar("huesped" + i + "@test.com"));
        }
        ExecutorService pool = Executors.newFixedThreadPool(intentos);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (String token : tokens) {
            resultados.add(pool.submit(() -> {
                largada.await();
                return mvc.perform(post("/api/reservas").header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReserva()))
                        .andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown();
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> f : resultados) {
            estados.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(estados.stream().filter(s -> s == 201).count()).isEqualTo(1);
        assertThat(estados.stream().filter(s -> s == 409).count()).isEqualTo(intentos - 1);
        Integer activas = jdbc.queryForObject(
                "select count(*) from reservas where estado in ('PENDIENTE_PAGO','CONFIRMADA','EN_CURSO')", Integer.class);
        assertThat(activas).isEqualTo(1);
    }

    /** La restricción de la base rechaza dos reservas superpuestas aunque se salteen las validaciones de Java. */
    @Test
    void laBaseDeDatosRechazaReservasSuperpuestas() {
        Long huesped = jdbc.queryForObject(
                "insert into usuarios (rol, nombre, apellido, email) values ('HUESPED','X','Y','x@test.com') returning id", Long.class);
        Long habitacion = jdbc.queryForObject("select id from habitaciones limit 1", Long.class);
        String insert = "insert into reservas (codigo, huesped_id, habitacion_id, tipo_id, check_in, check_out, "
                + "cantidad_huespedes, precio_total, estado, canal) values (?,?,?,?,?,?,2,1000,'CONFIRMADA','RECEPCION')";
        jdbc.update(insert, "AUR-AAAAA1", huesped, habitacion, tipoSuite, checkIn, checkOut);
        try {
            jdbc.update(insert, "AUR-AAAAA2", huesped, habitacion, tipoSuite, checkIn.plusDays(1), checkOut.plusDays(1));
            throw new AssertionError("La base debería haber rechazado la segunda reserva");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            assertThat(e.getMostSpecificCause().getMessage()).contains("reservas_sin_superposicion");
        }
        // Una que empieza justo el día de salida sí se permite
        jdbc.update(insert, "AUR-AAAAA3", huesped, habitacion, tipoSuite, checkOut, checkOut.plusDays(2));
    }

    @Test
    void recepcionCargaUnaReservaTelefonicaYCobraEnEfectivo() throws Exception {
        String recepcion = login("recepcion@test.com", "Recepcion123!");
        MvcResult res = mvc.perform(post("/api/recepcion/reservas").header("Authorization", "Bearer " + recepcion)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"huesped":{"nombre":"Luis","apellido":"Paz","email":"luis@test.com","telefono":"1122334455"},
                                 "tipoId":%d,"checkIn":"%s","checkOut":"%s","huespedes":2,"observaciones":"Llega tarde"}
                                """.formatted(tipoSuite, checkIn, checkOut)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.canal").value("RECEPCION"))
                .andReturn();
        JsonNode reserva = json.readTree(res.getResponse().getContentAsString());
        mvc.perform(post("/api/recepcion/reservas/" + reserva.get("codigo").asText() + "/pagos")
                        .header("Authorization", "Bearer " + recepcion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":100000,\"medio\":\"EFECTIVO\",\"concepto\":\"SENA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoPendiente").value(500000.0));

        // El huésped creado por recepción puede completar su cuenta registrándose con el mismo email
        registrar("luis@test.com");
    }
}
