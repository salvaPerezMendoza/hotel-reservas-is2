package ar.fiuba.is2.hotel.dominio.pago;

import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Movimiento de dinero asociado a una reserva: cobro o reintegro (RF-14 a RF-17). */
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedioPago medio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConceptoPago concepto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    /** Número de operación (lo devuelve la pasarela o lo genera recepción). */
    @Column(nullable = false)
    private String referencia;

    @Column(nullable = false)
    private Instant fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;

    protected Pago() {
    }

    public Pago(Reserva reserva, BigDecimal monto, MedioPago medio, ConceptoPago concepto,
                EstadoPago estado, String referencia, Instant fecha, Usuario registradoPor) {
        this.reserva = reserva;
        this.monto = monto;
        this.medio = medio;
        this.concepto = concepto;
        this.estado = estado;
        this.referencia = referencia;
        this.fecha = fecha;
        this.registradoPor = registradoPor;
    }

    public boolean estaAprobado() {
        return estado == EstadoPago.APROBADO;
    }

    public boolean esReintegro() {
        return concepto == ConceptoPago.REINTEGRO;
    }

    /** Aporte neto a lo pagado por el huésped: positivo si cobra, negativo si devuelve. */
    public BigDecimal aporteNeto() {
        if (!estaAprobado()) {
            return BigDecimal.ZERO;
        }
        return esReintegro() ? monto.negate() : monto;
    }

    public Long getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public MedioPago getMedio() {
        return medio;
    }

    public ConceptoPago getConcepto() {
        return concepto;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public String getReferencia() {
        return referencia;
    }

    public Instant getFecha() {
        return fecha;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }
}
