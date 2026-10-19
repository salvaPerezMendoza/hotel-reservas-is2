package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TipoHabitacionRepository extends JpaRepository<TipoHabitacion, Long> {

    List<TipoHabitacion> findByActivoTrueOrderByPrecioBaseAsc();

    List<TipoHabitacion> findAllByOrderByPrecioBaseAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Bloquea la fila del tipo de habitación durante la transacción. Así, dos
     * reservas simultáneas del mismo tipo se asignan de a una (sin pisarse).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TipoHabitacion t where t.id = :id")
    Optional<TipoHabitacion> findByIdParaReservar(@Param("id") Long id);
}
