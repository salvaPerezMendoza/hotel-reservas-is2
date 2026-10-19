package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigo(String codigo);

    // Consultas explícitas: en el nombre de un método, "In" es palabra clave de Spring Data
    // y se confundiría con la propiedad checkIn.
    @Query("select r from Reserva r where r.huesped.id = :huespedId order by r.checkIn desc")
    List<Reserva> findByHuespedIdOrderByCheckInDesc(@Param("huespedId") Long huespedId);

    /** Reservas que podrían bloquear alguna de estas habitaciones en el rango pedido. */
    @Query("""
            select r from Reserva r
            where r.habitacion.id in :habitaciones
              and r.estado in :estados
              and r.checkIn < :hasta and r.checkOut > :desde
            """)
    List<Reserva> superpuestas(@Param("habitaciones") Collection<Long> habitaciones,
                               @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                               @Param("estados") Collection<EstadoReserva> estados);

    /** Marca como expiradas las reservas pendientes cuya retención ya venció (RF-07). */
    @Modifying
    @Query("""
            update Reserva r set r.estado = ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva.EXPIRADA
            where r.estado = ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva.PENDIENTE_PAGO
              and r.retencionHasta < :ahora
            """)
    int expirarRetencionesVencidas(@Param("ahora") Instant ahora);

    @Query("""
            select r from Reserva r
            where (:estado is null or r.estado = :estado)
              and (:desde is null or r.checkOut > :desde)
              and (:hasta is null or r.checkIn < :hasta)
            order by r.checkIn asc, r.codigo asc
            """)
    List<Reserva> filtrar(@Param("estado") EstadoReserva estado,
                          @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            select r from Reserva r
            where lower(r.codigo) like lower(concat('%', :q, '%'))
               or lower(r.huesped.email) like lower(concat('%', :q, '%'))
               or lower(r.huesped.apellido) like lower(concat('%', :q, '%'))
               or lower(r.huesped.nombre) like lower(concat('%', :q, '%'))
               or r.huesped.documento like concat('%', :q, '%')
            order by r.checkIn desc
            """)
    List<Reserva> buscarTexto(@Param("q") String q);

    @Query("select r from Reserva r where r.checkIn = :fecha and r.estado = :estado order by r.codigo")
    List<Reserva> findByCheckInAndEstado(@Param("fecha") LocalDate fecha, @Param("estado") EstadoReserva estado);

    List<Reserva> findByEstado(EstadoReserva estado);

    @Query("""
            select r from Reserva r
            where r.estado = ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva.CONFIRMADA
              and r.checkIn = :fecha and r.recordatorioEnviado = false
            """)
    List<Reserva> pendientesDeRecordatorio(@Param("fecha") LocalDate fecha);

    /** Reservas que ocupan noches dentro del período (para el reporte de ocupación). */
    @Query("""
            select r from Reserva r
            where r.estado in (ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva.CONFIRMADA,
                               ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva.EN_CURSO,
                               ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva.FINALIZADA)
              and r.checkIn < :hasta and r.checkOut > :desde
            """)
    List<Reserva> ocupandoEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
