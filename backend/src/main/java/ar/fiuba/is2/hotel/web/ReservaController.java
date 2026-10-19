package ar.fiuba.is2.hotel.web;

import ar.fiuba.is2.hotel.servicio.ReservaService;
import ar.fiuba.is2.hotel.web.dto.ReservaDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Reservas del huésped que inició sesión (CU-02 y CU-03). */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservas;

    public ReservaController(ReservaService reservas) {
        this.reservas = reservas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDto crear(@Valid @RequestBody CrearReservaRequest req) {
        return reservas.crearOnline(req);
    }

    @GetMapping("/mias")
    public List<ReservaDto> mias() {
        return reservas.misReservas();
    }

    @GetMapping("/{codigo}")
    public ReservaDto detalle(@PathVariable String codigo) {
        return reservas.detalle(codigo);
    }

    @PostMapping("/{codigo}/pagos")
    public ResultadoPagoDto pagar(@PathVariable String codigo, @Valid @RequestBody PagoOnlineRequest req) {
        return reservas.pagarOnline(codigo, req);
    }

    @PutMapping("/{codigo}")
    public ReservaDto modificar(@PathVariable String codigo, @Valid @RequestBody ModificarReservaRequest req) {
        return reservas.modificar(codigo, req);
    }

    @GetMapping("/{codigo}/cancelacion")
    public SimulacionCancelacionDto simularCancelacion(@PathVariable String codigo) {
        return reservas.simularCancelacion(codigo);
    }

    @PostMapping("/{codigo}/cancelar")
    public ReservaDto cancelar(@PathVariable String codigo) {
        return reservas.cancelar(codigo);
    }
}
