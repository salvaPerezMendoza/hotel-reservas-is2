package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from Usuario u order by u.apellido, u.nombre")
    List<Usuario> findAllOrdenados();

    /** Cambia el rol (la subclase) de un usuario. JPA no permite cambiar el tipo de una entidad. */
    @Modifying
    @Query(value = "update usuarios set rol = :rol where id = :id", nativeQuery = true)
    int cambiarRol(@Param("id") Long id, @Param("rol") String rol);
}
