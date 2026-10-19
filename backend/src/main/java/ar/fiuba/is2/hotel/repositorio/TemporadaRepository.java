package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.tarifa.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findAllByOrderByFechaDesdeAsc();
}
