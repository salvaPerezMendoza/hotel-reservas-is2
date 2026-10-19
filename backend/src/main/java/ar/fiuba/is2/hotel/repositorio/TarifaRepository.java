package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.tarifa.CategoriaTemporada;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    List<Tarifa> findByTipoId(Long tipoId);

    Optional<Tarifa> findByTipoIdAndCategoria(Long tipoId, CategoriaTemporada categoria);
}
