package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.usuario.Huesped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HuespedRepository extends JpaRepository<Huesped, Long> {

    Optional<Huesped> findByEmailIgnoreCase(String email);

    @Query("""
            select h from Huesped h
            where lower(h.email) like lower(concat('%', :q, '%'))
               or lower(h.nombre) like lower(concat('%', :q, '%'))
               or lower(h.apellido) like lower(concat('%', :q, '%'))
               or h.documento like concat('%', :q, '%')
            order by h.apellido, h.nombre
            """)
    List<Huesped> buscar(@Param("q") String q);
}
