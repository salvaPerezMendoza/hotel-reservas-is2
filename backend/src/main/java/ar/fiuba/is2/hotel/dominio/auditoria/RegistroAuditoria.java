package ar.fiuba.is2.hotel.dominio.auditoria;

import jakarta.persistence.*;

import java.time.Instant;

/** Registro de una operación crítica, para rastrear errores o disputas (RNF-15). */
@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant fecha;

    private String usuario;

    @Column(nullable = false)
    private String accion;

    @Column(nullable = false)
    private String entidad;

    @Column(name = "entidad_id")
    private String entidadId;

    private String detalle;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(Instant fecha, String usuario, String accion, String entidad, String entidadId, String detalle) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public String getDetalle() {
        return detalle;
    }
}
