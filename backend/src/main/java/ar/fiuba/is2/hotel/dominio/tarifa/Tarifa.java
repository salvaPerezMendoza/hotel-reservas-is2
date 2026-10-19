package ar.fiuba.is2.hotel.dominio.tarifa;

import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import jakarta.persistence.*;

import java.math.BigDecimal;

/** Precio por noche de un tipo de habitación en una categoría de temporada (RF-23). */
@Entity
@Table(name = "tarifas")
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_id")
    private TipoHabitacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaTemporada categoria;

    @Column(name = "precio_noche", nullable = false)
    private BigDecimal precioNoche;

    protected Tarifa() {
    }

    public Tarifa(TipoHabitacion tipo, CategoriaTemporada categoria, BigDecimal precioNoche) {
        this.tipo = tipo;
        this.categoria = categoria;
        this.precioNoche = precioNoche;
    }

    public void cambiarPrecio(BigDecimal nuevoPrecio) {
        this.precioNoche = nuevoPrecio;
    }

    public Long getId() {
        return id;
    }

    public TipoHabitacion getTipo() {
        return tipo;
    }

    public CategoriaTemporada getCategoria() {
        return categoria;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }
}
