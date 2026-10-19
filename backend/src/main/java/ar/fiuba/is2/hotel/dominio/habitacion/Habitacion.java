package ar.fiuba.is2.hotel.dominio.habitacion;

import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import jakarta.persistence.*;

/** Habitación física del hotel. Su estado solo cambia a través de sus métodos. */
@Entity
@Table(name = "habitaciones")
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(nullable = false)
    private int piso;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_id")
    private TipoHabitacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoHabitacion estado = EstadoHabitacion.DISPONIBLE;

    @Column(nullable = false)
    private boolean activa = true;

    protected Habitacion() {
    }

    public Habitacion(String numero, int piso, TipoHabitacion tipo) {
        this.numero = numero;
        this.piso = piso;
        this.tipo = tipo;
    }

    public void actualizar(String numero, int piso, TipoHabitacion tipo) {
        this.numero = numero;
        this.piso = piso;
        this.tipo = tipo;
    }

    /** ¿Se le puede entregar la llave a un huésped ahora mismo? */
    public boolean estaListaParaOcupar() {
        return activa && estado == EstadoHabitacion.DISPONIBLE;
    }

    public void ocupar() {
        if (!estaListaParaOcupar()) {
            throw new ReglaDeNegocioException("La habitación " + numero + " no está lista (estado: " + estado + ")");
        }
        this.estado = EstadoHabitacion.OCUPADA;
    }

    /** Al hacer check-out la habitación queda pendiente de limpieza. */
    public void liberarParaLimpieza() {
        this.estado = EstadoHabitacion.LIMPIEZA;
    }

    /** Cambios manuales de estado que hace recepción (limpieza terminada, etc.). */
    public void cambiarEstado(EstadoHabitacion nuevo) {
        if (estado == EstadoHabitacion.OCUPADA && nuevo != EstadoHabitacion.OCUPADA) {
            throw new ReglaDeNegocioException("La habitación " + numero + " está ocupada: se libera haciendo el check-out");
        }
        if (nuevo == EstadoHabitacion.OCUPADA) {
            throw new ReglaDeNegocioException("Una habitación se ocupa solamente haciendo el check-in");
        }
        this.estado = nuevo;
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public int getPiso() {
        return piso;
    }

    public TipoHabitacion getTipo() {
        return tipo;
    }

    public EstadoHabitacion getEstado() {
        return estado;
    }

    public boolean isActiva() {
        return activa;
    }
}
