package ar.fiuba.is2.hotel.web;

import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import ar.fiuba.is2.hotel.servicio.RecepcionService;
import ar.fiuba.is2.hotel.web.dto.AdminDtos.EstadoHabitacionRequest;
import ar.fiuba.is2.hotel.web.dto.AdminDtos.HabitacionDto;
import ar.fiuba.is2.hotel.web.dto.ReporteDtos.TableroDto;
import ar.fiuba.is2.hotel.web.dto.ReservaDtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Operación de recepción (CU-02 presencial, CU-03 y CU-04). Requiere rol recepcionista o administrador. */
@RestController
@RequestMapping("/api/recepcion")
public class RecepcionController {

    private final RecepcionService recepcion;

    public RecepcionController(RecepcionService recepcion) {
        this.recepcion = recepcion;
    }

    @GetMapping("/tablero")
    public TableroDto tablero() {
        return recepcion.tablero();
    }

    @GetMapping("/reservas")
    public List<ReservaDto> reservas(@RequestParam(required = false) EstadoReserva estado,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @RequestParam(required = false) String q) {
        return recepcion.listar(estado, desde, hasta, q);
    }

    @GetMapping("/llegadas")
    public List<ReservaDto> llegadas() {
        return recepcion.llegadasDeHoy();
    }

    @GetMapping("/salidas")
    public List<ReservaDto> salidas() {
        return recepcion.salidasDeHoy();
    }

    @GetMapping("/en-curso")
    public List<ReservaDto> enCurso() {
        return recepcion.enCurso();
    }

    @GetMapping("/huespedes")
    public List<HuespedResumenDto> huespedes(@RequestParam String q) {
        return recepcion.buscarHuespedes(q);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDto crear(@Valid @RequestBody ReservaRecepcionRequest req) {
        return recepcion.crearReserva(req);
    }

    @PostMapping("/reservas/{codigo}/pagos")
    public ReservaDto pagar(@PathVariable String codigo, @Valid @RequestBody PagoPresencialRequest req) {
        return recepcion.registrarPago(codigo, req);
    }

    @GetMapping("/reservas/{codigo}/habitaciones-disponibles")
    public List<HabitacionDto> habitacionesParaCheckIn(@PathVariable String codigo) {
        return recepcion.habitacionesParaCheckIn(codigo);
    }

    @PostMapping("/reservas/{codigo}/check-in")
    public ReservaDto checkIn(@PathVariable String codigo, @RequestBody(required = false) CheckInRequest req) {
        return recepcion.hacerCheckIn(codigo, req);
    }

    @PostMapping("/reservas/{codigo}/check-out")
    public ReservaDto checkOut(@PathVariable String codigo) {
        return recepcion.hacerCheckOut(codigo);
    }

    @PostMapping("/reservas/{codigo}/consumos")
    public ReservaDto consumo(@PathVariable String codigo, @Valid @RequestBody ConsumoRequest req) {
        return recepcion.agregarConsumo(codigo, req);
    }

    @GetMapping("/habitaciones")
    public List<HabitacionDto> habitaciones() {
        return recepcion.estadoDeHabitaciones();
    }

    @PatchMapping("/habitaciones/{id}/estado")
    public HabitacionDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoHabitacionRequest req) {
        return recepcion.cambiarEstadoHabitacion(id, req.estado());
    }
}
