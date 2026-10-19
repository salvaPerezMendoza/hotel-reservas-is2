package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.usuario.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByToken(String token);
}
