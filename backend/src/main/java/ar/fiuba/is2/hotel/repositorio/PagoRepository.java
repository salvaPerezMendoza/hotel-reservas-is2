package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.pago.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("""
            select p from Pago p join fetch p.reserva r
            where p.estado = ar.fiuba.is2.hotel.dominio.pago.EstadoPago.APROBADO
              and p.fecha >= :desde and p.fecha < :hasta
            order by p.fecha
            """)
    List<Pago> aprobadosEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
