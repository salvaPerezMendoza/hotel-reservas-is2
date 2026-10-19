package ar.fiuba.is2.hotel.dominio.habitacion;

import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/** Bloqueo de una habitación por mantenimiento durante un rango [desde, hasta) (RF-25). */
@Entity
@Table(name = "bloqueos")
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "habitacion_id")
    private Habitacion habitacion;

    @Column(nullable = false)
    private LocalDate desde;

    @Column(nullable = false)
    private LocalDate hasta;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    protected Bloqueo() {
    }

    public Bloqueo(Habitacion habitacion, Estadia rango, String motivo) {
        this.habitacion = habitacion;
        this.desde = rango.checkIn();
        this.hasta = rango.checkOut();
        this.motivo = motivo;
    }

    public Estadia getRango() {
        return new Estadia(desde, hasta);
    }

    public Long getId() {
        return id;
    }

    public Habitacion getHabitacion() {
        return habitacion;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public String getMotivo() {
        return motivo;
    }
}
