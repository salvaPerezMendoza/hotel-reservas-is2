package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.excepciones.NoEncontradoException;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.excepciones.SinDisponibilidadException;
import ar.fiuba.is2.hotel.dominio.habitacion.EstadoHabitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.pago.EstadoPago;
import ar.fiuba.is2.hotel.dominio.pago.MedioPago;
import ar.fiuba.is2.hotel.dominio.pago.Pago;
import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import ar.fiuba.is2.hotel.dominio.reserva.GeneradorDeCodigo;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.dominio.usuario.Huesped;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import ar.fiuba.is2.hotel.repositorio.HabitacionRepository;
import ar.fiuba.is2.hotel.repositorio.HuespedRepository;
import ar.fiuba.is2.hotel.repositorio.ReservaRepository;
import ar.fiuba.is2.hotel.repositorio.UsuarioRepository;
import ar.fiuba.is2.hotel.seguridad.UsuarioActual;
import ar.fiuba.is2.hotel.servicio.notificacion.EventoReserva;
import ar.fiuba.is2.hotel.web.dto.AdminDtos.HabitacionDto;
import ar.fiuba.is2.hotel.web.dto.ReporteDtos.TableroDto;
import ar.fiuba.is2.hotel.web.dto.ReservaDtos.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Operación diaria del hotel desde el mostrador (CU-02 por recepción, CU-03 presencial y CU-04). */
@Service
public class RecepcionService {

    private final ReservaRepository reservas;
    private final HabitacionRepository habitaciones;
    private final HuespedRepository huespedes;
    private final UsuarioRepository usuarios;
    private final ReservaService reservaService;
    private final DisponibilidadService disponibilidad;
    private final TarifarioService tarifario;
    private final GeneradorDeCodigo generadorDeCodigo;
    private final UsuarioActual usuarioActual;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;
    private final Mapeador mapeador;
    private final Clock reloj;
    private final SecureRandom random = new SecureRandom();

    public RecepcionService(ReservaRepository reservas, HabitacionRepository habitaciones,
                            HuespedRepository huespedes, UsuarioRepository usuarios, ReservaService reservaService,
                            DisponibilidadService disponibilidad, TarifarioService tarifario,
                            GeneradorDeCodigo generadorDeCodigo, UsuarioActual usuarioActual,
                            AuditoriaService auditoria, ApplicationEventPublisher eventos, Mapeador mapeador,
                            Clock reloj) {
        this.reservas = reservas;
        this.habitaciones = habitaciones;
        this.huespedes = huespedes;
        this.usuarios = usuarios;
        this.reservaService = reservaService;
        this.disponibilidad = disponibilidad;
        this.tarifario = tarifario;
        this.generadorDeCodigo = generadorDeCodigo;
        this.usuarioActual = usuarioActual;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.mapeador = mapeador;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------
    // Listados y tablero
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReservaDto> listar(EstadoReserva estado, LocalDate desde, LocalDate hasta, String texto) {
        ConfiguracionHotel config = tarifario.configuracion();
        LocalDate hoy = hoy();
        List<Reserva> resultado = (texto != null && !texto.isBlank())
                ? reservas.buscarTexto(texto.trim())
                : reservas.filtrar(estado, desde, hasta);
        return resultado.stream()
                .filter(r -> estado == null || r.getEstado() == estado)
                .map(r -> mapeador.reserva(r, config, hoy))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaDto> llegadasDeHoy() {
        ConfiguracionHotel config = tarifario.configuracion();
        return reservas.findByCheckInAndEstado(hoy(), EstadoReserva.CONFIRMADA).stream()
                .map(r -> mapeador.reserva(r, config, hoy())).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaDto> salidasDeHoy() {
        ConfiguracionHotel config = tarifario.configuracion();
        return reservas.findByEstado(EstadoReserva.EN_CURSO).stream()
                .filter(r -> !r.getCheckOut().isAfter(hoy()))
                .map(r -> mapeador.reserva(r, config, hoy())).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaDto> enCurso() {
        ConfiguracionHotel config = tarifario.configuracion();
        return reservas.findByEstado(EstadoReserva.EN_CURSO).stream()
                .map(r -> mapeador.reserva(r, config, hoy())).toList();
    }

    @Transactional(readOnly = true)
    public TableroDto tablero() {
        LocalDate hoy = hoy();
        List<Habitacion> todas = habitaciones.findByActivaTrueOrderByNumeroAsc();
        long enCurso = reservas.findByEstado(EstadoReserva.EN_CURSO).size();
        long llegadas = reservas.findByCheckInAndEstado(hoy, EstadoReserva.CONFIRMADA).size();
        long salidas = reservas.findByEstado(EstadoReserva.EN_CURSO).stream()
                .filter(r -> !r.getCheckOut().isAfter(hoy)).count();
        long pendientes = reservas.findByEstado(EstadoReserva.PENDIENTE_PAGO).stream()
                .filter(r -> r.retencionVigente(reloj.instant())).count();
        long disponibles = todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.DISPONIBLE).count();
        long limpieza = todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.LIMPIEZA).count();
        long mantenimiento = todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.MANTENIMIENTO).count();
        BigDecimal ocupacion = todas.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(enCurso * 100).divide(BigDecimal.valueOf(todas.size()), 1, RoundingMode.HALF_UP);
        return new TableroDto(llegadas, salidas, enCurso, disponibles, todas.size(), limpieza, mantenimiento,
                pendientes, ocupacion);
    }

    @Transactional(readOnly = true)
    public List<HabitacionDto> estadoDeHabitaciones() {
        return habitaciones.findByActivaTrueOrderByNumeroAsc().stream().map(mapeador::habitacion).toList();
    }

    @Transactional(readOnly = true)
    public List<HuespedResumenDto> buscarHuespedes(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return huespedes.buscar(texto.trim()).stream().limit(20).map(mapeador::huesped).toList();
    }

    // ------------------------------------------------------------------
    // Reservas presenciales o telefónicas (RF-10)
    // ------------------------------------------------------------------

    @Transactional
    public ReservaDto crearReserva(ReservaRecepcionRequest req) {
        Usuario recepcionista = usuarioActual.obtener();
        Huesped huesped = buscarOCrearHuesped(req.huesped());
        Reserva reserva = reservaService.reservarHabitacion(req.tipoId(), req.checkIn(), req.checkOut(), req.huespedes(),
                (habitacion, precio) -> Reserva.porRecepcion(nuevoCodigo(), huesped, habitacion,
                        new Estadia(req.checkIn(), req.checkOut()), req.huespedes(), precio, recepcionista,
                        req.observaciones(), reloj.instant()));
        auditoria.registrar("RESERVA_CREADA", "Reserva", reserva.getCodigo(),
                "Por recepción para " + huesped.getEmail() + ", habitación " + reserva.getHabitacion().getNumero());
        eventos.publishEvent(new EventoReserva(reserva.getId(), EventoReserva.Tipo.CONFIRMADA));
        return mapeador.reserva(reserva, tarifario.configuracion(), hoy());
    }

    private Huesped buscarOCrearHuesped(DatosHuespedRequest datos) {
        String email = Usuario.normalizarEmail(datos.email());
        return huespedes.findByEmailIgnoreCase(email).orElseGet(() -> {
            if (usuarios.existsByEmailIgnoreCase(email)) {
                throw new ReglaDeNegocioException("Ese email pertenece a un usuario del personal del hotel");
            }
            Huesped nuevo = new Huesped(datos.nombre().trim(), datos.apellido().trim(), email, datos.telefono(),
                    datos.documento());
            auditoria.registrar("HUESPED_CREADO", "Usuario", email, "Alta desde recepción");
            return huespedes.save(nuevo);
        });
    }

    private String nuevoCodigo() {
        String codigo;
        do {
            codigo = generadorDeCodigo.nuevo();
        } while (reservas.existsByCodigo(codigo));
        return codigo;
    }

    // ------------------------------------------------------------------
    // Pagos presenciales (RF-15)
    // ------------------------------------------------------------------

    @Transactional
    public ReservaDto registrarPago(String codigo, PagoPresencialRequest req) {
        if (req.medio() == MedioPago.ONLINE) {
            throw new ReglaDeNegocioException("En recepción se registran pagos en efectivo, tarjeta o transferencia");
        }
        Usuario quien = usuarioActual.obtener();
        Reserva r = buscar(codigo);
        String referencia = "REC-" + (100000 + random.nextInt(900000));
        EstadoReserva antes = r.getEstado();
        Pago pago = r.registrarPago(req.monto(), req.medio(), req.concepto(), EstadoPago.APROBADO, referencia, quien,
                tarifario.configuracion().senaPara(r.getPrecioTotal()), reloj.instant());
        reservas.saveAndFlush(r);
        auditoria.registrar("PAGO_PRESENCIAL", "Reserva", r.getCodigo(),
                req.concepto() + " " + req.medio() + " $" + req.monto() + " ref. " + pago.getReferencia());
        if (antes == EstadoReserva.PENDIENTE_PAGO && r.getEstado() == EstadoReserva.CONFIRMADA) {
            eventos.publishEvent(new EventoReserva(r.getId(), EventoReserva.Tipo.CONFIRMADA));
        }
        eventos.publishEvent(new EventoReserva(r.getId(), EventoReserva.Tipo.PAGO_REGISTRADO));
        return mapeador.reserva(r, tarifario.configuracion(), hoy());
    }

    // ------------------------------------------------------------------
    // Check-in y check-out (RF-18, RF-19, RF-20)
    // ------------------------------------------------------------------

    /** Habitaciones del tipo reservado que están listas y libres para el resto de la estadía. */
    @Transactional(readOnly = true)
    public List<HabitacionDto> habitacionesParaCheckIn(String codigo) {
        Reserva r = buscar(codigo);
        if (!hoy().isBefore(r.getCheckOut())) {
            return List.of();
        }
        Estadia resto = new Estadia(maximo(hoy(), r.getCheckIn()), r.getCheckOut());
        return habitaciones.findByTipoIdAndActivaTrueOrderByNumeroAsc(r.getTipo().getId()).stream()
                .filter(Habitacion::estaListaParaOcupar)
                .filter(h -> disponibilidad.habitacionLibre(h, resto, r.getId()))
                .map(mapeador::habitacion)
                .toList();
    }

    @Transactional
    public ReservaDto hacerCheckIn(String codigo, CheckInRequest req) {
        Reserva r = buscar(codigo);
        Habitacion asignada = r.getHabitacion();
        if (req != null && req.habitacionId() != null && !req.habitacionId().equals(asignada.getId())) {
            asignada = habitaciones.findById(req.habitacionId())
                    .orElseThrow(() -> new NoEncontradoException("No existe esa habitación"));
            if (!hoy().isBefore(r.getCheckOut())) {
                throw new ReglaDeNegocioException("La estadía ya terminó: no se puede hacer check-in");
            }
            Estadia resto = new Estadia(maximo(hoy(), r.getCheckIn()), r.getCheckOut());
            if (!disponibilidad.habitacionLibre(asignada, resto, r.getId())) {
                throw new SinDisponibilidadException("La habitación " + asignada.getNumero() + " no está libre para esta estadía");
            }
        }
        if (!asignada.estaListaParaOcupar()) {
            throw new ReglaDeNegocioException("La habitación " + asignada.getNumero()
                    + " no está lista (" + asignada.getEstado().name().toLowerCase() + "). Asigná otra habitación del mismo tipo.");
        }
        r.hacerCheckIn(asignada, hoy(), reloj.instant());
        reservas.saveAndFlush(r);
        auditoria.registrar("CHECK_IN", "Reserva", r.getCodigo(), "Habitación " + asignada.getNumero());
        return mapeador.reserva(r, tarifario.configuracion(), hoy());
    }

    @Transactional
    public ReservaDto hacerCheckOut(String codigo) {
        Reserva r = buscar(codigo);
        r.hacerCheckOut(reloj.instant());
        reservas.saveAndFlush(r);
        auditoria.registrar("CHECK_OUT", "Reserva", r.getCodigo(), "Habitación " + r.getHabitacion().getNumero());
        eventos.publishEvent(new EventoReserva(r.getId(), EventoReserva.Tipo.FINALIZADA));
        return mapeador.reserva(r, tarifario.configuracion(), hoy());
    }

    @Transactional
    public ReservaDto agregarConsumo(String codigo, ConsumoRequest req) {
        Reserva r = buscar(codigo);
        r.agregarConsumo(req.descripcion().trim(), req.monto(), usuarioActual.obtener(), reloj.instant());
        reservas.saveAndFlush(r);
        auditoria.registrar("CONSUMO", "Reserva", r.getCodigo(), req.descripcion() + " $" + req.monto());
        return mapeador.reserva(r, tarifario.configuracion(), hoy());
    }

    @Transactional
    public HabitacionDto cambiarEstadoHabitacion(Long habitacionId, EstadoHabitacion estado) {
        Habitacion h = habitaciones.findById(habitacionId)
                .orElseThrow(() -> new NoEncontradoException("No existe esa habitación"));
        EstadoHabitacion anterior = h.getEstado();
        h.cambiarEstado(estado);
        auditoria.registrar("ESTADO_HABITACION", "Habitacion", h.getNumero(), anterior + " → " + estado);
        return mapeador.habitacion(h);
    }

    private Reserva buscar(String codigo) {
        return reservas.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new NoEncontradoException("No encontramos la reserva " + codigo));
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private static LocalDate maximo(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
