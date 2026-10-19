package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.disponibilidad.Estadia;
import ar.fiuba.is2.hotel.dominio.excepciones.NoEncontradoException;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.habitacion.Bloqueo;
import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.TipoPoliticaCancelacion;
import ar.fiuba.is2.hotel.dominio.politica.precio.TipoPoliticaPrecio;
import ar.fiuba.is2.hotel.dominio.reserva.EstadoReserva;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import ar.fiuba.is2.hotel.dominio.tarifa.CategoriaTemporada;
import ar.fiuba.is2.hotel.dominio.tarifa.Tarifa;
import ar.fiuba.is2.hotel.dominio.tarifa.Temporada;
import ar.fiuba.is2.hotel.dominio.usuario.*;
import ar.fiuba.is2.hotel.repositorio.*;
import ar.fiuba.is2.hotel.seguridad.UsuarioActual;
import ar.fiuba.is2.hotel.web.dto.AdminDtos.*;
import ar.fiuba.is2.hotel.web.dto.AuthDtos.UsuarioDto;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.HotelDto;
import ar.fiuba.is2.hotel.web.dto.CatalogoDtos.TipoHabitacionDto;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Administración del hotel (CU-05): catálogo, tarifas, políticas, bloqueos y usuarios. */
@Service
@Transactional
public class AdministracionService {

    private static final List<EstadoReserva> ESTADOS_ACTIVOS =
            List.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    private final TipoHabitacionRepository tipos;
    private final HabitacionRepository habitaciones;
    private final TemporadaRepository temporadas;
    private final TarifaRepository tarifas;
    private final BloqueoRepository bloqueos;
    private final ReservaRepository reservas;
    private final ConfiguracionHotelRepository configuraciones;
    private final UsuarioRepository usuarios;
    private final AuditoriaRepository registrosAuditoria;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActual usuarioActual;
    private final AuditoriaService auditoria;
    private final Mapeador mapeador;
    private final EntityManager em;
    private final Clock reloj;

    public AdministracionService(TipoHabitacionRepository tipos, HabitacionRepository habitaciones,
                                 TemporadaRepository temporadas, TarifaRepository tarifas, BloqueoRepository bloqueos,
                                 ReservaRepository reservas, ConfiguracionHotelRepository configuraciones,
                                 UsuarioRepository usuarios, AuditoriaRepository registrosAuditoria,
                                 PasswordEncoder passwordEncoder, UsuarioActual usuarioActual,
                                 AuditoriaService auditoria, Mapeador mapeador, EntityManager em, Clock reloj) {
        this.tipos = tipos;
        this.habitaciones = habitaciones;
        this.temporadas = temporadas;
        this.tarifas = tarifas;
        this.bloqueos = bloqueos;
        this.reservas = reservas;
        this.configuraciones = configuraciones;
        this.usuarios = usuarios;
        this.registrosAuditoria = registrosAuditoria;
        this.passwordEncoder = passwordEncoder;
        this.usuarioActual = usuarioActual;
        this.auditoria = auditoria;
        this.mapeador = mapeador;
        this.em = em;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------
    // Información pública del hotel y catálogo
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public HotelDto hotel() {
        ConfiguracionHotel c = configuracionHotel();
        var politica = c.politicaDeCancelacionVigente();
        return new HotelDto(c.getNombreHotel(), c.getHoraCheckIn(), c.getHoraCheckOut(), c.getPorcentajeSena(),
                c.getMinutosRetencion(), politica.nombre(), politica.descripcion(),
                c.politicaDePrecioVigente().descripcion());
    }

    @Transactional(readOnly = true)
    public List<TipoHabitacionDto> tiposPublicos() {
        return tipos.findByActivoTrueOrderByPrecioBaseAsc().stream()
                .map(t -> mapeador.tipo(t, habitaciones.countByTipoIdAndActivaTrue(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoHabitacionDto tipoPublico(Long id) {
        TipoHabitacion t = tipo(id);
        return mapeador.tipo(t, habitaciones.countByTipoIdAndActivaTrue(t.getId()));
    }

    // ------------------------------------------------------------------
    // Tipos de habitación y habitaciones (RF-22)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TipoHabitacionDto> tipos() {
        return tipos.findAllByOrderByPrecioBaseAsc().stream()
                .map(t -> mapeador.tipo(t, habitaciones.countByTipoIdAndActivaTrue(t.getId())))
                .toList();
    }

    public TipoHabitacionDto crearTipo(TipoHabitacionRequest req) {
        if (tipos.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw new ReglaDeNegocioException("Ya existe un tipo de habitación con ese nombre");
        }
        TipoHabitacion t = tipos.save(new TipoHabitacion(req.nombre().trim(), req.descripcion(), req.capacidad(),
                req.precioBase(), req.servicios(), req.imagenUrl()));
        auditoria.registrar("TIPO_CREADO", "TipoHabitacion", t.getId(), t.getNombre());
        return mapeador.tipo(t, 0);
    }

    public TipoHabitacionDto actualizarTipo(Long id, TipoHabitacionRequest req) {
        TipoHabitacion t = tipo(id);
        t.actualizar(req.nombre().trim(), req.descripcion(), req.capacidad(), req.precioBase(), req.servicios(),
                req.imagenUrl());
        auditoria.registrar("TIPO_MODIFICADO", "TipoHabitacion", t.getId(), t.getNombre());
        return mapeador.tipo(t, habitaciones.countByTipoIdAndActivaTrue(id));
    }

    /** Baja lógica: deja de ofrecerse, pero las reservas existentes se respetan. */
    public TipoHabitacionDto cambiarActivoTipo(Long id, boolean activo) {
        TipoHabitacion t = tipo(id);
        if (activo) {
            t.activar();
        } else {
            t.desactivar();
        }
        auditoria.registrar(activo ? "TIPO_ACTIVADO" : "TIPO_DESACTIVADO", "TipoHabitacion", id, t.getNombre());
        return mapeador.tipo(t, habitaciones.countByTipoIdAndActivaTrue(id));
    }

    @Transactional(readOnly = true)
    public List<HabitacionDto> habitaciones() {
        return habitaciones.findAllByOrderByNumeroAsc().stream().map(mapeador::habitacion).toList();
    }

    public HabitacionDto crearHabitacion(HabitacionRequest req) {
        if (habitaciones.existsByNumero(req.numero().trim())) {
            throw new ReglaDeNegocioException("Ya existe la habitación " + req.numero());
        }
        Habitacion h = habitaciones.save(new Habitacion(req.numero().trim(), req.piso(), tipo(req.tipoId())));
        auditoria.registrar("HABITACION_CREADA", "Habitacion", h.getNumero(), h.getTipo().getNombre());
        return mapeador.habitacion(h);
    }

    public HabitacionDto actualizarHabitacion(Long id, HabitacionRequest req) {
        Habitacion h = habitacion(id);
        TipoHabitacion nuevoTipo = tipo(req.tipoId());
        if (!h.getTipo().getId().equals(nuevoTipo.getId()) && tieneReservasFuturas(h)) {
            throw new ReglaDeNegocioException("La habitación tiene reservas activas: no se le puede cambiar el tipo");
        }
        if (!h.getNumero().equals(req.numero().trim()) && habitaciones.existsByNumero(req.numero().trim())) {
            throw new ReglaDeNegocioException("Ya existe la habitación " + req.numero());
        }
        h.actualizar(req.numero().trim(), req.piso(), nuevoTipo);
        auditoria.registrar("HABITACION_MODIFICADA", "Habitacion", h.getNumero(), nuevoTipo.getNombre());
        return mapeador.habitacion(h);
    }

    public HabitacionDto cambiarActivaHabitacion(Long id, boolean activa) {
        Habitacion h = habitacion(id);
        if (!activa && tieneReservasFuturas(h)) {
            throw new ReglaDeNegocioException("La habitación " + h.getNumero()
                    + " tiene reservas activas. Reasignalas o cancelalas antes de darla de baja.");
        }
        if (activa) {
            h.activar();
        } else {
            h.desactivar();
        }
        auditoria.registrar(activa ? "HABITACION_ACTIVADA" : "HABITACION_DESACTIVADA", "Habitacion", h.getNumero(), null);
        return mapeador.habitacion(h);
    }

    private boolean tieneReservasFuturas(Habitacion h) {
        LocalDate hoy = LocalDate.now(reloj);
        return !reservas.superpuestas(List.of(h.getId()), hoy, hoy.plusYears(5), ESTADOS_ACTIVOS).isEmpty();
    }

    // ------------------------------------------------------------------
    // Temporadas y tarifas (RF-23)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TemporadaDto> temporadas() {
        return temporadas.findAllByOrderByFechaDesdeAsc().stream().map(mapeador::temporada).toList();
    }

    public TemporadaDto crearTemporada(TemporadaRequest req) {
        validarSinSuperposicion(null, req.fechaDesde(), req.fechaHasta());
        Temporada t = temporadas.save(new Temporada(req.nombre().trim(), req.categoria(), req.fechaDesde(), req.fechaHasta()));
        auditoria.registrar("TEMPORADA_CREADA", "Temporada", t.getId(),
                t.getNombre() + " " + t.getCategoria() + " " + t.getFechaDesde() + "→" + t.getFechaHasta());
        return mapeador.temporada(t);
    }

    public TemporadaDto actualizarTemporada(Long id, TemporadaRequest req) {
        Temporada t = temporadas.findById(id).orElseThrow(() -> new NoEncontradoException("No existe esa temporada"));
        validarSinSuperposicion(id, req.fechaDesde(), req.fechaHasta());
        t.actualizar(req.nombre().trim(), req.categoria(), req.fechaDesde(), req.fechaHasta());
        auditoria.registrar("TEMPORADA_MODIFICADA", "Temporada", id, t.getNombre());
        return mapeador.temporada(t);
    }

    public void eliminarTemporada(Long id) {
        Temporada t = temporadas.findById(id).orElseThrow(() -> new NoEncontradoException("No existe esa temporada"));
        temporadas.delete(t);
        auditoria.registrar("TEMPORADA_ELIMINADA", "Temporada", id, t.getNombre());
    }

    private void validarSinSuperposicion(Long idPropio, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("La temporada debe terminar después de empezar");
        }
        temporadas.findAll().stream()
                .filter(t -> !t.getId().equals(idPropio) && t.seSuperponeCon(desde, hasta))
                .findFirst()
                .ifPresent(t -> {
                    throw new ReglaDeNegocioException("Se superpone con la temporada \"" + t.getNombre() + "\" ("
                            + t.getFechaDesde() + " al " + t.getFechaHasta() + ")");
                });
    }

    @Transactional(readOnly = true)
    public List<TarifasTipoDto> tarifas() {
        return tipos.findAllByOrderByPrecioBaseAsc().stream().map(this::tarifasDe).toList();
    }

    public TarifasTipoDto actualizarTarifas(Long tipoId, TarifasTipoRequest req) {
        TipoHabitacion tipo = tipo(tipoId);
        for (TarifaItem item : req.tarifas()) {
            tarifas.findByTipoIdAndCategoria(tipoId, item.categoria())
                    .ifPresentOrElse(t -> t.cambiarPrecio(item.precioNoche()),
                            () -> tarifas.save(new Tarifa(tipo, item.categoria(), item.precioNoche())));
        }
        em.flush();
        auditoria.registrar("TARIFAS_MODIFICADAS", "TipoHabitacion", tipoId, tipo.getNombre());
        return tarifasDe(tipo);
    }

    private TarifasTipoDto tarifasDe(TipoHabitacion tipo) {
        List<Tarifa> delTipo = tarifas.findByTipoId(tipo.getId());
        return new TarifasTipoDto(tipo.getId(), tipo.getNombre(), tipo.getPrecioBase(),
                precio(delTipo, CategoriaTemporada.ALTA), precio(delTipo, CategoriaTemporada.MEDIA),
                precio(delTipo, CategoriaTemporada.BAJA));
    }

    private BigDecimal precio(List<Tarifa> lista, CategoriaTemporada categoria) {
        return lista.stream().filter(t -> t.getCategoria() == categoria).map(Tarifa::getPrecioNoche)
                .findFirst().orElse(null);
    }

    // ------------------------------------------------------------------
    // Configuración: políticas de cancelación, seña y precio (RF-24)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ConfiguracionDto configuracion() {
        ConfiguracionHotel c = configuracionHotel();
        List<OpcionDto> politicas = Arrays.stream(TipoPoliticaCancelacion.values())
                .map(p -> new OpcionDto(p.name(), p.crear().nombre(), p.crear().descripcion())).toList();
        List<OpcionDto> estrategias = Arrays.stream(TipoPoliticaPrecio.values())
                .map(p -> new OpcionDto(p.name(), p == TipoPoliticaPrecio.POR_TEMPORADA ? "Por temporada" : "Por temporada con descuento",
                        p.crear().descripcion())).toList();
        return new ConfiguracionDto(c.getNombreHotel(), c.getPoliticaCancelacion(), c.getPorcentajeSena(),
                c.getMinutosRetencion(), c.getHoraCheckIn(), c.getHoraCheckOut(), c.getEstrategiaPrecio(),
                politicas, estrategias);
    }

    public ConfiguracionDto actualizarConfiguracion(ConfiguracionRequest req) {
        ConfiguracionHotel c = configuracionHotel();
        c.actualizar(req.nombreHotel().trim(), req.politicaCancelacion(), req.porcentajeSena(), req.minutosRetencion(),
                req.horaCheckIn(), req.horaCheckOut(), req.estrategiaPrecio());
        auditoria.registrar("CONFIGURACION", "Hotel", 1, "Cancelación " + req.politicaCancelacion() + ", seña "
                + req.porcentajeSena() + "%, precio " + req.estrategiaPrecio());
        return configuracion();
    }

    private ConfiguracionHotel configuracionHotel() {
        return configuraciones.findById(ConfiguracionHotel.ID_UNICO)
                .orElseThrow(() -> new IllegalStateException("Falta la configuración del hotel"));
    }

    // ------------------------------------------------------------------
    // Bloqueos por mantenimiento (RF-25)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BloqueoDto> bloqueos() {
        return bloqueos.vigentesDesde(LocalDate.now(reloj).minusDays(30)).stream().map(mapeador::bloqueo).toList();
    }

    public BloqueoDto crearBloqueo(BloqueoRequest req) {
        Habitacion h = habitacion(req.habitacionId());
        Estadia rango = new Estadia(req.desde(), req.hasta());
        List<Reserva> afectadas = reservas.superpuestas(List.of(h.getId()), rango.checkIn(), rango.checkOut(),
                ESTADOS_ACTIVOS).stream().filter(r -> r.bloqueaHabitacion(reloj.instant())).toList();
        if (!afectadas.isEmpty()) {
            throw new ReglaDeNegocioException("La habitación " + h.getNumero() + " tiene reservas en ese período ("
                    + String.join(", ", afectadas.stream().map(Reserva::getCodigo).toList())
                    + "). Reasignalas antes de bloquearla.");
        }
        Bloqueo b = bloqueos.save(new Bloqueo(h, rango, req.motivo().trim()));
        auditoria.registrar("BLOQUEO_CREADO", "Habitacion", h.getNumero(),
                rango.checkIn() + "→" + rango.checkOut() + ": " + req.motivo());
        return mapeador.bloqueo(b);
    }

    public void eliminarBloqueo(Long id) {
        Bloqueo b = bloqueos.findById(id).orElseThrow(() -> new NoEncontradoException("No existe ese bloqueo"));
        bloqueos.delete(b);
        auditoria.registrar("BLOQUEO_ELIMINADO", "Habitacion", b.getHabitacion().getNumero(), b.getMotivo());
    }

    // ------------------------------------------------------------------
    // Usuarios y roles (RF-26)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UsuarioDto> usuarios() {
        return usuarios.findAllOrdenados().stream().map(mapeador::usuario).toList();
    }

    public UsuarioDto crearUsuario(NuevoUsuarioRequest req) {
        String email = Usuario.normalizarEmail(req.email());
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ReglaDeNegocioException("Ya existe un usuario con ese email");
        }
        Usuario u = switch (req.rol()) {
            case ADMINISTRADOR -> new Administrador(req.nombre().trim(), req.apellido().trim(), email);
            case RECEPCIONISTA -> new Recepcionista(req.nombre().trim(), req.apellido().trim(), email);
            case HUESPED -> new Huesped(req.nombre().trim(), req.apellido().trim(), email, null, null);
        };
        u.cambiarPassword(passwordEncoder.encode(req.password()));
        usuarios.save(u);
        auditoria.registrar("USUARIO_CREADO", "Usuario", email, req.rol().name());
        return mapeador.usuario(u);
    }

    public UsuarioDto cambiarRol(Long id, Rol rol) {
        Usuario u = usuario(id);
        if (u.getId().equals(usuarioActual.obtener().getId())) {
            throw new ReglaDeNegocioException("No podés cambiar tu propio rol");
        }
        if (u.getRol() == Rol.HUESPED && rol != Rol.HUESPED && !reservas.findByHuespedIdOrderByCheckInDesc(id).isEmpty()) {
            throw new ReglaDeNegocioException("Este huésped tiene reservas: no se lo puede pasar a personal del hotel");
        }
        usuarios.cambiarRol(id, rol.name());
        em.flush();
        em.clear();
        auditoria.registrar("ROL_MODIFICADO", "Usuario", u.getEmail(), u.getRol() + " → " + rol);
        return mapeador.usuario(usuario(id));
    }

    public UsuarioDto cambiarActivoUsuario(Long id, boolean activo) {
        Usuario u = usuario(id);
        if (!activo && u.getId().equals(usuarioActual.obtener().getId())) {
            throw new ReglaDeNegocioException("No podés deshabilitar tu propia cuenta");
        }
        if (activo) {
            u.activar();
        } else {
            u.desactivar();
        }
        auditoria.registrar(activo ? "USUARIO_HABILITADO" : "USUARIO_DESHABILITADO", "Usuario", u.getEmail(), null);
        return mapeador.usuario(u);
    }

    // ------------------------------------------------------------------
    // Auditoría (RNF-15)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AuditoriaDto> auditoria(int limite) {
        return registrosAuditoria.findAllByOrderByFechaDesc(PageRequest.of(0, Math.min(limite, 500))).stream()
                .map(mapeador::auditoria).toList();
    }

    // ------------------------------------------------------------------

    private TipoHabitacion tipo(Long id) {
        return tipos.findById(id).orElseThrow(() -> new NoEncontradoException("No existe ese tipo de habitación"));
    }

    private Habitacion habitacion(Long id) {
        return habitaciones.findById(id).orElseThrow(() -> new NoEncontradoException("No existe esa habitación"));
    }

    private Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new NoEncontradoException("No existe ese usuario"));
    }
}
