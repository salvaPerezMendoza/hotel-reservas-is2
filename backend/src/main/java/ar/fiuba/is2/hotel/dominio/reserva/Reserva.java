package ar.fiuba.is2.hotel.dominio.reserva;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.pago.ConceptoPago;
import ar.fiuba.is2.hotel.dominio.pago.Consumo;
import ar.fiuba.is2.hotel.dominio.pago.EstadoPago;
import ar.fiuba.is2.hotel.dominio.pago.MedioPago;
import ar.fiuba.is2.hotel.dominio.pago.Pago;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.PoliticaDeCancelacion;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.ResultadoCancelacion;
import ar.fiuba.is2.hotel.dominio.usuario.Huesped;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Núcleo del dominio: vincula huésped, habitación y estadía, y controla su ciclo de vida.
 *
 * <p><b>Encapsulamiento:</b> el estado no tiene setter. Solo cambia a través de
 * {@link #registrarPago}, {@link #cancelar}, {@link #hacerCheckIn}, {@link #hacerCheckOut}
 * o {@link #expirar}, que validan que la transición sea válida.</p>
 */
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String codigo;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "huesped_id")
    private Huesped huesped;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "habitacion_id")
    private Habitacion habitacion;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_id")
    private TipoHabitacion tipo;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "cantidad_huespedes", nullable = false)
    private int cantidadHuespedes;

    @Column(name = "precio_total", nullable = false)
    private BigDecimal precioTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CanalReserva canal;

    @Column(name = "retencion_hasta")
    private Instant retencionHasta;

    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por_id", updatable = false)
    private Usuario creadaPor;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @Column(name = "cancelada_en")
    private Instant canceladaEn;

    private BigDecimal reembolso;

    @Column(name = "check_in_real")
    private Instant checkInReal;

    @Column(name = "check_out_real")
    private Instant checkOutReal;

    @Column(name = "recordatorio_enviado", nullable = false)
    private boolean recordatorioEnviado;

    @Version
    private long version;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<Pago> pagos = new ArrayList<>();

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<Consumo> consumos = new ArrayList<>();

    protected Reserva() {
    }

    private Reserva(String codigo, Huesped huesped, Habitacion habitacion, Estadia estadia, int cantidadHuespedes,
                    BigDecimal precioTotal, CanalReserva canal, Usuario creadaPor, Instant ahora) {
        if (!habitacion.getTipo().admite(cantidadHuespedes)) {
            throw new ReglaDeNegocioException("La habitación " + habitacion.getTipo().getNombre()
                    + " admite hasta " + habitacion.getTipo().getCapacidad() + " huéspedes");
        }
        this.codigo = codigo;
        this.huesped = huesped;
        this.habitacion = habitacion;
        this.tipo = habitacion.getTipo();
        this.checkIn = estadia.checkIn();
        this.checkOut = estadia.checkOut();
        this.cantidadHuespedes = cantidadHuespedes;
        this.precioTotal = precioTotal;
        this.canal = canal;
        this.creadaPor = creadaPor;
        this.creadaEn = ahora;
    }

    /**
     * Reserva hecha por el huésped en la web: queda pendiente de pago y la habitación
     * se retiene durante unos minutos (RF-07).
     */
    public static Reserva online(String codigo, Huesped huesped, Habitacion habitacion, Estadia estadia,
                                 int cantidadHuespedes, BigDecimal precioTotal, Instant ahora, int minutosRetencion) {
        Reserva reserva = new Reserva(codigo, huesped, habitacion, estadia, cantidadHuespedes, precioTotal,
                CanalReserva.ONLINE, huesped, ahora);
        reserva.estado = EstadoReserva.PENDIENTE_PAGO;
        reserva.retencionHasta = ahora.plus(minutosRetencion, ChronoUnit.MINUTES);
        return reserva;
    }

    /** Reserva cargada por recepción (teléfono o mostrador): queda confirmada (RF-10). */
    public static Reserva porRecepcion(String codigo, Huesped huesped, Habitacion habitacion, Estadia estadia,
                                       int cantidadHuespedes, BigDecimal precioTotal, Usuario recepcionista,
                                       String observaciones, Instant ahora) {
        Reserva reserva = new Reserva(codigo, huesped, habitacion, estadia, cantidadHuespedes, precioTotal,
                CanalReserva.RECEPCION, recepcionista, ahora);
        reserva.estado = EstadoReserva.CONFIRMADA;
        reserva.observaciones = observaciones;
        return reserva;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Estadia getEstadia() {
        return new Estadia(checkIn, checkOut);
    }

    /**
     * ¿Esta reserva ocupa su habitación en este momento? Una reserva pendiente
     * solo bloquea mientras su retención no haya vencido.
     */
    public boolean bloqueaHabitacion(Instant ahora) {
        if (estado == EstadoReserva.PENDIENTE_PAGO) {
            return retencionVigente(ahora);
        }
        return estado.bloqueaHabitacion();
    }

    public boolean retencionVigente(Instant ahora) {
        return estado == EstadoReserva.PENDIENTE_PAGO && retencionHasta != null && ahora.isBefore(retencionHasta);
    }

    /** Lo que el huésped pagó, neto de reintegros. */
    public BigDecimal totalPagado() {
        return pagos.stream().map(Pago::aporteNeto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalConsumos() {
        return consumos.stream().map(Consumo::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Estadía más consumos extra. */
    public BigDecimal totalACobrar() {
        return precioTotal.add(totalConsumos());
    }

    /** Lo que falta pagar. Si es negativo, el huésped tiene saldo a favor. */
    public BigDecimal saldoPendiente() {
        if (estado == EstadoReserva.CANCELADA || estado == EstadoReserva.EXPIRADA) {
            return BigDecimal.ZERO;
        }
        return totalACobrar().subtract(totalPagado());
    }

    public boolean perteneceA(Usuario usuario) {
        return huesped.getId().equals(usuario.getId());
    }

    // ------------------------------------------------------------------
    // Transiciones de estado
    // ------------------------------------------------------------------

    /**
     * Registra un pago. Si la reserva estaba pendiente y el pago aprobado cubre la
     * seña, queda confirmada.
     */
    public Pago registrarPago(BigDecimal monto, MedioPago medio, ConceptoPago concepto, EstadoPago estadoPago,
                              String referencia, Usuario registradoPor, BigDecimal senaMinima, Instant ahora) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto del pago debe ser mayor a cero");
        }
        if (concepto == ConceptoPago.REINTEGRO) {
            throw new ReglaDeNegocioException("Los reintegros se registran al cancelar la reserva");
        }
        switch (estado) {
            case PENDIENTE_PAGO -> {
                if (!retencionVigente(ahora)) {
                    throw new ReglaDeNegocioException("La retención de la habitación venció. Volvé a buscar disponibilidad.");
                }
            }
            case CONFIRMADA, EN_CURSO -> {
                if (monto.compareTo(saldoPendiente()) > 0) {
                    throw new ReglaDeNegocioException("El monto supera el saldo pendiente de la reserva");
                }
            }
            default -> throw new ReglaDeNegocioException("No se pueden registrar pagos en una reserva " + estado.name().toLowerCase());
        }
        Pago pago = new Pago(this, monto, medio, concepto, estadoPago, referencia, ahora, registradoPor);
        pagos.add(pago);
        if (pago.estaAprobado() && estado == EstadoReserva.PENDIENTE_PAGO && totalPagado().compareTo(senaMinima) >= 0) {
            confirmar();
        }
        return pago;
    }

    private void confirmar() {
        this.estado = EstadoReserva.CONFIRMADA;
        this.retencionHasta = null;
    }

    /** La retención venció sin pago: la habitación vuelve a estar libre. */
    public void expirar(Instant ahora) {
        if (estado == EstadoReserva.PENDIENTE_PAGO && !retencionVigente(ahora)) {
            this.estado = EstadoReserva.EXPIRADA;
        }
    }

    /**
     * Cambia fechas, cantidad de huéspedes o habitación (RF-11). El servicio ya
     * verificó la disponibilidad de la nueva habitación y recalculó el precio.
     */
    public void modificar(Habitacion nuevaHabitacion, Estadia nuevaEstadia, int nuevaCantidad,
                          BigDecimal nuevoPrecio, LocalDate hoy) {
        if (!estado.esModificable()) {
            throw new ReglaDeNegocioException("Solo se pueden modificar reservas pendientes o confirmadas");
        }
        if (!hoy.isBefore(checkIn)) {
            throw new ReglaDeNegocioException("La reserva ya no se puede modificar: la fecha de ingreso es hoy o ya pasó");
        }
        if (nuevaEstadia.checkIn().isBefore(hoy)) {
            throw new ReglaDeNegocioException("La nueva fecha de ingreso no puede ser anterior a hoy");
        }
        if (!nuevaHabitacion.getTipo().admite(nuevaCantidad)) {
            throw new ReglaDeNegocioException("La habitación elegida admite hasta "
                    + nuevaHabitacion.getTipo().getCapacidad() + " huéspedes");
        }
        this.habitacion = nuevaHabitacion;
        this.tipo = nuevaHabitacion.getTipo();
        this.checkIn = nuevaEstadia.checkIn();
        this.checkOut = nuevaEstadia.checkOut();
        this.cantidadHuespedes = nuevaCantidad;
        this.precioTotal = nuevoPrecio;
        this.recordatorioEnviado = false;
    }

    /** Lo que se reintegraría si se cancela hoy (para mostrarlo antes de confirmar). */
    public ResultadoCancelacion simularCancelacion(PoliticaDeCancelacion politica, LocalDate hoy) {
        validarCancelable();
        BigDecimal pagado = totalPagado().max(BigDecimal.ZERO);
        long diasDeAnticipacion = ChronoUnit.DAYS.between(hoy, checkIn);
        return politica.evaluar(pagado, diasDeAnticipacion);
    }

    /** Cancela la reserva aplicando la política vigente y registra el reintegro (RF-12 / RF-17). */
    public ResultadoCancelacion cancelar(PoliticaDeCancelacion politica, LocalDate hoy, Instant ahora, Usuario quien) {
        ResultadoCancelacion resultado = simularCancelacion(politica, hoy);
        if (resultado.montoReintegro().signum() > 0) {
            MedioPago medio = pagos.stream().filter(Pago::estaAprobado).reduce((a, b) -> b)
                    .map(Pago::getMedio).orElse(MedioPago.ONLINE);
            pagos.add(new Pago(this, resultado.montoReintegro(), medio, ConceptoPago.REINTEGRO,
                    EstadoPago.APROBADO, "REI-" + codigo, ahora, quien));
        }
        this.estado = EstadoReserva.CANCELADA;
        this.canceladaEn = ahora;
        this.reembolso = resultado.montoReintegro();
        this.retencionHasta = null;
        return resultado;
    }

    private void validarCancelable() {
        if (!estado.esModificable()) {
            throw new ReglaDeNegocioException("Solo se pueden cancelar reservas pendientes o confirmadas");
        }
    }

    /**
     * Check-in (RF-18): solo reservas confirmadas, dentro de las fechas de la estadía.
     * Recepción puede asignar otra habitación del mismo tipo si la reservada no está lista.
     */
    public void hacerCheckIn(Habitacion habitacionAsignada, LocalDate hoy, Instant ahora) {
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDeNegocioException("Solo se puede hacer check-in de reservas confirmadas (estado actual: "
                    + estado.name().toLowerCase().replace('_', ' ') + ")");
        }
        if (hoy.isBefore(checkIn)) {
            throw new ReglaDeNegocioException("Todavía no es la fecha de ingreso (" + checkIn + ")");
        }
        if (!hoy.isBefore(checkOut)) {
            throw new ReglaDeNegocioException("La estadía ya terminó: no se puede hacer check-in");
        }
        if (!habitacionAsignada.getTipo().getId().equals(tipo.getId())) {
            throw new ReglaDeNegocioException("La habitación asignada tiene que ser del tipo reservado (" + tipo.getNombre() + ")");
        }
        habitacionAsignada.ocupar();
        this.habitacion = habitacionAsignada;
        this.estado = EstadoReserva.EN_CURSO;
        this.checkInReal = ahora;
    }

    /** Check-out (RF-19): exige el saldo pagado; la habitación queda para limpieza. */
    public void hacerCheckOut(Instant ahora) {
        if (estado != EstadoReserva.EN_CURSO) {
            throw new ReglaDeNegocioException("Solo se puede hacer check-out de una estadía en curso");
        }
        if (saldoPendiente().signum() > 0) {
            throw new ReglaDeNegocioException("Hay un saldo pendiente de $" + saldoPendiente().toPlainString()
                    + ". Cobralo antes de hacer el check-out.");
        }
        habitacion.liberarParaLimpieza();
        this.estado = EstadoReserva.FINALIZADA;
        this.checkOutReal = ahora;
    }

    /** Consumo extra durante la estadía (RF-21). */
    public Consumo agregarConsumo(String descripcion, BigDecimal monto, Usuario quien, Instant ahora) {
        if (estado != EstadoReserva.EN_CURSO) {
            throw new ReglaDeNegocioException("Solo se pueden cargar consumos durante la estadía");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto del consumo debe ser mayor a cero");
        }
        Consumo consumo = new Consumo(this, descripcion, monto, ahora, quien);
        consumos.add(consumo);
        return consumo;
    }

    public void marcarRecordatorioEnviado() {
        this.recordatorioEnviado = true;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Huesped getHuesped() {
        return huesped;
    }

    public Habitacion getHabitacion() {
        return habitacion;
    }

    public TipoHabitacion getTipo() {
        return tipo;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getCantidadHuespedes() {
        return cantidadHuespedes;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public CanalReserva getCanal() {
        return canal;
    }

    public Instant getRetencionHasta() {
        return retencionHasta;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getCanceladaEn() {
        return canceladaEn;
    }

    public BigDecimal getReembolso() {
        return reembolso;
    }

    public Instant getCheckInReal() {
        return checkInReal;
    }

    public Instant getCheckOutReal() {
        return checkOutReal;
    }

    public boolean isRecordatorioEnviado() {
        return recordatorioEnviado;
    }

    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }

    public List<Consumo> getConsumos() {
        return Collections.unmodifiableList(consumos);
    }
}
