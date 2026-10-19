package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.disponibilidad.VerificadorDeDisponibilidad;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.habitacion.Bloqueo;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.repositorio.BloqueoRepository;
import ar.fiuba.is2.hotel.repositorio.HabitacionRepository;
import ar.fiuba.is2.hotel.repositorio.ReservaRepository;
import ar.fiuba.is2.hotel.repositorio.TipoHabitacionRepository;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.OpcionDisponibleDto;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.ResultadoBusquedaDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Búsqueda de habitaciones y consulta de disponibilidad en tiempo real (RF-01 a RF-03). */
@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    public static final int MAXIMO_NOCHES = 60;
    public static final int MAXIMO_DIAS_ANTICIPACION = 540;

    private static final List<EstadoReserva> ESTADOS_QUE_BLOQUEAN =
            List.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    private final TipoHabitacionRepository tipos;
    private final HabitacionRepository habitaciones;
    private final ReservaRepository reservas;
    private final BloqueoRepository bloqueos;
    private final VerificadorDeDisponibilidad verificador;
    private final TarifarioService tarifario;
    private final Mapeador mapeador;
    private final Clock reloj;

    public DisponibilidadService(TipoHabitacionRepository tipos, HabitacionRepository habitaciones,
                                 ReservaRepository reservas, BloqueoRepository bloqueos,
                                 VerificadorDeDisponibilidad verificador, TarifarioService tarifario,
                                 Mapeador mapeador, Clock reloj) {
        this.tipos = tipos;
        this.habitaciones = habitaciones;
        this.reservas = reservas;
        this.bloqueos = bloqueos;
        this.verificador = verificador;
        this.tarifario = tarifario;
        this.mapeador = mapeador;
        this.reloj = reloj;
    }

    public ResultadoBusquedaDto buscar(LocalDate checkIn, LocalDate checkOut, int huespedes, Long tipoId) {
        Estadia estadia = validarEstadiaFutura(checkIn, checkOut);
        if (huespedes < 1) {
            throw new ReglaDeNegocioException("Tiene que haber al menos un huésped");
        }
        ConfiguracionHotel config = tarifario.configuracion();
        List<OpcionDisponibleDto> opciones = new ArrayList<>();
        for (TipoHabitacion tipo : tipos.findByActivoTrueOrderByPrecioBaseAsc()) {
            if (!tipo.admite(huespedes) || (tipoId != null && !tipo.getId().equals(tipoId))) {
                continue;
            }
            List<Habitacion> libres = habitacionesLibres(tipo, estadia, null);
            if (!libres.isEmpty()) {
                var cotizacion = mapeador.cotizacion(tarifario.cotizar(tipo, estadia), config);
                long total = habitaciones.countByTipoIdAndActivaTrue(tipo.getId());
                opciones.add(new OpcionDisponibleDto(mapeador.tipo(tipo, total), libres.size(), cotizacion));
            }
        }
        return new ResultadoBusquedaDto(checkIn, checkOut, huespedes, estadia.noches(), opciones);
    }

    /**
     * Habitaciones activas del tipo que están libres para la estadía.
     *
     * @param reservaExcluidaId reserva que no se tiene en cuenta (al modificarla, no choca consigo misma)
     */
    public List<Habitacion> habitacionesLibres(TipoHabitacion tipo, Estadia estadia, Long reservaExcluidaId) {
        List<Habitacion> candidatas = habitaciones.findByTipoIdAndActivaTrueOrderByNumeroAsc(tipo.getId());
        if (candidatas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = candidatas.stream().map(Habitacion::getId).toList();
        Map<Long, List<Reserva>> reservasPorHabitacion = reservas
                .superpuestas(ids, estadia.checkIn(), estadia.checkOut(), ESTADOS_QUE_BLOQUEAN).stream()
                .filter(r -> !Objects.equals(r.getId(), reservaExcluidaId))
                .collect(Collectors.groupingBy(r -> r.getHabitacion().getId()));
        Map<Long, List<Bloqueo>> bloqueosPorHabitacion = bloqueos
                .superpuestos(ids, estadia.checkIn(), estadia.checkOut()).stream()
                .collect(Collectors.groupingBy(b -> b.getHabitacion().getId()));
        return verificador.habitacionesLibres(candidatas, estadia, reservasPorHabitacion, bloqueosPorHabitacion,
                reloj.instant());
    }

    public boolean habitacionLibre(Habitacion habitacion, Estadia estadia, Long reservaExcluidaId) {
        List<Reserva> superpuestas = reservas
                .superpuestas(List.of(habitacion.getId()), estadia.checkIn(), estadia.checkOut(), ESTADOS_QUE_BLOQUEAN)
                .stream().filter(r -> !Objects.equals(r.getId(), reservaExcluidaId)).toList();
        List<Bloqueo> bloqueosHab = bloqueos.superpuestos(List.of(habitacion.getId()), estadia.checkIn(), estadia.checkOut());
        return verificador.estaDisponible(habitacion, estadia, superpuestas, bloqueosHab, reloj.instant());
    }

    public Estadia validarEstadiaFutura(LocalDate checkIn, LocalDate checkOut) {
        Estadia estadia = new Estadia(checkIn, checkOut);
        LocalDate hoy = LocalDate.now(reloj);
        if (checkIn.isBefore(hoy)) {
            throw new ReglaDeNegocioException("La fecha de ingreso no puede ser anterior a hoy");
        }
        if (estadia.noches() > MAXIMO_NOCHES) {
            throw new ReglaDeNegocioException("La estadía no puede superar las " + MAXIMO_NOCHES + " noches");
        }
        if (checkIn.isAfter(hoy.plusDays(MAXIMO_DIAS_ANTICIPACION))) {
            throw new ReglaDeNegocioException("Todavía no se pueden tomar reservas para esas fechas");
        }
        return estadia;
    }
}
