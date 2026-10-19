package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.politica.precio.Cotizacion;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifario;
import ar.fiuba.is2.hotel.repositorio.ConfiguracionHotelRepository;
import ar.fiuba.is2.hotel.repositorio.TarifaRepository;
import ar.fiuba.is2.hotel.repositorio.TemporadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma el tarifario vigente y cotiza estadías con la política de precio configurada. */
@Service
@Transactional(readOnly = true)
public class TarifarioService {

    private final TemporadaRepository temporadas;
    private final TarifaRepository tarifas;
    private final ConfiguracionHotelRepository configuraciones;

    public TarifarioService(TemporadaRepository temporadas, TarifaRepository tarifas,
                            ConfiguracionHotelRepository configuraciones) {
        this.temporadas = temporadas;
        this.tarifas = tarifas;
        this.configuraciones = configuraciones;
    }

    public ConfiguracionHotel configuracion() {
        return configuraciones.findById(ConfiguracionHotel.ID_UNICO)
                .orElseThrow(() -> new IllegalStateException("Falta la configuración del hotel"));
    }

    public Tarifario tarifarioVigente() {
        return new Tarifario(temporadas.findAllByOrderByFechaDesdeAsc(), tarifas.findAll());
    }

    public Cotizacion cotizar(TipoHabitacion tipo, Estadia estadia) {
        return configuracion().politicaDePrecioVigente().cotizar(tipo, estadia, tarifarioVigente());
    }
}
