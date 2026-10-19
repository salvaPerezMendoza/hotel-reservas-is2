package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.config.AppProperties;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.usuario.Huesped;
import ar.fiuba.is2.hotel.dominio.usuario.TokenRecuperacion;
import ar.fiuba.is2.hotel.dominio.usuario.Usuario;
import ar.fiuba.is2.hotel.repositorio.TokenRecuperacionRepository;
import ar.fiuba.is2.hotel.repositorio.UsuarioRepository;
import ar.fiuba.is2.hotel.seguridad.JwtService;
import ar.fiuba.is2.hotel.seguridad.UsuarioActual;
import ar.fiuba.is2.hotel.servicio.notificacion.PlantillasDeCorreo;
import ar.fiuba.is2.hotel.servicio.notificacion.ServicioDeCorreo;
import ar.fiuba.is2.hotel.web.dto.AuthDtos.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

/** Registro, inicio de sesión y recuperación de contraseña (RF-05). */
@Service
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Email o contraseña incorrectos";

    private final UsuarioRepository usuarios;
    private final TokenRecuperacionRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioActual usuarioActual;
    private final ServicioDeCorreo correo;
    private final PlantillasDeCorreo plantillas;
    private final TarifarioService tarifario;
    private final AuditoriaService auditoria;
    private final Mapeador mapeador;
    private final AppProperties props;
    private final Clock reloj;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UsuarioRepository usuarios, TokenRecuperacionRepository tokens, PasswordEncoder passwordEncoder,
                       JwtService jwtService, UsuarioActual usuarioActual, ServicioDeCorreo correo,
                       PlantillasDeCorreo plantillas, TarifarioService tarifario, AuditoriaService auditoria,
                       Mapeador mapeador, AppProperties props, Clock reloj) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.usuarioActual = usuarioActual;
        this.correo = correo;
        this.plantillas = plantillas;
        this.tarifario = tarifario;
        this.auditoria = auditoria;
        this.mapeador = mapeador;
        this.props = props;
        this.reloj = reloj;
    }

    @Transactional
    public SesionDto registrar(RegistroRequest req) {
        String email = Usuario.normalizarEmail(req.email());
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
        if (usuario != null) {
            // Un huésped que reservó por teléfono ya existe pero sin contraseña: completa su cuenta.
            if (usuario instanceof Huesped && !usuario.tieneCredenciales()) {
                usuario.actualizarDatos(req.nombre().trim(), req.apellido().trim(), req.telefono(), req.documento());
                usuario.cambiarPassword(passwordEncoder.encode(req.password()));
                auditoria.registrar("CUENTA_ACTIVADA", "Usuario", email, "Completó el registro");
                return new SesionDto(jwtService.emitir(usuario), mapeador.usuario(usuario));
            }
            throw new ReglaDeNegocioException("Ya existe una cuenta con ese email. Iniciá sesión o recuperá tu contraseña.");
        }
        Huesped nuevo = new Huesped(req.nombre().trim(), req.apellido().trim(), email, req.telefono(), req.documento());
        nuevo.cambiarPassword(passwordEncoder.encode(req.password()));
        usuarios.save(nuevo);
        auditoria.registrar("REGISTRO", "Usuario", email, "Alta de huésped");
        return new SesionDto(jwtService.emitir(nuevo), mapeador.usuario(nuevo));
    }

    @Transactional(readOnly = true)
    public SesionDto iniciarSesion(LoginRequest req) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(Usuario.normalizarEmail(req.email()))
                .orElseThrow(() -> new ReglaDeNegocioException(CREDENCIALES_INVALIDAS));
        if (!usuario.tieneCredenciales() || !passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            throw new ReglaDeNegocioException(CREDENCIALES_INVALIDAS);
        }
        if (!usuario.isActivo()) {
            throw new ReglaDeNegocioException("La cuenta está deshabilitada. Contactá al hotel.");
        }
        return new SesionDto(jwtService.emitir(usuario), mapeador.usuario(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDto yo() {
        return mapeador.usuario(usuarioActual.obtener());
    }

    @Transactional
    public UsuarioDto actualizarPerfil(PerfilRequest req) {
        Usuario usuario = usuarioActual.obtener();
        usuario.actualizarDatos(req.nombre().trim(), req.apellido().trim(), req.telefono(), req.documento());
        return mapeador.usuario(usuario);
    }

    /** Siempre responde igual, exista o no el email, para no revelar qué cuentas existen. */
    @Transactional
    public void solicitarRecuperacion(RecuperarRequest req) {
        usuarios.findByEmailIgnoreCase(Usuario.normalizarEmail(req.email()))
                .filter(Usuario::isActivo)
                .ifPresent(usuario -> {
                    byte[] bytes = new byte[24];
                    random.nextBytes(bytes);
                    String token = HexFormat.of().formatHex(bytes);
                    tokens.save(new TokenRecuperacion(usuario, token, reloj.instant().plus(Duration.ofHours(1))));
                    String url = props.urlFrontend() + "/restablecer?token=" + token;
                    correo.enviar(usuario.getEmail(), "Restablecé tu contraseña",
                            plantillas.recuperacion(tarifario.configuracion(), usuario.getNombre(), url));
                });
    }

    @Transactional
    public void restablecer(RestablecerRequest req) {
        TokenRecuperacion token = tokens.findByToken(req.token())
                .filter(t -> t.esValido(reloj.instant()))
                .orElseThrow(() -> new ReglaDeNegocioException("El enlace no es válido o ya venció. Pedí uno nuevo."));
        token.getUsuario().cambiarPassword(passwordEncoder.encode(req.password()));
        token.marcarUsado();
        auditoria.registrar("PASSWORD_RESTABLECIDA", "Usuario", token.getUsuario().getEmail(), null);
    }
}
