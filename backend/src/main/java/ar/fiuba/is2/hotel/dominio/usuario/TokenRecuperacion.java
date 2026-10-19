package ar.fiuba.is2.hotel.dominio.usuario;

import jakarta.persistence.*;

import java.time.Instant;

/** Token de un solo uso para restablecer la contraseña (RF-05). */
@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean usado;

    protected TokenRecuperacion() {
    }

    public TokenRecuperacion(Usuario usuario, String token, Instant expiraEn) {
        this.usuario = usuario;
        this.token = token;
        this.expiraEn = expiraEn;
    }

    public boolean esValido(Instant ahora) {
        return !usado && ahora.isBefore(expiraEn);
    }

    public void marcarUsado() {
        this.usado = true;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getToken() {
        return token;
    }
}
