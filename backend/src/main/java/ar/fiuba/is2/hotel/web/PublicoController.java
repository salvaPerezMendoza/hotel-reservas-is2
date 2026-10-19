package ar.fiuba.is2.hotel.web;

import ar.fiuba.is2.hotel.servicio.AdministracionService;
import ar.fiuba.is2.hotel.servicio.DisponibilidadService;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.HotelDto;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.ResultadoBusquedaDto;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.TipoHabitacionDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Información pública: no hace falta iniciar sesión (CU-01). */
@RestController
@RequestMapping("/api")
public class PublicoController {

    private final AdministracionService administracion;
    private final DisponibilidadService disponibilidad;

    public PublicoController(AdministracionService administracion, DisponibilidadService disponibilidad) {
        this.administracion = administracion;
        this.disponibilidad = disponibilidad;
    }

    @GetMapping("/hotel")
    public HotelDto hotel() {
        return administracion.hotel();
    }

    @GetMapping("/catalogo/tipos")
    public List<TipoHabitacionDto> tipos() {
        return administracion.tiposPublicos();
    }

    @GetMapping("/catalogo/tipos/{id}")
    public TipoHabitacionDto tipo(@PathVariable Long id) {
        return administracion.tipoPublico(id);
    }

    /** RF-01 / RF-02 / RF-03: habitaciones libres para las fechas, con el precio total de la estadía. */
    @GetMapping("/disponibilidad")
    public ResultadoBusquedaDto buscar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                                       @RequestParam(defaultValue = "2") int huespedes,
                                       @RequestParam(required = false) Long tipoId) {
        return disponibilidad.buscar(checkIn, checkOut, huespedes, tipoId);
    }
}
