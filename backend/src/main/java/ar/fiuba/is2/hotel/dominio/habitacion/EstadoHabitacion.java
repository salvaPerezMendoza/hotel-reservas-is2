package ar.fiuba.is2.hotel.dominio.habitacion;

/**
 * Estado físico actual de la habitación (RF-20).
 * "Reservada" no es un estado guardado: surge de las reservas para cada fecha.
 */
public enum EstadoHabitacion {
    DISPONIBLE,
    OCUPADA,
    LIMPIEZA,
    MANTENIMIENTO
}
