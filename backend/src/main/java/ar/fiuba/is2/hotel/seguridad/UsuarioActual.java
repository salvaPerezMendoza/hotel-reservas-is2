package ar.fiuba.is2.hotel.seguridad;

import ar.fiuba.is2.hotel.dominio.excepciones.NoEncontradoException;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import ar.fiuba.is2.hotel.repositorio.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Acceso al usuario que hizo el pedido actual. */
@Component
public class UsuarioActual {

    private final UsuarioRepository usuarios;

    public UsuarioActual(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<UsuarioAutenticado> autenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public Usuario obtener() {
        UsuarioAutenticado u = autenticado().orElseThrow(() -> new NoEncontradoException("No hay un usuario autenticado"));
        return usuarios.findById(u.id()).orElseThrow(() -> new NoEncontradoException("El usuario ya no existe"));
    }

    public String emailOSistema() {
        return autenticado().map(UsuarioAutenticado::email).orElse("sistema");
    }
}
