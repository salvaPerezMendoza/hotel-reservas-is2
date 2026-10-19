package ar.fiuba.is2.hotel.dominio.usuario;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Clase base (abstracta) de toda persona que usa el sistema.
 *
 * <p>Se aplica <b>herencia</b>: Huesped, Recepcionista y Administrador comparten
 * identidad y datos de contacto, y cada subclase define sus permisos.
 * Se aplica <b>polimorfismo</b>: el resto del sistema pregunta
 * {@link #puedeOperarRecepcion()} o {@link #puedeAdministrar()} sin saber
 * qué tipo concreto de usuario tiene adelante.</p>
 */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rol", discriminatorType = DiscriminatorType.STRING)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private String telefono;

    private String documento;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    protected Usuario() {
    }

    protected Usuario(String nombre, String apellido, String email) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = normalizarEmail(email);
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Rol del usuario, definido por cada subclase. */
    public abstract Rol getRol();

    /** ¿Puede cargar reservas de terceros, cobrar y hacer check-in/out? */
    public abstract boolean puedeOperarRecepcion();

    /** ¿Puede configurar el hotel, las tarifas y ver reportes? */
    public abstract boolean puedeAdministrar();

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public boolean tieneCredenciales() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = nuevoHash;
    }

    public void actualizarDatos(String nombre, String apellido, String telefono, String documento) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.documento = documento;
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

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDocumento() {
        return documento;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
