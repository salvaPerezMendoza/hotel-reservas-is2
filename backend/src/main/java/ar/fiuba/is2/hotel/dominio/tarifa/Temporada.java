package ar.fiuba.is2.hotel.dominio.tarifa;

import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import jakarta.persistence.*;

import java.time.LocalDate;

/** Período del año con una categoría de precio (alta, media o baja). Fechas inclusivas. */
@Entity
@Table(name = "temporadas")
public class Temporada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaTemporada categoria;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    protected Temporada() {
    }

    public Temporada(String nombre, CategoriaTemporada categoria, LocalDate fechaDesde, LocalDate fechaHasta) {
        actualizar(nombre, categoria, fechaDesde, fechaHasta);
    }

    public void actualizar(String nombre, CategoriaTemporada categoria, LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde == null || fechaHasta == null || fechaHasta.isBefore(fechaDesde)) {
            throw new ReglaDeNegocioException("La temporada debe terminar después de empezar");
        }
        this.nombre = nombre;
        this.categoria = categoria;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
    }

    public boolean incluye(LocalDate fecha) {
        return !fecha.isBefore(fechaDesde) && !fecha.isAfter(fechaHasta);
    }

    public boolean seSuperponeCon(LocalDate desde, LocalDate hasta) {
        return !fechaDesde.isAfter(hasta) && !desde.isAfter(fechaHasta);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public CategoriaTemporada getCategoria() {
        return categoria;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }
}
