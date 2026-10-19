package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionHotelRepository extends JpaRepository<ConfiguracionHotel, Integer> {
}
