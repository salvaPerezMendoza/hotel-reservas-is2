package ar.fiuba.is2.hotel.dominio.disponibilidad;

import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Objeto de valor que representa una estadía: el rango [checkIn, checkOut).
 * La noche del checkOut no se cuenta (el huésped se va ese día).
 */
public record Estadia(LocalDate checkIn, LocalDate checkOut) {

    public Estadia {
        if (checkIn == null || checkOut == null) {
            throw new ReglaDeNegocioException("La estadía necesita fecha de ingreso y de egreso");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ReglaDeNegocioException("La fecha de egreso debe ser posterior a la de ingreso");
        }
    }

    /** Cantidad de noches de la estadía. */
    public long noches() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Dos estadías se superponen si una empieza antes de que termine la otra
     * y viceversa. Como el rango es semiabierto, quien sale el día 10 no choca
     * con quien entra el día 10.
     */
    public boolean seSuperponeCon(Estadia otra) {
        return checkIn.isBefore(otra.checkOut) && otra.checkIn.isBefore(checkOut);
    }

    /** Fechas de cada noche de la estadía (de checkIn a checkOut - 1). */
    public List<LocalDate> fechasDeNoches() {
        return checkIn.datesUntil(checkOut).toList();
    }

    /** Cantidad de noches que esta estadía comparte con otra. */
    public long nochesEnComunCon(Estadia otra) {
        if (!seSuperponeCon(otra)) {
            return 0;
        }
        LocalDate desde = checkIn.isAfter(otra.checkIn) ? checkIn : otra.checkIn;
        LocalDate hasta = checkOut.isBefore(otra.checkOut) ? checkOut : otra.checkOut;
        return ChronoUnit.DAYS.between(desde, hasta);
    }

    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(checkIn) && fecha.isBefore(checkOut);
    }
}
