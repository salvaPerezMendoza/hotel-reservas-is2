package ar.fiuba.is2.hotel.dominio.pago;

import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Cargo extra durante la estadía: frigobar, restaurante, lavandería (RF-21). */
@Entity
@Table(name = "consumos")
public class Consumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private Instant fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;

    protected Consumo() {
    }

    public Consumo(Reserva reserva, String descripcion, BigDecimal monto, Instant fecha, Usuario registradoPor) {
        this.reserva = reserva;
        this.descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.registradoPor = registradoPor;
    }

    public Long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public Instant getFecha() {
        return fecha;
    }
}
