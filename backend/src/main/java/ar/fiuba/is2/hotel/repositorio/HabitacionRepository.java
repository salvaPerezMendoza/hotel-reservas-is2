package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    List<Habitacion> findByTipoIdAndActivaTrueOrderByNumeroAsc(Long tipoId);

    List<Habitacion> findByActivaTrueOrderByNumeroAsc();

    List<Habitacion> findAllByOrderByNumeroAsc();

    long countByTipoIdAndActivaTrue(Long tipoId);

    boolean existsByNumero(String numero);
}
