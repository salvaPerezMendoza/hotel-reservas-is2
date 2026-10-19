package ar.fiuba.is2.hotel.dominio.habitacion;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Tipo de habitación que se ofrece al huésped (Standard, Suite, etc.). */
@Entity
@Table(name = "tipos_habitacion")
public class TipoHabitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String descripcion = "";

    @Column(nullable = false)
    private int capacidad;

    /** Precio por noche cuando la fecha no cae en ninguna temporada configurada. */
    @Column(name = "precio_base", nullable = false)
    private BigDecimal precioBase;

    /** Servicios separados por coma (wifi, aire acondicionado, etc.). */
    @Column(nullable = false)
    private String servicios = "";

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(nullable = false)
    private boolean activo = true;

    protected TipoHabitacion() {
    }

    public TipoHabitacion(String nombre, String descripcion, int capacidad, BigDecimal precioBase,
                          List<String> servicios, String imagenUrl) {
        actualizar(nombre, descripcion, capacidad, precioBase, servicios, imagenUrl);
    }

    public void actualizar(String nombre, String descripcion, int capacidad, BigDecimal precioBase,
                           List<String> servicios, String imagenUrl) {
        this.nombre = nombre;
        this.descripcion = descripcion == null ? "" : descripcion;
        this.capacidad = capacidad;
        this.precioBase = precioBase;
        this.servicios = servicios == null ? "" : String.join(",", servicios);
        this.imagenUrl = imagenUrl;
    }

    public boolean admite(int cantidadHuespedes) {
        return cantidadHuespedes > 0 && cantidadHuespedes <= capacidad;
    }

    public List<String> getListaServicios() {
        if (servicios == null || servicios.isBlank()) {
            return List.of();
        }
        return Arrays.stream(servicios.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public boolean isActivo() {
        return activo;
    }
}
