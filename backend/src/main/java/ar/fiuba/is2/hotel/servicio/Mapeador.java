package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.auditoria.RegistroAuditoria;
import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.habitacion.Bloqueo;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.pago.Consumo;
import ar.fiuba.is2.hotel.dominio.pago.Pago;
import ar.fiuba.is2.hotel.dominio.politica.precio.Cotizacion;
import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.dominio.tarifa.Temporada;
import ar.fiuba.is2.hotel.dominio.usuario.Huesped;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import ar.fiuba.is2.hotel.web.dto.AdminDtos.*;
import ar.fiuba.is2.hotel.web.dto.AuthDtos.UsuarioDto;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.*;
import ar.fiuba.is2.hotel.web.dto.ReservaDtos.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Convierte objetos del dominio en DTOs para la API. */
@Component
public class Mapeador {

    public UsuarioDto usuario(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(), u.getRol(),
                u.getTelefono(), u.getDocumento(), u.isActivo());
    }

    public HuespedResumenDto huesped(Huesped h) {
        return new HuespedResumenDto(h.getId(), h.getNombre(), h.getApellido(), h.getEmail(), h.getTelefono(),
                h.getDocumento());
    }

    public TipoHabitacionDto tipo(TipoHabitacion t, long cantidadHabitaciones) {
        return new TipoHabitacionDto(t.getId(), t.getNombre(), t.getDescripcion(), t.getCapacidad(), t.getPrecioBase(),
                t.getListaServicios(), t.getImagenUrl(), t.isActivo(), cantidadHabitaciones);
    }

    public CotizacionDto cotizacion(Cotizacion c, ConfiguracionHotel config) {
        return new CotizacionDto(
                c.noches().size(),
                c.noches().stream().map(n -> new NocheDto(n.fecha(), n.categoria(), n.precio())).toList(),
                c.subtotal(), c.descuento(), c.motivoDescuento(), c.total(), config.senaPara(c.total()));
    }

    public HabitacionDto habitacion(Habitacion h) {
        return new HabitacionDto(h.getId(), h.getNumero(), h.getPiso(), h.getTipo().getId(), h.getTipo().getNombre(),
                h.getEstado(), h.isActiva());
    }

    public TemporadaDto temporada(Temporada t) {
        return new TemporadaDto(t.getId(), t.getNombre(), t.getCategoria(), t.getFechaDesde(), t.getFechaHasta());
    }

    public BloqueoDto bloqueo(Bloqueo b) {
        return new BloqueoDto(b.getId(), b.getHabitacion().getId(), b.getHabitacion().getNumero(), b.getDesde(),
                b.getHasta(), b.getMotivo());
    }

    public AuditoriaDto auditoria(RegistroAuditoria r) {
        return new AuditoriaDto(r.getId(), r.getFecha(), r.getUsuario(), r.getAccion(), r.getEntidad(),
                r.getEntidadId(), r.getDetalle());
    }

    public PagoDto pago(Pago p) {
        return new PagoDto(p.getId(), p.getMonto(), p.getMedio(), p.getConcepto(), p.getEstado(), p.getReferencia(),
                p.getFecha());
    }

    public ConsumoDto consumo(Consumo c) {
        return new ConsumoDto(c.getId(), c.getDescripcion(), c.getMonto(), c.getFecha());
    }

    public ReservaDto reserva(Reserva r, ConfiguracionHotel config, LocalDate hoy) {
        BigDecimal sena = r.getEstado() == EstadoReserva.PENDIENTE_PAGO ? config.senaPara(r.getPrecioTotal()) : null;
        boolean modificable = r.getEstado().esModificable() && hoy.isBefore(r.getCheckIn());
        return new ReservaDto(
                r.getId(), r.getCodigo(), r.getEstado(), r.getCanal(),
                huesped(r.getHuesped()),
                r.getTipo().getId(), r.getTipo().getNombre(), r.getTipo().getImagenUrl(),
                r.getHabitacion().getId(), r.getHabitacion().getNumero(),
                r.getCheckIn(), r.getCheckOut(), r.getEstadia().noches(), r.getCantidadHuespedes(),
                r.getPrecioTotal(), r.totalConsumos(), r.totalACobrar(),
                r.totalPagado(), r.saldoPendiente(), sena,
                r.getRetencionHasta(), r.getCreadaEn(), r.getCanceladaEn(), r.getReembolso(),
                r.getCheckInReal(), r.getCheckOutReal(), r.getObservaciones(),
                r.getPagos().stream().map(this::pago).toList(),
                r.getConsumos().stream().map(this::consumo).toList(),
                modificable,
                r.getEstado().esModificable());
    }
}
