package ar.fiuba.is2.hotel.dominio.tarifa;

import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Responde dos preguntas: ¿en qué categoría cae una fecha? y
 * ¿cuánto sale una noche de un tipo de habitación en esa fecha?
 *
 * <p>Si una fecha no pertenece a ninguna temporada configurada se considera BAJA.
 * Si el tipo no tiene tarifa cargada para esa categoría, se usa su precio base.</p>
 */
public class Tarifario {

    private final List<Temporada> temporadas;
    private final List<Tarifa> tarifas;

    public Tarifario(List<Temporada> temporadas, List<Tarifa> tarifas) {
        this.temporadas = List.copyOf(temporadas);
        this.tarifas = List.copyOf(tarifas);
    }

    public CategoriaTemporada categoriaDe(LocalDate fecha) {
        return temporadas.stream()
                .filter(t -> t.incluye(fecha))
                .map(Temporada::getCategoria)
                .findFirst()
                .orElse(CategoriaTemporada.BAJA);
    }

    public BigDecimal precioNoche(TipoHabitacion tipo, LocalDate fecha) {
        CategoriaTemporada categoria = categoriaDe(fecha);
        return tarifaDe(tipo, categoria)
                .map(Tarifa::getPrecioNoche)
                .orElse(tipo.getPrecioBase());
    }

    private Optional<Tarifa> tarifaDe(TipoHabitacion tipo, CategoriaTemporada categoria) {
        return tarifas.stream()
                .filter(t -> t.getTipo().getId().equals(tipo.getId()) && t.getCategoria() == categoria)
                .findFirst();
    }
}
