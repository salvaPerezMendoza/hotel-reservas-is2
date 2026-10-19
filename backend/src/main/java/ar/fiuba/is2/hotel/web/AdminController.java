package ar.fiuba.is2.hotel.web;

import ar.fiuba.is2.hotel.servicio.AdministracionService;
import ar.fiuba.is2.hotel.servicio.ReporteService;
import ar.fiuba.is2.hotel.web.dto.AdminDtos.*;
import ar.fiuba.is2.hotel.web.dto.AuthDtos.UsuarioDto;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.TipoHabitacionDto;
import ar.fiuba.is2.hotel.web.dto.ReporteDtos.ReporteIngresosDto;
import ar.fiuba.is2.hotel.web.dto.ReporteDtos.ReporteOcupacionDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Administración del hotel (CU-05). Requiere rol administrador. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdministracionService admin;
    private final ReporteService reportes;

    public AdminController(AdministracionService admin, ReporteService reportes) {
        this.admin = admin;
        this.reportes = reportes;
    }

    // --- Tipos de habitación y habitaciones ---

    @GetMapping("/tipos")
    public List<TipoHabitacionDto> tipos() {
        return admin.tipos();
    }

    @PostMapping("/tipos")
    @ResponseStatus(HttpStatus.CREATED)
    public TipoHabitacionDto crearTipo(@Valid @RequestBody TipoHabitacionRequest req) {
        return admin.crearTipo(req);
    }

    @PutMapping("/tipos/{id}")
    public TipoHabitacionDto actualizarTipo(@PathVariable Long id, @Valid @RequestBody TipoHabitacionRequest req) {
        return admin.actualizarTipo(id, req);
    }

    @PatchMapping("/tipos/{id}/activo")
    public TipoHabitacionDto activarTipo(@PathVariable Long id, @RequestParam boolean valor) {
        return admin.cambiarActivoTipo(id, valor);
    }

    @GetMapping("/habitaciones")
    public List<HabitacionDto> habitaciones() {
        return admin.habitaciones();
    }

    @PostMapping("/habitaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public HabitacionDto crearHabitacion(@Valid @RequestBody HabitacionRequest req) {
        return admin.crearHabitacion(req);
    }

    @PutMapping("/habitaciones/{id}")
    public HabitacionDto actualizarHabitacion(@PathVariable Long id, @Valid @RequestBody HabitacionRequest req) {
        return admin.actualizarHabitacion(id, req);
    }

    @PatchMapping("/habitaciones/{id}/activa")
    public HabitacionDto activarHabitacion(@PathVariable Long id, @RequestParam boolean valor) {
        return admin.cambiarActivaHabitacion(id, valor);
    }

    // --- Temporadas y tarifas ---

    @GetMapping("/temporadas")
    public List<TemporadaDto> temporadas() {
        return admin.temporadas();
    }

    @PostMapping("/temporadas")
    @ResponseStatus(HttpStatus.CREATED)
    public TemporadaDto crearTemporada(@Valid @RequestBody TemporadaRequest req) {
        return admin.crearTemporada(req);
    }

    @PutMapping("/temporadas/{id}")
    public TemporadaDto actualizarTemporada(@PathVariable Long id, @Valid @RequestBody TemporadaRequest req) {
        return admin.actualizarTemporada(id, req);
    }

    @DeleteMapping("/temporadas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTemporada(@PathVariable Long id) {
        admin.eliminarTemporada(id);
    }

    @GetMapping("/tarifas")
    public List<TarifasTipoDto> tarifas() {
        return admin.tarifas();
    }

    @PutMapping("/tarifas/{tipoId}")
    public TarifasTipoDto actualizarTarifas(@PathVariable Long tipoId, @Valid @RequestBody TarifasTipoRequest req) {
        return admin.actualizarTarifas(tipoId, req);
    }

    // --- Configuración y bloqueos ---

    @GetMapping("/configuracion")
    public ConfiguracionDto configuracion() {
        return admin.configuracion();
    }

    @PutMapping("/configuracion")
    public ConfiguracionDto actualizarConfiguracion(@Valid @RequestBody ConfiguracionRequest req) {
        return admin.actualizarConfiguracion(req);
    }

    @GetMapping("/bloqueos")
    public List<BloqueoDto> bloqueos() {
        return admin.bloqueos();
    }

    @PostMapping("/bloqueos")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueoDto crearBloqueo(@Valid @RequestBody BloqueoRequest req) {
        return admin.crearBloqueo(req);
    }

    @DeleteMapping("/bloqueos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBloqueo(@PathVariable Long id) {
        admin.eliminarBloqueo(id);
    }

    // --- Usuarios ---

    @GetMapping("/usuarios")
    public List<UsuarioDto> usuarios() {
        return admin.usuarios();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crearUsuario(@Valid @RequestBody NuevoUsuarioRequest req) {
        return admin.crearUsuario(req);
    }

    @PatchMapping("/usuarios/{id}/rol")
    public UsuarioDto cambiarRol(@PathVariable Long id, @Valid @RequestBody CambioRolRequest req) {
        return admin.cambiarRol(id, req.rol());
    }

    @PatchMapping("/usuarios/{id}/activo")
    public UsuarioDto activarUsuario(@PathVariable Long id, @RequestParam boolean valor) {
        return admin.cambiarActivoUsuario(id, valor);
    }

    @GetMapping("/auditoria")
    public List<AuditoriaDto> auditoria(@RequestParam(defaultValue = "100") int limite) {
        return admin.auditoria(limite);
    }

    // --- Reportes (RF-28, RF-29, RF-30) ---

    @GetMapping("/reportes/ocupacion")
    public ReporteOcupacionDto ocupacion(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.ocupacion(desde, hasta);
    }

    @GetMapping("/reportes/ingresos")
    public ReporteIngresosDto ingresos(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.ingresos(desde, hasta);
    }

    @GetMapping("/reportes/excel")
    public ResponseEntity<byte[]> excel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] contenido = reportes.exportarExcel(desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-" + desde + "-al-" + hasta + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenido);
    }
}
