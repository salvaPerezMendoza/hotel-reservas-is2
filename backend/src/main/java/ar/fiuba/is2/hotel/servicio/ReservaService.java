package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.excepciones.NoEncontradoException;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.excepciones.SinDisponibilidadException;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.pago.ConceptoPago;
import ar.fiuba.is2.hotel.dominio.pago.EstadoPago;
import ar.fiuba.is2.hotel.dominio.pago.MedioPago;
import ar.fiuba.is2.hotel.dominio.pago.Pago;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.ResultadoCancelacion;
import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import ar.fiuba.is2.hotel.dominio.reserva.GeneradorDeCodigo;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.dominio.usuario.Huesped;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import ar.fiuba.is2.hotel.repositorio.ReservaRepository;
import ar.fiuba.is2.hotel.repositorio.TipoHabitacionRepository;
import ar.fiuba.is2.hotel.seguridad.UsuarioActual;
import ar.fiuba.is2.hotel.servicio.notificacion.EventoReserva;
import ar.fiuba.is2.hotel.servicio.pago.PasarelaDePago;
import ar.fiuba.is2.hotel.servicio.pago.ResultadoPasarela;
import ar.fiuba.is2.hotel.web.dto.ReservaDtos.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Casos de uso del huésped sobre sus reservas (CU-02 y CU-03) y la lógica de
 * reserva que comparte con recepción.
 */
@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final TipoHabitacionRepository tipos;
    private final DisponibilidadService disponibilidad;
    private final TarifarioService tarifario;
    private final GeneradorDeCodigo generadorDeCodigo;
    private final PasarelaDePago pasarela;
    private final UsuarioActual usuarioActual;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;
    private final Mapeador mapeador;
    private final Clock reloj;
    private final TransactionTemplate transaccion;

    public ReservaService(ReservaRepository reservas, TipoHabitacionRepository tipos,
                          DisponibilidadService disponibilidad, TarifarioService tarifario,
                          GeneradorDeCodigo generadorDeCodigo, PasarelaDePago pasarela, UsuarioActual usuarioActual,
                          AuditoriaService auditoria, ApplicationEventPublisher eventos, Mapeador mapeador,
                          Clock reloj, PlatformTransactionManager transactionManager) {
        this.reservas = reservas;
        this.tipos = tipos;
        this.disponibilidad = disponibilidad;
        this.tarifario = tarifario;
        this.generadorDeCodigo = generadorDeCodigo;
        this.pasarela = pasarela;
        this.usuarioActual = usuarioActual;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.mapeador = mapeador;
        this.reloj = reloj;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------------
    // Crear (RF-06, RF-07, RF-08, RF-09)
    // ------------------------------------------------------------------

    /** El huésped reserva online: la habitación queda retenida mientras paga. */
    @Transactional
    public ReservaDto crearOnline(CrearReservaRequest req) {
        Usuario usuario = usuarioActual.obtener();
        if (!(usuario instanceof Huesped huesped)) {
            throw new ReglaDeNegocioException("Las reservas online son para huéspedes. Desde recepción usá \"Nueva reserva\".");
        }
        ConfiguracionHotel config = tarifario.configuracion();
        Instant ahora = reloj.instant();
        Reserva reserva = reservarHabitacion(req.tipoId(), req.checkIn(), req.checkOut(), req.huespedes(),
                (habitacion, precio) -> Reserva.online(nuevoCodigo(), huesped, habitacion,
                        new Estadia(req.checkIn(), req.checkOut()), req.huespedes(), precio, ahora,
                        config.getMinutosRetencion()));
        auditoria.registrar("RESERVA_CREADA", "Reserva", reserva.getCodigo(),
                "Online, habitación " + reserva.getHabitacion().getNumero() + ", total " + reserva.getPrecioTotal());
        return mapeador.reserva(reserva, config, hoy());
    }

    @FunctionalInterface
    interface FabricaDeReserva {
        Reserva crear(Habitacion habitacion, BigDecimal precio);
    }

    /**
     * Pasos comunes para reservar: bloquear el tipo, liberar retenciones vencidas,
     * buscar una habitación libre, cotizar y guardar.
     */
    Reserva reservarHabitacion(Long tipoId, LocalDate checkIn, LocalDate checkOut, int huespedes,
                               FabricaDeReserva fabrica) {
        Estadia estadia = disponibilidad.validarEstadiaFutura(checkIn, checkOut);
        TipoHabitacion tipo = tipos.findByIdParaReservar(tipoId)
                .orElseThrow(() -> new NoEncontradoException("No existe ese tipo de habitación"));
        if (!tipo.isActivo()) {
            throw new ReglaDeNegocioException("Ese tipo de habitación no está disponible para reservar");
        }
        if (!tipo.admite(huespedes)) {
            throw new ReglaDeNegocioException(tipo.getNombre() + " admite hasta " + tipo.getCapacidad() + " huéspedes");
        }
        reservas.expirarRetencionesVencidas(reloj.instant());
        List<Habitacion> libres = disponibilidad.habitacionesLibres(tipo, estadia, null);
        if (libres.isEmpty()) {
            throw new SinDisponibilidadException("Ya no quedan habitaciones " + tipo.getNombre() + " libres para esas fechas");
        }
        BigDecimal precio = tarifario.cotizar(tipo, estadia).total();
        Reserva reserva = fabrica.crear(libres.getFirst(), precio);
        return reservas.saveAndFlush(reserva);
    }

    private String nuevoCodigo() {
        String codigo;
        do {
            codigo = generadorDeCodigo.nuevo();
        } while (reservas.existsByCodigo(codigo));
        return codigo;
    }

    // ------------------------------------------------------------------
    // Pago online (RF-14)
    // ------------------------------------------------------------------

    /**
     * Cobra por la pasarela (simulada). La llamada a la pasarela se hace fuera de
     * la transacción para no tener la base bloqueada mientras "procesa".
     */
    public ResultadoPagoDto pagarOnline(String codigo, PagoOnlineRequest req) {
        Usuario usuario = usuarioActual.obtener();
        BigDecimal monto = transaccion.execute(tx -> {
            Reserva r = buscarVisible(codigo, usuario);
            return montoAPagarOnline(r, req.concepto());
        });
        ResultadoPasarela resultado = pasarela.cobrar(monto, "Reserva " + codigo, req.simularRechazo());
        return transaccion.execute(tx -> {
            Reserva r = buscarVisible(codigo, usuario);
            ConfiguracionHotel config = tarifario.configuracion();
            EstadoReserva antes = r.getEstado();
            Pago pago = r.registrarPago(monto, MedioPago.ONLINE, req.concepto(),
                    resultado.aprobado() ? EstadoPago.APROBADO : EstadoPago.RECHAZADO,
                    resultado.referencia(), usuario, config.senaPara(r.getPrecioTotal()), reloj.instant());
            reservas.saveAndFlush(r);
            auditoria.registrar(resultado.aprobado() ? "PAGO_APROBADO" : "PAGO_RECHAZADO", "Reserva", r.getCodigo(),
                    req.concepto() + " " + monto + " ref. " + resultado.referencia());
            if (resultado.aprobado()) {
                if (antes == EstadoReserva.PENDIENTE_PAGO && r.getEstado() == EstadoReserva.CONFIRMADA) {
                    eventos.publishEvent(new EventoReserva(r.getId(), EventoReserva.Tipo.CONFIRMADA));
                }
                eventos.publishEvent(new EventoReserva(r.getId(), EventoReserva.Tipo.PAGO_REGISTRADO));
            }
            return new ResultadoPagoDto(resultado.aprobado(), resultado.mensaje(), mapeador.pago(pago),
                    mapeador.reserva(r, config, hoy()));
        });
    }

    private BigDecimal montoAPagarOnline(Reserva r, ConceptoPago concepto) {
        ConfiguracionHotel config = tarifario.configuracion();
        return switch (r.getEstado()) {
            case PENDIENTE_PAGO -> {
                if (!r.retencionVigente(reloj.instant())) {
                    throw new ReglaDeNegocioException("La retención de la habitación venció. Volvé a buscar disponibilidad.");
                }
                yield switch (concepto) {
                    case SENA -> {
                        BigDecimal sena = config.senaPara(r.getPrecioTotal());
                        if (sena.signum() == 0) {
                            throw new ReglaDeNegocioException("Este hotel no trabaja con seña: pagá el total");
                        }
                        yield sena;
                    }
                    case TOTAL -> r.saldoPendiente();
                    default -> throw new ReglaDeNegocioException("Para confirmar la reserva pagá la seña o el total");
                };
            }
            case CONFIRMADA, EN_CURSO -> {
                if (r.saldoPendiente().signum() <= 0) {
                    throw new ReglaDeNegocioException("La reserva no tiene saldo pendiente");
                }
                yield r.saldoPendiente();
            }
            default -> throw new ReglaDeNegocioException("Esta reserva no admite pagos");
        };
    }

    // ------------------------------------------------------------------
    // Consultas (RF-13)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReservaDto> misReservas() {
        Usuario usuario = usuarioActual.obtener();
        ConfiguracionHotel config = tarifario.configuracion();
        LocalDate hoy = hoy();
        return reservas.findByHuespedIdOrderByCheckInDesc(usuario.getId()).stream()
                .map(r -> mapeador.reserva(r, config, hoy))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservaDto detalle(String codigo) {
        Reserva r = buscarVisible(codigo, usuarioActual.obtener());
        return mapeador.reserva(r, tarifario.configuracion(), hoy());
    }

    // ------------------------------------------------------------------
    // Modificar (RF-11)
    // ------------------------------------------------------------------

    @Transactional
    public ReservaDto modificar(String codigo, ModificarReservaRequest req) {
        Usuario usuario = usuarioActual.obtener();
        Reserva r = buscarVisible(codigo, usuario);
        if (!r.getEstado().esModificable()) {
            throw new ReglaDeNegocioException("Solo se pueden modificar reservas pendientes o confirmadas");
        }
        Estadia nuevaEstadia = disponibilidad.validarEstadiaFutura(req.checkIn(), req.checkOut());
        TipoHabitacion tipo = tipos.findByIdParaReservar(req.tipoId())
                .orElseThrow(() -> new NoEncontradoException("No existe ese tipo de habitación"));
        if (!tipo.admite(req.huespedes())) {
            throw new ReglaDeNegocioException(tipo.getNombre() + " admite hasta " + tipo.getCapacidad() + " huéspedes");
        }
        reservas.expirarRetencionesVencidas(reloj.instant());
        Habitacion nuevaHabitacion = elegirHabitacionParaModificar(r, tipo, nuevaEstadia);
        BigDecimal nuevoPrecio = tarifario.cotizar(tipo, nuevaEstadia).total();
        String antes = r.getTipo().getNombre() + " " + r.getCheckIn() + "→" + r.getCheckOut() + " $" + r.getPrecioTotal();
        r.modificar(nuevaHabitacion, nuevaEstadia, req.huespedes(), nuevoPrecio, hoy());
        reservas.saveAndFlush(r);
        auditoria.registrar("RESERVA_MODIFICADA", "Reserva", r.getCodigo(),
                antes + "  ⇒  " + tipo.getNombre() + " " + r.getCheckIn() + "→" + r.getCheckOut() + " $" + nuevoPrecio);
        eventos.publishEvent(new EventoReserva(r.getId(), EventoReserva.Tipo.MODIFICADA));
        return mapeador.reserva(r, tarifario.configuracion(), hoy());
    }

    /** Si la habitación actual sigue libre para las nuevas fechas, se mantiene; si no, se busca otra. */
    private Habitacion elegirHabitacionParaModificar(Reserva r, TipoHabitacion tipo, Estadia nuevaEstadia) {
        Habitacion actual = r.getHabitacion();
        if (actual.getTipo().getId().equals(tipo.getId())
                && disponibilidad.habitacionLibre(actual, nuevaEstadia, r.getId())) {
            return actual;
        }
        List<Habitacion> libres = disponibilidad.habitacionesLibres(tipo, nuevaEstadia, r.getId());
        if (libres.isEmpty()) {
            throw new SinDisponibilidadException("No hay habitaciones " + tipo.getNombre() + " libres para las nuevas fechas");
        }
        return libres.getFirst();
    }

    // ------------------------------------------------------------------
    // Cancelar (RF-12, RF-17)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SimulacionCancelacionDto simularCancelacion(String codigo) {
        Reserva r = buscarVisible(codigo, usuarioActual.obtener());
        ResultadoCancelacion resultado = r.simularCancelacion(
                tarifario.configuracion().politicaDeCancelacionVigente(), hoy());
        return new SimulacionCancelacionDto(resultado.porcentajeReintegro(), resultado.montoReintegro(),
                r.totalPagado(), resultado.explicacion());
    }

    @Transactional
    public ReservaDto cancelar(String codigo) {
        Usuario usuario = usuarioActual.obtener();
        Reserva r = buscarVisible(codigo, usuario);
        ResultadoCancelacion resultado = r.cancelar(tarifario.configuracion().politicaDeCancelacionVigente(),
                hoy(), reloj.instant(), usuario);
        reservas.saveAndFlush(r);
        auditoria.registrar("RESERVA_CANCELADA", "Reserva", r.getCodigo(),
                resultado.explicacion() + " Reintegro: $" + resultado.montoReintegro());
        eventos.publishEvent(new EventoReserva(r.getId(), EventoReserva.Tipo.CANCELADA));
        return mapeador.reserva(r, tarifario.configuracion(), hoy());
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /** Un huésped solo ve sus reservas; recepción y administración ven todas. */
    Reserva buscarVisible(String codigo, Usuario usuario) {
        Reserva r = reservas.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new NoEncontradoException("No encontramos la reserva " + codigo));
        if (!usuario.puedeOperarRecepcion() && !r.perteneceA(usuario)) {
            throw new NoEncontradoException("No encontramos la reserva " + codigo);
        }
        return r;
    }

    LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
