package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.habitacion.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    @Query("""
            select b from Bloqueo b
            where b.habitacion.id in :habitaciones and b.desde < :hasta and b.hasta > :desde
            """)
    List<Bloqueo> superpuestos(@Param("habitaciones") Collection<Long> habitaciones,
                               @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select b from Bloqueo b where b.hasta > :desde order by b.desde")
    List<Bloqueo> vigentesDesde(@Param("desde") LocalDate desde);
}
