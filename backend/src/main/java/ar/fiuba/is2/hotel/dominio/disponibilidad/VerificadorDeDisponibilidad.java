package ar.fiuba.is2.hotel.dominio.disponibilidad;

import ar.fiuba.is2.hotel.dominio.habitacion.Bloqueo;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <b>Algoritmo central del sistema</b> (punto 6 del TP): decide si una habitación
 * está libre para una estadía.
 *
 * <pre>
 * función estaDisponible(habitación, estadía):
 *     si la habitación está dada de baja: devolver FALSO
 *     para cada bloqueo b de la habitación:
 *         si b se superpone con la estadía: devolver FALSO
 *     para cada reserva r de la habitación:
 *         si r bloquea la habitación (confirmada, en curso o retenida)
 *            y r se superpone con la estadía: devolver FALSO
 *     devolver VERDADERO
 *
 * se superponen(A, B)  ⇔  A.ingreso &lt; B.egreso  Y  B.ingreso &lt; A.egreso
 * </pre>
 *
 * <p>Esta verificación se usa para buscar y elegir habitación. Como segunda
 * barrera, la base de datos tiene una restricción de exclusión que impide guardar
 * dos reservas superpuestas aunque lleguen en el mismo instante.</p>
 */
public class VerificadorDeDisponibilidad {

    public boolean estaDisponible(Habitacion habitacion, Estadia estadia,
                                  Collection<Reserva> reservasDeLaHabitacion,
                                  Collection<Bloqueo> bloqueosDeLaHabitacion,
                                  Instant ahora) {
        if (!habitacion.isActiva()) {
            return false;
        }
        for (Bloqueo bloqueo : bloqueosDeLaHabitacion) {
            if (bloqueo.getRango().seSuperponeCon(estadia)) {
                return false;
            }
        }
        for (Reserva reserva : reservasDeLaHabitacion) {
            if (reserva.bloqueaHabitacion(ahora) && reserva.getEstadia().seSuperponeCon(estadia)) {
                return false;
            }
        }
        return true;
    }

    /** Filtra las habitaciones candidatas y devuelve las libres para la estadía. */
    public List<Habitacion> habitacionesLibres(Collection<Habitacion> candidatas, Estadia estadia,
                                               Map<Long, List<Reserva>> reservasPorHabitacion,
                                               Map<Long, List<Bloqueo>> bloqueosPorHabitacion,
                                               Instant ahora) {
        return candidatas.stream()
                .filter(h -> estaDisponible(h, estadia,
                        reservasPorHabitacion.getOrDefault(h.getId(), List.of()),
                        bloqueosPorHabitacion.getOrDefault(h.getId(), List.of()),
                        ahora))
                .toList();
    }
}
