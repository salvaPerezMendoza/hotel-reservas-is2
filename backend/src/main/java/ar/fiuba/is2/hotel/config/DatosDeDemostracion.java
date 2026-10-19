package ar.fiuba.is2.hotel.config;

import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.habitacion.EstadoHabitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.pago.ConceptoPago;
import ar.fiuba.is2.hotel.dominio.pago.EstadoPago;
import ar.fiuba.is2.hotel.dominio.pago.MedioPago;
import ar.fiuba.is2.hotel.dominio.politica.precio.PrecioPorTemporada;
import ar.fiuba.is2.hotel.dominio.reserva.GeneradorDeCodigo;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.dominio.tarifa.CategoriaTemporada;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifa;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifario;
import ar.fiuba.is2.hotel.dominio.tarifa.Temporada;
import ar.fiuba.is2.hotel.dominio.usuario.Administrador;
import ar.fiuba.is2.hotel.dominio.usuario.Huesped;
import ar.fiuba.is2.hotel.dominio.usuario.Recepcionista;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import ar.fiuba.is2.hotel.repositorio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/**
 * Carga datos de ejemplo la primera vez que arranca el sistema (base vacía):
 * catálogo, tarifas, temporadas, usuarios de prueba y un historial de reservas
 * para que los reportes y la recepción tengan movimiento desde el primer día.
 */
@Component
public class DatosDeDemostracion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDeDemostracion.class);

    private final AppProperties props;
    private final UsuarioRepository usuarios;
    private final TipoHabitacionRepository tipos;
    private final HabitacionRepository habitaciones;
    private final TemporadaRepository temporadas;
    private final TarifaRepository tarifas;
    private final ReservaRepository reservas;
    private final ConfiguracionHotelRepository configuraciones;
    private final PasswordEncoder passwordEncoder;
    private final GeneradorDeCodigo generadorDeCodigo;
    private final Clock reloj;
    private final TransactionTemplate transaccion;
    private final Random random = new Random(11);

    public DatosDeDemostracion(AppProperties props, UsuarioRepository usuarios, TipoHabitacionRepository tipos,
                               HabitacionRepository habitaciones, TemporadaRepository temporadas,
                               TarifaRepository tarifas, ReservaRepository reservas,
                               ConfiguracionHotelRepository configuraciones, PasswordEncoder passwordEncoder,
                               GeneradorDeCodigo generadorDeCodigo, Clock reloj,
                               PlatformTransactionManager transactionManager) {
        this.props = props;
        this.usuarios = usuarios;
        this.tipos = tipos;
        this.habitaciones = habitaciones;
        this.temporadas = temporadas;
        this.tarifas = tarifas;
        this.reservas = reservas;
        this.configuraciones = configuraciones;
        this.passwordEncoder = passwordEncoder;
        this.generadorDeCodigo = generadorDeCodigo;
        this.reloj = reloj;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.datosDemo() || usuarios.count() > 0) {
            return;
        }
        log.info("Base vacía: cargando datos de demostración...");
        transaccion.executeWithoutResult(tx -> cargar());
        log.info("Datos de demostración cargados.");
    }

    private void cargar() {
        // --- Personal y huésped de prueba ---
        Administrador admin = new Administrador("Carolina", "Méndez", "admin@hotelaurora.com");
        admin.cambiarPassword(passwordEncoder.encode("Admin123!"));
        Recepcionista recepcion = new Recepcionista("Martín", "Gómez", "recepcion@hotelaurora.com");
        recepcion.cambiarPassword(passwordEncoder.encode("Recepcion123!"));
        Huesped ana = new Huesped("Ana", "Fernández", "huesped@demo.com", "+54 9 11 5555-0101", "34111222");
        ana.cambiarPassword(passwordEncoder.encode("Huesped123!"));
        usuarios.saveAll(List.of(admin, recepcion, ana));

        List<Huesped> clientes = new ArrayList<>(List.of(ana));
        String[][] nombres = {
                {"Julián", "Pereyra"}, {"Sofía", "Acosta"}, {"Tomás", "Herrera"}, {"Valentina", "Ruiz"},
                {"Mateo", "Castro"}, {"Camila", "Ortiz"}, {"Lautaro", "Silva"}, {"Martina", "Romero"},
                {"Benjamín", "Torres"}, {"Florencia", "Navarro"}, {"Joaquín", "Molina"}, {"Agustina", "Vega"},
                {"Nicolás", "Ibarra"}, {"Paula", "Medina"}, {"Facundo", "Sosa"}, {"Rocío", "Cabrera"}};
        for (int i = 0; i < nombres.length; i++) {
            String email = (nombres[i][0] + "." + nombres[i][1]).toLowerCase()
                    .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    + "@correo.com";
            clientes.add(new Huesped(nombres[i][0], nombres[i][1], email, "+54 9 11 5555-" + (1000 + i),
                    String.valueOf(30_000_000 + i * 731_337)));
        }
        usuarios.saveAll(clientes.subList(1, clientes.size()));

        // --- Catálogo ---
        TipoHabitacion standard = tipos.save(new TipoHabitacion("Standard",
                "Habitación cómoda y luminosa con cama matrimonial o dos camas individuales. Ideal para escapadas cortas y viajes de trabajo.",
                2, new BigDecimal("95000"),
                List.of("Wi-Fi", "Aire acondicionado", "Smart TV", "Desayuno incluido", "Caja fuerte"), null));
        TipoHabitacion superior = tipos.save(new TipoHabitacion("Superior",
                "Más amplia, con vista a la ciudad, sillón de lectura y escritorio. Pensada para quienes buscan un plus de confort.",
                2, new BigDecimal("125000"),
                List.of("Wi-Fi", "Aire acondicionado", "Smart TV", "Desayuno incluido", "Frigobar", "Cafetera", "Vista a la ciudad"), null));
        TipoHabitacion familiar = tipos.save(new TipoHabitacion("Familiar",
                "Dos ambientes conectados con cama matrimonial y dos camas individuales. Espacio de sobra para viajar en familia.",
                4, new BigDecimal("160000"),
                List.of("Wi-Fi", "Aire acondicionado", "Smart TV", "Desayuno incluido", "Frigobar", "Dos ambientes"), null));
        TipoHabitacion suite = tipos.save(new TipoHabitacion("Suite",
                "Nuestra mejor habitación: living independiente, hidromasaje y balcón privado con vista panorámica.",
                3, new BigDecimal("230000"),
                List.of("Wi-Fi", "Aire acondicionado", "Smart TV", "Desayuno incluido", "Frigobar", "Hidromasaje", "Balcón", "Room service 24 h"), null));

        for (TipoHabitacion t : List.of(standard, superior, familiar, suite)) {
            tarifas.save(new Tarifa(t, CategoriaTemporada.BAJA, t.getPrecioBase()));
            tarifas.save(new Tarifa(t, CategoriaTemporada.MEDIA, redondear(t.getPrecioBase().multiply(new BigDecimal("1.2")))));
            tarifas.save(new Tarifa(t, CategoriaTemporada.ALTA, redondear(t.getPrecioBase().multiply(new BigDecimal("1.5")))));
        }

        List<Habitacion> todas = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            todas.add(new Habitacion("10" + i, 1, standard));
        }
        for (int i = 1; i <= 5; i++) {
            todas.add(new Habitacion("20" + i, 2, superior));
        }
        for (int i = 1; i <= 4; i++) {
            todas.add(new Habitacion("30" + i, 3, familiar));
        }
        for (int i = 1; i <= 3; i++) {
            todas.add(new Habitacion("40" + i, 4, suite));
        }
        habitaciones.saveAll(todas);

        int anio = LocalDate.now(reloj).getYear();
        for (int y = anio - 1; y <= anio + 1; y++) {
            temporadas.save(new Temporada("Otoño " + y, CategoriaTemporada.MEDIA, LocalDate.of(y, 3, 1), LocalDate.of(y, 4, 30)));
            temporadas.save(new Temporada("Vacaciones de invierno " + y, CategoriaTemporada.ALTA, LocalDate.of(y, 7, 10), LocalDate.of(y, 7, 31)));
            temporadas.save(new Temporada("Primavera " + y, CategoriaTemporada.MEDIA, LocalDate.of(y, 9, 15), LocalDate.of(y, 11, 30)));
            temporadas.save(new Temporada("Verano " + y + "/" + (y + 1), CategoriaTemporada.ALTA, LocalDate.of(y, 12, 15), LocalDate.of(y + 1, 2, 28)));
        }

        // --- Historial de reservas ---
        ConfiguracionHotel config = configuraciones.findById(ConfiguracionHotel.ID_UNICO).orElseThrow();
        Tarifario tarifario = new Tarifario(temporadas.findAll(), tarifas.findAll());
        PrecioPorTemporada politicaPrecio = new PrecioPorTemporada();
        LocalDate hoy = LocalDate.now(reloj);

        for (Habitacion habitacion : todas) {
            LocalDate dia = hoy.minusDays(60 + random.nextInt(5));
            while (dia.isBefore(hoy.plusDays(35))) {
                dia = dia.plusDays(random.nextInt(4));
                int noches = 1 + random.nextInt(5);
                Estadia estadia = new Estadia(dia, dia.plusDays(noches));
                Huesped huesped = clientes.get(1 + random.nextInt(clientes.size() - 1));
                int personas = 1 + random.nextInt(habitacion.getTipo().getCapacidad());
                BigDecimal precio = politicaPrecio.cotizar(habitacion.getTipo(), estadia, tarifario).total();
                boolean dejarLibreHoy = estadia.contiene(hoy) && random.nextInt(10) < 3;
                // A futuro hay menos reservas: así la búsqueda muestra disponibilidad real.
                boolean futuraDescartada = estadia.checkIn().isAfter(hoy) && random.nextInt(10) < 6;
                if (!dejarLibreHoy && !futuraDescartada) {
                    crearHistorica(habitacion, huesped, estadia, personas, precio, config, recepcion, hoy);
                }
                dia = estadia.checkOut();
            }
            if (habitacion.getEstado() != EstadoHabitacion.OCUPADA && random.nextInt(10) == 0) {
                habitacion.cambiarEstado(EstadoHabitacion.LIMPIEZA);
            }
        }
        // Una habitación en mantenimiento para mostrar ese estado
        Habitacion enMantenimiento = todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.DISPONIBLE)
                .reduce((a, b) -> b).orElseThrow();
        enMantenimiento.cambiarEstado(EstadoHabitacion.MANTENIMIENTO);

        crearReservasDeAna(ana, clientes, tarifario, politicaPrecio, config, hoy);
    }

    /** Crea una reserva con el estado que le corresponde según sus fechas, usando las reglas del dominio. */
    private void crearHistorica(Habitacion habitacion, Huesped huesped, Estadia estadia, int personas, BigDecimal precio,
                                ConfiguracionHotel config, Usuario recepcion, LocalDate hoy) {
        ZoneId zona = reloj.getZone();
        Instant creada = estadia.checkIn().minusDays(5 + random.nextInt(25)).atTime(10 + random.nextInt(10), random.nextInt(60))
                .atZone(zona).toInstant();
        boolean online = random.nextBoolean();
        Reserva r = online
                ? Reserva.online(generadorDeCodigo.nuevo(), huesped, habitacion, estadia, personas, precio, creada, config.getMinutosRetencion())
                : Reserva.porRecepcion(generadorDeCodigo.nuevo(), huesped, habitacion, estadia, personas, precio, recepcion,
                null, creada);
        BigDecimal sena = config.senaPara(precio);
        if (online) {
            boolean total = random.nextInt(3) == 0;
            r.registrarPago(total ? precio : sena, MedioPago.ONLINE, total ? ConceptoPago.TOTAL : ConceptoPago.SENA,
                    EstadoPago.APROBADO, "PAY-" + (100000 + random.nextInt(900000)), huesped, sena, creada.plusSeconds(90));
        } else if (random.nextBoolean()) {
            r.registrarPago(sena, MedioPago.TRANSFERENCIA, ConceptoPago.SENA, EstadoPago.APROBADO,
                    "REC-" + (100000 + random.nextInt(900000)), recepcion, sena, creada.plusSeconds(600));
        }

        boolean cancelar = !estadia.checkIn().isAfter(hoy.plusDays(3)) && random.nextInt(12) == 0;
        if (cancelar && estadia.checkIn().isAfter(hoy.minusDays(55))) {
            LocalDate diaCancelacion = estadia.checkIn().minusDays(1 + random.nextInt(6));
            r.cancelar(config.politicaDeCancelacionVigente(), diaCancelacion,
                    diaCancelacion.atTime(12, 0).atZone(zona).toInstant(), huesped);
            reservas.save(r);
            return;
        }
        // Las llegadas de hoy quedan sin check-in, y las salidas de hoy sin check-out,
        // para que recepción tenga tareas para mostrar en la demo.
        boolean llegaHoySinCheckIn = estadia.checkIn().equals(hoy);
        if (!estadia.checkIn().isAfter(hoy) && !llegaHoySinCheckIn) {
            Instant ingreso = estadia.checkIn().atTime(14 + random.nextInt(6), random.nextInt(60)).atZone(zona).toInstant();
            r.hacerCheckIn(habitacion, estadia.checkIn(), ingreso);
            if (random.nextInt(3) == 0) {
                r.agregarConsumo(random.nextBoolean() ? "Cena en el restaurante" : "Frigobar",
                        BigDecimal.valueOf(8000 + random.nextInt(30) * 1000L), recepcion, ingreso.plusSeconds(20_000));
            }
            if (estadia.checkOut().isBefore(hoy)) {
                Instant egreso = estadia.checkOut().atTime(9, random.nextInt(60)).atZone(zona).toInstant();
                BigDecimal saldo = r.saldoPendiente();
                if (saldo.signum() > 0) {
                    MedioPago medio = List.of(MedioPago.EFECTIVO, MedioPago.TARJETA, MedioPago.TRANSFERENCIA).get(random.nextInt(3));
                    r.registrarPago(saldo, medio, ConceptoPago.SALDO, EstadoPago.APROBADO,
                            "REC-" + (100000 + random.nextInt(900000)), recepcion, sena, egreso);
                }
                r.hacerCheckOut(egreso);
                habitacion.cambiarEstado(EstadoHabitacion.DISPONIBLE);
            }
        }
        reservas.save(r);
    }

    private void crearReservasDeAna(Huesped ana, List<Huesped> clientes, Tarifario tarifario,
                                    PrecioPorTemporada politicaPrecio, ConfiguracionHotel config, LocalDate hoy) {
        // Próxima estadía de Ana en una Suite, dentro de 50 días (fuera del rango generado).
        Habitacion suite = habitaciones.findAllByOrderByNumeroAsc().stream()
                .filter(h -> h.getTipo().getNombre().equals("Suite")).findFirst().orElseThrow();
        Estadia futura = new Estadia(hoy.plusDays(50), hoy.plusDays(54));
        BigDecimal precio = politicaPrecio.cotizar(suite.getTipo(), futura, tarifario).total();
        Instant creada = reloj.instant().minus(Duration.ofDays(2));
        Reserva proxima = Reserva.online(generadorDeCodigo.nuevo(), ana, suite, futura, 2, precio, creada,
                config.getMinutosRetencion());
        proxima.registrarPago(config.senaPara(precio), MedioPago.ONLINE, ConceptoPago.SENA, EstadoPago.APROBADO,
                "PAY-" + (100000 + random.nextInt(900000)), ana, config.senaPara(precio), creada.plusSeconds(60));
        reservas.save(proxima);
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP).multiply(new BigDecimal("1000"));
    }
}
